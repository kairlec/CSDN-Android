package tem.csdn.compose.jetchat.dao

import androidx.room.*
import tem.csdn.compose.jetchat.model.LocalMessage
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.model.UserAndMessage

@Database(entities = [User::class, LocalMessage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg users: User)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id=(SELECT max(id) FROM messages)")
    fun getLast(): LocalMessage?

    @Query("SELECT * FROM messages")
    fun getAll(): List<LocalMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg messages: LocalMessage)

    @Transaction
    @Query("SELECT * FROM users")
    fun getUsersAndLibraries(): List<UserAndMessage>
}
