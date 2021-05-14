package tem.csdn.compose.jetchat.data

import tem.csdn.compose.jetchat.profile.ProfileScreenState

object OnlineData {
    fun getProfile(userId: String): ProfileScreenState {
        return if (userId == meProfile.userId) {
            meProfile
        } else {
            colleagueProfile
        }
    }

    fun getProfileOrNull(userId: String): ProfileScreenState? {
        return if (userId == meProfile.userId) {
            meProfile
        } else {
            colleagueProfile
        }
    }
}