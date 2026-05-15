package cn.a10miaomiao.bilimiao.compose.pages.setting

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.rememberPreferenceFlow
import cn.a10miaomiao.bilimiao.compose.components.preference.listStylePreference
import cn.a10miaomiao.bilimiao.compose.components.preference.textIntPreference
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.store.WindowStore
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
class HomeSettingPage : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel: HomeSettingPageViewModel = diViewModel()
        HomeSettingPageContent(viewModel)
    }
}

private class HomeSettingPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val fragment by instance<Fragment>()
    private val pageNavigation by instance<PageNavigation>()

    val entryViews = mapOf(
        SettingConstants.HOME_ENTRY_VIEW_DEFAULT to "默认",
        SettingConstants.HOME_ENTRY_VIEW_RECOMMEND to "推荐",
        SettingConstants.HOME_ENTRY_VIEW_POPULAR to "热门",
        SettingConstants.HOME_ENTRY_VIEW_DYNAMIC to "动态",
        SettingConstants.HOME_ENTRY_VIEW_TIME_SELECT to "时光精选",
    )

    fun toTimeSelectSettingPage() {
        pageNavigation.navigate(TimeSelectSettingPage)
    }

}


@Composable
private fun HomeSettingPageContent(
    viewModel: HomeSettingPageViewModel
) {
    PageConfig(
        title = "首页设置"
    )
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val context = LocalContext.current
    val dataStore = remember {
        SettingPreferences.run { context.dataStore }
    }

    var showTimeSelect by remember {
        mutableStateOf(
            kotlinx.coroutines.runBlocking {
                SettingPreferences.run {
                    SettingPreferences.mapData(context) { prefs ->
                        prefs[SettingPreferences.TimeSelectShow] ?: true
                    }
                }
            }
        )
    }

    // 监听 DataStore 变化，即时更新 showTimeSelect
    LaunchedEffect(dataStore) {
        dataStore.data.collect { prefs ->
            showTimeSelect = prefs[SettingPreferences.TimeSelectShow] ?: true
        }
    }

    val entryViewValues = if (showTimeSelect) {
        viewModel.entryViews.keys.toList()
    } else {
        viewModel.entryViews.keys.toList() - SettingConstants.HOME_ENTRY_VIEW_TIME_SELECT
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
            preferenceCategory(
                key = "top_nav",
                title = {
                    Text("首页顶部设置")
                }
            )
            listPreference(
                key = SettingPreferences.HomeEntryView.name,
                defaultValue = SettingConstants.HOME_ENTRY_VIEW_DEFAULT,
                type = ListPreferenceType.DROPDOWN_MENU,
                title = {
                    Text("首页入口")
                },
                summary = {
                    Text(text = "当前: " + viewModel.entryViews[it])
                },
                values = entryViewValues,
                valueToText = {
                    val text = viewModel.entryViews[it]
                    AnnotatedString(text ?: "未知")
                },
            )
            switchPreference(
                key = SettingPreferences.HomeTimeMachineShow.name,
                title = {
                    Text("显示时光姬")
                },
                defaultValue = true,
            )
            switchPreference(
                key = SettingPreferences.TimeSelectShow.name,
                title = {
                    Text("显示时光精选")
                },
                defaultValue = true,
            )
            switchPreference(
                key = SettingPreferences.HomeRecommendShow.name,
                title = {
                    Text("显示推荐")
                },
                defaultValue = true,
            )
            switchPreference(
                key = SettingPreferences.HomePopularShow.name,
                title = {
                    Text("显示热门")
                },
                defaultValue = true,
            )

            preference(
                key = "time_select",
                title = { Text("时光精选设置") },
                summary = { Text("自定义精选高质量旧视频的算法") },
                enabled = showTimeSelect,
                onClick = if (showTimeSelect) viewModel::toTimeSelectSettingPage else null,
            )

            preferenceCategory(
                key = "popular",
                title = {
                    Text("热门设置")
                }
            )
            switchPreference(
                key = SettingPreferences.HomePopularCarryToken.name,
                title = {
                    Text("个性化热门列表")
                },
                summary = {
                    Text("修改后需手动刷新列表")
                },
                defaultValue = true,
            )

            preferenceCategory(
                key = "recommend",
                title = {
                    Text("推荐设置")
                }
            )
            listStylePreference(
                key = SettingPreferences.HomeRecommendListStyle.name,
                defaultValue = 0,
            )
            textIntPreference(
                key = SettingPreferences.VideoMinDuration.name,
                defaultValue = SettingConstants.VIDEO_MIN_DURATION_DEFAULT,
                title = {
                    Text("最小视频时长过滤")
                },
                summary = {
                    Text("过滤短视频")
                },
                label = "秒",
            )
            textIntPreference(
                key = SettingPreferences.VideoMinPlayCount.name,
                defaultValue = SettingConstants.VIDEO_MIN_PLAY_COUNT_DEFAULT,
                title = {
                    Text("最小播放量过滤")
                },
                summary = {
                    Text("0不过滤")
                },
                label = "个",
            )
            switchPreference(
                key = SettingPreferences.VideoHideCover.name,
                title = {
                    Text("不显示封面")
                },
                defaultValue = false,
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
