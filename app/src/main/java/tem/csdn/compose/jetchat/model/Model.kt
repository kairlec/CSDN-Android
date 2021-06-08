package tem.csdn.compose.jetchat.model

import androidx.compose.runtime.Composable
import androidx.room.*
import tem.csdn.compose.jetchat.data.ChatServer
import java.io.Serializable

data class Message(
    val id: Long,
    val content: String,
    val timestamp: Int,
    val image: String?,
    val author: User
) {
    fun toLocal(): LocalMessage = LocalMessage(id, content, timestamp, image, author.displayId)

    @Composable
    fun getImagePainter(chatServer: ChatServer): String? {
        return if (image != null) {
            chatServer.chatAPI.image(image)
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
    val photo: String?,
    val github: String?,
    val qq: String?,
    val weChat: String?,
) : Serializable, Comparable<User> {
    companion object {
        private const val serialVersionUID = 47073173576278320L
    }

    @Composable
    fun getPhotoPainter(chatServer: ChatServer): String? {
        return if (photo != null) {
            chatServer.chatAPI.image(photo)
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
    val image: String?,
    val authorDisplayId: String
) {
    fun toNonLocal(users: Map<String, User>): Message {
        return Message(id, content, timestamp, image, users[authorDisplayId]!!)
    }

    @Composable
    fun getImagePainter(chatServer: ChatServer): String? {
        return if (image != null) {
            chatServer.chatAPI.image(image)
        } else {
            null
        }
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
