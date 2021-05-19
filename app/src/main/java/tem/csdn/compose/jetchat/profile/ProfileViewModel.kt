package tem.csdn.compose.jetchat.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.model.User

class ProfileViewModel : ViewModel() {

    private var currentProfile: User = ChatServer.currentChatServer.meProfile

    fun setProfile(newProfile: User?) {
        if (currentProfile != newProfile) {
            currentProfile = newProfile ?: ChatServer.currentChatServer.meProfile
        }
        // Workaround for simplicity
        _userData.value = currentProfile
    }

    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> = _userData
}
