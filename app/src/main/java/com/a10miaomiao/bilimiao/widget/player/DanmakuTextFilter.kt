package com.a10miaomiao.bilimiao.widget.player

import master.flame.danmaku.controller.DanmakuFilters
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.android.DanmakuContext

/**
 * 弹幕关键词过滤
 * 根据配置的关键词列表过滤弹幕
 */
class DanmakuTextFilter : DanmakuFilters.BaseDanmakuFilter<Set<String>>() {

    private val keywords = mutableSetOf<String>()
    private val regexPatterns = mutableListOf<Regex>()

    override fun filter(
        danmaku: BaseDanmaku,
        index: Int,
        totalsizeInScreen: Int,
        timer: DanmakuTimer?,
        fromCachingTask: Boolean,
        config: DanmakuContext?
    ): Boolean {
        if (keywords.isEmpty()) return false
        val text = danmaku.text?.toString() ?: return false

        // 先按正则匹配
        for (pattern in regexPatterns) {
            if (pattern.containsMatchIn(text)) {
                danmaku.mFilterParam = danmaku.mFilterParam or (1 shl 20)
                return true
            }
        }

        // 再按关键词匹配
        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true)) {
                danmaku.mFilterParam = danmaku.mFilterParam or (1 shl 20)
                return true
            }
        }

        return false
    }

    override fun setData(data: Set<String>?) {
        reset()
        if (data != null) {
            for (item in data) {
                if (item.startsWith("/") && item.endsWith("/") && item.length > 2) {
                    // 正则表达式：/pattern/
                    val pattern = item.substring(1, item.length - 1)
                    try {
                        regexPatterns.add(Regex(pattern))
                    } catch (_: Exception) {
                        // 无效的正则表达式，直接作为普通关键词
                        keywords.add(item)
                    }
                } else {
                    keywords.add(item)
                }
            }
        }
    }

    override fun reset() {
        keywords.clear()
        regexPatterns.clear()
    }

    override fun clear() {
        reset()
    }
}
