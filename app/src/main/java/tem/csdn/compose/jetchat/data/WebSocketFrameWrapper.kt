package tem.csdn.compose.jetchat.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

data class RawWebSocketFrameWrapper<T : Any> private constructor(
    val type: RawFrameType,
    val content: T
) {
    enum class RawFrameType {
        TEXT,
        BINARY,
        IMAGE_TEXT,
        TEXT_WRAPPER,
    }

    companion object {
        fun ofBinary(content: ByteArray): RawWebSocketFrameWrapper<ByteArray> {
            return RawWebSocketFrameWrapper(RawFrameType.BINARY, content)
        }

        fun ofImageText(content: String): RawWebSocketFrameWrapper<String> {
            return RawWebSocketFrameWrapper(RawFrameType.IMAGE_TEXT, content)
        }

        fun ofText(content: String): RawWebSocketFrameWrapper<String> {
            return RawWebSocketFrameWrapper(RawFrameType.TEXT, content)
        }

        fun ofTextWrapper(content: TextWebSocketFrameWrapper): RawWebSocketFrameWrapper<TextWebSocketFrameWrapper> {
            return RawWebSocketFrameWrapper(RawFrameType.TEXT_WRAPPER, content)
        }
    }
}

private val RawWebSocketFrameWrapper<*>.binaryContent: ByteArray
    get() = content as ByteArray

private val RawWebSocketFrameWrapper<*>.textContent: String
    get() = content as String

suspend fun RawWebSocketFrameWrapper<*>.ifRawText(
    event: suspend (String) -> Unit
) {
    if (type == RawWebSocketFrameWrapper.RawFrameType.TEXT) {
        event(textContent)
    }
}

suspend fun RawWebSocketFrameWrapper<*>.ifImageText(
    event: suspend (String) -> Unit
) {
    if (type == RawWebSocketFrameWrapper.RawFrameType.IMAGE_TEXT) {
        event(textContent)
    }
}

suspend fun RawWebSocketFrameWrapper<*>.ifTextWrapper(
    objectMapper: ObjectMapper,
    event: suspend (TextWebSocketFrameWrapper) -> Unit
) {
    if (type == RawWebSocketFrameWrapper.RawFrameType.TEXT_WRAPPER) {
        if (content is TextWebSocketFrameWrapper) {
            event(content)
        } else {
            event(objectMapper.readValue(textContent))
        }
    }
}

suspend fun RawWebSocketFrameWrapper<*>.ifBinary(
    objectMapper: ObjectMapper,
    event: suspend (ByteArray) -> Unit
) {
    if (type == RawWebSocketFrameWrapper.RawFrameType.BINARY) {
        event(binaryContent)
    }
}

data class TextWebSocketFrameWrapper private constructor(
    val type: FrameType,
    val content: Any?
) {
    enum class FrameType {
        //用户更新消息(客户端仅接受,服务端仅发送)
        UPDATE_USER,

        // 图片消息体(客户端仅发送,服务端仅接受)
        IMAGE_MESSAGE,

        // 文本消息体
        TEXT_MESSAGE,

        // 新的连接消息
        NEW_CONNECTION,

        // 新的断开消息
        NEW_DISCONNECTION,

        // 心跳包
        HEARTBEAT,

        // 心跳返回包
        HEARTBEAT_ACK,

        // 需要客户端重新同步
        NEED_SYNC,
    }

    companion object {
        fun ofMessage(content: String): TextWebSocketFrameWrapper {
            return TextWebSocketFrameWrapper(FrameType.TEXT_MESSAGE, content)
        }

        fun ofImageMessage(content: String): TextWebSocketFrameWrapper {
            return TextWebSocketFrameWrapper(FrameType.IMAGE_MESSAGE, content)
        }

        fun ofHeartbeat(content: String): TextWebSocketFrameWrapper {
            return TextWebSocketFrameWrapper(FrameType.HEARTBEAT, content)
        }

        fun ofHeartbeatAck(content: String): TextWebSocketFrameWrapper {
            return TextWebSocketFrameWrapper(FrameType.HEARTBEAT_ACK, content)
        }
    }
}