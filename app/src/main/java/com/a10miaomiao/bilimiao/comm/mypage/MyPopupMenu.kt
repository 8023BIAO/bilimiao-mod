package com.a10miaomiao.bilimiao.comm.mypage

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu

class MyPopupMenu(
    private val activity: Activity,
    private val myPage: MyPage,
    private val myPageMenu: MyPageMenu,
    private val anchorView: View,
    themeColor: Int = 0, // 保留兼容但不使用（Material3 isChecked 走主题）
): PopupMenu.OnMenuItemClickListener {

    private val popupMenu = PopupMenu(activity, anchorView)
    private var currentCheckedKey = myPageMenu.checkedKey

    init {
        popupMenu.menu.apply {
            initMenu()
        }
        popupMenu.setOnMenuItemClickListener(this)
        if (myPageMenu.checkable) {
            updateChecked()
        }
    }

    private fun updateChecked() {
        for (i in 0 until popupMenu.menu.size()) {
            val item = popupMenu.menu.getItem(i)
            val shouldCheck = myPageMenu.checkable && item.itemId == currentCheckedKey
            item.isChecked = shouldCheck
        }
    }

    private fun Menu.initMenu() {
        addItems(Menu.FIRST, myPageMenu, true)
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
        val myItem = myPageMenu.findMyItemByKey(item.itemId)
        if (myItem != null) {
            myPage.onMenuItemClick(anchorView, myItem)
        }
        return false
    }

    fun show() {
        popupMenu.show()
    }

}
