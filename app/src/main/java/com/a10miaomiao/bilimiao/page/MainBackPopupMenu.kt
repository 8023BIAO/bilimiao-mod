package com.a10miaomiao.bilimiao.page

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.a10miaomiao.bilimiao.MainActivity
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate

class MainBackPopupMenu(
    private val activity: Activity,
    private val anchor: View,
    private val basePlayerDelegate: BasePlayerDelegate,
): PopupMenu.OnMenuItemClickListener {

    private fun PopupMenu.initMenu() {
        menu.add(Menu.FIRST, 0, 0, "返回首页")
        menu.add(Menu.FIRST, 1, 0, "退出播放")
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            0 -> {
                if(activity is MainActivity) {
                    activity.currentNav.goBackHome()
                }
            }
            1 -> {
                basePlayerDelegate.closePlayer()
            }
        }
        return false
    }

    private fun createPopupMenu(): PopupMenu {
        val wrapper = ContextThemeWrapper(activity, activity.theme)
        return PopupMenu(wrapper, anchor).apply {
            initMenu()
            setOnMenuItemClickListener(this@MainBackPopupMenu)
        }
    }

    fun show() {
        createPopupMenu().apply {
            show()
        }
    }


}