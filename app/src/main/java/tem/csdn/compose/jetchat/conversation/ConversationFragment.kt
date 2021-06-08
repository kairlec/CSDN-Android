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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import tem.csdn.compose.jetchat.MainViewModel
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.theme.JetchatTheme
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ViewWindowInsetObserver
import com.google.accompanist.insets.navigationBarsPadding
import com.zxy.tiny.Tiny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.chat.ChatViewModel
import tem.csdn.compose.jetchat.components.FullScreenDialog
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.RawWebSocketFrameWrapper
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
            val chatServer by chatViewModel.chatServer.observeAsState()
            val meProfile by chatViewModel.meProfile.observeAsState()
            val messages by chatViewModel.allMessages.observeAsState(emptyList())
            val onlineMembers by chatViewModel.onlineMembers.observeAsState(0)
            val online by chatViewModel.webSocketStatus.observeAsState(false)
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
            var retryEvent: (() -> Unit)? = null

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
                                    val options: Tiny.FileCompressOptions =
                                        Tiny.FileCompressOptions().apply {
                                            size = 200f
                                        }
                                    Tiny.getInstance().source(it).asFile().withOptions(options)
                                        .compress { isSuccess, outfile, t ->
                                            Log.d("CSDN_DEBUG_TINY", "outfile=${outfile}")
                                            if (!isSuccess) {
                                                if (t != null) {
                                                    MainScope().launch(Dispatchers.Main) {
                                                        uploadError =
                                                            R.string.image_upload_failed to t.localizedMessage
                                                        uploadErrorShow = true
                                                    }
                                                }
                                            } else {
                                                MainScope().launch(Dispatchers.IO) {
                                                    try {
                                                        val data = File(outfile).readBytes()
                                                        val sha256 = data.sha256()
                                                        if (chatServer!!.updateImageCheck(
                                                                chatServer!!.chatAPI.upc(sha256)
                                                            )
                                                        ) {
                                                            ChatServer.current.send(
                                                                RawWebSocketFrameWrapper.ofImageText(
                                                                    sha256
                                                                )
                                                            )
                                                        } else {
                                                            ChatServer.current.send(
                                                                RawWebSocketFrameWrapper.ofBinary(
                                                                    data
                                                                )
                                                            )
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
                AlertDialog(
                    onDismissRequest = {
                        uploadErrorShow = false
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            uploadErrorShow = false
                            retryEvent?.invoke()
                        }) {
                            Text(text = stringResource(id = R.string.retry))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            uploadErrorShow = false
                        }) {
                            Text(text = stringResource(id = R.string.ok))
                        }
                    },
                    text = {
                        Column {
                            Text(stringResource(id = uploadError!!.first))
                            if (uploadError!!.second != null) {
                                Text(text = uploadError!!.second!!)
                            }
                        }
                    })
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
            if (chatData != null && chatServer != null && meProfile != null) {
                CompositionLocalProvider(
                    LocalBackPressedDispatcher provides requireActivity().onBackPressedDispatcher,
                    LocalWindowInsets provides windowInsets,
                ) {
                    JetchatTheme {
                        ConversationContent(
                            chatData = chatData!!,
                            onlineMembers = onlineMembers,
                            messages = messages,
//                            uiState = ConversationUiState(chatData!!, onlineMembers, messages),
                            navigateToProfile = { user ->
                                // Click callback
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
                            chatServer = chatServer!!,
                            getProfile = {
                                chatViewModel.userDao.getByDisplayId(it)
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
            }
        }
    }
}
