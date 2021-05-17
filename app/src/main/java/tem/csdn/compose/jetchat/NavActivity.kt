package tem.csdn.compose.jetchat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.ads.identifier.AdvertisingIdClient
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.room.Room
import tem.csdn.compose.jetchat.components.JetchatScaffold
import tem.csdn.compose.jetchat.conversation.BackPressHandler
import tem.csdn.compose.jetchat.conversation.LocalBackPressedDispatcher
import tem.csdn.compose.jetchat.databinding.ContentMainBinding
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tem.csdn.compose.jetchat.chat.ChatAPI
import tem.csdn.compose.jetchat.dao.AppDatabase
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.chatData
import tem.csdn.compose.jetchat.data.colleagueProfile
import tem.csdn.compose.jetchat.data.meProfile
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.client
import tem.csdn.compose.jetchat.util.connectWebSocketToServer

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
            val adIdListener = AdvertisingIdClient.getAdvertisingIdInfo(LocalContext.current)

            var chatDisplayName by mutableStateOf("")
            var onlineNumbers by mutableStateOf(0)
            val chatAPI = ChatAPI("http://localhost:18080")

            GlobalScope.launch(Dispatchers.IO) {
                val adId = adIdListener.get().id
                val inputChannel = Channel<String>(Channel.UNLIMITED)
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "database-csdn-android"
                ).build()
                val messageDao = db.messageDao()
                val lastMessage = messageDao.getLast()
                val userDao = db.userDao()
                val chatServer = ChatServer(chatAPI, adId, client, lastMessage?.id)
                val newMessages = chatServer.messages.map { it.toLocal() }
                messageDao.update(*newMessages.toTypedArray())
                val newProfiles = chatServer.profiles
                userDao.update(*newProfiles.toTypedArray())
                val allUser = userDao.getAll().associateBy { it.displayId }
                val allMessage = messageDao.getAll().map { it.toNonLocal(allUser) }

                withContext(Dispatchers.Main) {
                    chatDisplayName = chatServer.chatDisplayName
                    onlineNumbers = chatServer.count
                    User.meProfile = chatServer.meProfile
                }
                connectWebSocketToServer(
                    port = 18080,
                    path = "/${adId}",
                    inputMessageChannel = inputChannel
                ) {

                }
            }

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
                            val bundle = bundleOf("a" to "b")
                            findNavController().popBackStack(R.id.nav_home, true)
                            scope.launch {
                                scaffoldState.drawerState.close()
                            }
                        },
                        onProfileClicked = {
                            val bundle = bundleOf("profile" to it, "chatAPI" to chatAPI)
                            findNavController().navigate(R.id.nav_profile, bundle)
                            scope.launch {
                                scaffoldState.drawerState.close()
                            }
                        },
                        chat = chatData,
                        profiles = listOf(meProfile, colleagueProfile),
                        chatAPI = chatAPI
                    ) {
                        AndroidViewBinding(ContentMainBinding::inflate)
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
