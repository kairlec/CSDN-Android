package tem.csdn.compose.jetchat.chat

import android.util.Log
import java.io.Serializable

class ChatAPI(val ssl: Boolean, val host: String, val port: Int? = null) : Serializable {
    private val baseUrl =
        "${if (ssl) "https" else "http"}://${host}${if (port == null) "" else ":${port}"}"

    init {
        Log.d("CSDN_DEBUG_API", "api_baseurl=${baseUrl}")
    }

    companion object {
        private const val serialVersionUID = 13546100697343249L
    }

    enum class ImageType(val method: String) {
        PHOTO("photo"),
        IMAGE("image"),
        CHAT("chat"),
    }

    fun image(imageType: ImageType, id: String): String {
        return "${baseUrl}/img/${imageType.method}/${id}"
    }

    fun profilePhoto(): String {
        return "${baseUrl}/photo"
    }

    fun messages(): String {
        return "${baseUrl}/messages"
    }

    fun profiles(): String {
        return "${baseUrl}/profiles"
    }

    fun profile(): String {
        return "${baseUrl}/profile"
    }

    fun chatName(): String {
        return "${baseUrl}/chat_name"
    }

    fun count(): String {
        return "${baseUrl}/count"
    }

    fun init(id: String): String {
        return "${baseUrl}/csdnchat/${id}"
    }

    fun webSocket(id: String): String {
        return "/csdnchat/${id}"
    }
}