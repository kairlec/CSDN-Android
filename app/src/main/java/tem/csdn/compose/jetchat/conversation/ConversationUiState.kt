package tem.csdn.compose.jetchat.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.profile.ProfileScreenState

class ConversationUiState(
    val channelName: String,
    val channelMembers: Int,
    initialMessages: List<Message>
) {
    private val _messages: MutableList<Message> =
        mutableStateListOf(*initialMessages.toTypedArray())
    val messages: List<Message> = _messages

    fun addMessage(msg: Message) {
        _messages.add(msg)
    }
}

@Immutable
data class Message(
    val author: ProfileScreenState,
    val messageId: String,
    val content: String,
    val timestamp: Long,
    val image: String? = null,
){
    @Composable
    fun getImagePainter(): Painter? {
        if (image == null) {
            return null
        }
        return if (image.startsWith("http")) {
            image(url = image)
        } else {
            image.toIntOrNull()?.let { painterResource(id = it) }
        }
    }

    @Composable
    fun getImagePainterOrDefault(): Painter {
        return getImagePainter() ?: painterResource(id = R.drawable.ic_broken_cable)
    }
}
