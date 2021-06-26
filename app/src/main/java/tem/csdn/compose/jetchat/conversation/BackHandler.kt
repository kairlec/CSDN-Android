package tem.csdn.compose.jetchat.conversation

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 这个 [Composable] 可以用 [LocalBackPressedDispatcher] 拦截返回事件
 *
 * @param onBackPressed (Event) 当返回事件拦截的时候要做的事件
 *
 */
@Composable
fun BackPressHandler(onBackPressed: () -> Unit) {
    // 当有新的提供者的时候安全更新 `onBack` lambda
    val currentOnBackPressed by rememberUpdatedState(onBackPressed)

    // 记住在 Composition 中调用 `onBackPressed` lambda 的返回回调
    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentOnBackPressed()
            }
        }
    }

    val backDispatcher = LocalBackPressedDispatcher.current

    // 当有新的调度器的时候设置新的回调
    DisposableEffect(backDispatcher) {
        backDispatcher.addCallback(backCallback)
        // 当离开Composition或者有新的调度器的时候移除回调
        onDispose {
            backCallback.remove()
        }
    }
}

/**
 * This [CompositionLocal] is used to provide an [OnBackPressedDispatcher]:
 *
 * ```
 * CompositionLocalProvider(
 *     LocalBackPressedDispatcher provides requireActivity().onBackPressedDispatcher
 * ) { }
 * ```
 *
 * and setting up the callbacks with [BackPressHandler].
 */
val LocalBackPressedDispatcher =
    staticCompositionLocalOf<OnBackPressedDispatcher> { error("No Back Dispatcher provided") }
