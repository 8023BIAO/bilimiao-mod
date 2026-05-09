package com.a10miaomiao.bilimiao.comm.delegate.player

import android.app.Activity
import android.view.Menu
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.delegate.player.entity.PlayerSourceInfo
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.setCheckMarkTint

class QualityPopupMenu(
    private val activity: Activity,
    private val anchor: View,
    private val userStore: UserStore,
    private val list: List<PlayerSourceInfo.AcceptInfo>,
    private val value: Int,
    private val themeColor: Int,
) {
    val MAX_QUALITY_NOT_LOGIN = 48 // 48[480P 清晰]
    val MAX_QUALITY_NOT_VIP = 80 // 80[1080P 高清]
    private var currentValue = value

    private fun PopupMenu.updateChecked() {
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = (list[i].quality == currentValue)
        }
    }

    private fun Menu.initMenu() {
        list.forEachIndexed { index, item ->
            add(Menu.FIRST, index, 0, item.description).apply {
                isCheckable = true
                if (item.quality > MAX_QUALITY_NOT_VIP) {
                    setIcon(R.drawable.ic_big_vip)
                    isEnabled = userStore.isVip()
                } else if (item.quality > MAX_QUALITY_NOT_LOGIN && !userStore.isLogin()) {
                    setIcon(R.drawable.ic_login)
                    isEnabled = false
                }
            }
        }
    }

    private var changedQualityListener: ((Int) -> Unit)? = null

    fun setOnChangedQualityListener(changedQuality: (Int) -> Unit) {
        changedQualityListener = changedQuality
    }

    private fun createPopupMenu(): PopupMenu {
        val wrapper = ContextThemeWrapper(activity, activity.theme)
        return PopupMenu(wrapper, anchor).apply {
            menu.apply { initMenu() }
            updateChecked()
            setOnMenuItemClickListener {
                val position = it.itemId
                currentValue = list[position].quality
                updateChecked()
                changedQualityListener?.invoke(currentValue)
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
