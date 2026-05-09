package com.a10miaomiao.bilimiao.comm.delegate.player

import android.app.Activity
import android.view.Menu
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.a10miaomiao.bilimiao.comm.utils.setCheckMarkTint
import com.shuyu.gsyvideoplayer.utils.GSYVideoType

class ScalePopupMenu(
    private val activity: Activity,
    private val anchor: View,
    private val value: Int,
    private val themeColor: Int,
) {
    private var currentValue = value

    private val scaleList = listOf(
        GSYVideoType.SCREEN_TYPE_DEFAULT to "默认比例",
        GSYVideoType.SCREEN_TYPE_16_9 to "16:9",
        GSYVideoType.SCREEN_TYPE_4_3 to "4:3",
        GSYVideoType.SCREEN_TYPE_FULL to "全屏裁减",
        GSYVideoType.SCREEN_MATCH_FULL to "全屏拉伸",
    )

    private fun PopupMenu.updateChecked() {
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = (scaleList[i].first == currentValue)
        }
    }

    private fun Menu.initMenu() {
        scaleList.forEachIndexed { index, item ->
            add(Menu.FIRST, index, 0, item.second).apply {
                isCheckable = true
            }
        }
    }

    private var changedScaleListener: ((Int) -> Unit)? = null

    fun setOnChangedScaleListener(changedScale: (Int) -> Unit) {
        changedScaleListener = changedScale
    }

    private fun createPopupMenu(): PopupMenu {
        val wrapper = ContextThemeWrapper(activity, activity.theme)
        return PopupMenu(wrapper, anchor).apply {
            menu.apply { initMenu() }
            updateChecked()
            setOnMenuItemClickListener {
                val position = it.itemId
                currentValue = scaleList[position].first
                updateChecked()
                changedScaleListener?.invoke(currentValue)
                false
            }
        }
    }

    fun show() {
        createPopupMenu().apply {
            show()
            setCheckMarkTint(themeColor)
        }
    }
}
