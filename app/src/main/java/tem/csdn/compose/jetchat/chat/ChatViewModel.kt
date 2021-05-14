package tem.csdn.compose.jetchat.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tem.csdn.compose.jetchat.conversation.avatarImage

class ChatViewModel : ViewModel() {
    private var chatId: String = "1"

    fun withChatId(newChatId: String) {
        if (newChatId != chatId) {
            chatId = newChatId
        }
    }

    private val _chatData = MutableLiveData<ChatDataScreenState>()
    val chatData: LiveData<ChatDataScreenState> = _chatData
}

@Immutable
data class ChatDataScreenState(
    val chatUrl: String,
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