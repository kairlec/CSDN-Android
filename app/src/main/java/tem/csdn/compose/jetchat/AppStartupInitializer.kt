package tem.csdn.compose.jetchat

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.startup.Initializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.dao.AppDatabase
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.util.UUIDHelper
import tem.csdn.compose.jetchat.util.client

class AppStartupInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Log.i("CSDN_INIT", "app start up initializer start")
        MainScope().launch(Dispatchers.IO) {
            val uuid = UUIDHelper[context]
            val progressChannel = Channel<Pair<Float, Int?>>(11)
            val db = Room
                .databaseBuilder(context, AppDatabase::class.java, "database-csdn-android")
                .build()
            val messageDao = db.messageDao()
            val lastMessage = messageDao.getLast()
            val userDao = db.userDao()
            progressChannel.send(0.2f to null)
            val chatAPI = ChatAPI(false, "10.0.2.2", 18080)
            val chatServer = ChatServer(
                chatAPI,
                uuid,
                client,
                lastMessage?.id,
                messageDao,
                userDao,
                progressChannel
            )
            chatServer.addProgress(0.1f, R.string.wait_for_server_sync)
            Log.d("CSDN_DEBUG", "chatServer init")
            chatServer.addProgress(0.1f, null)
            val newMessages = chatServer.messages.map { it.toLocal() }
            chatServer.addProgress(0.1f, null)
            messageDao.update(*newMessages.toTypedArray())
            chatServer.addProgress(0.1f, null)
            val newProfiles = chatServer.profiles
            chatServer.addProgress(0.1f, null)
            userDao.update(*newProfiles.toTypedArray())
            chatServer.addProgress(0.1f, R.string.handle_server_data)
            val allUser = userDao.getAll().associateBy { it.displayId }
            chatServer.chatData.allUser.send(allUser)
            chatServer.addProgress(0.1f, null)
            val allMessage = messageDao.getAll().map { it.toNonLocal(allUser) }
            chatServer.chatData.allMessage.send(allMessage)
            chatServer.addProgress(0.1f, null)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}