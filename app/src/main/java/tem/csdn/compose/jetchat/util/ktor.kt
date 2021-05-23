package tem.csdn.compose.jetchat.util

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.lang.Exception


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
    ssl: Boolean,
    host: String = "localhost",
    port: Int? = DEFAULT_PORT,
    path: String = "/",
    method: HttpMethod = HttpMethod.Get,
    objectMapper: ObjectMapper,
    inputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>,
    outputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>,
    onConnected: suspend (DefaultClientWebSocketSession) -> Unit,
    onDisconnected: suspend () -> Unit,
) {
    try {
        client.webSocket(
            method = method,
            host = host,
            port = port ?: DEFAULT_PORT,
            path = path,
            request = {
                if (ssl) {
                    url.protocol = URLProtocol.WSS
                }
            }) {
            onConnected(this)
            val messageOutputRoutine = launch { outputMessages(objectMapper, outputMessageChannel) }
            val userInputRoutine = launch {
                inputMessages(objectMapper, inputMessageChannel) {
                    this@webSocket.close(CloseReason(CloseReason.Codes.NORMAL, "send error"))
                }
            }

            userInputRoutine.join()
            messageOutputRoutine.cancelAndJoin()
        }
    } finally {
        Log.i("CSDN_DEBUG", "websocket has be disconnected")
        onDisconnected()
    }
}

suspend fun DefaultClientWebSocketSession.outputMessages(
    objectMapper: ObjectMapper,
    outputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>
) {
    try {
        for (message in incoming) {
            Log.d("CSDN_DEBUG", "receive frame:${message}")
            when (message) {
                is Frame.Text -> {
                    val text = message.readText()
                    val rawWrapper = try {
                        RawWebSocketFrameWrapper.ofTextWrapper(objectMapper.readValue(text))
                    } catch (e: Exception) {
                        RawWebSocketFrameWrapper.ofText(text)
                    }
                    outputMessageChannel.send(rawWrapper)
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
    inputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>,
    onSendFailed: suspend () -> Unit,
) {
    for (rawWebSocketFrameWrapper in inputMessageChannel) {
        var needClose = false
        rawWebSocketFrameWrapper.ifRawText {
            if (!sendRetry(
                    Frame.Text(
                        objectMapper.writeValueAsString(
                            TextWebSocketFrameWrapper.ofMessage(
                                it
                            )
                        )
                    )
                )
            ) {
                Log.e("CSDN_WEBSOCKET_SEND", "send error")
                needClose = true
                onSendFailed()
            }
        }
        rawWebSocketFrameWrapper.ifTextWrapper(objectMapper) {
            if (!sendRetry(
                    Frame.Text(
                        objectMapper.writeValueAsString(it)
                    )
                )
            ) {
                Log.e("CSDN_WEBSOCKET_SEND", "send error")
                needClose = true
                onSendFailed()
            }
        }
        rawWebSocketFrameWrapper.ifBinary(objectMapper) {
            if (!sendRetry(Frame.Binary(false, it))) {
                Log.e("CSDN_WEBSOCKET_SEND", "send error")
                needClose = true
                onSendFailed()
            }
        }
        if (needClose) {
            Log.i("CSDN_DEBUG", "send failed and receive channel need to pause")
            break
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