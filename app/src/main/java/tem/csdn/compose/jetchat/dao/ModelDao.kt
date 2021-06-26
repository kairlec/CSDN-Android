package tem.csdn.compose.jetchat.dao

import androidx.room.*
import tem.csdn.compose.jetchat.model.LocalMessage
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.model.UserAndMessage
// region 粟唐焕 DAO库使用
@Database(entities = [User::class, LocalMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
}
// endregion

//region 陈卡 用户DAO设计
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): List<User>

    @Query("SELECT * FROM users WHERE displayId = :displayId")
    fun getByDisplayId(displayId:String): User?

    // 使用REPLACE以便强制更新已经存在的用户
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg users: User)
}
//endregion

// region 待定 消息DAOP设计
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id=(SELECT max(id) FROM messages)")
    fun getLast(): LocalMessage?

    @Query("SELECT * FROM messages ORDER BY id DESC")
    fun getAll(): List<LocalMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg messages: LocalMessage)

    @Transaction
    @Query("SELECT * FROM users")
    fun getUsersAndLibraries(): List<UserAndMessage>
}
// endregion