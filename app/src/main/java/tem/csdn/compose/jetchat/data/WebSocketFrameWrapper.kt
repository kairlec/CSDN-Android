package tem.csdn.compose.jetchat.data

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