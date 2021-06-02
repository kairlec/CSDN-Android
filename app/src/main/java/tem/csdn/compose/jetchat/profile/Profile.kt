package tem.csdn.compose.jetchat.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import tem.csdn.compose.jetchat.conversation.LoadImage
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.model.User

@Composable
fun ProfileScreen(
    userData: User,
    meProfile: User,
    chatServer: ChatServer?,
    editMode: Boolean = false,
    onEditSubmit: (User) -> Unit,
    onEditModeClick: () -> Unit,
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
        var getNewUser: (() -> User?)? = null
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                ) {
                    if (chatServer != null) {
                        ProfileHeader(
                            scrollState,
                            userData,
                            this@BoxWithConstraints.maxHeight,
                            chatServer
                        )
                    }
                    if (!editMode) {
                        UserInfoFields(userData, this@BoxWithConstraints.maxHeight)
                    } else {
                        val name = remember {
                            mutableStateOf(userData.name)
                        }
                        val displayName = remember {
                            mutableStateOf(userData.displayName)
                        }
                        val position = remember {
                            mutableStateOf(userData.position)
                        }
                        val qq = remember {
                            mutableStateOf(userData.qq ?: "")
                        }
                        val weChat = remember {
                            mutableStateOf(userData.weChat ?: "")
                        }
                        val github = remember {
                            mutableStateOf(userData.github ?: "")
                        }
                        val context = LocalContext.current
                        getNewUser = {
                            when {
                                name.value.isBlank() -> {
                                    Toast.makeText(context, "用户名不能为空", Toast.LENGTH_SHORT).show()
                                    null
                                }
                                displayName.value.isBlank() -> {
                                    Toast.makeText(context, "昵称不能为空", Toast.LENGTH_SHORT).show()
                                    null
                                }
                                else -> {
                                    User(
                                        meProfile.displayId,
                                        name.value,
                                        displayName.value,
                                        position.value,
                                        photo = meProfile.photo,
                                        github.value,
                                        qq.value,
                                        weChat.value
                                    )
                                }
                            }
                        }
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                NameEdit(
                                    name,
                                    modifier = Modifier.baselineHeight(32.dp),
                                    stringResource(id = R.string.please_input_name)
                                )
                                PositionEdit(
                                    position,
                                    modifier = Modifier
                                        .padding(bottom = 20.dp)
                                        .baselineHeight(24.dp),
                                    "${stringResource(id = R.string.please_input_position)}(${
                                        stringResource(
                                            id = R.string.optional
                                        )
                                    })"
                                )
                            }

                            ProfileEditProperty(
                                stringResource(R.string.display_name),
                                displayName,
                                stringResource(id = R.string.please_input_display_name)
                            )

                            ProfileEditProperty(
                                stringResource(id = R.string.qq),
                                qq,
                                "${stringResource(id = R.string.please_input_qq)}(${
                                    stringResource(
                                        id = R.string.optional
                                    )
                                })"
                            )

                            ProfileEditProperty(
                                stringResource(id = R.string.wechat),
                                weChat,
                                "${stringResource(id = R.string.please_input_wechat)}(${
                                    stringResource(
                                        id = R.string.optional
                                    )
                                })"
                            )

                            ProfileEditProperty(
                                stringResource(id = R.string.github),
                                github,
                                "${stringResource(id = R.string.please_input_github)}(${
                                    stringResource(
                                        id = R.string.optional
                                    )
                                })"
                            )

                            // Add a spacer that always shows part (320.dp) of the fields list regardless of the device,
                            // in order to always leave some content at the top.
                            Spacer(
                                Modifier.height(
                                    (this@BoxWithConstraints.maxHeight - 320.dp).coerceAtLeast(
                                        0.dp
                                    )
                                )
                            )
                        }
                    }
                }
            }
            val userIsMe = userData.displayId == meProfile.displayId
            ProfileFab(
                extended = scrollState.value == 0,
                userIsMe = userIsMe,
                modifier = Modifier.align(Alignment.BottomEnd),
                editMode = editMode,
                onFabClicked = {
                    when {
                        editMode -> {
                            getNewUser?.invoke()?.let {
                                onEditSubmit(it)
                            }
                        }
                        userIsMe -> {
                            onEditModeClick()
                        }
                        else -> {
                            functionalityNotAvailablePopupShown = true
                        }
                    }
                }
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
            if (it.isNotBlank()) {
                ProfileProperty(stringResource(id = R.string.qq), it)
            }
        }
        userData.weChat?.let {
            if (it.isNotBlank()) {
                ProfileProperty(stringResource(id = R.string.wechat), it)
            }
        }
        userData.github?.let {
            if (it.isNotBlank()) {
                ProfileProperty(stringResource(id = R.string.github), it, isLink = true)
            }
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
private fun NameEdit(
    text: MutableState<String>,
    modifier: Modifier = Modifier,
    placeholder: String
) {
    TextField(
        value = text.value,
        onValueChange = {
            text.value = it
        },
        modifier = modifier,
        textStyle = MaterialTheme.typography.h5,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        maxLines = 1,
        placeholder = {
            Text(text = placeholder, style = MaterialTheme.typography.h5, modifier = modifier)
        }
    )
}

@Composable
private fun PositionEdit(
    text: MutableState<String>,
    modifier: Modifier = Modifier,
    placeholder: String
) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        TextField(
            value = text.value,
            onValueChange = {
                text.value = it
            },
            modifier = modifier,
            textStyle = MaterialTheme.typography.body1,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            maxLines = 1,
            placeholder = {
                Text(
                    text = placeholder,
                )
            }
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
        LoadImage(url = it,
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
fun ProfileEditProperty(
    label: String,
    value: MutableState<String>,
    placeholder: String
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)) {
        Divider()
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            TextField(
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.caption
                    )
                },
                value = value.value,
                onValueChange = {
                    value.value = it
                },
                modifier = Modifier.baselineHeight(24.dp),
                maxLines = 1,
                textStyle = MaterialTheme.typography.body1,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.body1,
                    )
                }
            )
        }
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
    editMode: Boolean,
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
                val text = when {
                    editMode -> R.string.save_profile
                    userIsMe -> R.string.edit_profile
                    else -> R.string.message
                }
                val icon = when {
                    editMode -> Icons.Outlined.Save
                    userIsMe -> Icons.Outlined.Create
                    else -> Icons.Outlined.Chat
                }
                AnimatingFabContent(
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(text)
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(text),
                        )
                    },
                    extended = extended
                )
            }
        }
    }
}

