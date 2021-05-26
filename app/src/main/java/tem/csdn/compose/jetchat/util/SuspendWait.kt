package tem.csdn.compose.jetchat.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class SuspendWait(private val owner: Any? = null, locked: Boolean = false) {
    private val mutex = Mutex(locked = locked)
    private var onPauseHandler: ((Throwable?) -> Unit)? = null
    private var onResumeHandler: (() -> Unit)? = null

    suspend fun pause() {
        if (!mutex.isLocked) {
            mutex.lock(owner)
        }
    }

    suspend fun <T> listen(action: suspend () -> T): T {
        return if (mutex.isLocked) {
            mutex.withLock(owner) {}
            action()
        } else {
            action()
        }
    }

    fun resume() {
        if(mutex.isLocked){
            mutex.unlock(owner)
        }
    }

    fun invokeOnPause(handler: ((cause: Throwable?) -> Unit)?) {
        onPauseHandler = handler
    }

    fun invokeOnResume(handler: (() -> Unit)?) {
        onResumeHandler = handler
    }
}