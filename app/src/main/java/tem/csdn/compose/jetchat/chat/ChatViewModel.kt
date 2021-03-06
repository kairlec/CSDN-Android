package tem.csdn.compose.jetchat.chat

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.dao.AppDatabase
import tem.csdn.compose.jetchat.dao.MessageDao
import tem.csdn.compose.jetchat.dao.UserDao
import tem.csdn.compose.jetchat.data.*
import tem.csdn.compose.jetchat.model.HeartBeatException
import tem.csdn.compose.jetchat.model.LocalMessage
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.DeviceIdUtil
import tem.csdn.compose.jetchat.util.UUIDHelper
import tem.csdn.compose.jetchat.util.client
import java.util.*

class ChatViewModel : ViewModel() {
    private fun withNewInitProgress(progress: Float, @StringRes textId: Int? = null) {
        Log.d("CSDN_DEBUG_PROGRESS", "progress=${_initProgress.value}")
        if (_initProgress.value == null) {
            _initProgress.value = progress
        } else {
            _initProgress.value = progress + _initProgress.value!!
        }
        textId?.let { _initProgressTextId.value = it }
    }

    fun updateProfile(user: User) {
        this._meProfile.value = user
        _allProfiles.value?.set(user.displayId, user)
    }

    private var inited = false
    private var reloading = false
    lateinit var messageDao: MessageDao
        private set
    lateinit var userDao: UserDao
        private set

    private suspend fun reload() {
        reloading = true
        try {
            coroutineScope {
                val count = chatServer.value!!.getOnlineNumber()
                withContext(Dispatchers.Main) {
                    _onlineMembers.value = count
                    Log.d("CSDN_UPDATE", "reload online members success")
                }
                val newMessages = chatServer.value!!.getMessages().map { it.toLocal() }
                withContext(Dispatchers.Main) {
                    withNewInitProgress(0.1f)
                }
                messageDao.update(*newMessages.toTypedArray())
                withContext(Dispatchers.Main) {
                    withNewInitProgress(0.1f)
                }
                val newProfiles = chatServer.value!!.getProfiles()
                withContext(Dispatchers.Main) {
                    withNewInitProgress(0.1f)
                }
                userDao.update(*newProfiles.toTypedArray())
                withContext(Dispatchers.Main) {
                    withNewInitProgress(0.1f, R.string.handle_server_data)
                }
                val allUser =
                    userDao.getAll().map { it.displayId to it }.toTypedArray()
                val allUserMap = mutableStateMapOf(*allUser)
                withContext(Dispatchers.Main) {
                    this@ChatViewModel._allProfiles.value = allUserMap
                    withNewInitProgress(0.1f)
                }
                val allMessage = messageDao.getAll().apply {
                    Log.d("CSDN_DEBUG_MESSAGES_DAO", "msg->${this}")
                    //it.toNonLocal(allUserMap)
                }
                withContext(Dispatchers.Main) {
                    this@ChatViewModel._allMessages.value =
                        mutableStateListOf(*allMessage.toTypedArray())
                    Log.d("CSDN_UPDATE", "reload all messages success")
                    withNewInitProgress(0.1f)
                }
            }
        } finally {
            reloading = false
        }
    }