@Preview
@Composable
fun ConvPreviewLandscapeMeDefault() {
    val meProfile = User("abcd", "Kairlec", "KairlecD", "", null, null, null, null)
    val otherProfile = User("abcde", "KairlecB", "KairlecO", "", null, null, null, null)
    ProvideWindowInsets(consumeWindowInsets = false) {
        JetchatTheme {
            ProfileScreen(meProfile, meProfile, null, true, {}, {}, {})
        }
    }
}

@Preview
@Composable
fun ConvPreviewLandscapeMeDefaultDark() {
    val meProfile = User("abcd", "Kairlec", "KairlecD", "", null, null, null, null)
    val otherProfile = User("abcde", "KairlecB", "KairlecO", "", null, null, null, null)
    ProvideWindowInsets(consumeWindowInsets = false) {
        JetchatTheme(isDarkTheme = true) {
            ProfileScreen(meProfile, meProfile, null, true, {}, {}, {})
        }
    }
}

@Preview
@Composable
fun ConvPreviewLandscapeOtherDefault() {
    val meProfile = User("abcd", "Kairlec", "KairlecD", "", null, null, null, null)
    val otherProfile = User("abcde", "KairlecB", "KairlecO", "", null, null, null, null)
    ProvideWindowInsets(consumeWindowInsets = false) {
        JetchatTheme {
            ProfileScreen(otherProfile, meProfile, null, false, {}, {}, {})
        }
    }
}

@Preview
@Composable
fun ConvPreviewLandscapeMeShowOnly() {
    val meProfile = User("abcd", "Kairlec", "KairlecD", "", null, null, null, null)
    val otherProfile = User("abcde", "KairlecB", "KairlecO", "", null, null, null, null)
    ProvideWindowInsets(consumeWindowInsets = false) {
        JetchatTheme {
            ProfileScreen(meProfile, meProfile, null, false, {}, {}, {})
        }
    }
}

//@Preview(widthDp = 360, heightDp = 480)
//@Composable
//fun ConvPreviewPortraitMeDefault() {
//    ProvideWindowInsets(consumeWindowInsets = false) {
//        JetchatTheme {
//            ProfileScreen(meProfile, ChatAPI(""))
//        }
//    }
//}

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
            ProfileFab(extended = true, userIsMe = true, editMode = true)
        }
    }
}
