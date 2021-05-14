package tem.csdn.compose.jetchat.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.profile.ProfileScreenState
import tem.csdn.compose.jetchat.theme.JetchatTheme

@Composable
fun JetchatScaffold(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    onProfileClicked: (ProfileScreenState) -> Unit,
    onChatClicked: () -> Unit,
    chat: ChatDataScreenState,
    profiles: Iterable<ProfileScreenState>,
    content: @Composable (PaddingValues) -> Unit,
) {
    JetchatTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                JetchatDrawer(
                    onProfileClicked = onProfileClicked,
                    onChatClicked = onChatClicked,
                    chat = chat,
                    profiles = profiles
                )
            },
            content = content
        )
    }
}
