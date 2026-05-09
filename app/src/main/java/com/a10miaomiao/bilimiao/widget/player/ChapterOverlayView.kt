package com.a10miaomiao.bilimiao.widget.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
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
        textSize = 9f * context.resources.displayMetrics.density
        color = 0xFFFFFFFF.toInt()
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = false
    }
    private val measurePaint = Paint(textPaint)

    var chapters: List<ChapterInfo> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onChapterClick: ((Long) -> Unit)? = null  // 回调传递起始时间（ms）

    private val barHeight = 12f * context.resources.displayMetrics.density
    private val dividerWidth = 2f * context.resources.displayMetrics.density

    init {
        bgPaint.color = 0x73000000  // 半透明黑底
        dividerPaint.color = 0xFF333333.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chapters.isEmpty() || width == 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        // 背景条
        canvas.drawRoundRect(RectF(0f, 0f, w, barHeight), 4f, 4f, bgPaint)

        if (chapters.size <= 1) return

        // 分隔线和标题
        val cl = textPaint.fontMetrics.let { it.descent - it.ascent }
        for (i in 1 until chapters.size) {
            val chap = chapters[i]
            val x = chap.startFraction * w

            // 分隔线
            canvas.drawRect(x, 0f, x + dividerWidth, barHeight, dividerPaint)

            // 标题文字
            val title = chap.title
            if (!title.isNullOrEmpty()) {
                val textWidth = measurePaint.measureText(title)
                val prevStart = chapters[i - 1].startFraction * w
                val segWidth = x - prevStart

                if (segWidth > dividerWidth + 4f) {
                    val cx = prevStart + segWidth / 2
                    val ty = (barHeight + cl) / 2
                    val isOverflow = textWidth > segWidth - 4f
                    if (isOverflow) {
                        val scale = (segWidth - 4f) / textWidth
                        canvas.save()
                        canvas.translate(prevStart + 2f, ty - cl * scale / 2)
                        canvas.scale(scale, scale)
                        canvas.drawText(title, 0f, cl, textPaint)
                        canvas.restore()
                    } else {
                        canvas.drawText(title, cx - textWidth / 2, ty - cl / 2 + cl, textPaint)
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
        if (event.action == MotionEvent.ACTION_UP && chapters.isNotEmpty()) {
            val fraction = event.x / width
            // 找点击位置之后的第一个章节（二分查找）
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
            if (lo < chapters.size) {
                onChapterClick?.invoke(chapters[lo].startMs)
            }
            return true
        }
        return false
    }
}
