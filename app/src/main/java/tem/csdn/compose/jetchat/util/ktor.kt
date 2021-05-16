package tem.csdn.compose.jetchat.util

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


val client = HttpClient {
    install(WebSockets)
}

suspend fun connectWebSocketToServer(
    host: String = "localhost",
    port: Int = DEFAULT_PORT,
    path: String = "/",
    method: HttpMethod = HttpMethod.Get,
    inputMessageChannel: Channel<String>,
    onReceiveMessageEvent: (String) -> Unit
) {
    client.webSocket(method = method, host = host, port = port, path = path) {
        val messageOutputRoutine = launch { outputMessages(onReceiveMessageEvent) }
        val userInputRoutine = launch { inputMessages(inputMessageChannel) }

        userInputRoutine.join()
        messageOutputRoutine.cancelAndJoin()
    }
    client.close()
}

suspend fun DefaultClientWebSocketSession.outputMessages(onReceiveMessageEvent: (String) -> Unit) {
    try {
        for (message in incoming) {
            message as? Frame.Text ?: continue
            onReceiveMessageEvent(message.readText())
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