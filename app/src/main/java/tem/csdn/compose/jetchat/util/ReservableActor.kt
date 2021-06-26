package tem.csdn.compose.jetchat.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.SelectClause2
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
//region 待定 可保留可丢弃的消费者队列设计
/**
 * 消费者队列的接受处理器作用域
 */
data class ReceiveHandlerScope<E> internal constructor(
    var hold: Boolean = false,
    var cause: Throwable? = null,
    var throwToErrorHandler: Boolean = true,
    val next: suspend () -> E,
    val scope: ActorScope<E>,
    val channel: ReservableActor<E>
)

/**
 * 消费者队列的错误处理器作用域
 */
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

/**
 * 消费者队列
 */
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

    /**
     * 获取发送监听器的作业协程
     */
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

    /**
     * 发送的管道
     * 这个管道会在协程中启动一个消费者对垒
     * 然后这个消费者队列会一直尝试进行读取
     * 若有新的消息来临,会调用接受处理器来执行接受事件
     * 若接受出现异常,则将异常丢到异常处理器中去执行
     * 相关的参数在相关的作用域中定义
     */
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

    /**
     * 关闭消费者队列
     */
    override fun close(cause: Throwable?): Boolean {
        listenJob?.cancel()
        return sendChannel.close(cause)
    }

    /**
     * 发送内容到此消费者队列
     */
    override suspend fun send(element: T) {
        sendHandlerWaiter.listen {
            sendChannel.send(element)
        }
    }

    /**
     * 暂停接受处理器
     */
    suspend fun pauseReceiveHandler() = receiveHandlerWaiter.pause()

    /**
     * 暂停发送处理器
     */
    suspend fun pauseSendHandler() = sendHandlerWaiter.pause()

    /**
     * 恢复接受处理器
     */
    fun resumeReceiveHandler() = receiveHandlerWaiter.resume()

    /**
     * 恢复发送处理器
     */
    fun resumeSendHandler() = sendHandlerWaiter.resume()

    /**
     * 开启监听
     */
    fun startListen() = listenJob?.start() ?: false

    /**
     * 开启所有(包括监听器,接受处理器和发送处理器)
     */
    fun startAll() {
        startListen()
        resumeReceiveHandler()
        resumeSendHandler()
    }

    override val isClosedForSend: Boolean
        get() = sendChannel.isClosedForSend
    override val onSend: SelectClause2<T, SendChannel<T>>
        get() = sendChannel.onSend

    /**
     * 关闭时事件
     */
    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
        sendChannel.invokeOnClose(handler)
    }

    /**
     * 发送处理器暂停事件
     */
    fun invokeOnSendPause(handler: ((cause: Throwable?) -> Unit)?) =
        sendHandlerWaiter.invokeOnPause(handler)

    /**
     * 接受处理器暂停事件
     */
    fun invokeOnReceivePause(handler: ((cause: Throwable?) -> Unit)?) =
        receiveHandlerWaiter.invokeOnPause(handler)

    /**
     * 发送处理器恢复事件
     */
    fun invokeOnSendResume(handler: (() -> Unit)?) =
        sendHandlerWaiter.invokeOnResume(handler)

    /**
     * 接受处理器恢复事件
     */
    fun invokeOnReceiveResume(handler: (() -> Unit)?) =
        receiveHandlerWaiter.invokeOnResume(handler)

    /**
     * 接受事件
     */
    fun invokeOnReceive(handler: suspend ReceiveHandlerScope<T>.(T) -> Unit = {  }) {
        onReceiveHandler = handler
    }

    /**
     * 接受出错事件
     */
    fun invokeOnReceiveError(handler: (suspend ReceiveErrorHandlerScope<T>.(T) -> Unit)? = null) {
        onReceiveErrorHandler = handler
    }

    /**
     * 发送监听器
     */
    fun sendListener(
        sendListenerHandler: (suspend () -> T)? = null,
        start: CoroutineStart = CoroutineStart.DEFAULT
    ) {
        this.sendListenerHandler = sendListenerHandler
        listenJob = genJob(start)
    }

    /**
     * 尝试发送到管道消息
     */
    override fun trySend(element: T): ChannelResult<Unit> {
        return sendChannel.trySend(element)
    }

}
//endregion