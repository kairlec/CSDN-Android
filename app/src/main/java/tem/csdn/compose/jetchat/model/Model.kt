package tem.csdn.compose.jetchat.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.room.*
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.data.ChatServer
import java.io.Serializable

data class Message(
    val id: Long,
    val content: String,
    val timestamp: Int,
    val image: Boolean?,
    val author: User
) {
    fun toLocal(): LocalMessage = LocalMessage(id, content, timestamp, image, author.displayId)

    @Composable
    fun getImagePainter(chatServer: ChatServer): String? {
        return if (image == true) {
            chatServer.chatAPI.image(
                ChatAPI.ImageType.IMAGE,
                id.toString()
            )
        } else {
            null
        }
    }
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val displayId: String,
    val name: String,
    val displayName: String,
    val position: String,
    val photo: Boolean?,
    val github: String?,
    val qq: String?,
    val weChat: String?,
) : Serializable, Comparable<User> {
    companion object {
        private const val serialVersionUID = 47073173576278320L
    }

    @Composable
    fun getPhotoPainter(chatServer: ChatServer): String? {
        return if (photo == true) {
            chatServer.chatAPI.image(ChatAPI.ImageType.PHOTO, displayId)
        } else {
            null
        }
    }

    override fun compareTo(other: User): Int {
        return name.compareTo(other.name)
    }
}


@Entity(tableName = "messages")
data class LocalMessage(
    @PrimaryKey val id: Long,
    val content: String,
    val timestamp: Int,
    val image: Boolean?,
    val authorDisplayId: String
) {
    fun toNonLocal(users: Map<String, User>): Message {
        return Message(id, content, timestamp, image, users[authorDisplayId]!!)
    }
}

data class UserAndMessage(
    @Embedded val user: User,
    @Relation(
        parentColumn = "displayId",
        entityColumn = "authorDisplayId"
    )
    val message: LocalMessage
)
