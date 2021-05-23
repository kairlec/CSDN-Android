package tem.csdn.compose.jetchat.conversation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import tem.csdn.compose.jetchat.MainViewModel
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.theme.JetchatTheme
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ViewWindowInsetObserver
import com.google.accompanist.insets.navigationBarsPadding
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.chat.ChatViewModel
import tem.csdn.compose.jetchat.data.ChatServer

class ConversationFragment : Fragment() {
    private val chatViewModel: ChatViewModel by activityViewModels()
    private val activityViewModel: MainViewModel by activityViewModels()

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
                                chatServer!!.userDao.getByDisplayId(it)
                            },
                            meProfile = meProfile!!,
                            chatServerOffline = !online
                        )
                    }
                }
            }
        }
    }
}
