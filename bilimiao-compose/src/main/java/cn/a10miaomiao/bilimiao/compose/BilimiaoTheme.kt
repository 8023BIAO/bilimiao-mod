package cn.a10miaomiao.bilimiao.compose

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.a10miaomiao.bilimiao.comm.ToastManager
import com.a10miaomiao.bilimiao.comm.store.AppStore
import com.materialkolor.rememberDynamicColorScheme
@Composable
fun BilimiaoTheme(
    appState: AppStore.State,
    systemDark: Boolean,
    content: @Composable () -> Unit
) {
    val themeState = appState.theme ?: return
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        ToastManager.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg.text)
        }
    }
    MaterialTheme(
        colorScheme = appColorScheme(themeState, systemDark),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = MaterialTheme.shapes.small,
                    )
                }
            )
        }
    }
}

@Composable
fun appColorScheme(
    themeState: AppStore.ThemeSettingState,
    systemDark: Boolean
): ColorScheme {

    val themeColor = Color(themeState.color)
    val isDarkTheme = when(themeState.darkMode) {
        0 -> systemDark
        1 -> false
        else -> true
    }
//    if (dynamicColor) {
//        return if (isDarkTheme) {
//            dynamicDarkColorScheme(LocalContext.current)
//        } else {
//            dynamicLightColorScheme(LocalContext.current)
//        }
//    }
    val colorScheme = rememberDynamicColorScheme(
        themeColor,
        isDarkTheme,
        isAmoled = true
    )
    return colorScheme
}