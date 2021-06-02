package tem.csdn.compose.jetchat.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tem.csdn.compose.jetchat.model.User

class ProfileViewModel : ViewModel() {

    fun setProfile(newProfile: User?) {
        _userData.value = newProfile
    }

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData
}
