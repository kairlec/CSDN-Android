package tem.csdn.compose.jetchat.conversation

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tem.csdn.compose.jetchat.FunctionalityNotAvailablePopup
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.components.JetchatAppBar
import tem.csdn.compose.jetchat.theme.elevatedSurface
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.toPaddingValues
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.RawWebSocketFrameWrapper
import tem.csdn.compose.jetchat.model.Message
import tem.csdn.compose.jetchat.model.User
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
    onlineMembers: Int,
    messages: List<Message>,
    navigateToProfile: (User) -> Unit,
    getProfile: (String) -> User?,
    modifier: Modifier = Modifier,
    chatServer: ChatServer,
    meProfile: User,
    onNavIconPressed: () -> Unit = { }
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Messages(
                    messages = messages,
                    navigateToProfile = navigateToProfile,
                    modifier = Modifier.weight(1f),
                    scrollState = scrollState,
                    chatServer = chatServer,
                    getProfile = getProfile,
                    meProfile = meProfile
                )
                UserInput(
                    onMessageSent = { content ->
                        runBlocking {
                            chatServer.inputChannel.send(
                                RawWebSocketFrameWrapper.ofText(
                                    content
                                )
                            )
                        }
                    },
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
            // Channel name bar floats above the messages
            ChannelNameBar(
                channelName = chatData.displayName,
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
    messages: List<Message>,
    navigateToProfile: (User) -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    getProfile: (String) -> User?,
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
            contentPadding = LocalWindowInsets.current.statusBars.toPaddingValues(
                additionalTop = 90.dp
            ),
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize()
        ) {
            var nextDraw = false
            for (index in messages.indices) {
                val prevMessage = messages.getOrNull(index - 1)
                val nextMessage = messages.getOrNull(index + 1)
                val content = messages[index]
                val prevAuthor = prevMessage?.author
                val nextAuthor = nextMessage?.author
                val isFirstMessageByAuthor = prevAuthor != content.author
                val isLastMessageByAuthor = nextAuthor != content.author

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
                        val (ap, hour) = stringResource(
                            if (msgTime.hour >= 12) {
                                R.string.pm_time_format
                            } else {
                                R.string.am_time_format
                            }
                        ) to msgTime.hour - 12
                        "%s %02d:%02d".format(ap, hour, msgTime.minute)
                    }
                    Message(
                        onAuthorClick = { displayId -> navigateToProfile(displayId) },
                        msg = content,
                        isUserMe = content.author.displayId == meProfile.displayId,
                        isFirstMessageByAuthor = isFirstMessageByAuthor,
                        isLastMessageByAuthor = isLastMessageByAuthor,
                        msgTimeString = msgTimeString,
                        chatServer = chatServer,
                        getProfile = getProfile
                    )
                }
                if (nextMessage == null) {
                    draw()
                } else {
                    if (nextDraw && nextMessage.author.displayId != content.author.displayId) {
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
                            if (nextMessage.author.displayId == content.author.displayId) {
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

@Composable
fun Message(
    onAuthorClick: (User) -> Unit,
    getProfile: (String) -> User?,
    msg: Message,
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

    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier
    Row(modifier = spaceBetweenAuthors) {
        if (isLastMessageByAuthor) {
            // Avatar
            Image(
                modifier = Modifier
                    .clickable(onClick = { onAuthorClick(msg.author) })
                    .padding(horizontal = 16.dp)
                    .size(42.dp)
                    .border(1.5.dp, borderColor, CircleShape)
                    .border(3.dp, MaterialTheme.colors.surface, CircleShape)
                    .clip(CircleShape)
                    .align(Alignment.Top),
                painter = msg.author.getPhotoPainterOrDefault(chatServer),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        } else {
            // Space under avatar
            Spacer(modifier = Modifier.width(74.dp))
        }
        AuthorAndTextMessage(
            msg = msg,
            chatServer = chatServer,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            authorClicked = onAuthorClick,
            getProfile = getProfile,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
            msgTimeString = msgTimeString
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    msg: Message,
    chatServer: ChatServer,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    authorClicked: (User) -> Unit,
    getProfile: (String) -> User?,
    modifier: Modifier = Modifier,
    msgTimeString: String
) {
    Column(modifier = modifier) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(msg, msgTimeString)
        }
        ChatItemBubble(
            msg,
            chatServer,
            isFirstMessageByAuthor,
            authorClicked = authorClicked,
            getProfile = getProfile
        )
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(msg: Message, msgTimeString: String) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = msg.author.displayName,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
        )
        Spacer(modifier = Modifier.width(8.dp))
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = msgTimeString,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.alignBy(LastBaseline)
            )
        }
    }
}

private val ChatBubbleShape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp)
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
    message: Message,
    chatServer: ChatServer,
    lastMessageByAuthor: Boolean,
    getProfile: (String) -> User?,
    authorClicked: (User) -> Unit
) {

    val backgroundBubbleColor =
        if (MaterialTheme.colors.isLight) {
            Color(0xFFF5F5F5)
        } else {
            MaterialTheme.colors.elevatedSurface(2.dp)
        }

    val bubbleShape = if (lastMessageByAuthor) LastChatBubbleShape else ChatBubbleShape
    Column {
        Surface(color = backgroundBubbleColor, shape = bubbleShape) {
            ClickableMessage(
                message = message,
                getProfile = getProfile,
                authorClicked = authorClicked
            )
        }

        message.getImagePainter(chatServer)?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(color = backgroundBubbleColor, shape = bubbleShape) {
                Image(
                    painter = it,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(160.dp),
                    contentDescription = stringResource(id = R.string.attached_image)
                )
            }
        }
    }
}

@Composable
fun ClickableMessage(
    message: Message,
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
//
//@Preview
//@Composable
//fun ConversationPreview() {
//    JetchatTheme {
//        ConversationContent(
//            uiState = exampleUiState,
//            navigateToProfile = { },
//            chatAPI = ChatAPI("")
//        )
//    }
//}
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