    fun initIfNeed(context: Context) {
        if (inited) {
            return
        }
        inited = true
        Log.i("CSDN_INIT", "app start up initializer start")
        MainScope().launch(Dispatchers.IO) {
//            val uuid = UUIDHelper[context]
            val uuid = DeviceIdUtil.getDeviceId(context)!!
            Log.d("CSDN_DEBUG", "uuid=${uuid}")
            val db = Room
                .databaseBuilder(context, AppDatabase::class.java, "database-csdn-android")
                .build()
            messageDao = db.messageDao()
            val lastMessage = messageDao.getLast()
            userDao = db.userDao()
            withContext(Dispatchers.Main) {
                withNewInitProgress(0.1f)
            }
            val chatAPI = ChatAPI(false, "120.77.179.218", 18080)
            Log.d("CSDN_DEBUG", "ready to create chatServer")
            val chatServer = ChatServer(
                chatAPI,
                uuid,
                client,
                lastMessage?.id,
                this
            ) {
                Log.d("CSDN_DEBUG_WEBSOCKET_STATUS", "status = $it")
                withContext(Dispatchers.Main) {
                    _webSocketStatus.value = it
                }
            }
            chatServer.initContext(context)
            withContext(Dispatchers.Main) {
                _chatServer.value = chatServer
                Log.d("CSDN_UPDATE", "update chatServer success")
                withNewInitProgress(0.1f, R.string.wait_for_server_sync)
            }
            Log.d("CSDN_DEBUG", "chatServer initing")
            val meProfile = chatServer.getMeProfile()
            withContext(Dispatchers.Main) {
                _meProfile.value = meProfile
                Log.d("CSDN_UPDATE", "update me profile success")
                withNewInitProgress(0.1f)
            }
            val chatName = chatServer.getChatDisplayName()
            val count = chatServer.getOnlineNumber()
            withContext(Dispatchers.Main) {
                _chatData.value = ChatDataScreenState(chatName)
                Log.d("CSDN_UPDATE", "update chatData success")
                _onlineMembers.value = count
                Log.d("CSDN_UPDATE", "update online members success")
                withNewInitProgress(0.1f)
            }
            val newMessages = chatServer.getMessages().map { it.toLocal() }
            withContext(Dispatchers.Main) {
                withNewInitProgress(0.1f)
            }
            messageDao.update(*newMessages.toTypedArray())
            withContext(Dispatchers.Main) {
                withNewInitProgress(0.1f)
            }
            val newProfiles = chatServer.getProfiles()
            withContext(Dispatchers.Main) {
                withNewInitProgress(0.1f)
            }
            userDao.update(*newProfiles.toTypedArray())
            withContext(Dispatchers.Main) {
                withNewInitProgress(0.1f, R.string.handle_server_data)
            }
            val allUser = userDao.getAll().map { it.displayId to it }.toTypedArray()
            val allUserMap = mutableStateMapOf(*allUser)
            withContext(Dispatchers.Main) {
                this@ChatViewModel._allProfiles.value = allUserMap
                withNewInitProgress(0.1f)
            }
            val allMessage = messageDao.getAll().apply {
                Log.d("CSDN_DEBUG_MESSAGES_DAO", "msg->${this}")
//                it.toNonLocal(allUserMap)
            }
            var lastHeartBeatUUIDString: String? = null
            launch {
                var heartBeatJob: Job? = null
                while (isActive) {
                    try {
                        chatServer.connect({
                            Log.d("CSDN_DEBUG", "server has connect,start to heartbeat")
                            heartBeatJob = launch {
                                while (isActive) {
                                    if (lastHeartBeatUUIDString != null) {
                                        val exp = HeartBeatException.HeartBeatTimeoutException()
                                        close(
                                            CloseReason(
                                                CloseReason.Codes.CANNOT_ACCEPT,
                                                exp.toString()
                                            )
                                        )
                                        break
                                    }
                                    lastHeartBeatUUIDString = UUID.randomUUID().toString()
                                    Log.d("CSDN_HEARTBEAT", "New Heartbeat")
                                    chatServer.send(
                                        RawWebSocketFrameWrapper.ofTextWrapper(
                                            TextWebSocketFrameWrapper.ofHeartbeat(
                                                lastHeartBeatUUIDString!!
                                            )
                                        )
                                    )
                                    //30s????????????
                                    delay(30_000)
                                }
                            }
                        }) {
                            Log.d("CSDN_DEBUG", "websocket has disconnect,stop heartbeat job")
                            heartBeatJob?.cancel()
                            lastHeartBeatUUIDString = null
                            //??????????????????????????????
                            while (isActive) {
                                try {
                                    if (!reloading) {
                                        reload()
                                        break
                                    }
                                } catch (e: Throwable) {
                                    Log.d("CSDN_DEBUG", "try reload error, delay 3000 and retry", e)
                                }
                                delay(3000)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.d("CSDN_DEBUG", "try connect error, delay 3000 and retry", e)
                        delay(3000)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                this@ChatViewModel._allMessages.value =
                    mutableStateListOf(*allMessage.toTypedArray())
                Log.d("CSDN_UPDATE", "update all messages success")
                withNewInitProgress(0.1f)
            }
            launch {
                for (rawWebSocketFrameWrapper in chatServer.outputChannel) {
                    try {
                        rawWebSocketFrameWrapper.ifTextWrapper(chatServer.objectMapper) {
                            when (it.type) {
                                TextWebSocketFrameWrapper.FrameType.IMAGE_MESSAGE,
                                TextWebSocketFrameWrapper.FrameType.TEXT_MESSAGE -> {
                                    val msg =
                                        chatServer.objectMapper.convertValue<Message>(it.content!!)
                                    Log.d("CSDN_DEBUG_RECEIVE", "new msg:${msg}")
                                    msg.toLocal().let {
                                        messageDao.update(it)
                                        withContext(Dispatchers.Main) {
                                            _allMessages.value?.add(0, it)
                                        }
                                    }
                                }
                                TextWebSocketFrameWrapper.FrameType.NEW_CONNECTION -> {
                                    val user =
                                        chatServer.objectMapper.convertValue<User>(it.content!!)
                                    Log.d("CSDN_DEBUG_RECEIVE", "new connection:${user}")
                                    userDao.update(user)
                                    withContext(Dispatchers.Main) {
                                        _allProfiles.value?.set(user.displayId, user)
                                        if (user.displayId != meProfile.displayId) {
                                            _onlineMembers.value = _onlineMembers.value!! + 1
                                        }
                                    }
                                }
                                TextWebSocketFrameWrapper.FrameType.UPDATE_USER -> {
                                    val user =
                                        chatServer.objectMapper.convertValue<User>(it.content!!)
                                    Log.d("CSDN_DEBUG_RECEIVE", "new update event:${user}")
                                    userDao.update(user)
                                    withContext(Dispatchers.Main) {
                                        _allProfiles.value?.set(user.displayId, user)
                                    }
                                }
                                TextWebSocketFrameWrapper.FrameType.NEW_DISCONNECTION -> {
                                    val user =
                                        chatServer.objectMapper.convertValue<User>(it.content!!)
                                    Log.d("CSDN_DEBUG_RECEIVE", "new dis connection:${user}")
                                    userDao.update(user)
                                    withContext(Dispatchers.Main) {
                                        _allProfiles.value?.set(user.displayId, user)
                                        _onlineMembers.value = _onlineMembers.value!! - 1
                                    }
                                }
                                TextWebSocketFrameWrapper.FrameType.HEARTBEAT -> {
                                    val content =
                                        chatServer.objectMapper.convertValue<String>(it.content!!)
                                    Log.d("CSDN_DEBUG", "HEARTBEAT")
                                    chatServer.send(
                                        RawWebSocketFrameWrapper.ofTextWrapper(
                                            TextWebSocketFrameWrapper.ofHeartbeatAck(content)
                                        )
                                    )
                                }
                                TextWebSocketFrameWrapper.FrameType.HEARTBEAT_ACK -> {
                                    val contentString =
                                        chatServer.objectMapper.convertValue<String>(it.content!!)
                                    //????????????????????????
                                    if (contentString != lastHeartBeatUUIDString) {
                                        val exp =
                                            HeartBeatException.HeartBeatContentMismatchException(
                                                lastHeartBeatUUIDString ?: "null",
                                                contentString
                                            )
                                        Log.w("CSDN_HEARTBEAT", exp)
                                    }
                                    lastHeartBeatUUIDString = null
                                }
                                TextWebSocketFrameWrapper.FrameType.NEED_SYNC -> {
                                    if (!reloading) {
                                        reload()
                                    }
                                }
                            }
                        }
                        rawWebSocketFrameWrapper.ifBinary(chatServer.objectMapper) {
                            //TODO ??????
                        }
                        rawWebSocketFrameWrapper.ifRawText {
                            //TODO ??????
                        }
                    } catch (e: Throwable) {
                        Log.w("CSDN_RECEIVE", "wrong text format:${e.message}")
                    }
                }
            }
        }
    }

    private val _chatData = MutableLiveData<ChatDataScreenState>()
    private val _initProgress = MutableLiveData<Float>()
    private val _initProgressTextId = MutableLiveData<Int>()
    private val _onlineMembers = MutableLiveData<Int>()
    private val _allMessages = MutableLiveData<MutableList<LocalMessage>>()
    private val _allProfiles = MutableLiveData<MutableMap<String, User>>()
    private val _webSocketStatus = MutableLiveData<Boolean>()
    private val _chatServer = MutableLiveData<ChatServer>()
    private val _meProfile = MutableLiveData<User>()

    val chatData: LiveData<ChatDataScreenState> = _chatData
    val initProgress: LiveData<Float> = _initProgress
    val initProgressTextId: LiveData<Int> = _initProgressTextId
    val allMessages: LiveData<MutableList<LocalMessage>> = _allMessages
    val allProfiles: LiveData<MutableMap<String, User>> = _allProfiles
    val onlineMembers: LiveData<Int> = _onlineMembers
    val webSocketStatus: LiveData<Boolean> = _webSocketStatus
    val chatServer: LiveData<ChatServer> = _chatServer
    val meProfile: LiveData<User> = _meProfile
}

@Immutable
data class ChatDataScreenState(
    val displayName: String,
) {
    @Composable
    fun getPhotoPainter(chatServer: ChatServer): String {
        return chatServer.chatAPI.image("0")
    }
}