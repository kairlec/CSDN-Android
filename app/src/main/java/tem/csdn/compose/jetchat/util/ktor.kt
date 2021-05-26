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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tem.csdn.compose.jetchat.data.*
import java.lang.Exception
import java.lang.IllegalStateException

val client = HttpClient(CIO) {
    install(WebSockets) {
        pingInterval = 5_000
    }
    install(JsonFeature)
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
}

fun currentHttpClient() = client

suspend fun connectWebSocketToServer(
    ssl: Boolean,
    host: String = "localhost",
    port: Int? = DEFAULT_PORT,
    path: String = "/",
    method: HttpMethod = HttpMethod.Get,
    objectMapper: ObjectMapper,
    webSocketKeepAliveMutex: Mutex,
    outputMessageChannel: Channel<RawWebSocketFrameWrapper<*>>,
    onConnected: suspend (DefaultClientWebSocketSession) -> Unit,
    onDisconnected: suspend () -> Unit,
) {
    var inSession = false
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
            inSession = true
            onConnected(this)
            try {
                val messageOutputRoutine =
                    launch { outputMessages(objectMapper, outputMessageChannel) }
                Log.d("CSDN_DEBUG", "ready to lock to keep alive")
                webSocketKeepAliveMutex.withLock {}
                Log.d("CSDN_DEBUG", "unlock and ready to cancel output")
                messageOutputRoutine.cancel()
                Log.d("CSDN_DEBUG", "cancel output")
            } finally {
                Log.d("CSDN_DEBUG", "websocket has be disconnected")
                onDisconnected()
            }
        }
    } finally {
        if (!inSession) {
            Log.d("CSDN_DEBUG", "websocket has be disconnected")
            onDisconnected()
        }
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

suspend fun DefaultClientWebSocketSession.trySend(
    objectMapper: ObjectMapper,
    rawWebSocketFrameWrapper: RawWebSocketFrameWrapper<*>,
    onSendFailed: suspend (Throwable) -> Unit
): Boolean {
    var error: Throwable? = null
    rawWebSocketFrameWrapper.ifRawText {
        error = sendRetry(
            Frame.Text(
                objectMapper.writeValueAsString(
                    TextWebSocketFrameWrapper.ofMessage(
                        it
                    )
                )
            )
        )
    }
    rawWebSocketFrameWrapper.ifTextWrapper(objectMapper) {
        error = sendRetry(
            Frame.Text(
                objectMapper.writeValueAsString(it)
            )
        )
    }
    rawWebSocketFrameWrapper.ifBinary(objectMapper) {
        error = sendRetry(Frame.Binary(false, it))
    }
    return if (error != null) {
        Log.e("CSDN_WEBSOCKET_SEND", "send error")
        onSendFailed(error!!)
        false
    } else {
        true
    }
}

suspend fun DefaultClientWebSocketSession.sendRetry(frame: Frame, retryCnt: Int = 3): Throwable? {
    if (retryCnt <= 0) {
        return IllegalStateException("Parameter retryCnt must be greater than 0")
    }
    return try {
        send(frame)
        Log.d("CSDN_WEBSOCKET_SEND_RETRY", "sended:${frame}")
        null
    } catch (e: ClosedSendChannelException) {
        e
    } catch (e: Throwable) {
        Log.e(
            "CSDN_WEBSOCKET_SEND_RETRY",
            "send error when retry(${retryCnt}): ${e.localizedMessage}",
            e
        )
        if (retryCnt == 1) {
            e
        } else {
            sendRetry(frame, retryCnt - 1)
        }
    }
}