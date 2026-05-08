package cn.a10miaomiao.bilimiao.compose.common.foundation

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.comm.delegate.player.PlayerSeekBus
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@Stable
sealed class AnnotatedTextNode {
    @Stable
    class Text(val text: String) : AnnotatedTextNode()
    @Stable
    class Emote(
        val text: String,
        val url: String,
//        val width: Int,
//        val height: Int
    ) : AnnotatedTextNode()
    @Stable
    class Link(
        val text: String,
        val url: String,
        val withLineBreak: Boolean = false, // 表示前面要加换行符
    ) : AnnotatedTextNode()
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun inlineAnnotatedContent(
    nodes: List<AnnotatedTextNode>,
    size: TextUnit = 20.sp,
    inlineVerticalAlign: PlaceholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
):  Map<String, InlineTextContent> {
    return nodes.filterIsInstance<AnnotatedTextNode.Emote>().map { node ->
        node.text to InlineTextContent(
            placeholder = Placeholder(
                width = size,
                height = size,
                placeholderVerticalAlign = inlineVerticalAlign,
            ),
            children = {
                GlideImage(
                    model = UrlUtil.autoHttps(node.url),
                    contentDescription = null,
                )
            }
        )
    }.toMap()
}

@Composable
fun annotatedText(
    nodes: List<AnnotatedTextNode>
): AnnotatedString {
    val onSeekTime = LocalOnSeekTime.current
    return buildAnnotatedString {
        nodes.forEach {
            when (it) {
                is AnnotatedTextNode.Text -> append(it.text)
                is AnnotatedTextNode.Emote -> {
                    appendInlineContent(it.text)
                }
                is AnnotatedTextNode.Link -> {
                    if (it.withLineBreak) {
                        append("\n")
                    }
                    // 时间戳链接：直接用 PlayerSeekBus 全局桥接播放器seek
                    if (it.url.startsWith("bilimiao://seek/")) {
                        val seconds = it.url.substringAfter("//seek/").toIntOrNull() ?: 0
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "seek",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = Color(0xFF00A1D6) // 蓝色
                                    )
                                ),
                                linkInteractionListener = LinkInteractionListener {
                                    // 优先用全局bus（任何位置都可用）
                                    PlayerSeekBus.onSeek?.invoke(seconds * 1000L)
                                        // 再用CompositionLocal（视频详情页生效）
                                        ?: onSeekTime?.invoke(seconds)
                                }
                            )
                        ) {
                            append(it.text)
                        }
                    } else {
                        withLink(
                            LinkAnnotation.Url(
                                it.url,
                                TextLinkStyles(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            )
                        ) {
                            append(it.text)
                        }
                    }
                }
            }
        }
    }
}