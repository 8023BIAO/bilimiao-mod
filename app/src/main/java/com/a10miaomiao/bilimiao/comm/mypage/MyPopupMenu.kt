package com.a10miaomiao.bilimiao.comm.mypage

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.a10miaomiao.bilimiao.comm.utils.setCheckMarkTint

class MyPopupMenu(
    private val activity: Activity,
    private val myPage: MyPage,
    private val myPageMenu: MyPageMenu,
    private val anchorView: View,
    private val themeColor: Int = 0,
): PopupMenu.OnMenuItemClickListener {

    private var currentCheckedKey = myPageMenu.checkedKey

    private fun PopupMenu.updateChecked() {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val shouldCheck = myPageMenu.checkable && item.itemId == currentCheckedKey
            item.isChecked = shouldCheck
        }
    }

    private fun PopupMenu.initMenu() {
        menu.addItems(Menu.FIRST, myPageMenu, true)
    }

    private fun Menu.addItems(groupId: Int, myMenu: MyPageMenu, topLevel: Boolean = false) {
        myMenu.items.forEach {
            val key = it.key ?: return@forEach
            add(groupId, key, 0, it.title).apply {
                if (topLevel && myMenu.checkable) {
                    isCheckable = true
                }
            }
            it.childMenu?.let { childMenu ->
                addItems(key, childMenu)
            }
        }
    }

    private fun MyPageMenu.findMyItemByKey(key: Int): MenuItemPropInfo? {
        for (item in items) {
            if (item.key == key) {
                return item
            }
            val childMenu = item.childMenu ?: continue
            childMenu.findMyItemByKey(key)?.let {
                return@findMyItemByKey it
            }
        }
        return null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        currentCheckedKey = item.itemId
        val myItem = myPageMenu.findMyItemByKey(item.itemId)
        if (myItem != null) {
            myPage.onMenuItemClick(anchorView, myItem)
        }
        return false
    }

    private fun createPopupMenu(): PopupMenu {
        val wrapper = ContextThemeWrapper(activity, activity.theme)
        return PopupMenu(wrapper, anchorView).apply {
            menu.apply { initMenu() }
            setOnMenuItemClickListener(this@MyPopupMenu)
            if (myPageMenu.checkable) {
                updateChecked()
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
