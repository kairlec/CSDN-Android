package tem.csdn.compose.jetchat.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import tem.csdn.compose.jetchat.theme.JetchatTheme

@Composable
fun JetchatScaffold(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    onProfileClicked: (String) -> Unit,
    onChatClicked: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    JetchatTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                JetchatDrawer(
                    onProfileClicked = onProfileClicked,
                    onChatClicked = onChatClicked
                )
            },
            content = content
        )
    }
}
