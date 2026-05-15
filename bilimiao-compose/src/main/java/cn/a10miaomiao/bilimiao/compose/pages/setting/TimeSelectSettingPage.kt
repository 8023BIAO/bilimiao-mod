package cn.a10miaomiao.bilimiao.compose.pages.setting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.preference.rememberPreferenceFlow
import cn.a10miaomiao.bilimiao.compose.components.preference.textIntPreference
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.utils.TimeSelectUtil
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import org.kodein.di.compose.rememberInstance

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
                valueToText = { AnnotatedString(timeModeLabel(it)) },
            )
            // 只看过去天数
            textIntPreference(
                key = SettingPreferences.TimeSelectPastDays.name,
                defaultValue = SettingConstants.TIME_SELECT_DEFAULT_PAST_DAYS,
                title = { Text("只看过去 N 天以前") },
                summary = { Text("N天前的视频才展示") },
                label = "天",
            )
            textIntPreference(
                key = SettingPreferences.TimeSelectExcludeRecent.name,
                defaultValue = SettingConstants.TIME_SELECT_DEFAULT_EXCLUDE_RECENT,
                title = { Text("排除近 N 天") },
                summary = { Text("排除最近N天的视频，0=不过滤") },
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
                            prefs[SettingPreferences.TimeSelectWeights]
                                ?: SettingConstants.TIME_SELECT_DEFAULT_WEIGHTS
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
            // 4个权重滑条（无 coin，API不返回）
            weightSliderItem(
                keyPrefix = "weight_favorite",
                weightKey = "favorite",
                label = "收藏",
                defaultValue = 75,
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
                summary = { Text("每个分区取N页数据") },
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

private fun timeModeLabel(value: Int): String {
    return when (value) {
        SettingConstants.TIME_SELECT_TIME_MODE_ALL -> "全部时间"
        SettingConstants.TIME_SELECT_TIME_MODE_PAST -> "只看过去"
        SettingConstants.TIME_SELECT_TIME_MODE_CUSTOM -> "自定义范围"
        else -> "未知"
    }
}

private fun LazyListScope.weightSliderItem(
    keyPrefix: String,
    weightKey: String,
    label: String,
    defaultValue: Int,
    context: android.content.Context,
) {
    item(key = keyPrefix, contentType = "WeightSlider") {
        val scope = rememberCoroutineScope()
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
                scope.launch {
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
