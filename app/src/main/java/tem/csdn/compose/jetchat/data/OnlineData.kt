package tem.csdn.compose.jetchat.data

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import java.lang.Exception
import java.util.*

object OnlineData {
    fun getProfile(userId: String): User {
        return if (userId == meProfile.userId) {
            meProfile
        } else {
            colleagueProfile
        }
    }

    fun getProfileOrNull(userId: String): User? {
        return if (userId == meProfile.userId) {
            meProfile
        } else {
            colleagueProfile
        }
    }

}

class ChatServerInitException(
    val code: Int = -1,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

class ChatServer(chatBaseUrl: String, id: String, client: HttpClient, lastMessageId: Long?) {
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

    private val chatAPI = ChatAPI(chatBaseUrl)
    val chatDisplayName: String
    val meProfile: User
    val profiles: List<User>
    val count: Int
    val messages: List<Message>

    init {
        meProfile = runBlocking {
            client.post<Result<User>>(chatAPI.init(id)).checked().data!!
        }
        chatDisplayName = runBlocking {
            client.get<Result<String>>(chatAPI.chatName()).checked().data!!
        }
        profiles = runBlocking {
            client.get<Result<List<User>>>(chatAPI.profiles()).checked().data!!
        }
        count = runBlocking {
            client.get<Result<Int>>(chatAPI.count()).checked().data!!
        }
        messages =
            runBlocking {
                client.get<Result<List<Message>>>(chatAPI.messages()) {
                    if (lastMessageId != null) {
                        this.parameter("after_id", lastMessageId)
                    }
                }.checked().data!!
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
