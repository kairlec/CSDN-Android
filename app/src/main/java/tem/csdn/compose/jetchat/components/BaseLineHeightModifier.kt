package tem.csdn.compose.jetchat.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * 应用于文本，设置顶部和第一个基线之间的距离
 * 它使元素底部与文本的最后一条基线重合。
 *
 *     _______________
 *     |             |   ↑
 *     |             |   |  基线高度
 *     |Hello, World!|   ↓
 *     ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
 *
 * 此修饰符可用于使用基线之间的特定距离分布多个文本元素
 */
data class BaselineHeightModifier(
    val heightFromBaseline: Dp
) : LayoutModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val textPlaceable = measurable.measure(constraints)
        val firstBaseline = textPlaceable[FirstBaseline]
        val lastBaseline = textPlaceable[LastBaseline]

        val height = heightFromBaseline.roundToPx() + lastBaseline - firstBaseline
        return layout(constraints.maxWidth, height) {
            val topY = heightFromBaseline.roundToPx() - firstBaseline
            textPlaceable.place(0, topY)
        }
    }
}

fun Modifier.baselineHeight(heightFromBaseline: Dp): Modifier =
    this.then(BaselineHeightModifier(heightFromBaseline))
