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

    fun image(sha256: String): String {
        return "${baseUrl}/image/${sha256}"
    }

    fun upc(sha256: String): String {
        return "${baseUrl}/upc/${sha256}"
    }

    fun profilePhoto(sha256: String?): String {
        return if (sha256 == null) {
            "${baseUrl}/photo"
        } else {
            "${baseUrl}/photo/${sha256}"
        }
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