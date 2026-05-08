package com.a10miaomiao.bilimiao.widget.menu

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu

class CheckPopupMenu<T>(
    private val context: Context,
    private val anchor: View,
    private val menus: List<MenuItemInfo<T>>,
    private val value: T,
    themeColor: Int = 0, // 保留兼容但不使用（Material3 isChecked 走主题）
) : PopupMenu.OnMenuItemClickListener {

    private val popupMenu = PopupMenu(context, anchor)
    private var currentValue = value

    var onMenuItemClick: ((item: MenuItemInfo<T>) -> Unit)? = null

    init {
        popupMenu.menu.apply { initMenu() }
        popupMenu.setOnMenuItemClickListener(this)
    }

    private fun Menu.initMenu() {
        menus.forEachIndexed { index, item ->
            add(Menu.FIRST, index, 0, item.title).apply {
                isCheckable = true
                isChecked = (item.value == currentValue)
            }
        }
    }

    private fun updateChecked() {
        for (i in 0 until popupMenu.menu.size()) {
            popupMenu.menu.getItem(i).isChecked = (menus[i].value == currentValue)
        }
    }

    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener) {
        popupMenu.setOnMenuItemClickListener(listener)
    }

    fun show() {
        popupMenu.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        currentValue = menus[item.itemId].value
        updateChecked()
        onMenuItemClick?.invoke(menus[item.itemId])
        return true
    }

    class MenuItemInfo<T>(
        var title: String,
        var value: T,
    )

}
