package tem.csdn.compose.jetchat.conversation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.imageloading.ImageLoadState
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import tem.csdn.compose.jetchat.R
import tem.csdn.compose.jetchat.util.MediaFileCacheHelper
import tem.csdn.compose.jetchat.util.currentHttpClient
import java.io.InputStream

//@Composable
//fun avatarImage(
//    url: String,
//    authUUID: String,
//    error: @Composable () -> Painter = { painterResource(id = R.drawable.ic_default_avatar_man) }
//): Painter {
//    val glideUrl = GlideUrl(url, LazyHeaders.Builder().apply {
//        addHeader("auth-uuid", authUUID)
//    }.build())
//    val painter =
//        rememberGlidePainter(glideUrl, fadeIn = true, previewPlaceholder = R.drawable.ic_loading)
//    return when (painter.loadState) {
//        is ImageLoadState.Empty -> {
//            painterResource(id = R.drawable.ic_default_avatar_man)
//        }
//        is ImageLoadState.Loading,
//        is ImageLoadState.Success -> {
//            painter
//        }
//        is ImageLoadState.Error -> {
//            error()
//        }
//    }
//}

//@Composable
//fun image(
//    url: String,
//    authUUID: String,
//    error: @Composable () -> Painter = { painterResource(id = R.drawable.ic_broken_cable) }
//): Painter {
//    val glideUrl = GlideUrl(url, LazyHeaders.Builder().apply {
//        addHeader("auth-uuid", authUUID)
//    }.build())
//    val painter =
//        rememberGlidePainter(glideUrl, fadeIn = true, previewPlaceholder = R.drawable.ic_loading)
//    return when (painter.loadState) {
//        ImageLoadState.Empty -> {
//            painterResource(id = R.drawable.ic_broken_cable)
//        }
//        is ImageLoadState.Loading,
//        is ImageLoadState.Success -> {
//            painter
//        }
//        is ImageLoadState.Error -> {
//            error()
//        }
//    }
//}


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CustomImage(
    url: String,
    error: @Composable (Throwable?) -> Unit = {},
    loading: @Composable () -> Unit = {},
    ok: @Composable (Painter) -> Unit
) {
    val status = remember { mutableStateOf<Any?>(null) }
    MainScope().launch(Dispatchers.IO) {
        val result = try {
            MediaFileCacheHelper.current.loadBitmap(url) {
                val response = currentHttpClient().get<HttpResponse>(url)
                response.receive()
            }.asImageBitmap()
        } catch (e: Throwable) {
            e
        }
        withContext(Dispatchers.Main) {
            status.value = result
        }
    }
    when (status.value) {
        is Throwable -> {
            Log.e(
                "CSDN_IMAGE",
                "error to load image:${(status.value as Throwable).localizedMessage}",
                (status.value as Throwable)
            )
            error(status.value as Throwable)
        }
        is ImageBitmap -> {
            ok(BitmapPainter(status.value as ImageBitmap))
        }
        is Painter ->
            ok(status.value as Painter)
        null ->
            loading()
        else ->
            error(null)
    }
}