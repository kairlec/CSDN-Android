package tem.csdn.compose.jetchat.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.conversation.avatarImage
import tem.csdn.compose.jetchat.data.colleagueProfile
import tem.csdn.compose.jetchat.data.meProfile
import java.io.Serializable

class ProfileViewModel : ViewModel() {

    private var currentProfile: ProfileScreenState = meProfile

    fun setProfile(newProfile: ProfileScreenState?) {
        if (currentProfile != newProfile) {
            currentProfile = newProfile ?: meProfile
        }
        // Workaround for simplicity
        _userData.value = currentProfile
    }

    private val _userData = MutableLiveData<ProfileScreenState>()
    val userData: LiveData<ProfileScreenState> = _userData
}

@Immutable
data class ProfileScreenState(
    val userId: String,
    val photo: String?,
    val name: String,
    val displayName: String,
    val position: String,
    val github: String?,
    val qq: String?,
    val weChat: String?
) : Serializable {
    companion object {
        private const val serialVersionUID = 47073173576278320L
    }

    fun isMe() = userId == meProfile.userId

    @Composable
    fun getPhotoPainter(): Painter? {
        if (photo == null) {
            return null
        }
        return if (photo.startsWith("http")) {
            avatarImage(url = photo)
        } else {
            photo.toIntOrNull()?.let { painterResource(id = it) }
        }
    }

    @Composable
    fun getPhotoPainterOrDefault(): Painter {
        return getPhotoPainter() ?: painterResource(id = R.drawable.ic_default_avatar_man)
    }
}
