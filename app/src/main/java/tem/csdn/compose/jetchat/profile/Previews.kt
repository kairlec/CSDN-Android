package tem.csdn.compose.jetchat.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tem.csdn.compose.jetchat.data.colleagueProfile
import tem.csdn.compose.jetchat.data.meProfile
import tem.csdn.compose.jetchat.theme.JetchatTheme

@Preview(widthDp = 340, name = "340 width - Me")
@Composable
fun ProfilePreview340() {
    JetchatTheme {
        ProfileScreen(meProfile)
    }
}

@Preview(widthDp = 480, name = "480 width - Me")
@Composable
fun ProfilePreview480Me() {
    JetchatTheme {
        ProfileScreen(meProfile)
    }
}

@Preview(widthDp = 480, name = "480 width - Other")
@Composable
fun ProfilePreview480Other() {
    JetchatTheme {
        ProfileScreen(colleagueProfile)
    }
}
@Preview(widthDp = 340, name = "340 width - Me - Dark")
@Composable
fun ProfilePreview340MeDark() {
    JetchatTheme(isDarkTheme = true) {
        ProfileScreen(meProfile)
    }
}

@Preview(widthDp = 480, name = "480 width - Me - Dark")
@Composable
fun ProfilePreview480MeDark() {
    JetchatTheme(isDarkTheme = true) {
        ProfileScreen(meProfile)
    }
}

@Preview(widthDp = 480, name = "480 width - Other - Dark")
@Composable
fun ProfilePreview480OtherDark() {
    JetchatTheme(isDarkTheme = true) {
        ProfileScreen(colleagueProfile)
    }
}
