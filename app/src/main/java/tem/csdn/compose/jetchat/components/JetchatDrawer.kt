package tem.csdn.compose.jetchat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tem.csdn.compose.jetchat.R
import com.google.accompanist.insets.statusBarsHeight
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.conversation.LoadImage
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.model.User

@Composable
fun ColumnScope.JetchatDrawer(
    onProfileClicked: (User) -> Unit,
    onChatClicked: () -> Unit,
    chat: ChatDataScreenState,
    chatServer: ChatServer,
    chatServerOffline: Boolean,
    profiles: Iterable<User>,
    meProfile: User,
) {
    // Use statusBarsHeight() to add a spacer which pushes the drawer content
    // below the status bar (y-axis)
    Spacer(Modifier.statusBarsHeight())
    DrawerHeader()
    Divider()
    val channelName = if (chatServerOffline) {
        "${chat.displayName}(${stringResource(id = R.string.offline)})"
    } else {
        chat.displayName
    }
    DrawerItemHeader(stringResource(id = R.string.chat_header))
    ChatItem(channelName, true, chat.getPhotoPainter(chatServer = chatServer)) {
        onChatClicked()
    }
    DrawerItemHeader(stringResource(id = R.string.profile_header))
    ProfileItem(
        text = "${meProfile.displayName}(${stringResource(id = R.string.author_me)})",
        profilePic = meProfile.getPhotoPainter(chatServer)
    ) {
        onProfileClicked(meProfile)
    }
    profiles.forEach {
        if (it.displayId != meProfile.displayId) {
            ProfileItem(text = it.displayName, profilePic = it.getPhotoPainter(chatServer)) {
                onProfileClicked(it)
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_jetchat),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.jetchat_logo),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun DrawerItemHeader(text: String) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(text, style = MaterialTheme.typography.caption, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ChatItem(
    text: String,
    selected: Boolean,
    chatPhoto: String?,
    onChatClicked: () -> Unit
) {
    val background = if (selected) {
        Modifier.background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(background)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onChatClicked),
        verticalAlignment = CenterVertically
    ) {
        val iconTint = if (selected) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        }
        if (chatPhoto == null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_jetchat),
                tint = iconTint,
                modifier = Modifier.padding(8.dp),
                contentDescription = null
            )
        } else {
            LoadImage(
                url = chatPhoto,
                loading = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_loading),
                        tint = iconTint,
                        modifier = Modifier.padding(8.dp),
                        contentDescription = null
                    )
                },
                error = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_broken_cable),
                        tint = iconTint,
                        modifier = Modifier.padding(8.dp),
                        contentDescription = null
                    )
                }
            ) {
                Icon(
                    painter = it,
                    tint = iconTint,
                    modifier = Modifier.padding(8.dp),
                    contentDescription = null
                )
            }
        }
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text,
                style = MaterialTheme.typography.body2,
                color = if (selected) MaterialTheme.colors.primary else LocalContentColor.current,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ProfileItem(text: String, profilePic: String?, onProfileClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onProfileClicked),
        verticalAlignment = CenterVertically
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            val widthPaddingModifier = Modifier
                .padding(8.dp)
                .size(24.dp)
            if (profilePic != null) {
                LoadImage(
                    url = profilePic,
                    error = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_broken_cable),
                            modifier = widthPaddingModifier.then(Modifier.clip(CircleShape)),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    },
                    loading = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_loading),
                            modifier = widthPaddingModifier.then(Modifier.clip(CircleShape)),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }
                ) {
                    Image(
                        painter = it,
                        modifier = widthPaddingModifier.then(Modifier.clip(CircleShape)),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }
            } else {
//                Spacer(modifier = widthPaddingModifier)
                Image(
                    painter = painterResource(id = R.drawable.ic_default_avatar_man),
                    modifier = widthPaddingModifier.then(Modifier.clip(CircleShape)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }
            Text(text, style = MaterialTheme.typography.body2, modifier = Modifier.padding(8.dp))
        }
    }
}
//
//@Composable
//@Preview
//fun DrawerPreview() {
//    JetchatTheme {
//        Surface {
//            Column {
//                JetchatDrawer(
//                    {},
//                    {},
//                    chatData,
//                    ChatAPI(""),
//                    listOf(meProfile, colleagueProfile)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//@Preview
//fun DrawerPreviewDark() {
//    JetchatTheme(isDarkTheme = true) {
//        Surface {
//            Column {
//                JetchatDrawer({}, {}, chatData, ChatAPI(""), listOf(meProfile, colleagueProfile))
//            }
//        }
//    }
//}
