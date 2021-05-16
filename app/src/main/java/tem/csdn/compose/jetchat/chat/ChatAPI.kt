package tem.csdn.compose.jetchat.chat

class ChatAPI(private val baseUrl: String) {
    enum class ImageType(val method: String) {
        PHOTO("photo"),
        IMAGE("image"),
        CHAT("chat"),
    }

    fun image(imageType: ImageType, id: String): String {
        return "${baseUrl}/${imageType.method}/${id}"
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