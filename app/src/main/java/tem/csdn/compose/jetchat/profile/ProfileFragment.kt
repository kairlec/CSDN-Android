package tem.csdn.compose.jetchat.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import tem.csdn.compose.jetchat.MainViewModel
import tem.csdn.compose.jetchat.theme.JetchatTheme
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ViewWindowInsetObserver
import com.zxy.tiny.Tiny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.chat.ChatViewModel
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.model.User
import tem.csdn.compose.jetchat.util.sha256
import java.io.File

class ProfileFragment : Fragment() {
    private val chatViewModel: ChatViewModel by activityViewModels()
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
            val meProfile by chatViewModel.meProfile.observeAsState()
            val context = LocalContext.current
            val updateFailedText = stringResource(id = R.string.update_failed)
            var editMode by remember { mutableStateOf(false) }

            CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
                JetchatTheme {
                    var uploadError by remember {
                        mutableStateOf<Pair<Int, String?>?>(null)
                    }
                    var uploadErrorShow by remember {
                        mutableStateOf(false)
                    }

                    var retryEvent: (() -> Unit)? = null

                    // region 丁钰 个人信息图片发送
                    /**
                     * 选择图片的启动器
                     */
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
                                            // 先进行压缩,个人信息页面不允许使用动图
                                            // 所以不做GIF判断,直接进行压缩
                                            val options: Tiny.FileCompressOptions =
                                                Tiny.FileCompressOptions().apply {
                                                    size = 200f
                                                }
                                            Tiny.getInstance().source(it).asFile()
                                                .withOptions(options)
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
                                                                // 对图片获取sha256进行判断
                                                                val data = File(outfile).readBytes()
                                                                val sha256 = data.sha256()
                                                                if (!ChatServer.current.updateImageCheck(
                                                                        ChatServer.current.chatAPI.upc(
                                                                            sha256
                                                                        )
                                                                    )
                                                                ) {
                                                                    // 图片不存在,则调用上传图片
                                                                    val newUser =
                                                                        ChatServer.current.updateProfilePhoto(
                                                                            data,
                                                                            File(outfile).name
                                                                        )
                                                                    chatViewModel.userDao.update(
                                                                        newUser
                                                                    )
                                                                    // 拿到新的newUser,更新viewModel
                                                                    withContext(Dispatchers.Main) {
                                                                        chatViewModel.updateProfile(
                                                                            newUser
                                                                        )
                                                                        viewModel.setProfile(newUser)
                                                                        editMode = false
                                                                    }
                                                                } else {
                                                                    // 图片已经存在,只调用通知sha256
                                                                    val newUser =
                                                                        ChatServer.current.updateProfilePhoto(
                                                                            sha256
                                                                        )
                                                                    chatViewModel.userDao.update(
                                                                        newUser
                                                                    )
                                                                    // 拿到新的newUser,更新viewModel
                                                                    withContext(Dispatchers.Main) {
                                                                        chatViewModel.updateProfile(
                                                                            newUser
                                                                        )
                                                                        viewModel.setProfile(newUser)
                                                                        editMode = false
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
                    // endregion
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
                                TextButton(onClick = { uploadErrorShow = false }) {
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
                            }
                        )
                    }
                    if (userData == null) {
                        ProfileError()
                    } else {
                        ProfileScreen(
                            userData = userData!!,
                            onNavIconPressed = {
                                activityViewModel.openDrawer()
                            },
                            chatServer = ChatServer.current,
                            editMode = editMode,
                            onEditSubmit = { user ->
                                // region 陈卡 更新用户信息按钮点击
                                MainScope().launch(Dispatchers.IO) {
                                    try {
                                        // 构建了新的User,直接调用chatServer封装好的更新函数
                                        val newUser = ChatServer.current.updateProfile(user)
                                        chatViewModel.userDao.update(newUser)
                                        // 更新了之后再更新viewModel
                                        withContext(Dispatchers.Main) {
                                            chatViewModel.updateProfile(newUser)
                                            viewModel.setProfile(newUser)
                                            editMode = false
                                        }
                                    } catch (e: Throwable) {
                                        // 更新失败的toast提示
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                updateFailedText,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                // endregion
                            },
                            onAvatarClick = {
                                launcher.launch("image/*")
                            },
                            onEditModeClick = {
                                editMode = true
                            },
                            meProfile = meProfile!!
                        )
                    }
                }
            }
        }
    }
}
