package tem.csdn.compose.jetchat.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.imageloading.ImageLoadState
import tem.csdn.compose.jetchat.R


@Composable
fun avatarImage(
    url: String,
    error: @Composable () -> Painter = { painterResource(id = R.drawable.ic_default_avatar_man) }
): Painter {
    val painter =
        rememberGlidePainter(url, fadeIn = true, previewPlaceholder = R.drawable.ic_loading)
    return when (painter.loadState) {
        is ImageLoadState.Empty -> {
            painterResource(id = R.drawable.ic_default_avatar_man)
        }
        is ImageLoadState.Loading,
        is ImageLoadState.Success -> {
            painter
        }
        is ImageLoadState.Error -> {
            error()
        }
    }
}

@Composable
fun image(
    url: String,
    error: @Composable () -> Painter = { painterResource(id = R.drawable.ic_broken_cable) }
): Painter {
    val painter =
        rememberGlidePainter(url, fadeIn = true, previewPlaceholder = R.drawable.ic_loading)
    return when (painter.loadState) {
        ImageLoadState.Empty -> {
            painterResource(id = R.drawable.ic_broken_cable)
        }
        is ImageLoadState.Loading,
        is ImageLoadState.Success -> {
            painter
        }
        is ImageLoadState.Error -> {
            error()
        }
    }
}