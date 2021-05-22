package tem.csdn.compose.jetchat.conversation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User

class ConversationUiState(
    val chatData: ChatDataScreenState,
    val onlineMembers: Int,
    messages: List<Message>
) {
    private val _messages: MutableList<Message> = mutableStateListOf(*messages.toTypedArray())
    val messages: List<Message> = _messages
}

//class ConversationUiState(
//    val channelName: String,
//    val channelMembers: Int,
//    initialMessages: List<Message>,
//    val onSend: (String) -> Unit
//) {
//    private val _messages: MutableList<Message> =
//        mutableStateListOf(*initialMessages.toTypedArray())
//
//    val messages: List<Message> = _messages
//
//    fun receive(msg: Message) {
//        _messages.add(msg)
//    }
//
//    fun addMessage(msg: String) {
//        try {
//            onSend(msg)
//        } catch (e: Throwable) {
//            Log.e("CSDN-ADNROID-ADDMESSAGE", "send error:${e.localizedMessage}", e)
//        }
//    }
//}
//
//@Immutable
//data class Message(
//    val author: User,
//    val messageId: String,
//    val content: String,
//    val timestamp: Long,
//    val image: String? = null,
//) {
//    @Composable
//    fun getImagePainter(): Painter? {
//        if (image == null) {
//            return null
//        }
//        return if (image.startsWith("http")) {
//            image(url = image)
//        } else {
//            image.toIntOrNull()?.let { painterResource(id = it) }
//        }
//    }
//
//    @Composable
//    fun getImagePainterOrDefault(): Painter {
//        return getImagePainter() ?: painterResource(id = R.drawable.ic_broken_cable)
//    }
//}
