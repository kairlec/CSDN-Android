package tem.csdn.compose.jetchat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import tem.csdn.compose.jetchat.data.ChatServer

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var uploadConfirm by remember {
                mutableStateOf<Pair<Uri, ByteArray>?>(null)
            }
            var uploadError by remember {
                mutableStateOf<Pair<Int, String?>?>(null)
            }
            var uploadErrorShow by remember {
                mutableStateOf(false)
            }
            var retryEvent by remember {
                mutableStateOf<(() -> Unit)?>(null)
            }
            val context = LocalContext.current
            val chatServer = ChatServer.getCurrent(context)
            when {
                intent?.action == Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        intent.handleSendText {

                        }
                    } else if (intent.type?.startsWith("image/") == true) {
                        intent.handleSendImage {
                            uploadConfirm = it to context.contentResolver.openInputStream(it)!!
                                .use { it.readBytes() }
                        }
                    }
                }
//                intent?.action == Intent.ACTION_SEND_MULTIPLE
//                        && intent.type?.startsWith("image/") == true -> {
//                    intent.handleSendMultipleImages {
//
//                    }
//                }
                else -> {
                    // Handle other intents, such as being started from the home screen
                    startActivity(Intent(this, NavActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    finish()
                }
            }
            if (uploadConfirm != null) {
                SendImageConfirm(
                    chatServer = chatServer,
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
                        { }// 留空表示这个不执行发送,发送交给NavActivity
                    },
                    onSuccess = {
                        startActivity(Intent(this, NavActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            putExtra(SendIntent.IMAGE_URI, uploadConfirm!!.first)
                        })
                        finish()
                    }
                )
            }
            if (uploadErrorShow) {
                if (uploadErrorShow) {
                    RetryDialog(
                        onClose = {
                            uploadErrorShow = false
                            startActivity(Intent(this, NavActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            })
                            finish()
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
            }
        }
    }

    private fun Intent.handleSendText(handle: (String) -> Unit) {
        this.getStringExtra(Intent.EXTRA_TEXT)?.let(handle)
    }

    private fun Intent.handleSendImage(handle: (Uri) -> Unit) {
        (this.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let(handle)
    }

    private fun Intent.handleSendMultipleImages(handle: (Uri) -> Unit) {
        this.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.run {
            forEach {
                (it as? Uri)?.let(handle)
            }
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, NavActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
        finish()
    }
}