package com.a10miaomiao.bilimiao.comm

import android.widget.Toast

/**
 * 全局提示管理器，使用 Android 原生 Toast
 * 自动跟随系统深浅主题，带原生圆角，最上层不会被遮挡
 */
object ToastManager {

    fun show(text: String) {
        Toast.makeText(
            BilimiaoCommApp.commApp.app,
            text,
            Toast.LENGTH_SHORT,
        ).show()
    }
}

/**
 * 全局 toast 函数
 */
fun toast(text: String) {
    ToastManager.show(text)
}
