package tem.csdn.compose.jetchat

import android.content.Context
import androidx.startup.Initializer

// APP启动的初始化
// 由于库不需要初始化了,这个也懒得删了
class AppStartupInitializer : Initializer<Unit> {

    override fun create(context: Context) {

    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}