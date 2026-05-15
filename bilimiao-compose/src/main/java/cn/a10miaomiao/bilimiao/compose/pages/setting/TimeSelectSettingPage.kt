package cn.a10miaomiao.bilimiao.compose.pages.setting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.preference.rememberPreferenceFlow
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.store.RegionStore
import com.a10miaomiao.bilimiao.comm.utils.TimeSelectUtil
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

@Serializable
object TimeSelectSettingPage : ComposePage() {

    @Composable
    override fun Content() {
        TimeSelectSettingPageContent()
    }
}

@Composable
private fun TimeSelectSettingPageContent() {
    PageConfig(
        title = "时光精选设置"
    )
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val context = LocalContext.current
    val dataStore = remember {
        SettingPreferences.run { context.dataStore }
    }

    ProvidePreferenceLocals(
        flow = rememberPreferenceFlow(dataStore)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = windowInsets.leftDp.dp,
                    end = windowInsets.rightDp.dp,
                )
        ) {
            item("top") {
                Spacer(
                    modifier = Modifier.height(windowInsets.topDp.dp)
                )
            }

            // ========== 显示控制 ==========
            preferenceCategory(
                key = "display",
                title = { Text("显示控制") }
            )
            switchPreference(
                key = SettingPreferences.TimeSelectShow.name,
                defaultValue = true,
                title = { Text("显示时光精选") },
                summary = { Text("在首页标签栏显示时光精选") },
            )
            listPreference(
                key = SettingPreferences.HomeEntryView.name,
                defaultValue = SettingConstants.HOME_ENTRY_VIEW_DEFAULT,
                type = ListPreferenceType.DROPDOWN_MENU,
                title = { Text("首页入口") },
                summary = { Text("当前: ${entryViewLabel(it)}") },
                values = listOf(
                    SettingConstants.HOME_ENTRY_VIEW_DEFAULT,
                    SettingConstants.HOME_ENTRY_VIEW_RECOMMEND,
                    SettingConstants.HOME_ENTRY_VIEW_POPULAR,
                    SettingConstants.HOME_ENTRY_VIEW_DYNAMIC,
                    SettingConstants.HOME_ENTRY_VIEW_TIME_SELECT,
                ),
                valueToText = { Text(entryViewLabel(it)) },
            )

            // ========== 时间线 ==========
            preferenceCategory(
                key = "time",
                title = { Text("时间线设置") }
            )
            listPreference(
                key = SettingPreferences.TimeSelectTimeMode.name,
                defaultValue = SettingConstants.TIME_SELECT_TIME_MODE_ALL,
                type = ListPreferenceType.DROPDOWN_MENU,
                title = { Text("时间模式") },
                summary = { Text(timeModeLabel(it)) },
                values = listOf(
                    SettingConstants.TIME_SELECT_TIME_MODE_ALL,
                    SettingConstants.TIME_SELECT_TIME_MODE_PAST,
                    SettingConstants.TIME_SELECT_TIME_MODE_CUSTOM,
                ),
                valueToText = { Text(timeModeLabel(it)) },
            )
            // 只看过去天数 - 动态显示
            textIntPreference(
                key = SettingPreferences.TimeSelectPastDays.name,
                defaultValue = SettingConstants.TIME_SELECT_DEFAULT_PAST_DAYS,
                title = { Text("只看过去 N 天以前") },
                summary = { Text("只看 ${it} 天以前的视频，${it}天内的不展示") },
                enabled = { timeModeValue(context) == SettingConstants.TIME_SELECT_TIME_MODE_PAST },
            )
            textIntPreference(
                key = SettingPreferences.TimeSelectExcludeRecent.name,
                defaultValue = SettingConstants.TIME_SELECT_DEFAULT_EXCLUDE_RECENT,
                title = { Text("排除近 N 天") },
                summary = { Text("排除最近 ${it} 天的视频，0=不过滤") },
                label = "天",
            )

            // ========== 排序权重 ==========
            preferenceCategory(
                key = "weights",
                title = { Text("排序权重") }
            )
            item(key = "weight_desc") {
                val weightsStr = runBlocking {
                    SettingPreferences.run {
                        SettingPreferences.mapData(context) { prefs ->
                            prefs[SettingPreferences.TimeSelectWeights] ?: SettingConstants.TIME_SELECT_DEFAULT_WEIGHTS
                        }
                    }
                }
                val weights = TimeSelectUtil.parseWeights(weightsStr)
                Text(
                    text = "当前公式：${TimeSelectUtil.weightsToDisplayString(weights)}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            // 5个权重滑条 - 每个用一个单独的"假" key 但写入 TimeSelectWeights
            // 实际存储是 string，这里用 int key 是取不出来的，需要用自定义方式
            weightSliderItem(
                keyPrefix = "weight_coin",
                weightKey = "coin",
                label = "硬币",
                defaultValue = 40,
                context = context,
            )
            weightSliderItem(
                keyPrefix = "weight_favorite",
                weightKey = "favorite",
                label = "收藏",
                defaultValue = 35,
                context = context,
            )
            weightSliderItem(
                keyPrefix = "weight_click",
                weightKey = "click",
                label = "播放",
                defaultValue = 15,
                context = context,
            )
            weightSliderItem(
                keyPrefix = "weight_danmaku",
                weightKey = "danmaku",
                label = "弹幕",
                defaultValue = 5,
                context = context,
            )
            weightSliderItem(
                keyPrefix = "weight_reply",
                weightKey = "reply",
                label = "评论",
                defaultValue = 5,
                context = context,
            )

            // ========== 分区选择 ==========
            preferenceCategory(
                key = "regions",
                title = { Text("分区选择") }
            )
            switchPreference(
                key = SettingPreferences.TimeSelectAllRegions.name,
                defaultValue = true,
                title = { Text("全部分区") },
                summary = { Text(if (it) "遍历所有分区" else "手动选择分区") },
            )

            // ========== 高级设置 ==========
            preferenceCategory(
                key = "filter",
                title = { Text("过滤") }
            )
            textIntPreference(
                key = SettingPreferences.TimeSelectPagesPerRegion.name,
                defaultValue = SettingConstants.TIME_SELECT_DEFAULT_PAGES,
                title = { Text("每分区取前 N 页") },
                summary = { Text("每个分区取 ${it} 页数据") },
            )
            textIntPreference(
                key = SettingPreferences.TimeSelectMinDuration.name,
                defaultValue = 0,
                title = { Text("最小时长(秒)") },
                summary = { Text("过滤短视频，0=不过滤") },
            )
            textIntPreference(
                key = SettingPreferences.TimeSelectMinPlayCount.name,
                defaultValue = 0,
                title = { Text("最小播放量") },
                summary = { Text("低于此不展示，0=不过滤") },
                label = "个",
            )
            switchPreference(
                key = SettingPreferences.TimeSelectOriginalOnly.name,
                defaultValue = true,
                title = { Text("只看原创") },
                summary = { Text("过滤转载内容") },
            )

            item("bottom") {
                Spacer(
                    modifier = Modifier.height(
                        windowInsets.bottomDp.dp + windowStore.bottomAppBarHeightDp.dp
                    )
                )
            }
        }
    }
}

