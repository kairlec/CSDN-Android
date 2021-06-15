package tem.csdn.compose.jetchat.conversation

import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.insets.*
import tem.csdn.compose.jetchat.FunctionalityNotAvailablePopup
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.components.JetchatAppBar
import tem.csdn.compose.jetchat.theme.elevatedSurface
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.RawWebSocketFrameWrapper
import tem.csdn.compose.jetchat.model.LocalMessage
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.theme.JetchatTheme
import tem.csdn.compose.jetchat.util.OkHttpCacheHelper
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

/**
 * Entry point for a conversation screen.
 *
 * @param uiState [ConversationUiState] that contains messages to display
 * @param navigateToProfile User action when navigation to a profile is requested
 * @param modifier [Modifier] to apply to this layout node
 * @param onNavIconPressed Sends an event up when the user clicks on the menu
 */
@Composable
fun ConversationContent(
//    uiState: ConversationUiState,
    chatData: ChatDataScreenState,
    chatServerOffline: Boolean,
    onlineMembers: Int,
    messages: List<LocalMessage>,
    navigateToProfile: (User) -> Unit,
    getProfile: (String) -> User?,
    modifier: Modifier = Modifier,
    chatServer: ChatServer,
    meProfile: User,
    painterClicked: (Painter) -> Unit,
    onImageSelect: () -> Unit,
    onNavIconPressed: () -> Unit = { }
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                var atEvent: ((String) -> Unit)? = null
                Messages(
                    messages = messages,
                    navigateToProfile = navigateToProfile,
                    modifier = Modifier.weight(1f),
                    scrollState = scrollState,
                    chatServer = chatServer,
                    getProfile = getProfile,
                    meProfile = meProfile,
                    painterClicked = painterClicked,
                    atUser = {
                        if (atEvent == null) {
                            Log.e("CSDN_ERROR", "at add null")
                        } else {
                            Log.i("CSDN_INFO", "at add ${it.displayId}")
                        }
                        atEvent?.invoke("@${it.displayId} ")
                    }
                )
                atEvent = userInput(
                    onMessageSent = { content ->
                        runBlocking {
                            chatServer.send(
                                RawWebSocketFrameWrapper.ofText(
                                    content
                                )
                            )
                        }
                    },
                    onImageSelect = onImageSelect,
                    resetScroll = {
                        scope.launch {
                            scrollState.scrollToItem(0)
                        }
                    },
                    // Use navigationBarsWithImePadding(), to move the input panel above both the
                    // navigation bar, and on-screen keyboard (IME)
                    modifier = Modifier.navigationBarsWithImePadding(),
                )
            }
            val channelName = if (chatServerOffline) {
                "${chatData.displayName}(${stringResource(id = R.string.offline)})"
            } else {
                chatData.displayName
            }
            // Channel name bar floats above the messages
            ChannelNameBar(
                channelName = channelName,
                channelMembers = onlineMembers,
                onNavIconPressed = onNavIconPressed,
                // Use statusBarsPadding() to move the app bar content below the status bar
                modifier = Modifier.statusBarsPadding(),
            )
        }
    }
}

@Composable
fun ChannelNameBar(
    channelName: String,
    channelMembers: Int,
    modifier: Modifier = Modifier,
    onNavIconPressed: () -> Unit = { }
) {
    var functionalityNotAvailablePopupShown by remember { mutableStateOf(false) }
    if (functionalityNotAvailablePopupShown) {
        FunctionalityNotAvailablePopup { functionalityNotAvailablePopupShown = false }
    }
    JetchatAppBar(
        modifier = modifier,
        onNavIconPressed = onNavIconPressed,
        title = {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Channel name
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.subtitle1
                )
                // Number of members
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = stringResource(R.string.members, channelMembers),
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        actions = {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                // Search icon
                Icon(
                    imageVector = Icons.Outlined.Search,
                    modifier = Modifier
                        .clickable(onClick = { functionalityNotAvailablePopupShown = true })
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .height(24.dp),
                    contentDescription = stringResource(id = R.string.search)
                )
                // Info icon
                Icon(
                    imageVector = Icons.Outlined.Info,
                    modifier = Modifier
                        .clickable(onClick = { functionalityNotAvailablePopupShown = true })
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .height(24.dp),
                    contentDescription = stringResource(id = R.string.info)
                )
            }
        }
    )
}

const val ConversationTestTag = "ConversationTestTag"

