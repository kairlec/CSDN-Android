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

// region 待定 服务器相关接口封装
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

    /**
     * 初始化上下文
     */
    fun initContext(context: Context) {
        // 初始化coil的imageLoader
        // 加载GifDecoder(新版是ImageDecoderDecoder)
        // 加载对ByteArray的解析支持
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
    // 输出的管道,所有接受的消息会写入到这个管道内,使用UNLIMITED来保证管道不会被阻塞,以便与服务器快速通讯
    val outputChannel by lazy { Channel<RawWebSocketFrameWrapper<*>>(Channel.UNLIMITED) }

    // 检查图片(是否已经存在)
    suspend fun updateImageCheck(checkUrl: String): Boolean {
        val result = client.get<Result<Boolean>>(checkUrl).checked()
        return result.data == true
    }

    // region 陈卡 个人信息同步接口
    /**
     * 更新个人信息
     */
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

    /**
     * 更新个人信息的图片(头像)
     */
    suspend fun updateProfilePhoto(byteArray: ByteArray, filename: String? = null): User {
        return client.submitFormWithBinaryData<Result<User>> {
            url(chatAPI.profilePhoto(null))
            method = HttpMethod.Post
            body = MultiPartContent.build {
                add("avatar", byteArray, filename = filename)
            }
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "update profile:${this}")
        }
    }

    /**
     * 更新个人信息的图片(头像)
     * 这个是提交sha256值,不需要重新上传图片
     */
    suspend fun updateProfilePhoto(sha256: String): User {
        return client.submitFormWithBinaryData<Result<User>> {
            url(chatAPI.profilePhoto(sha256))
            method = HttpMethod.Post
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "update profile:${this}")
        }
    }

    /**
     * 获取自己的信息
     */
    suspend fun getMeProfile(): User {
        return client.post<Result<User>>(chatAPI.init(id)).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for me profile:${this}")
        }
    }

    /**
     * 获取所有用户信息
     */
    suspend fun getProfiles(): List<User> {
        return client.get<Result<List<User>>>(chatAPI.profiles()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for profiles:${this}")
        }
    }
    // endregion

    /**
     * 获取群聊名称
     */
    suspend fun getChatDisplayName(): String {
        return client.get<Result<String>>(chatAPI.chatName()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for chat display name:${this}")
        }
    }

    /**
     * 获取在线的成员数量
     */
    suspend fun getOnlineNumber(): Int {
        return client.get<Result<Int>>(chatAPI.count()).checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for count:${this}")
        }
    }

    /**
     * 获取所有的消息(如果lastMessageId不为空,则是从上一次消息之后来获取,既获取从上一次之后未收到的消息)
     */
    suspend fun getMessages(): List<Message> {
        return client.get<Result<List<Message>>>(chatAPI.messages()) {
            if (lastMessageId != null) {
                parameter("after_id", lastMessageId)
            }
        }.checked().data!!.apply {
            Log.d("CSDN_DEBUG", "init for messages:${this}")
        }
    }

    /**
     * 连接WebSocket
     * 这个suspend函数会一直挂起当前协程,直到WebSocket断开连接
     * 所以需要将其丢置到新的协程里执行避免阻塞死协程
     */
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
                // 提前锁住保活锁,以便让websocket保持连接不会自动关闭
                if (!webSocketKeepAliveMutex.isLocked) {
                    webSocketKeepAliveMutex.lock()
                }
                // 当消费者队列产生新的数据的时候,表示有新的消息需要发送给服务器
                // 调用Websocket的trySend拓展方法,让其尝试三次发送
                // 若连续失败,则标记发送出现错误,在当前DSL内标记当前错误体需要丢弃到异常处理器
                // 如果当前的消息为心跳包或者心跳响应包
                // 既不重要的消息,可以被丢弃,则将hold置为false,让消费者队列丢弃这一次的失败传输
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
                // 消费者队列出现发送异常的时候,异常处理器所要做的事
                // 既上面的发送出错
                // 关闭保活锁,让websocket自然关闭
                // 同时调用WsSession.close,尝试关闭WebSocket所在的协程作用域
                inputReservableActor.invokeOnReceiveError {
                    if (webSocketKeepAliveMutex.isLocked) {
                        webSocketKeepAliveMutex.unlock()
                    }
                    wsSession.close()
                }
                Log.d("CSDN_DEBUG", "start input listen")
                // 开启消费者队列
                inputReservableActor.startAll()
                // 标记WebSocket状态为true
                onWebSocketEvent(true)
                // 执行已连接的事件
                onConnect(wsSession)
            },
            onDisconnected = {
                Log.d("CSDN_DEBUG", "disconnect,pause receive Handelr")
                // 断开连接了,需要暂停消费者队列的执行,避免继续发送消息造成连续错误
                inputReservableActor.pauseReceiveHandler()
                // 标记WebSocket状态为false
                onWebSocketEvent(false)
                // 执行断连事件
                onDisconnect()
            }
        )
        Log.d("CSDN_DEBUG", "init for websocket")
    }

    /**
     * 往消费者队列里发送新的帧包装器,也就是发送消息
     */
    suspend fun send(rawWebSocketFrameWrapper: RawWebSocketFrameWrapper<*>) {
        inputReservableActor.send(rawWebSocketFrameWrapper)
    }

    /**
     * 初始化的时候设置current,让其全局都可以直接获取来使用
     */
    init {
        current = this
    }
}
//endregion

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
