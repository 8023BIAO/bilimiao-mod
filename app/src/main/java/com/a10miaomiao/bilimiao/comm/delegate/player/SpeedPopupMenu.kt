package com.a10miaomiao.bilimiao.comm.delegate.player

import android.app.Activity
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu

class SpeedPopupMenu(
    private val activity: Activity,
    private val anchor: View,
    private val value: Float,
    private val list: List<Float>,
    themeColor: Int, // 保留兼容但不使用（Material3 isChecked 走主题）
) {
    private val popupMenu = PopupMenu(activity, anchor)
    private var currentValue = value

    init {
        popupMenu.menu.apply { initMenu() }
        updateChecked()
    }

    private fun updateChecked() {
        for (i in 0 until popupMenu.menu.size()) {
            popupMenu.menu.getItem(i).isChecked = (list[i] == currentValue)
        }
    }

    private fun Menu.initMenu() {
        list.forEachIndexed { index, item ->
            add(Menu.FIRST, index, 0, item.toString()).apply {
                isCheckable = true
            }
        }
    }

    fun setOnChangedSpeedListener(changedSpeed: (Float) -> Unit) {
        popupMenu.setOnMenuItemClickListener {
            val position = it.itemId
            currentValue = list[position]
            updateChecked()
            changedSpeed(currentValue)
            false
        }
    }

    fun show() {
        popupMenu.show()
    }
}
