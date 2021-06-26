package tem.csdn.compose.jetchat.conversation

import android.util.Log
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import tem.csdn.compose.jetchat.model.User

//region 待定 消息样式匹配

// 正则表达式匹配这些语法符号
val symbolPattern by lazy {
    Regex("""(https?://[^\s\t\n]+)|(`[^`]+`)|(@[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})|(\*[\w]+\*)|(_[\w]+_)|(~[\w]+~)""")
}

// ClickableTextWrapper所接受的类型
enum class SymbolAnnotationType {
    PERSON, LINK
}

typealias StringAnnotation = AnnotatedString.Range<String>
// 匹配语法标记时，为 ClickableText 配对返回样式内容和注释
typealias SymbolAnnotation = Pair<AnnotatedString, StringAnnotation?>

/**
 * 轻量级的Markdown格式化消息
 * | @displayId -> 加粗,使用primary颜色并标记可点击
 * | http(s)://... -> 可点击的链接,可以在浏览器里打开
 * | *bold* -> 粗体
 * | _italic_ -> 删除线
 * | ~strikethrough~ -> 斜体
 * | `MyClass.myMethod` -> 行内代码样式
 *
 * @param text 包含要解析的内容消息
 * @return 带有 ClickableTextWrapper 注释的 AnnotatedString
 */
@Composable
fun messageFormatter(
    text: String,
    getProfile: (String) -> User?
): AnnotatedString {
    val tokens = symbolPattern.findAll(text)

    return buildAnnotatedString {

        var cursorPosition = 0

        val codeSnippetBackground =
            if (MaterialTheme.colors.isLight) {
                Color(0xFFDEDEDE)
            } else {
                Color(0xFF424242)
            }

        for (token in tokens) {
            append(text.slice(cursorPosition until token.range.first))

            val (annotatedString, stringAnnotation) = getSymbolAnnotation(
                matchResult = token,
                colors = MaterialTheme.colors,
                codeSnippetBackground = codeSnippetBackground,
                getProfile = getProfile
            )
            append(annotatedString)

            if (stringAnnotation != null) {
                val (item, start, end, tag) = stringAnnotation
                addStringAnnotation(tag = tag, start = start, end = end, annotation = item)
            }

            cursorPosition = token.range.last + 1
        }

        if (!tokens.none()) {
            append(text.slice(cursorPosition..text.lastIndex))
        } else {
            append(text)
        }
    }
}

/**
 * 使用支持的语法符号映射在消息中找到的正则表达式匹配
 *
 * @param matchResult 与语法符号匹配的正则表达式结果
 * @return 在 ClickableTextWrapper 中使用的带注释（可选）的一对 AnnotatedString
 */
private fun getSymbolAnnotation(
    matchResult: MatchResult,
    colors: Colors,
    codeSnippetBackground: Color,
    getProfile: (String) -> User?
): SymbolAnnotation {
    return when (matchResult.value.first()) {
        '@' -> {
            Log.d("CSDN_DEBUG", "@value=${matchResult.value}")
            val userDisplayId = matchResult.value.substring(1)/*.let {
                if (it.length > 36) {
                    it.substring(0, 36)
                } else {
                    it
                }
            }*/
            val profile = getProfile(userDisplayId)
            if (profile == null) {
                SymbolAnnotation(AnnotatedString(matchResult.value), null)
            } else {
                SymbolAnnotation(
                    AnnotatedString(
                        text = " @${profile.displayName} ",
                        spanStyle = SpanStyle(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ),
                    StringAnnotation(
                        item = userDisplayId,
                        start = matchResult.range.first,
                        end = matchResult.range.first + profile.displayName.length,
                        tag = SymbolAnnotationType.PERSON.name
                    )
                )
            }
        }
        '*' -> SymbolAnnotation(
            AnnotatedString(
                text = matchResult.value.trim('*'),
                spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
            ),
            null
        )
        '_' -> SymbolAnnotation(
            AnnotatedString(
                text = matchResult.value.trim('_'),
                spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
            ),
            null
        )
        '~' -> SymbolAnnotation(
            AnnotatedString(
                text = matchResult.value.trim('~'),
                spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
            ),
            null
        )
        '`' -> SymbolAnnotation(
            AnnotatedString(
                text = matchResult.value.trim('`'),
                spanStyle = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    background = codeSnippetBackground,
                    baselineShift = BaselineShift(0.2f)
                )
            ),
            null
        )
        'h' -> SymbolAnnotation(
            AnnotatedString(
                text = matchResult.value,
                spanStyle = SpanStyle(
                    color = colors.primary
                )
            ),
            StringAnnotation(
                item = matchResult.value,
                start = matchResult.range.first,
                end = matchResult.range.last,
                tag = SymbolAnnotationType.LINK.name
            )
        )
        else -> SymbolAnnotation(AnnotatedString(matchResult.value), null)
    }
}
//endregion