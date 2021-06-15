package tem.csdn.compose.jetchat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.coil.rememberCoilPainter
import com.zxy.tiny.Tiny
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import tem.csdn.compose.jetchat.components.FullScreenDialog
import tem.csdn.compose.jetchat.data.ChatServer
import tem.csdn.compose.jetchat.data.RawWebSocketFrameWrapper
import tem.csdn.compose.jetchat.theme.elevatedSurface
import tem.csdn.compose.jetchat.util.sha256
import java.io.File
import java.io.FileNotFoundException

/**
 * 带UPC(上传图片检查)的发送
 * 这个会首先检查图片的sha256是否在服务器已经存在,如果存在,则发送ImageText消息
 * 否则发送图片内容(原始ByteArray)
 * 注意:如果要压缩图片,需要在此方法调用前进行压缩
 */
suspend inline fun ByteArray.sendWithUPC(chatServer: ChatServer = ChatServer.getInitedCurrent()!!) {
    val sha256 = this.sha256()
    if (chatServer.updateImageCheck(chatServer.chatAPI.upc(sha256))) {
        Log.d("CSDN_DEBUG_UPC", "upc检查存在")
        chatServer.send(RawWebSocketFrameWrapper.ofImageText(sha256))
    } else {
        Log.d("CSDN_DEBUG_UPC", "upc检查不存在")
        chatServer.send(
            RawWebSocketFrameWrapper.ofBinary(this)
        )
    }
}

val tinyInstance: Tiny = Tiny.getInstance()
val options: Tiny.FileCompressOptions =
    Tiny.FileCompressOptions().apply {
        size = 200f
    }

/**
 * 对ByteArray(图片内容)进行压缩
 * @param onError 压缩错误时候的处理
 * @param onSuccess 压缩成功时候的处理
 */
inline fun ByteArray.tiny(
    crossinline onError: (Throwable) -> Unit = {},
    crossinline onSuccess: (String) -> Unit
) {
    tinyInstance.source(this).asFile()
        .withOptions(options)
        .compress { isSuccess, outfile, t ->
            Log.d("CSDN_DEBUG_TINY", "outfile=${outfile}")
            if (!isSuccess) {
                t?.let(onError)
            } else {
                onSuccess(outfile)
            }
        }
}

interface SendImageScope {
    fun send()
}

fun sendImage(context: Context, uri: Uri, onError: (Throwable) -> Unit, onSuccess: () -> Unit) {
    val mime =
        context.contentResolver.getType(uri)
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    if (bytes == null) {
        onError(FileNotFoundException(uri.toString()))
        return
    }
    if (mime.equals("image/gif", true)) {
        MainScope().launch(Dispatchers.IO) {
            try {
                bytes.sendWithUPC()
                onSuccess()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    } else {
        bytes.tiny(onError) {
            MainScope().launch(Dispatchers.IO) {
                try {
                    File(it).readBytes().sendWithUPC()
                    onSuccess()
                } catch (e: Throwable) {
                    onError(e)
                }
            }
        }
    }
}

fun sendImage(
    context: Context,
    uri: Uri,
    bytes: ByteArray,
    onError: (Throwable) -> Unit,
    onSuccess: () -> Unit
) {
    val mime =
        context.contentResolver.getType(uri)
    if (mime.equals("image/gif", true)) {
        MainScope().launch(Dispatchers.IO) {
            try {
                bytes.sendWithUPC()
                onSuccess()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    } else {
        bytes.tiny(onError) {
            MainScope().launch(Dispatchers.IO) {
                try {
                    File(it).readBytes().sendWithUPC()
                    onSuccess()
                } catch (e: Throwable) {
                    onError(e)
                }
            }
        }
    }
}

/**
 * 发送图片确认框
 * 注意:这是一个全屏框,意味着会覆盖显示之后的内容
 */
@Composable
fun SendImageConfirm(
    uploadConfirm: Pair<Uri, ByteArray>,
    chatServer: ChatServer = ChatServer.getInitedCurrent()!!,
    onClose: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onRetryEvent: SendImageScope.() -> (() -> Unit) = { {} },
    onFinish: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = object : SendImageScope {
        override fun send() {
            sendImage(context, uploadConfirm.first, uploadConfirm.second, onError, onSuccess)
        }
    }
    val retryEvent = onRetryEvent(scope)
    FullScreenDialog(onClose = onClose) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberCoilPainter(
                    request = uploadConfirm.second,
                    imageLoader = chatServer.imageLoader
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            val background = if (MaterialTheme.colors.isLight) {
                Color(0xFFE4D0E4)
            } else {
                MaterialTheme.colors.elevatedSurface(2.dp)
            }
            val textStyle =
                MaterialTheme.typography.body1.copy(color = LocalContentColor.current)
            TextButton(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomEnd)
                    .background(background),
                shape = RoundedCornerShape(40),
                onClick = {
                    retryEvent()
                    onFinish()
                }
            ) {
                Text(text = stringResource(id = R.string.send), style = textStyle)
            }
        }
    }
}

@Composable
fun CustomDialog(
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    confirm: @Composable () -> Unit,
    onDismiss: () -> Unit,
    dismiss: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onClose()
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onClose()
            }) {
                confirm()
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onClose()
            }) {
                dismiss()
            }
        },
        text = content
    )
}

@Composable
fun RetryDialog(
    onClose: () -> Unit,
    retryEvent: () -> Unit,
    content: @Composable () -> Unit
) {
    CustomDialog(
        onClose = onClose,
        onConfirm = {
            retryEvent.invoke()
        },
        confirm = {
            Text(text = stringResource(id = R.string.retry))
        },
        onDismiss = {},
        dismiss = {
            Text(text = stringResource(id = R.string.ok))
        },
        content = content
    )
}

object SendIntent {
    const val IMAGE_URI = "tem.csdn.compose.jetchat.intent.image.uri"
}