@Composable
fun Messages(
    messages: List<LocalMessage>,
    navigateToProfile: (User) -> Unit,
    atUser: (User) -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    getProfile: (String) -> User?,
    painterClicked: (Painter) -> Unit,
    chatServer: ChatServer,
    meProfile: User
) {
    val today = LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
    val yesterday = today.plusDays(-1)
    val beforeYesterday = yesterday.plusDays(-1)
    val thisYear = today.withDayOfYear(1)
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            // Add content padding so that the content can be scrolled (y-axis)
            // below the status bar + app bar
            // TODO: Get height from somewhere
            contentPadding = rememberInsetsPaddingValues(
                insets = LocalWindowInsets.current.statusBars,
                additionalTop = 90.dp
            ),
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize()
        ) {
            var nextDraw = false
            for (index in messages.indices.reversed()) {
                val prevMessage = messages.getOrNull(index + 1)
                val nextMessage = messages.getOrNull(index - 1)
                val content = messages[index]
                val isFirstMessageByAuthor = prevMessage?.authorDisplayId != content.authorDisplayId
                val isLastMessageByAuthor = nextMessage?.authorDisplayId != content.authorDisplayId

                // Hardcode day dividers for simplicity
                val msgTime =
                    LocalDateTime.ofEpochSecond(
                        content.timestamp.toLong(),
                        0,
                        OffsetDateTime.now().offset
                    )

                fun draw() {
                    item {
                        when {
                            msgTime.isAfter(today) -> {
                                DayHeader(dayString = stringResource(id = R.string.today))
                            }
                            msgTime.isAfter(yesterday) -> {
                                DayHeader(dayString = stringResource(id = R.string.yesterday))
                            }
                            msgTime.isAfter(beforeYesterday) -> {
                                DayHeader(dayString = stringResource(id = R.string.before_yesterday))
                            }
                            msgTime.isAfter(thisYear) -> {
                                DayHeader(
                                    dayString = stringResource(
                                        id = R.string.this_year_format,
                                        msgTime.monthValue,
                                        msgTime.dayOfMonth
                                    )
                                )
                            }
                            else -> {
                                DayHeader(
                                    dayString = stringResource(
                                        id = R.string.previous_year_format,
                                        msgTime.year,
                                        msgTime.monthValue,
                                        msgTime.dayOfMonth
                                    )
                                )
                            }
                        }
                    }
                }
                item {
                    val msgTimeString = if (DateFormat.is24HourFormat(LocalContext.current)) {
                        "%02d:%02d".format(msgTime.hour, msgTime.minute)
                    } else {
                        val ap = if (msgTime.hour >= 12) {
                            stringResource(id = R.string.pm_time_format)
                        } else {
                            stringResource(id = R.string.am_time_format)
                        }
                        val hour = if (msgTime.hour >= 13) {
                            msgTime.hour - 12
                        } else {
                            msgTime.hour
                        }
                        "%s %02d:%02d".format(ap, hour, msgTime.minute)
                    }
                    Message(
                        onAuthorClick = { user -> navigateToProfile(user) },
                        onAuthorLongClick = { user -> atUser(user) },
                        msg = content,
                        isUserMe = content.authorDisplayId == meProfile.displayId,
                        isFirstMessageByAuthor = isFirstMessageByAuthor,
                        isLastMessageByAuthor = isLastMessageByAuthor,
                        msgTimeString = msgTimeString,
                        chatServer = chatServer,
                        getProfile = getProfile,
                        painterClicked = painterClicked
                    )
                }
                if (nextMessage == null) {
                    draw()
                } else {
                    if (nextDraw && nextMessage.authorDisplayId != content.authorDisplayId) {
                        draw()
                        nextDraw = false
                    } else {
                        val nextMsgTime =
                            LocalDateTime.ofEpochSecond(
                                nextMessage.timestamp.toLong(),
                                0,
                                OffsetDateTime.now().offset
                            )
                        if (nextMsgTime.toLocalDate() != msgTime.toLocalDate()) {
                            if (nextMessage.authorDisplayId == content.authorDisplayId) {
                                nextDraw = true
                            } else {
                                draw()
                            }
                        }
                    }
                }
            }
        }
        // Jump to bottom button shows up when user scrolls past a threshold.
        // Convert to pixels:
        val jumpThreshold = with(LocalDensity.current) {
            JumpToBottomThreshold.toPx()
        }

        // Show the button if the first visible item is not the first one or if the offset is
        // greater than the threshold.
        val jumpToBottomButtonEnabled by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 ||
                        scrollState.firstVisibleItemScrollOffset > jumpThreshold
            }
        }

        JumpToBottom(
            // Only show if the scroller is not at the bottom
            enabled = jumpToBottomButtonEnabled,
            onClicked = {
                scope.launch {
                    scrollState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Message(
    onAuthorClick: (User) -> Unit,
    onAuthorLongClick: (User) -> Unit,
    painterClicked: (Painter) -> Unit,
    getProfile: (String) -> User?,
    msg: LocalMessage,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    msgTimeString: String,
    chatServer: ChatServer
) {
    val borderColor = if (isUserMe) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondary
    }
    val author = getProfile(msg.authorDisplayId)!!

    @Composable
    fun Photo() {
        if (isLastMessageByAuthor) {
            // Avatar
            author.photo?.let { chatServer.chatAPI.image(it) }?.let {
                val context = LocalContext.current
                Image(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onAuthorClick(author) },
                            onLongClick = { onAuthorLongClick(author) }
                        )
                        .padding(horizontal = 16.dp)
                        .size(42.dp)
                        .border(1.5.dp, borderColor, CircleShape)
                        .border(3.dp, MaterialTheme.colors.surface, CircleShape)
                        .clip(CircleShape),
                    painter = rememberCoilPainter(
                        request = OkHttpCacheHelper.getCacheFileOrUrl(context, it),
                        imageLoader = chatServer.imageLoader
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            } ?: run {
                Image(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onAuthorClick(author) },
                            onLongClick = { onAuthorLongClick(author) }
                        )
                        .padding(horizontal = 16.dp)
                        .size(42.dp)
                        .border(1.5.dp, borderColor, CircleShape)
                        .border(3.dp, MaterialTheme.colors.surface, CircleShape)
                        .clip(CircleShape),
                    painter = painterResource(id = R.drawable.ic_default_avatar_man),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            }
        } else {
            // Space under avatar
            Spacer(modifier = Modifier.width(74.dp))
        }
    }

    @Composable
    fun AuthorAndTextMessage(modifier: Modifier) {
        AuthorAndTextMessage(
            msg = msg,
            author = author,
            isUserMe = isUserMe,
            chatServer = chatServer,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            authorClicked = onAuthorClick,
            getProfile = getProfile,
            modifier = modifier,
            msgTimeString = msgTimeString,
            painterClicked = painterClicked
        )
    }

    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier

    Row(modifier = spaceBetweenAuthors) {
        if (isUserMe) {
            AuthorAndTextMessage(
                Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            )
            Photo()
        } else {
            Photo()
            AuthorAndTextMessage(
                Modifier
                    .padding(end = 16.dp)
                    .weight(1f)
            )
        }
    }
}

@Composable
fun AuthorAndTextMessage(
    msg: LocalMessage,
    author: User,
    isUserMe: Boolean,
    chatServer: ChatServer,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    authorClicked: (User) -> Unit,
    painterClicked: (Painter) -> Unit,
    getProfile: (String) -> User?,
    modifier: Modifier = Modifier,
    msgTimeString: String
) {
    @Composable
    fun Content() {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(author, isUserMe, msgTimeString)
        }
        ChatItemBubble(
            msg,
            chatServer,
            isFirstMessageByAuthor,
            authorClicked = authorClicked,
            getProfile = getProfile,
            painterClicked = painterClicked,
            isUserMe = isUserMe
        )
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    if (isUserMe) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End
        ) {
            Content()
        }
    } else {
        Column(
            modifier = modifier,
        ) {
            Content()
        }
    }
}

