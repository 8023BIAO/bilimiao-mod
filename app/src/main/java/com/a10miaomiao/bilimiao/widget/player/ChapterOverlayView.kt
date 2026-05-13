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
 * 绘制时自动应用 padding 以对齐 SeekBar
 */
class ChapterOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 用 DP 固定大小，不随系统字体缩放
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
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

    var onChapterClick: ((Long) -> Unit)? = null

    // 全部用 DP 固定值，不随系统缩放
    private val barHeight = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 14f, context.resources.displayMetrics
    )
    private val dividerWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics
    )
    private val cornerRadius = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 3f, context.resources.displayMetrics
    )

    init {
        bgPaint.color = 0x73000000
        dividerPaint.color = 0xFF666666.toInt()
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chapters.isEmpty() || width == 0) return

        val w = width.toFloat()
        if (w <= 0) return

        // 背景条
        canvas.drawRoundRect(RectF(0f, 0f, w, barHeight), cornerRadius, cornerRadius, bgPaint)

        if (chapters.size <= 1) return

        val cl = textPaint.fontMetrics.let { it.descent - it.ascent }
        val textBaseY = (barHeight + cl) / 2f

        for (i in 0 until chapters.size) {
            val chap = chapters[i]
            val segStart = chap.startFraction * w
            val segEnd = if (i + 1 < chapters.size) {
                chapters[i + 1].startFraction * w
            } else {
                w
            }

            // 分隔线（每个章节起点画一条，不含第一个）
            if (i > 0) {
                canvas.drawRect(segStart, 0f, segStart + dividerWidth, barHeight, dividerPaint)
            }

            // 标题文字（当前分段中间）
            val title = chap.title
            if (!title.isNullOrEmpty()) {
                val textWidth = measurePaint.measureText(title)
                val segWidth = segEnd - segStart - dividerWidth
                if (segWidth > 4f) {
                    val cx = (segStart + segEnd) / 2f
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
        if (chapters.isEmpty() || width == 0) return false

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val fraction = (event.x / width).coerceIn(0f, 1f)
                // 找点击位置对应的章节分段
                val idx = chapters.indices.firstOrNull { i ->
                    chapters[i].startFraction > fraction
                }?.minus(1) ?: (chapters.size - 1)
                if (idx in chapters.indices) {
                    onChapterClick?.invoke(chapters[idx].startMs)
                }
                return true
            }
            MotionEvent.ACTION_DOWN -> return true
            else -> return true
        }
    }
}
