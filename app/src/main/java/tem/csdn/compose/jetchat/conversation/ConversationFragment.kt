package tem.csdn.compose.jetchat.conversation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ViewWindowInsetObserver
import com.google.accompanist.insets.navigationBarsPadding
import com.zxy.tiny.Tiny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tem.csdn.compose.jetchat.*
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.chat.ChatViewModel
import tem.csdn.compose.jetchat.components.FullScreenDialog
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.RawWebSocketFrameWrapper
import tem.csdn.compose.jetchat.theme.JetchatTheme
import tem.csdn.compose.jetchat.theme.elevatedSurface
import tem.csdn.compose.jetchat.util.sha256
import java.io.File


class ConversationFragment : Fragment() {
    private val chatViewModel: ChatViewModel by activityViewModels()
    private val activityViewModel: MainViewModel by activityViewModels()

//    private var systemUiVisible: Boolean = true

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        requireActivity().window.addSystemUIVisibilityListener {
//            systemUiVisible = it
//        }
//    }

//    private fun toggleSystemUi() {
//        if (systemUiVisible) {
//            Log.d("CSDN_DEBUG", "hide systemui")
//            requireActivity().hideSystemUI()
//        } else {
//            Log.d("CSDN_DEBUG", "show systemui")
//            requireActivity().showSystemUI()
//        }
//    }

    @OptIn(ExperimentalAnimatedInsets::class) // Opt-in to experiment animated insets support
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

        // Create a ViewWindowInsetObserver using this view, and call start() to
        // start listening now. The WindowInsets instance is returned, allowing us to
        // provide it to AmbientWindowInsets in our content below.
        val windowInsets = ViewWindowInsetObserver(this)
            // We use the `windowInsetsAnimationsEnabled` parameter to enable animated
            // insets support. This allows our `ConversationContent` to animate with the
            // on-screen keyboard (IME) as it enters/exits the screen.
            .start(windowInsetsAnimationsEnabled = true)
        setContent {
            val chatData by chatViewModel.chatData.observeAsState()
            val chatServer = ChatServer.getCurrent()
            val meProfile by chatViewModel.meProfile.observeAsState()
            val messages by chatViewModel.allMessages.observeAsState(emptyList())
            val onlineMembers by chatViewModel.onlineMembers.observeAsState(0)
            val online by chatViewModel.webSocketStatus.observeAsState(false)
            val allProfiles by chatViewModel.allProfiles.observeAsState()
            Log.d("CSDN_CON", "chatData=${chatData}")
            Log.d("CSDN_CON", "chatServer=${chatServer}")
            Log.d("CSDN_CON", "meProfile=${meProfile}")
            Log.d("CSDN_CON", "messages=${messages}")
            Log.d("CSDN_CON", "onlineMembers=${onlineMembers}")

            var uploadError by remember {
                mutableStateOf<Pair<Int, String?>?>(null)
            }
            var uploadErrorShow by remember {
                mutableStateOf(false)
            }
            var retryEvent by remember {
                mutableStateOf<(() -> Unit)?>(null)
            }

            var uploadConfirm by remember {
                mutableStateOf<Pair<Uri, ByteArray>?>(null)
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    retryEvent = {
                        MainScope().launch(Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use {
                                    it.readBytes()
                                }?.let {
                                    withContext(Dispatchers.Main) {
                                        uploadConfirm = uri to it
                                    }
                                } ?: run {
                                    withContext(Dispatchers.Main) {
                                        uploadError = R.string.file_not_found to null
                                        uploadErrorShow = true
                                    }
                                }
                            } catch (e: Throwable) {
                                withContext(Dispatchers.Main) {
                                    uploadError =
                                        R.string.image_upload_failed to e.localizedMessage
                                    uploadErrorShow = true
                                }
                            }
                        }
                    }
                    retryEvent!!.invoke()
                }
            }


            if (uploadErrorShow) {
                RetryDialog(
                    onClose = {
                        uploadErrorShow = false
                    },
                    retryEvent = {
                        retryEvent?.invoke()
                    },
                    content = {
                        Column {
                            Text(stringResource(id = uploadError!!.first))
                            if (uploadError!!.second != null) {
                                Text(text = uploadError!!.second!!)
                            }
                        }
                    }
                )
            }

            var fullScreenShowBitmap by remember {
                mutableStateOf<Painter?>(null)
            }

            if (fullScreenShowBitmap != null) {
//                toggleSystemUi()
                FullScreenDialog(onClose = {
                    fullScreenShowBitmap = null
//                    toggleSystemUi()
                }) {
                    if (fullScreenShowBitmap != null) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = fullScreenShowBitmap!!,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        fullScreenShowBitmap = null
                                    }
                            )
                        }
                    }
                }
            }
            if (uploadConfirm != null) {
                SendImageConfirm(
                    uploadConfirm = uploadConfirm!!,
                    onClose = {
                        uploadConfirm = null
                    },
                    onFinish = {
                        uploadConfirm = null
                    },
                    onError = {
                        MainScope().launch(Dispatchers.Main) {
                            uploadError =
                                R.string.image_upload_failed to it.localizedMessage
                            uploadErrorShow = true
                        }
                    },
                    onRetryEvent = {
                        retryEvent = this::send
                        this::send
                    }
                )
            }

            if (chatData != null && meProfile != null) {
                CompositionLocalProvider(
                    LocalBackPressedDispatcher provides requireActivity().onBackPressedDispatcher,
                    LocalWindowInsets provides windowInsets,
                ) {
                    JetchatTheme {
                        ConversationContent(
                            chatData = chatData!!,
                            onlineMembers = onlineMembers,
                            messages = messages,
                            navigateToProfile = { user ->
                                val bundle = bundleOf(
                                    "profile" to user
                                )
                                findNavController().navigate(
                                    R.id.nav_profile,
                                    bundle
                                )
                            },
                            onNavIconPressed = {
                                activityViewModel.openDrawer()
                            },
                            // Add padding so that we are inset from any left/right navigation bars
                            // (usually shown when in landscape orientation)
                            modifier = Modifier.navigationBarsPadding(bottom = false),
                            chatServer = chatServer,
                            getProfile = {
                                allProfiles?.get(it)
                            },
                            meProfile = meProfile!!,
                            chatServerOffline = !online,
                            onImageSelect = {
                                launcher.launch("image/*")
                            },
                            painterClicked = {
                                fullScreenShowBitmap = it
                            }
                        )
                    }
                }
            } else {
                val errorString = buildString {
                    if (chatData == null) {
                        append(" chatData -> null")
                    }
                    if (meProfile == null) {
                        append(" meProfile -> null")
                    }
                }
                Log.e("CSDN_ERROR", "failed to load conversationFragment because:${errorString}")
                CompositionLocalProvider(
                    LocalBackPressedDispatcher provides requireActivity().onBackPressedDispatcher,
                    LocalWindowInsets provides windowInsets,
                ) {
                    JetchatTheme {
                        Text("failed to load conversationFragment because:${errorString}")
                    }
                }
            }
        }
    }
}