@Composable
private fun AuthorNameTimestamp(
    author: User,
    isUserMe: Boolean,
    msgTimeString: String
) {
    // Combine author and timestamp for a11y.
    @Composable
    fun RowScope.TimeString() {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = msgTimeString,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.alignBy(LastBaseline)
            )
        }
    }

    @Composable
    fun RowScope.AuthorName(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
        )
    }

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        if (isUserMe) {
            TimeString()
            Spacer(modifier = Modifier.width(8.dp))
            //AuthorName("${msg.author.displayName}(${stringResource(id = R.string.author_me)})")
            AuthorName(stringResource(id = R.string.author_me))
        } else {
            AuthorName(author.displayName)
            Spacer(modifier = Modifier.width(8.dp))
            TimeString()
        }
    }
}

private val ChatBubbleShape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp)
private val MeChatBubbleShape = RoundedCornerShape(8.dp, 0.dp, 0.dp, 8.dp)
private val MeLastChatBubbleShape = RoundedCornerShape(8.dp, 0.dp, 8.dp, 8.dp)
private val LastChatBubbleShape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp)

@Composable
fun DayHeader(dayString: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(16.dp)
    ) {
        DayHeaderLine()
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = dayString,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.overline
            )
        }
        DayHeaderLine()
    }
}

