package cn.a10miaomiao.bilimiao.compose.pages.setting

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.defaultNavOptions
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.SettingsExport
import com.a10miaomiao.bilimiao.comm.datastore.SettingsExporter
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
class ExportSettingPage : ComposePage() {

    @Composable
    override fun Content() {
        ExportSettingPageContent()
    }
}

@Composable
private fun ExportSettingPageContent() {
    val context = LocalContext.current
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())
    val scope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingJson by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = SettingsExporter.exportToJson(context)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    statusMessage = "\u2705 导出成功"
                } catch (e: Exception) {
                    statusMessage = "\u274c 导出失败: ${e.message}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                        } ?: throw Exception("无法读取文件")
                    }
                    // Validate JSON
                    kotlinx.serialization.json.Json.decodeFromString<SettingsExport>(jsonString)
                    pendingJson = jsonString
                    pendingFileName = uri.lastPathSegment
                    showImportConfirm = true
                } catch (e: Exception) {
                    statusMessage = "\u274c 读取失败: ${e.message}"
                }
            }
        }
    }

    PageConfig(title = "设置导入导出")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = windowInsets.leftDp.dp,
                end = windowInsets.rightDp.dp,
            )
    ) {
        item("top") {
            Spacer(modifier = Modifier.height(windowInsets.topDp.dp + 16.dp))
        }

        item("info") {
            Text(
                text = "导出全部设置到 JSON 文件，包括：\n• 播放/弹幕/主题/首页等参数\n• 屏蔽关键字/UP主/标签\n• 评论屏蔽词 / 弹幕过滤\n• 时光姬时间 / DPI / 代理\n\n导入后会自动重启应用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item("export_btn") {
            OutlinedButton(
                onClick = {
                    exportLauncher.launch("bilimiao_settings_${System.currentTimeMillis()}.json")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("导出设置文件")
            }
        }

        item("import_btn") {
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("导入设置文件")
            }
        }

        statusMessage?.let { msg ->
            item("status") {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.contains("\u2705")) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        item("bottom") {
            Spacer(
                modifier = Modifier.height(
                    windowInsets.bottomDp.dp + windowStore.bottomAppBarHeightDp.dp
                )
            )
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            title = { Text("确认导入") },
            text = {
                Text(
                    "将从 \"${pendingFileName}\" 导入设置。\n\n" +
                    "警告：当前所有设置将被覆盖，导入后需重启应用生效。"
                )
            },
            onDismissRequest = {
                showImportConfirm = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        scope.launch {
                            try {
                                val count = SettingsExporter.importFromJson(context, pendingJson!!)
                                statusMessage = "\u2705 已导入 $count 项设置，请重启应用"
                                // Kill process to force restart
                                withContext(Dispatchers.Main) {
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                }
                            } catch (e: Exception) {
                                statusMessage = "\u274c 导入失败: ${e.message}"
                            }
                        }
                    }
                ) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
