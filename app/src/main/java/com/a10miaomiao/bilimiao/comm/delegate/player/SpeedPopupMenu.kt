package com.a10miaomiao.bilimiao.comm.delegate.player

import android.app.Activity
import android.view.Menu
import android.view.View
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.a10miaomiao.bilimiao.comm.utils.setCheckMarkTint

class SpeedPopupMenu(
    private val activity: Activity,
    private val anchor: View,
    private val value: Float,
    private val list: List<Float>,
    private val themeColor: Int,
) {
    private var currentValue = value

    private fun PopupMenu.updateChecked() {
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = (list[i] == currentValue)
        }
    }

    private fun Menu.initMenu() {
        list.forEachIndexed { index, item ->
            add(Menu.FIRST, index, 0, item.toString()).apply {
                isCheckable = true
            }
        }
    }

    private var changedSpeedListener: ((Float) -> Unit)? = null

    fun setOnChangedSpeedListener(changedSpeed: (Float) -> Unit) {
        changedSpeedListener = changedSpeed
    }

    private fun createPopupMenu(): PopupMenu {
        val wrapper = ContextThemeWrapper(activity, activity.theme).apply {
            theme.applyStyle(androidx.appcompat.R.style.Theme_AppCompat_Light, true)
            theme.applyStyle(android.R.style.Theme_Material_Light, true)
            val typedValue = TypedValue()
            theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        }
        return PopupMenu(wrapper, anchor).apply {
            menu.apply { initMenu() }
            updateChecked()
            setOnMenuItemClickListener {
                val position = it.itemId
                currentValue = list[position]
                updateChecked()
                changedSpeedListener?.invoke(currentValue)
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
