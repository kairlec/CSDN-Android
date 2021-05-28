package tem.csdn.compose.jetchat.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.SelectClause2
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

data class ReceiveHandlerScope<E> internal constructor(
    var hold: Boolean = false,
    var cause: Throwable? = null,
    var throwToErrorHandler: Boolean = true,
    val next: suspend () -> E,
    val scope: ActorScope<E>,
    val channel: ReservableActor<E>
)

data class ReceiveErrorHandlerScope<E> internal constructor(
    var hold: Boolean = true,
    val cause: Throwable,
    val next: suspend () -> E,
    val scope: ActorScope<E>,
    val channel: ReservableActor<E>
)

fun <T> CoroutineScope.reservableActor(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    capacity: Int = 0,
    sendListener: (suspend () -> T)? = null,
    onReceive: suspend ReceiveHandlerScope<T>.(T) -> Unit,
    onReceiveError: suspend ReceiveErrorHandlerScope<T>.(T) -> Unit
) = ReservableActor<T>(this, context, start, capacity).apply {
    this.sendListener(sendListener)
    this.invokeOnReceive(onReceive)
    this.invokeOnReceiveError(onReceiveError)
}

class ReservableActor<T>(
    private val coroutineScope: CoroutineScope,
    private val context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    capacity: Int = 0,
) : SendChannel<T> {
    private val receiveHandlerWaiter = SuspendWait(locked = start == CoroutineStart.LAZY)
    private val sendHandlerWaiter = SuspendWait(locked = start == CoroutineStart.LAZY)

    private var sendListenerHandler: (suspend () -> T)? = null
    private var onReceiveHandler: (suspend ReceiveHandlerScope<T>.(T) -> Unit) = {  }
    private var onReceiveErrorHandler: (suspend ReceiveErrorHandlerScope<T>.(T) -> Unit)? = null

    private var listenJob = genJob(start)

    private fun genJob(start: CoroutineStart): Job? {
        return if (sendListenerHandler != null) {
            coroutineScope.launch(context, start) {
                while (isActive) {
                    try {
                        val sendValue = sendListenerHandler!!()
                        sendHandlerWaiter.listen {
                            sendChannel.send(sendValue)
                        }
                    } catch (e: Throwable) {
                    }
                }
            }
        } else {
            null
        }
    }

    private val sendChannel = coroutineScope.actor<T>(context, capacity = capacity) {
        var element = receive()
        while (isActive) {
            try {
                Log.d("CSDN_DEBUG_RSC", "receive:${element}")
                receiveHandlerWaiter.listen {
                    val scope = ReceiveHandlerScope(
                        next = ::receive,
                        scope = this,
                        channel = this@ReservableActor
                    )
                    onReceiveHandler(scope, element)
                    if (scope.throwToErrorHandler && scope.cause != null) {
                        throw scope.cause!!
                    }
                    if (!scope.hold) {
                        element = receive()
                        Log.d("CSDN_DEBUG_RSC", "receiveNEXT")
                    }
                }
            } catch (t: Throwable) {
                Log.d("CSDN_DEBUG_RSC", "receiveError(${t.localizedMessage}):${element}")
                try {
                    val scope = ReceiveErrorHandlerScope(
                        next = ::receive,
                        scope = this,
                        channel = this@ReservableActor,
                        cause = t
                    )
                    onReceiveErrorHandler?.invoke(scope, element)
                    if (!scope.hold) {
                        element = receive()
                        Log.d("CSDN_DEBUG_RSC", "receiveNEXT(onError)")
                    }
                } catch (e: Throwable) {
                }
            }
        }
    }

    override fun close(cause: Throwable?): Boolean {
        listenJob?.cancel()
        return sendChannel.close(cause)
    }

    override suspend fun send(element: T) {
        sendHandlerWaiter.listen {
            sendChannel.send(element)
        }
    }

    suspend fun pauseReceiveHandler() = receiveHandlerWaiter.pause()

    suspend fun pauseSendHandler() = sendHandlerWaiter.pause()

    fun resumeReceiveHandler() = receiveHandlerWaiter.resume()

    fun resumeSendHandler() = sendHandlerWaiter.resume()

    fun startListen() = listenJob?.start() ?: false

    fun startAll() {
        startListen()
        resumeReceiveHandler()
        resumeSendHandler()
    }

    override val isClosedForSend: Boolean
        get() = sendChannel.isClosedForSend
    override val onSend: SelectClause2<T, SendChannel<T>>
        get() = sendChannel.onSend

    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
        sendChannel.invokeOnClose(handler)
    }

    fun invokeOnSendPause(handler: ((cause: Throwable?) -> Unit)?) =
        sendHandlerWaiter.invokeOnPause(handler)

    fun invokeOnReceivePause(handler: ((cause: Throwable?) -> Unit)?) =
        receiveHandlerWaiter.invokeOnPause(handler)

    fun invokeOnSendResume(handler: (() -> Unit)?) =
        sendHandlerWaiter.invokeOnResume(handler)

    fun invokeOnReceiveResume(handler: (() -> Unit)?) =
        receiveHandlerWaiter.invokeOnResume(handler)

    fun invokeOnReceive(handler: suspend ReceiveHandlerScope<T>.(T) -> Unit = {  }) {
        onReceiveHandler = handler
    }

    fun invokeOnReceiveError(handler: (suspend ReceiveErrorHandlerScope<T>.(T) -> Unit)? = null) {
        onReceiveErrorHandler = handler
    }

    fun sendListener(
        sendListenerHandler: (suspend () -> T)? = null,
        start: CoroutineStart = CoroutineStart.DEFAULT
    ) {
        this.sendListenerHandler = sendListenerHandler
        listenJob = genJob(start)
    }

    override fun offer(element: T): Boolean {
        return sendChannel.offer(element)
    }

    @ExperimentalCoroutinesApi
    override val isFull: Boolean
        @Suppress("OverridingDeprecatedMember")
        get() {
            @Suppress("DEPRECATION_ERROR")
            return sendChannel.isFull
        }

}
