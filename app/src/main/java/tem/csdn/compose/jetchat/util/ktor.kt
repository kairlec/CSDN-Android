package tem.csdn.compose.jetchat.util

import android.util.Log
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


val client = HttpClient(CIO) {
    install(WebSockets)
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
    inputMessageChannel: Channel<String>,
    outputMessageChannel: Channel<String>,
    onConnected: suspend () -> Unit,
    onDisconnected: suspend () -> Unit,
) {
    client.webSocket(method = method, host = host, port = port ?: DEFAULT_PORT, path = path) {
        onConnected()
        val messageOutputRoutine = launch { outputMessages(outputMessageChannel) }
        val userInputRoutine = launch { inputMessages(inputMessageChannel) }

        userInputRoutine.join()
        messageOutputRoutine.cancelAndJoin()
    }
    onDisconnected()
    client.close()
}

suspend fun DefaultClientWebSocketSession.outputMessages(outputMessageChannel: Channel<String>) {
    try {
        for (message in incoming) {
            message as? Frame.Text ?: continue
            outputMessageChannel.send(message.readText())
        }
    } catch (e: Throwable) {
        Log.e("CSDN_WEBSOCKET_RECEIVE", "Error while receiving: ${e.localizedMessage}", e)
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages(inputMessageChannel: Channel<String>) {
    val message = inputMessageChannel.receive()
    if (!sendRetry(Frame.Text(message))) {
        Log.e("CSDN_WEBSOCKET_SEND", "send error")
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