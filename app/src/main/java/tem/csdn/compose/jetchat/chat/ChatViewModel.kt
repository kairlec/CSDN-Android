package tem.csdn.compose.jetchat.chat

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.conversation.Message
import tem.csdn.compose.jetchat.conversation.avatarImage
import tem.csdn.compose.jetchat.dao.AppDatabase
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.UUIDHelper
import tem.csdn.compose.jetchat.util.client
import java.util.concurrent.atomic.AtomicBoolean

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

    private var inited = false

    fun initIfNeed(context: Context) {
        if (inited) {
            return
        }
        inited = true
        Log.i("CSDN_INIT", "app start up initializer start")
        MainScope().launch(Dispatchers.IO) {
            val uuid = UUIDHelper[context]
            val db = Room
                .databaseBuilder(context, AppDatabase::class.java, "database-csdn-android")
                .build()
            val messageDao = db.messageDao()
            val lastMessage = messageDao.getLast()
            val userDao = db.userDao()
            withContext(Dispatchers.Main) {
                withNewInitProgress(0.1f)
            }
            val chatAPI = ChatAPI(true, "csdnmsg.kairlec.com")
            Log.d("CSDN_DEBUG", "ready to create chatServer")
            val chatServer = ChatServer(
                chatAPI,
                uuid,
                client,
                lastMessage?.id,
                messageDao,
                userDao,
            ) {
                Log.d("CSDN_DEBUG_WEBSOCKET_STATUS", "status = $it")
                withContext(Dispatchers.Main) {
                    _webSocketStatus.value = it
                }
            }
            withContext(Dispatchers.Main) {
                _chatServer.value = chatServer
                Log.d("CSDN_UPDATE","update chatServer success")
                withNewInitProgress(0.1f, R.string.wait_for_server_sync)
            }
            Log.d("CSDN_DEBUG", "chatServer initing")
            val meProfile = chatServer.getMeProfile()
            withContext(Dispatchers.Main) {
                _meProfile.value = meProfile
                Log.d("CSDN_UPDATE","update me profile success")
                withNewInitProgress(0.1f)
            }
            val chatName = chatServer.getChatDisplayName()
            val count = chatServer.getOnlineNumber()
            withContext(Dispatchers.Main) {
                _chatData.value = ChatDataScreenState(
                    chatServer.chatAPI.image(ChatAPI.ImageType.CHAT, "0"),
                    chatName
                )
                Log.d("CSDN_UPDATE","update chatData success")
                _onlineMembers.value = count
                Log.d("CSDN_UPDATE","update online members success")
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
            val allUser = userDao.getAll().associateBy { it.displayId }
            withContext(Dispatchers.Main) {
                this@ChatViewModel._allProfiles.value = allUser
                withNewInitProgress(0.1f)
            }
            val allMessage = messageDao.getAll().map {
                Log.d("CSDN_DEBUG_MESSAGES_DAO", "msg->${it}")
                it.toNonLocal(allUser)
            }
            launch {
                chatServer.connect()
            }
            withContext(Dispatchers.Main) {
                this@ChatViewModel._allMessages.value = allMessage
                Log.d("CSDN_UPDATE","update all messages success")
                withNewInitProgress(0.1f)
            }
        }
    }

    private val _chatData = MutableLiveData<ChatDataScreenState>()
    private val _initProgress = MutableLiveData<Float>()
    private val _initProgressTextId = MutableLiveData<Int>()
    private val _onlineMembers = MutableLiveData<Int>()
    private val _allMessages = MutableLiveData<List<Message>>()
    private val _allProfiles = MutableLiveData<Map<String, User>>()
    private val _webSocketStatus = MutableLiveData<Boolean>()
    private val _chatServer = MutableLiveData<ChatServer>()
    private val _meProfile = MutableLiveData<User>()

    val chatData: LiveData<ChatDataScreenState> = _chatData
    val initProgress: LiveData<Float> = _initProgress
    val initProgressTextId: LiveData<Int> = _initProgressTextId
    val allMessages: LiveData<List<Message>> = _allMessages
    val allProfiles: LiveData<Map<String, User>> = _allProfiles
    val onlineMembers: LiveData<Int> = _onlineMembers
    val webSocketStatus: LiveData<Boolean> = _webSocketStatus
    val chatServer: LiveData<ChatServer> = _chatServer
    val meProfile: LiveData<User> = _meProfile
}

@Immutable
data class ChatDataScreenState(
    // 群聊图片(头像)
    val photo: String?,
    val displayName: String,
) {
    @Composable
    fun getPhotoPainter(): Painter? {
        if (photo == null) {
            return null
        }
        return if (photo.startsWith("http")) {
            avatarImage(url = photo)
        } else {
            photo.toIntOrNull()?.let { painterResource(id = it) }
        }
    }
}