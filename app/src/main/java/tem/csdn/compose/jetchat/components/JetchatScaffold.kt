package tem.csdn.compose.jetchat.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.theme.JetchatTheme

@Composable
fun JetchatScaffold(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    onProfileClicked: (User) -> Unit,
    onChatClicked: () -> Unit,
    chat: ChatDataScreenState,
    profiles: Iterable<User>,
    chatAPI: ChatAPI,
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
                    profiles = profiles,
                    chatAPI = chatAPI
                )
            },
            content = content
        )
    }
}
