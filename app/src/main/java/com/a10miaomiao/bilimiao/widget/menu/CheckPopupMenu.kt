package com.a10miaomiao.bilimiao.widget.menu

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.a10miaomiao.bilimiao.comm.utils.setCheckMarkTint

class CheckPopupMenu<T>(
    private val context: Context,
    private val anchor: View,
    private val menus: List<MenuItemInfo<T>>,
    private val value: T,
    private val themeColor: Int = 0,
) : PopupMenu.OnMenuItemClickListener {

    private var currentValue = value

    var onMenuItemClick: ((item: MenuItemInfo<T>) -> Unit)? = null

    private fun PopupMenu.initMenu() {
        menus.forEachIndexed { index, item ->
            menu.add(Menu.FIRST, index, 0, item.title).apply {
                isCheckable = true
                isChecked = (item.value == currentValue)
            }
        }
    }

    private fun PopupMenu.updateChecked() {
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = (menus[i].value == currentValue)
        }
    }

    private var externalClickListener: PopupMenu.OnMenuItemClickListener? = null

    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener) {
        externalClickListener = listener
    }

    private fun createPopupMenu(): PopupMenu {
        val wrapper = ContextThemeWrapper(context, context.theme)
        return PopupMenu(wrapper, anchor).apply {
            initMenu()
            updateChecked()
            setOnMenuItemClickListener(this@CheckPopupMenu)
        }
    }

    fun show() {
        createPopupMenu().apply {
            show()
            setCheckMarkTint(themeColor)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        currentValue = menus[item.itemId].value
        onMenuItemClick?.invoke(menus[item.itemId])
        return externalClickListener?.onMenuItemClick(item) ?: true
    }

    class MenuItemInfo<T>(
        var title: String,
        var value: T,
    )

}
