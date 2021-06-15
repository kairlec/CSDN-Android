package tem.csdn.compose.jetchat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.launch
import tem.csdn.compose.jetchat.chat.ChatViewModel
import tem.csdn.compose.jetchat.components.JetchatScaffold
import tem.csdn.compose.jetchat.conversation.BackPressHandler
import tem.csdn.compose.jetchat.conversation.LocalBackPressedDispatcher
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.databinding.ContentMainBinding
import tem.csdn.compose.jetchat.theme.JetchatTheme


/**
 * Main activity for the app.
 */
class NavActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getParcelableExtra<Parcelable>(SendIntent.IMAGE_URI)?.let { it as? Uri }?.let {
            sendImage(this, it, {}, {})
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            chatViewModel.initIfNeed(context)
            val chatData by chatViewModel.chatData.observeAsState()
            val meProfile by chatViewModel.meProfile.observeAsState()
            val progress by chatViewModel.initProgress.observeAsState(initial = 0f)
            val progressTextId by
            chatViewModel.initProgressTextId.observeAsState(initial = R.string.init_app)
            val profiles by chatViewModel.allProfiles.observeAsState(initial = emptyMap())
            Log.d("CSDN_DEBUG", "(main)progress=${progress}")
            if (progress < 1f || chatData == null) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                )
                JetchatTheme {
                    Loading(animatedProgress, stringResource(id = progressTextId))
                }
            } else {
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

                        val chatServer = ChatServer.getCurrent()
                        val chatServerOnline by chatViewModel.webSocketStatus.observeAsState(false)
                        Log.d("CSDN_CON", "main:chatServer=${chatServer}")
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
                                    bundleOf("profile" to it)
                                findNavController().navigate(R.id.nav_profile, bundle)
                                scope.launch {
                                    scaffoldState.drawerState.close()
                                }
                            },
                            chat = chatData!!,
                            profiles = profiles.values,
                            chatServer = chatServer,
                            chatServerOffline = !chatServerOnline,
                            meProfile = meProfile!!
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

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }
}

@Preview
@Composable
fun LoadingPreview() {
    JetchatTheme(isDarkTheme = true) {
        Loading(progress = 0.5f, text = stringResource(id = R.string.wait_for_server_sync))
    }
}

@Composable
fun Loading(progress: Float, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(progress = progress)
        Spacer(Modifier.requiredHeight(30.dp))
        Text(
            color = if (MaterialTheme.colors.isLight) {
                Color.Unspecified
            } else {
                Color(0xFFDEDEDE)
            },
            text = text
        )
    }
}