package tem.csdn.compose.jetchat.conversation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import tem.csdn.compose.jetchat.util.MediaFileCacheHelper
import tem.csdn.compose.jetchat.util.currentHttpClient

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LoadImage(
    url: String,
    error: @Composable (Throwable?) -> Unit = {},
    loading: @Composable () -> Unit = {},
    ok: @Composable (Painter) -> Unit
) {
    val status = remember { mutableStateOf<Any?>(null) }
    MainScope().launch(Dispatchers.IO) {
        val result = try {
            MediaFileCacheHelper.current.loadCache(url) {
                val response = currentHttpClient().get<HttpResponse>(url)
                response.receive()
            }
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
        is Bitmap -> {
            ok(BitmapPainter((status.value as Bitmap).asImageBitmap()))
        }
        is Painter -> {
            ok(status.value as Painter)
        }
        null ->
            loading()
        else ->
            error(null)
    }
}