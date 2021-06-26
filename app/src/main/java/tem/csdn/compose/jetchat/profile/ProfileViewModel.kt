package tem.csdn.compose.jetchat.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tem.csdn.compose.jetchat.model.User

/**
 * 个人信息页面的ViewModel
 * 这个viewmodel控制的是当前显示的是哪一个人的个人信息
 */
class ProfileViewModel : ViewModel() {

    fun setProfile(newProfile: User?) {
        _userData.value = newProfile
    }

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData
}
