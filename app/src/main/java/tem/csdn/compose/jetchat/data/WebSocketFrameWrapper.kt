package tem.csdn.compose.jetchat.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue

data class RawWebSocketFrameWrapper<T : Any>(
    val type: RawFrameType,
    val content: T
) {
    enum class RawFrameType {
        TEXT,
        BINARY
    }

    companion object {
        fun ofBinary(content: ByteArray): RawWebSocketFrameWrapper<ByteArray> {
            return RawWebSocketFrameWrapper(RawFrameType.BINARY, content)
        }

        fun ofText(content: String): RawWebSocketFrameWrapper<String> {
            return RawWebSocketFrameWrapper(RawFrameType.TEXT, content)
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

suspend fun RawWebSocketFrameWrapper<*>.ifText(
    objectMapper: ObjectMapper,
    event: suspend (TextWebSocketFrameWrapper) -> Unit
) {
    if (type == RawWebSocketFrameWrapper.RawFrameType.TEXT) {
        event(objectMapper.readValue(textContent))
    }
}

suspend fun RawWebSocketFrameWrapper<*>.ifBinary(
    objectMapper: ObjectMapper,
    event: suspend (ByteArray) -> Unit
) {
    if (type == RawWebSocketFrameWrapper.RawFrameType.BINARY) {
        event(objectMapper.convertValue(binaryContent))
    }
}

data class TextWebSocketFrameWrapper(
    val type: FrameType,
    val content: Any?
) {
    enum class FrameType {
        MESSAGE,
        NEW_CONNECTION,
        NEW_DISCONNECTION,
        HEARTBEAT
    }

    companion object {
        fun ofMessage(content: String): TextWebSocketFrameWrapper {
            return TextWebSocketFrameWrapper(FrameType.MESSAGE, content)
        }
    }
}