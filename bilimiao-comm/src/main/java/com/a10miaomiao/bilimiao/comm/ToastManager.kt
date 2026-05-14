package com.a10miaomiao.bilimiao.comm

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 全局轻提示管理器，统一使用 Material3 Snackbar 风格
 */
object ToastManager {

    data class ToastMessage(
        val text: String,
    )

    private val _messages = Channel<ToastMessage>(Channel.BUFFERED)
    val messages: Flow<ToastMessage> = _messages.receiveAsFlow()

    fun show(text: String) {
        _messages.trySend(ToastMessage(text))
    }
}

/**
 * 全局 toast 函数，替代 toast()
 * 所有 toast(...) 应替换为 toast(...)
 */
fun toast(text: String) {
    ToastManager.show(text)
}
