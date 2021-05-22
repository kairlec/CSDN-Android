package tem.csdn.compose.jetchat.util

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import tem.csdn.compose.jetchat.data.*


val client = HttpClient(CIO) {
    install(WebSockets) {
        pingInterval = 30_000
    }
    install(JsonFeature)
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
}

suspend fun connectWebSocketToServer(
    host: String = "localhost",
    port: Int? = DEFAULT_PORT,
    path: String = "/",
    method: HttpMethod = HttpMethod.Get,
    objectMapper: ObjectMapper,
    inputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>,
    outputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>,
    onConnected: suspend () -> Unit,
    onDisconnected: suspend () -> Unit,
) {
    client.webSocket(method = method, host = host, port = port ?: DEFAULT_PORT, path = path) {
        onConnected()
        val messageOutputRoutine = launch { outputMessages(outputMessageChannel) }
        val userInputRoutine = launch { inputMessages(objectMapper, inputMessageChannel) }

        userInputRoutine.join()
        messageOutputRoutine.cancelAndJoin()
    }
    onDisconnected()
    client.close()
}

suspend fun DefaultClientWebSocketSession.outputMessages(outputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>) {
    try {
        for (message in incoming) {
            when (message) {
                is Frame.Text -> {
                    outputMessageChannel.send(RawWebSocketFrameWrapper.ofText(message.readText()))
                }
                is Frame.Binary -> {
                    outputMessageChannel.send(RawWebSocketFrameWrapper.ofBinary(message.readBytes()))
                }
                else -> continue
            }

        }
    } catch (e: Throwable) {
        Log.e("CSDN_WEBSOCKET_RECEIVE", "Error while receiving: ${e.localizedMessage}", e)
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages(
    objectMapper: ObjectMapper,
    inputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>
) {
    for (rawWebSocketFrameWrapper in inputMessageChannel) {
        rawWebSocketFrameWrapper.ifRawText {
            if (!sendRetry(Frame.Text(objectMapper.writeValueAsString(TextWebSocketFrameWrapper.ofMessage(it))))) {
                Log.e("CSDN_WEBSOCKET_SEND", "send error")
            }
        }
        rawWebSocketFrameWrapper.ifBinary(objectMapper) {
            if (!sendRetry(Frame.Binary(false, it))) {
                Log.e("CSDN_WEBSOCKET_SEND", "send error")
            }
        }
    }
}

suspend fun DefaultClientWebSocketSession.sendRetry(frame: Frame, retryCnt: Int = 3): Boolean {
    if (retryCnt == 0) {
        return false
    }
    return try {
        send(frame)
        true
    } catch (e: ClosedSendChannelException) {
        false
    } catch (e: Throwable) {
        Log.e(
            "CSDN_WEBSOCKET_SEND_RETRY",
            "send error when retry(${retryCnt}): ${e.localizedMessage}",
            e
        )
        sendRetry(frame, retryCnt - 1)
    }
}