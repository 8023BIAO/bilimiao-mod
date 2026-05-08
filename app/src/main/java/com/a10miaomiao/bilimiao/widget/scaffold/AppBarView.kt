package com.a10miaomiao.bilimiao.widget.scaffold

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.widget.scaffold.ui.AppBarUi
import com.a10miaomiao.bilimiao.widget.scaffold.ui.AppBarVerticalUi
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class AppBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var canBack = false
    var showPointer = false
    var pointerOrientation = true
    var onBackClick: View.OnClickListener? = null
    var onOpenMenuClick: View.OnClickListener? = null
    var onBackLongClick: View.OnLongClickListener? = null
    var onMenuItemClick: ((MenuItemView) -> Unit)? = null
    var onPointerClick: View.OnClickListener? = null
    var onPointerLongClick: View.OnLongClickListener? = null

    private var prop: PropInfo? = null
        set(value) {
            field = value
            updateProp()
        }

    private val navigationClick = OnClickListener { view ->
        if (canBack) {
            onBackClick?.onClick(view)
        } else {
            onOpenMenuClick?.onClick(view)
        }
    }
    private val navigationLongClick = OnLongClickListener { view ->
        onBackLongClick?.onLongClick(view) ?: false
    }

    private val menuItemClick = OnClickListener { view ->
        (view as? MenuItemView)?.let {
            if (it.prop.key == MenuKeys.back) {
                onBackClick?.onClick(it)
            } else if (it.prop.key == MenuKeys.menu) {
                onOpenMenuClick?.onClick(it)
            } else {
                onMenuItemClick?.invoke(it)
            }
        }
    }
    private val menuItemLongClick = OnLongClickListener { view ->
        if (view is MenuItemView
            && view.prop.key == MenuKeys.back) {
            onBackLongClick?.onLongClick(view) ?: false
        } else {
            false
        }
    }

    private val pointerClick = OnClickListener { view ->
        onPointerClick?.onClick(view)
    }
    private val pointerLongClick = OnLongClickListener { view ->
        onPointerLongClick?.onLongClick(view) ?: false
    }

    private var mUi = createUi()

    var themeColor = 0

    init {
        updateProp()
        setView(mUi.root)
    }

    private fun createUi(): AppBarUi {
        return AppBarVerticalUi(
            context,
            this,
            menuItemClick = menuItemClick,
            menuItemLongClick = menuItemLongClick,
        )
    }

    fun setView(view: View) {
        if (childCount > 0) {
            removeAllViews()
        }
        addView(view, 0, lParams {
            width = matchParent
            height = matchParent
        })
    }

    fun updateTheme(color: Int, bgColor: Int) {
        themeColor = color
        mUi.updateTheme(color, bgColor)
        backgroundColor = bgColor
    }

    fun setWindowInsets(left: Int, top: Int, right: Int, bottom: Int) {
        setPadding(left, 0, right, bottom)
    }

    fun clearProp() {
        this.prop = newProp()
    }

    @OptIn(ExperimentalContracts::class)
    @SuppressLint("RestrictedApi")
    fun setProp(block: PropInfo.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val prop = newProp()
        prop.block()
        this.prop = prop
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun newProp(): PropInfo {
        val prop = PropInfo()
        val theme = context.theme
        if (canBack) {
            prop.navigationButtonIcon = resources.getDrawable(R.drawable.ic_back_24dp, theme)
            prop.navigationButtonKey = MenuKeys.back
        } else {
            prop.navigationButtonIcon = resources.getDrawable(R.drawable.ic_baseline_menu_24, theme)
            prop.navigationButtonKey = MenuKeys.menu
        }
        if (showPointer) {
            prop.navigationPointerIcon = resources.getDrawable(R.drawable.ic_pointer_24dp, theme)
            prop.pointerIconOrientation = pointerOrientation
        }
        return prop
    }

    private fun updateProp() {
        prop?.let {
            mUi.setProp(prop)
        }
    }

    fun showMenu() {
        (mUi as? AppBarVerticalUi)?.showMenu()
    }

    fun hideMenu() {
        (mUi as? AppBarVerticalUi)?.hideMenu()
    }


    class PropInfo(
        var title: String? = null,
        var navigationButtonIcon: Drawable? = null,
        var navigationButtonKey: Int = 0,
        var navigationPointerIcon: Drawable? = null,
        var pointerIconOrientation: Boolean = true,
        var menus: List<MenuItemPropInfo>? = null,
        var isNavigationMenu: Boolean = false,
        var navigationKey: Int = 0,
    )

}
