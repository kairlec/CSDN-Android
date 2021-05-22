package tem.csdn.compose.jetchat.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import tem.csdn.compose.jetchat.MainViewModel
import tem.csdn.compose.jetchat.theme.JetchatTheme
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ViewWindowInsetObserver
import tem.csdn.compose.jetchat.chat.ChatViewModel
import tem.csdn.compose.jetchat.model.User

class ProfileFragment : Fragment() {
    private val chatViewModel:ChatViewModel by activityViewModels()
    private val viewModel: ProfileViewModel by activityViewModels()
    private val activityViewModel: MainViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Consider using safe args plugin
        val profile = arguments?.getSerializable("profile") as? User?
        viewModel.setProfile(profile)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Create a ViewWindowInsetObserver using this view, and call start() to
        // start listening now. The WindowInsets instance is returned, allowing us to
        // provide it to AmbientWindowInsets in our content below.
        val windowInsets = ViewWindowInsetObserver(this).start()

        setContent {
            val userData by viewModel.userData.observeAsState()
            val chatServer by chatViewModel.chatServer.observeAsState()
            val meProfile by chatViewModel.meProfile.observeAsState()
            CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
                JetchatTheme {
                    if (userData == null) {
                        ProfileError()
                    } else {
                        ProfileScreen(
                            userData = userData!!,
                            onNavIconPressed = {
                                activityViewModel.openDrawer()
                            },
                            chatServer = chatServer!!,
                            meProfile = meProfile!!
                        )
                    }
                }
            }
        }
    }
}
