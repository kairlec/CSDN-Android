package tem.csdn.compose.jetchat.data

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import okhttp3.Cache
import okhttp3.OkHttpClient
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.*
import java.io.File
import java.lang.Exception

class ChatServerInitException(
    val code: Int = -1,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

class ChatServer(
    val chatAPI: ChatAPI,
    val id: String,
    private val client: HttpClient,
    private val lastMessageId: Long?,
    coroutineScope: CoroutineScope,
    val onWebSocketEvent: suspend (Boolean) -> Unit
) {
    companion object {
        lateinit var current: ChatServer
            private set
            @get:Composable
            get
    }

    lateinit var imageLoader: ImageLoader
        private set

    fun initContext(context: Context) {
        imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .error(R.drawable.ic_broken_cable)
            .placeholder(R.drawable.ic_loading)
            .availableMemoryPercentage(0.7)
            .bitmapPoolPercentage(0.7)
            .allowRgb565(true)
            .componentRegistry {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder(context))
                } else {
                    add(GifDecoder())
                }
                add(CoilByteArrayFetcher())
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(OkHttpCacheHelper.getCache(context))
                    .build()
            }
            .build()
    }

    val objectMapper = jacksonObjectMapper()

    // 这个锁用来保持WebSocket不会被关闭,防止提前关闭输出导致需要重新连接
    private val webSocketKeepAliveMutex = Mutex()
    private val inputReservableActor by lazy {
        ReservableActor<RawWebSocketFrameWrapper<*>>(
            coroutineScope = coroutineScope,
            context = Dispatchers.IO,
            capacity = Channel.UNLIMITED,
            start = CoroutineStart.LAZY
        )
    }
    val outputChannel by lazy { Channel<RawWebSocketFrameWrapper<*>>(Channel.UNLIMITED) }

    suspend fun updateImageCheck(checkUrl: String): Boolean {
        val result = client.get<Result<Boolean>>(checkUrl).checked()
        return result.data == true
    }

    suspend fun updateProfile(user: User): User {
        return client.post<Result<User>>(chatAPI.profile()) {
            body = FormDataContent(Parameters.build {
                append("name", user.name)
                append("displayName", user.displayName)
                append("position", user.position)
                append("github", user.github ?: "")
                append("qq", user.qq ?: "")
                append("weChat", user.weChat ?: "")
            })
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "update profile:${this}")
        }
    }

    suspend fun updateProfilePhoto(byteArray: ByteArray, filename: String? = null): User {
        return client.submitFormWithBinaryData<Result<User>> {
            url(chatAPI.profilePhoto())
            method = HttpMethod.Post
            body = MultiPartContent.build {
                add("avatar", byteArray, filename = filename)
            }
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "update profile:${this}")
        }
    }

    suspend fun getMeProfile(): User {
        return client.post<Result<User>>(chatAPI.init(id)).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for me profile:${this}")
        }
    }

    suspend fun getChatDisplayName(): String {
        return client.get<Result<String>>(chatAPI.chatName()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for chat display name:${this}")
        }
    }

    suspend fun getProfiles(): List<User> {
        return client.get<Result<List<User>>>(chatAPI.profiles()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for profiles:${this}")
        }
    }

    suspend fun getOnlineNumber(): Int {
        return client.get<Result<Int>>(chatAPI.count()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for count:${this}")
        }
    }

    suspend fun getMessages(): List<Message> {
        return client.get<Result<List<Message>>>(chatAPI.messages()) {
            if (lastMessageId != null) {
                parameter("after_id", lastMessageId)
            }
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for messages:${this}")
        }
    }

    suspend fun connect(
        onConnect: suspend DefaultClientWebSocketSession.() -> Unit,
        onDisconnect: suspend () -> Unit
    ) {
        if (webSocketKeepAliveMutex.isLocked) {
            webSocketKeepAliveMutex.unlock()
        }
        connectWebSocketToServer(
            ssl = chatAPI.ssl,
            host = chatAPI.host,
            port = chatAPI.port,
            path = chatAPI.webSocket(id),
            objectMapper = objectMapper,
            webSocketKeepAliveMutex = webSocketKeepAliveMutex,
            outputMessageChannel = outputChannel,
            onConnected = { wsSession ->
                Log.d(
                    "CSDN_DEBUG",
                    "locked to keep alive, redirect to send handler and send error handler"
                )
                if (!webSocketKeepAliveMutex.isLocked) {
                    webSocketKeepAliveMutex.lock()
                }
                inputReservableActor.invokeOnReceive {
                    wsSession.trySend(objectMapper, it) { throwable ->
                        this.cause = throwable
                        this.throwToErrorHandler = true
                        it.ifTextWrapper(objectMapper) {
                            if (it.type == TextWebSocketFrameWrapper.FrameType.HEARTBEAT || it.type == TextWebSocketFrameWrapper.FrameType.HEARTBEAT_ACK) {
                                this.hold = false
                            }
                        }
                    }
                }
                inputReservableActor.invokeOnReceiveError {
                    if (webSocketKeepAliveMutex.isLocked) {
                        webSocketKeepAliveMutex.unlock()
                    }
                    wsSession.close()
                }
                Log.d("CSDN_DEBUG", "start input listen")
                inputReservableActor.startAll()
                onWebSocketEvent(true)
                onConnect(wsSession)
            },
            onDisconnected = {
                Log.d("CSDN_DEBUG", "disconnect,pause receive Handelr")
                inputReservableActor.pauseReceiveHandler()
                onWebSocketEvent(false)
                onDisconnect()
            }
        )
        Log.d("CSDN_DEBUG", "init for websocket")
    }

    suspend fun send(rawWebSocketFrameWrapper: RawWebSocketFrameWrapper<*>) {
        inputReservableActor.send(rawWebSocketFrameWrapper)
    }


    init {
        current = this
    }
}

class Result<T>(
    val code: Int = 0,
    val msg: String? = null,
    val data: T? = null
) {
    override fun toString(): String {
        return "Result(code=$code, msg=$msg, data=$data)"
    }

    fun checked() = apply {
        if (code != 0) {
            throw ChatServerInitException(code, msg)
        }
    }
}
