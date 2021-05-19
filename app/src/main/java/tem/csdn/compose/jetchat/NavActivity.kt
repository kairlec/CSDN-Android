package tem.csdn.compose.jetchat

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import tem.csdn.compose.jetchat.components.JetchatScaffold
import tem.csdn.compose.jetchat.conversation.BackPressHandler
import tem.csdn.compose.jetchat.conversation.LocalBackPressedDispatcher
import tem.csdn.compose.jetchat.databinding.ContentMainBinding
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.*
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.chat.ChatDataScreenState
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.colleagueProfile
import tem.csdn.compose.jetchat.data.meProfile


/**
 * Main activity for the app.
 */
class NavActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val chatServer = ChatServer.currentChatServer
            var progress by remember { mutableStateOf(0.0f) }
            val initAppText = stringResource(id = R.string.init_app)
            var progressText by remember { mutableStateOf(initAppText) }
            var chatDisplayName by mutableStateOf("")
            var onlineNumbers by mutableStateOf(0)
            val composableScope = rememberCoroutineScope()

            composableScope.launch(Dispatchers.IO) {
                while (true) {
                    val receive = chatServer.receiveProgress()
                    withContext(Dispatchers.Main){
                        progress += receive.first
                        if (receive.second != null) {
                            progressText = context.resources.getString(receive.second!!)
                        }
                    }
                    if (progress >= 1f) {
                        chatServer.closeProgress()
                        break
                    }
                }
            }

            Log.d("CSDN_DEBUG", "(main)progress=${progress}")
            if (progress < 1f) {
                Log.d("CSDN_DEBUG", "(main)ready init:${progress}")
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(progress = animatedProgress)
                    Spacer(Modifier.requiredHeight(30.dp))
                    Text(progressText)
                }
            } else {
                val chatData = ChatDataScreenState(
                    chatServer.chatAPI.image(ChatAPI.ImageType.CHAT, "0"),
                    chatServer.chatDisplayName
                )
                val profiles = runBlocking { chatServer.chatData.allUser.receive() }
                // Provide WindowInsets to our content. We don't want to consume them, so that
                // they keep being pass down the view hierarchy (since we're using fragments).
                ProvideWindowInsets(consumeWindowInsets = false) {
                    CompositionLocalProvider(
                        LocalBackPressedDispatcher provides this.onBackPressedDispatcher
                    ) {
                        val scaffoldState = rememberScaffoldState()

                        val drawerOpen by viewModel.drawerShouldBeOpened.collectAsState()
                        if (drawerOpen) {
                            // Open drawer and reset state in VM.
                            LaunchedEffect(Unit) {
                                scaffoldState.drawerState.open()
                                viewModel.resetOpenDrawerAction()
                            }
                        }

                        // Intercepts back navigation when the drawer is open
                        val scope = rememberCoroutineScope()
                        if (scaffoldState.drawerState.isOpen) {
                            BackPressHandler {
                                scope.launch {
                                    scaffoldState.drawerState.close()
                                }
                            }
                        }

                        JetchatScaffold(
                            scaffoldState,
                            onChatClicked = {
                                findNavController().popBackStack(R.id.nav_home, true)
                                scope.launch {
                                    scaffoldState.drawerState.close()
                                }
                            },
                            onProfileClicked = {
                                val bundle =
                                    bundleOf("profile" to it, "chatAPI" to chatServer.chatAPI)
                                findNavController().navigate(R.id.nav_profile, bundle)
                                scope.launch {
                                    scaffoldState.drawerState.close()
                                }
                            },
                            chat = chatData,
                            profiles = profiles.values,
                            chatAPI = chatServer.chatAPI
                        ) {
                            AndroidViewBinding(ContentMainBinding::inflate)
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController().navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * See https://issuetracker.google.com/142847973
     */
    private fun findNavController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }
}