@Composable
private fun RowScope.DayHeaderLine() {
    Divider(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
fun ChatItemBubble(
    message: LocalMessage,
    chatServer: ChatServer,
    lastMessageByAuthor: Boolean,
    getProfile: (String) -> User?,
    authorClicked: (User) -> Unit,
    painterClicked: (Painter) -> Unit,
    isUserMe: Boolean
) {
    val backgroundBubbleColor =
        if (MaterialTheme.colors.isLight) {
            if (isUserMe) {
                Color(0xFFD1B3FF)
            } else {
                Color(0xFFF5F5F5)
            }
        } else {
            if (isUserMe) {
                Color(0xFF5C0099)
            } else {
                MaterialTheme.colors.elevatedSurface(2.dp)
            }
        }

    val bubbleShape = if (lastMessageByAuthor) {
        if (isUserMe) {
            MeLastChatBubbleShape
        } else {
            LastChatBubbleShape
        }
    } else {
        if (isUserMe) {
            MeChatBubbleShape
        } else {
            ChatBubbleShape
        }
    }
    Column {
        if (message.image != null) {
            val context = LocalContext.current
            message.image.let { chatServer.chatAPI.image(it) }.let {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = backgroundBubbleColor,
                    shape = bubbleShape
                ) {
                    val painter =
                        rememberCoilPainter(
                            request = OkHttpCacheHelper.getCacheFileOrUrl(context, it),
                            imageLoader = chatServer.imageLoader
                        )
                    Image(
                        painter = painter,
                        modifier = Modifier
                            .widthIn(min = 160.dp, max = 240.dp)
                            .clickable {
                                painterClicked(painter)
                            },
                        contentScale = ContentScale.FillWidth,
                        contentDescription = stringResource(id = R.string.attached_image)
                    )
                }
            }
        } else {
            Surface(
                color = backgroundBubbleColor,
                shape = bubbleShape
            ) {
                ClickableMessage(
                    message = message,
                    getProfile = getProfile,
                    authorClicked = authorClicked
                )
            }
        }
    }
}

@Composable
fun ClickableMessage(
    message: LocalMessage,
    getProfile: (String) -> User?,
    authorClicked: (User) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    val styledMessage = messageFormatter(text = message.content, getProfile)

    ClickableText(
        text = styledMessage,
        style = MaterialTheme.typography.body1.copy(color = LocalContentColor.current),
        modifier = Modifier.padding(8.dp),
        onClick = {
            styledMessage
                .getStringAnnotations(start = it, end = it)
                .firstOrNull()
                ?.let { annotation ->
                    when (annotation.tag) {
                        SymbolAnnotationType.LINK.name -> uriHandler.openUri(annotation.item)
                        SymbolAnnotationType.PERSON.name -> {
                            getProfile(annotation.item)?.let(authorClicked) ?: Unit
                        }
                        else -> Unit
                    }
                }
        }
    )
}

@Preview
@Composable
fun ConversationPreview() {
    val client = HttpClient {
    }
    val context = LocalContext.current
    val meProfile = User("abcd", "Kairlec", "KairlecD", "", null, null, null, null)
    val otherProfile = User("abcde", "KairlecB", "KairlecO", "", null, null, null, null)
    val allProfiles =
        mapOf(meProfile.displayId to meProfile, otherProfile.displayId to otherProfile)
    val messages =
        listOf(
            Message(1, "卧槽", (System.currentTimeMillis() / 1000).toInt(), null, meProfile),
            Message(
                2,
                "长消息卧槽",
                (System.currentTimeMillis() / 1000 + 120).toInt(),
                null,
                meProfile
            ),
            Message(
                3,
                "卧槽O",
                (System.currentTimeMillis() / 1000 + 240).toInt(),
                null,
                otherProfile
            ),
            Message(
                4,
                "长消息卧槽O",
                (System.currentTimeMillis() / 1000 + 360).toInt(),
                null,
                otherProfile
            )
        ).map { it.toLocal() }
    JetchatTheme(isDarkTheme = true) {
        @Suppress("DEPRECATION_ERROR")
        ConversationContent(
            chatData = ChatDataScreenState("SCN105NBTest"),
            chatServerOffline = false,
            onlineMembers = 2,
            messages = messages,
            navigateToProfile = { },
            getProfile = { allProfiles[it] },
            chatServer = ChatServer(
                context,
                ChatAPI(false, "120.77.179.218", 18080),
                "uuid",
                client,
            ),
            meProfile = meProfile,
            painterClicked = {},
            onImageSelect = {},
            onNavIconPressed = {},
            modifier = Modifier.navigationBarsPadding(bottom = false)
        )
    }
}
//
//@Preview
//@Composable
//fun channelBarPrev() {
//    JetchatTheme {
//        ChannelNameBar(channelName = "composers", channelMembers = 52)
//    }
//}
//
//@Preview
//@Composable
//fun DayHeaderPrev() {
//    DayHeader("Aug 6")
//}

private val JumpToBottomThreshold = 56.dp

private fun ScrollState.atBottom(): Boolean = value == 0
