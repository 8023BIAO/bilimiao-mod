package com.a10miaomiao.bilimiao.widget.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

/**
 * 在进度条上方绘制视频章节标记
 * 参考 PiliPlus 的 ViewPointSegmentProgressBar
 */
class ChapterOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 11f, context.resources.displayMetrics
        )
        color = 0xFFFFFFFF.toInt()
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = true
    }
    private val measurePaint = Paint(textPaint)

    var chapters: List<ChapterInfo> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onChapterClick: ((Long) -> Unit)? = null  // 回调传递起始时间（ms）

    private val barHeight = 20f * context.resources.displayMetrics.density
    private val dividerWidth = 2f * context.resources.displayMetrics.density
    private val cornerRadius = 4f * context.resources.displayMetrics.density

    init {
        bgPaint.color = 0x73000000  // 半透明黑底
        dividerPaint.color = 0xFF555555.toInt()
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chapters.isEmpty() || width == 0) return

        val w = width.toFloat()
        val padding = paddingLeft.toFloat()

        // 背景条
        canvas.drawRoundRect(RectF(0f, 0f, w, barHeight), cornerRadius, cornerRadius, bgPaint)

        if (chapters.size <= 1) return

        val cl = textPaint.fontMetrics.let { it.descent - it.ascent }
        // 文字垂直居中
        val textBaseY = (barHeight + cl) / 2f

        for (i in 0 until chapters.size) {
            val chap = chapters[i]
            val segStart = chap.startFraction * w
            val segEnd = if (i + 1 < chapters.size) {
                chapters[i + 1].startFraction * w
            } else {
                w // 最后一个分段延伸到右边界
            }

            // 分隔线（每个章节起点画一条，不含第一个）
            if (i > 0) {
                canvas.drawRect(segStart, 0f, segStart + dividerWidth, barHeight, dividerPaint)
            }

            // 标题文字（每个分段中间）
            val title = chap.title
            if (!title.isNullOrEmpty()) {
                val textWidth = measurePaint.measureText(title)
                val segWidth = segEnd - segStart - dividerWidth
                if (segWidth > 4f) {
                    val cx = segStart + (segEnd - segStart) / 2f
                    val isOverflow = textWidth > segWidth
                    if (isOverflow) {
                        val scale = (segWidth / textWidth).coerceAtMost(1f)
                        canvas.save()
                        canvas.translate(segStart + 2f, textBaseY - cl * scale / 2f)
                        canvas.scale(scale, scale)
                        canvas.drawText(title, 0f, cl, textPaint)
                        canvas.restore()
                    } else {
                        canvas.drawText(title, cx - textWidth / 2f, textBaseY, textPaint)
                    }
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            barHeight.toInt()
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (chapters.isEmpty()) return false
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val adjustedX = event.x - paddingLeft
                val drawWidth = width - paddingLeft - paddingRight
                if (drawWidth <= 0) return true
                val fraction = (adjustedX / drawWidth).coerceIn(0f, 1f)
                // 找点击位置之后（或等于）的第一个章节
                var lo = 0
                var hi = chapters.size
                while (lo < hi) {
                    val mid = (lo + hi) / 2
                    if (chapters[mid].startFraction <= fraction) {
                        lo = mid + 1
                    } else {
                        hi = mid
                    }
                }
                if (lo > 0) lo-- // 落在分段内就跳这个分段的起点，否则跳下一个
                if (lo in chapters.indices) {
                    onChapterClick?.invoke(chapters[lo].startMs)
                }
                return true
            }
            MotionEvent.ACTION_DOWN -> return true
            else -> return true
        }
    }
}