private fun entryViewLabel(value: Int): String {
    return when (value) {
        SettingConstants.HOME_ENTRY_VIEW_DEFAULT -> "默认"
        SettingConstants.HOME_ENTRY_VIEW_RECOMMEND -> "推荐"
        SettingConstants.HOME_ENTRY_VIEW_POPULAR -> "热门"
        SettingConstants.HOME_ENTRY_VIEW_DYNAMIC -> "动态"
        SettingConstants.HOME_ENTRY_VIEW_TIME_SELECT -> "时光精选"
        else -> "未知"
    }
}

private fun timeModeLabel(value: Int): String {
    return when (value) {
        SettingConstants.TIME_SELECT_TIME_MODE_ALL -> "全部时间"
        SettingConstants.TIME_SELECT_TIME_MODE_PAST -> "只看过去"
        SettingConstants.TIME_SELECT_TIME_MODE_CUSTOM -> "自定义范围"
        else -> "未知"
    }
}

private fun timeModeValue(context: android.content.Context): Int {
    return runBlocking {
        SettingPreferences.run {
            SettingPreferences.mapData(context) { prefs ->
                prefs[SettingPreferences.TimeSelectTimeMode] ?: SettingConstants.TIME_SELECT_TIME_MODE_ALL
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.lazy.LazyListScope.textIntPreference(
    key: String,
    defaultValue: Int,
    title: @Composable (Int) -> Unit,
    summary: @Composable (Int) -> Unit = {},
    enabled: @Composable () -> Boolean = { true },
    label: String = "",
) {
    cn.a10miaomiao.bilimiao.compose.components.preference.textIntPreference(
        key = key,
        defaultValue = defaultValue,
        title = title,
        summary = summary,
        enabled = { enabled() },
        label = label,
    )
}

@Composable
private fun androidx.compose.foundation.lazy.LazyListScope.weightSliderItem(
    keyPrefix: String,
    weightKey: String,
    label: String,
    defaultValue: Int,
    context: android.content.Context,
) {
    item(key = keyPrefix, contentType = "WeightSlider") {
        var weightsStr by remember { mutableStateOf(
            runBlocking {
                SettingPreferences.run {
                    SettingPreferences.mapData(context) { prefs ->
                        prefs[SettingPreferences.TimeSelectWeights]
                            ?: SettingConstants.TIME_SELECT_DEFAULT_WEIGHTS
                    }
                }
            }
        ) }
        val weights = TimeSelectUtil.parseWeights(weightsStr)
        var sliderValue by remember { mutableIntStateOf(weights[weightKey] ?: defaultValue) }

        me.zhanghai.compose.preference.SliderPreference(
            value = sliderValue.toFloat(),
            onValueChange = { newValue ->
                sliderValue = newValue.toInt()
                // 读取当前权重 map，更新，写回
                val currentWeights = TimeSelectUtil.parseWeights(weightsStr).toMutableMap()
                currentWeights[weightKey] = newValue.toInt()
                val newWeightsStr = TimeSelectUtil.formatWeights(currentWeights)
                weightsStr = newWeightsStr
                // 异步写入 DataStore
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    SettingPreferences.run {
                        SettingPreferences.edit(context) { prefs ->
                            prefs[SettingPreferences.TimeSelectWeights] = newWeightsStr
                        }
                    }
                }
            },
            sliderValue = sliderValue.toFloat(),
            onSliderValueChange = { sliderValue = it.toInt() },
            title = { Text("$label: ${sliderValue}%") },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..100f,
            valueSteps = 5,
        )
    }
}
