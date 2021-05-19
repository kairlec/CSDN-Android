package tem.csdn.compose.jetchat.data

import android.util.Log
import androidx.annotation.StringRes
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.dao.MessageDao
import tem.csdn.compose.jetchat.dao.UserDao
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.connectWebSocketToServer
import java.lang.Exception
import kotlin.properties.Delegates

class ChatServerInitException(
    val code: Int = -1,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

data class ChatData(
    val allUser: Channel<Map<String, User>>,
    val allMessage: Channel<List<Message>>,
    val messageDao: MessageDao,
    val userDao: UserDao,
)

class ChatServer(
    val chatAPI: ChatAPI,
    val id: String,
    private val client: HttpClient,
    private val lastMessageId: Long?,
    messageDao: MessageDao,
    userDao: UserDao,
    private val _progressChannel: Channel<Pair<Float, Int?>>
) {
    init {
        currentChatServer = this
        init()
    }

    companion object {
        lateinit var currentChatServer: ChatServer
            private set
    }

    val inputChannel = Channel<String>(Channel.UNLIMITED)
    val outputChannel = Channel<String>(Channel.UNLIMITED)

    suspend fun receiveProgress(): Pair<Float, Int?> {
        return _progressChannel.receive()
    }

    val webSocketStatusChannel = Channel<Boolean>(8)
    val chatData = ChatData(Channel(1), Channel(1), messageDao, userDao)

    suspend fun addProgress(value: Float, @StringRes updateTextId: Int?) {
        if (_progressChannel.isClosedForSend || _progressChannel.isClosedForReceive) {
            return
        }
        _progressChannel.send(value to updateTextId)
    }

    fun closeProgress() {
        if(_progressChannel.isClosedForReceive||_progressChannel.isClosedForSend) {
            _progressChannel.close()
        }
    }

    enum class ChatServerStatus {
        INITIALIZING,
        OK,
        DISCONNECTED
    }

    data class WebSocketFrameWrapper(
        val type: FrameType,
        val content: Any
    ) {
        enum class FrameType {
            MESSAGE,
            NEW_CONNECTION,
            NEW_DISCONNECTION
        }
    }

    lateinit var chatDisplayName: String
        private set
    lateinit var meProfile: User
        private set
    lateinit var profiles: List<User>
        private set
    var count = 0
        private set
    lateinit var messages: List<Message>
        private set

    private fun init() {
        runBlocking {
        meProfile = runBlocking { client.post<Result<User>>(chatAPI.init(id)).checked().data!! }
        Log.d("CSDN_DEBUG", "init for me profile:${meProfile}")
        chatDisplayName =
             client.get<Result<String>>(chatAPI.chatName()).checked().data!! }
        Log.d("CSDN_DEBUG", "init for chat display name:${chatDisplayName}")
        profiles =
            runBlocking { client.get<Result<List<User>>>(chatAPI.profiles()).checked().data!! }
        Log.d("CSDN_DEBUG", "init for profiles:${profiles}")
        count = client.get<Result<Int>>(chatAPI.count()).checked().data!!.apply {
                
            }
        }

        messages = runBlocking {
            client.get<Result<List<Message>>>(chatAPI.messages()) {
                if (lastMessageId != null) {
                    parameter("after_id", lastMessageId)
                }
            }.checked().data!!
        }
        Log.d("CSDN_DEBUG", "init for messages:${messages}")
        MainScope().launch(Dispatchers.IO) {
            connectWebSocketToServer(
                host = chatAPI.host,
                port = chatAPI.port,
                path = chatAPI.webSocket(id),
                inputMessageChannel = inputChannel,
                outputMessageChannel = outputChannel,
                onConnected = { webSocketStatusChannel.send(true) },
                onDisconnected = { webSocketStatusChannel.send(false) }
            )
        }
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
