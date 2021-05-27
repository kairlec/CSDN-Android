package tem.csdn.compose.jetchat.data

import android.util.Log
import androidx.compose.runtime.Composable
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.dao.MessageDao
import tem.csdn.compose.jetchat.dao.UserDao
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.ReservableSendChannel
import tem.csdn.compose.jetchat.util.connectWebSocketToServer
import tem.csdn.compose.jetchat.util.trySend
import java.lang.Exception

class ChatServerInitException(
    val code: Int = -1,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

class ChatServer(
    val chatAPI: ChatAPI,
    val id: String,
    private val client: HttpClient,
    private val lastMessageId: Long?,
    val messageDao: MessageDao,
    val userDao: UserDao,
    val coroutineScope: CoroutineScope,
    val onWebSocketEvent: suspend (Boolean) -> Unit
) {
    companion object {
        lateinit var current: ChatServer
            private set
            @get:Composable
            get
    }

    val objectMapper = jacksonObjectMapper()

    // 这个锁用来保持WebSocket不会被关闭,防止提前关闭输出导致需要重新连接
    private val webSocketKeepAliveMutex = Mutex()
    private val inputReservableSendChannel = ReservableSendChannel<RawWebSocketFrameWrapper<*>>(
        coroutineScope = coroutineScope,
        context = Dispatchers.IO,
        capacity = Channel.UNLIMITED,
        start = CoroutineStart.LAZY
    )
    val outputChannel = Channel<RawWebSocketFrameWrapper<*>>(Channel.UNLIMITED)

    suspend fun getMeProfile(): User {
        return client.post<Result<User>>(chatAPI.init(id)).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for me profile:${this}")
        }
    }

    suspend fun getChatDisplayName(): String {
        return client.get<Result<String>>(chatAPI.chatName()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for chat display name:${this}")
        }
    }

    suspend fun getProfiles(): List<User> {
        return client.get<Result<List<User>>>(chatAPI.profiles()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for profiles:${this}")
        }
    }

    suspend fun getOnlineNumber(): Int {
        return client.get<Result<Int>>(chatAPI.count()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for count:${this}")
        }
    }

    suspend fun getMessages(): List<Message> {
        return client.get<Result<List<Message>>>(chatAPI.messages()) {
            if (lastMessageId != null) {
                parameter("after_id", lastMessageId)
            }
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for messages:${this}")
        }
    }

    suspend fun connect(
        onConnect: suspend DefaultClientWebSocketSession.() -> Unit,
        onDisconnect: suspend () -> Unit
    ) {
        if (webSocketKeepAliveMutex.isLocked) {
            webSocketKeepAliveMutex.unlock()
        }
        connectWebSocketToServer(
            ssl = chatAPI.ssl,
            host = chatAPI.host,
            port = chatAPI.port,
            path = chatAPI.webSocket(id),
            objectMapper = objectMapper,
            webSocketKeepAliveMutex = webSocketKeepAliveMutex,
            outputMessageChannel = outputChannel,
            onConnected = { wsSession ->
                Log.d(
                    "CSDN_DEBUG",
                    "locked to keep alive, redirect to send handler and send error handler"
                )
                if (!webSocketKeepAliveMutex.isLocked) {
                    webSocketKeepAliveMutex.lock()
                }
                inputReservableSendChannel.invokeOnReceive {
                    wsSession.trySend(objectMapper, it) { throwable ->
                        throw throwable
                    }
                }
                inputReservableSendChannel.invokeOnReceiveError {
                    if (webSocketKeepAliveMutex.isLocked) {
                        webSocketKeepAliveMutex.unlock()
                    }
                    wsSession.close()
                }
                Log.d("CSDN_DEBUG", "start input listen")
                inputReservableSendChannel.startAll()
                onWebSocketEvent(true)
                onConnect(wsSession)
            },
            onDisconnected = {
                Log.d("CSDN_DEBUG", "disconnect,pause receive Handelr")
                inputReservableSendChannel.pauseReceiveHandler()
                onWebSocketEvent(false)
                onDisconnect()
            }
        )
        Log.d("CSDN_DEBUG", "init for websocket")
    }

    suspend fun send(rawWebSocketFrameWrapper: RawWebSocketFrameWrapper<*>) {
        inputReservableSendChannel.send(rawWebSocketFrameWrapper)
    }


    init {
        current = this
    }
}

class Result<T>(
    val code: Int = 0,
    val msg: String? = null,
    val data: T? = null
) {
    override fun toString(): String {
        return "Result(code=$code, msg=$msg, data=$data)"
    }

    fun checked() = apply {
        if (code != 0) {
            throw ChatServerInitException(code, msg)
        }
    }
}
