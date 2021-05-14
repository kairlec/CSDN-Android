package tem.csdn.compose.jetchat.conversation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
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
    val timestamp: Int,
    val image: String? = null,
)
