package tem.csdn.compose.jetchat.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tem.csdn.compose.jetchat.FunctionalityNotAvailablePopup
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.components.AnimatingFabContent
import tem.csdn.compose.jetchat.components.JetchatAppBar
import tem.csdn.compose.jetchat.components.baselineHeight
import tem.csdn.compose.jetchat.theme.JetchatTheme
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import tem.csdn.compose.jetchat.conversation.CustomImage
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.model.User

@Composable
fun ProfileScreen(
    userData: User,
    meProfile: User,
    chatServer: ChatServer,
    onNavIconPressed: () -> Unit = { }
) {
    var functionalityNotAvailablePopupShown by remember { mutableStateOf(false) }
    if (functionalityNotAvailablePopupShown) {
        FunctionalityNotAvailablePopup { functionalityNotAvailablePopupShown = false }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        JetchatAppBar(
            // Use statusBarsPadding() to move the app bar content below the status bar
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            onNavIconPressed = onNavIconPressed,
            title = { },
            actions = {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    // More icon
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        modifier = Modifier
                            .clickable(onClick = { functionalityNotAvailablePopupShown = true })
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                            .height(24.dp),
                        contentDescription = stringResource(id = R.string.more_options)
                    )
                }
            }
        )
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    ProfileHeader(
                        scrollState,
                        userData,
                        this@BoxWithConstraints.maxHeight,
                        chatServer
                    )
                    UserInfoFields(userData, this@BoxWithConstraints.maxHeight)
                }
            }
            ProfileFab(
                extended = scrollState.value == 0,
                userIsMe = userData.displayId == meProfile.displayId,
                modifier = Modifier.align(Alignment.BottomEnd),
                onFabClicked = { functionalityNotAvailablePopupShown = true }
            )
        }
    }
}

@Composable
private fun UserInfoFields(userData: User, containerHeight: Dp) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))

        NameAndPosition(userData)

        ProfileProperty(stringResource(R.string.display_name), userData.displayName)

        userData.qq?.let {
            ProfileProperty(stringResource(id = R.string.qq), it)
        }
        userData.weChat?.let {
            ProfileProperty(stringResource(id = R.string.wechat), it)
        }
        userData.github?.let {
            ProfileProperty(stringResource(id = R.string.github), it, isLink = true)
        }

        // Add a spacer that always shows part (320.dp) of the fields list regardless of the device,
        // in order to always leave some content at the top.
        Spacer(Modifier.height((containerHeight - 320.dp).coerceAtLeast(0.dp)))
    }
}

@Composable
private fun NameAndPosition(
    userData: User
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Name(
            userData,
            modifier = Modifier.baselineHeight(32.dp)
        )
        Position(
            userData,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .baselineHeight(24.dp)
        )
    }
}

@Composable
private fun Name(userData: User, modifier: Modifier = Modifier) {
    Text(
        text = userData.name,
        modifier = modifier,
        style = MaterialTheme.typography.h5
    )
}

@Composable
private fun Position(userData: User, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
            text = userData.position,
            modifier = modifier,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun ProfileHeader(
    scrollState: ScrollState,
    data: User,
    containerHeight: Dp,
    chatServer: ChatServer,
) {
    val offset = (scrollState.value / 2)
    val offsetDp = with(LocalDensity.current) { offset.toDp() }

    data.getPhotoPainter(chatServer)?.let {
        CustomImage(url = it,
            error = {
                Image(
                    modifier = Modifier
                        .heightIn(max = containerHeight / 2)
                        .fillMaxWidth()
                        .padding(top = offsetDp),
                    painter = painterResource(id = R.drawable.ic_broken_cable),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            },
            loading = {
                Image(
                    modifier = Modifier
                        .heightIn(max = containerHeight / 2)
                        .fillMaxWidth()
                        .padding(top = offsetDp),
                    painter = painterResource(id = R.drawable.ic_loading),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }) {
            Image(
                modifier = Modifier
                    .heightIn(max = containerHeight / 2)
                    .fillMaxWidth()
                    .padding(top = offsetDp),
                painter = it,
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }
    } ?: run {
        Image(
            modifier = Modifier
                .heightIn(max = containerHeight / 2)
                .fillMaxWidth()
                .padding(top = offsetDp),
            painter = painterResource(id = R.drawable.ic_default_avatar_man),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}

@Composable
fun ProfileProperty(label: String, value: String, isLink: Boolean = false) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Divider()
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = label,
                modifier = Modifier.baselineHeight(24.dp),
                style = MaterialTheme.typography.caption
            )
        }
        val style = if (isLink) {
            MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.primary)
        } else {
            MaterialTheme.typography.body1
        }
        Text(
            text = value,
            modifier = Modifier.baselineHeight(24.dp),
            style = style
        )
    }
}

@Composable
fun ProfileError() {
    Text(stringResource(R.string.profile_error))
}

@Composable
fun ProfileFab(
    extended: Boolean,
    userIsMe: Boolean,
    modifier: Modifier = Modifier,
    onFabClicked: () -> Unit = { }
) {
    if (userIsMe) {
        key(userIsMe) { // Prevent multiple invocations to execute during composition
            FloatingActionButton(
                onClick = onFabClicked,
                modifier = modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .height(48.dp)
                    .widthIn(min = 48.dp),
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            ) {
                AnimatingFabContent(
                    icon = {
                        Icon(
                            imageVector = if (userIsMe) Icons.Outlined.Create else Icons.Outlined.Chat,
                            contentDescription = stringResource(
                                if (userIsMe) R.string.edit_profile else R.string.message
                            )
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(
                                id = if (userIsMe) R.string.edit_profile else R.string.message
                            ),
                        )
                    },
                    extended = extended

                )
            }
        }
    }
}

//@Preview(widthDp = 640, heightDp = 360)
//@Composable
//fun ConvPreviewLandscapeMeDefault() {
//    ProvideWindowInsets(consumeWindowInsets = false) {
//        JetchatTheme {
//            ProfileScreen(meProfile, ChatAPI(""))
//        }
//    }
//}
//
//@Preview(widthDp = 360, heightDp = 480)
//@Composable
//fun ConvPreviewPortraitMeDefault() {
//    ProvideWindowInsets(consumeWindowInsets = false) {
//        JetchatTheme {
//            ProfileScreen(meProfile, ChatAPI(""))
//        }
//    }
//}
//
//@Preview(widthDp = 360, heightDp = 480)
//@Composable
//fun ConvPreviewPortraitOtherDefault() {
//    ProvideWindowInsets(consumeWindowInsets = false) {
//        JetchatTheme {
//            ProfileScreen(colleagueProfile,ChatAPI(""))
//        }
//    }
//}

@Preview
@Composable
fun ProfileFabPreview() {
    ProvideWindowInsets(consumeWindowInsets = false) {
        JetchatTheme {
            ProfileFab(extended = true, userIsMe = false)
        }
    }
}
