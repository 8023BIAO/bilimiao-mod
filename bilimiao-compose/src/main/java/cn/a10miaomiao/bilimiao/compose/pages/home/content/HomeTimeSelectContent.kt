package cn.a10miaomiao.bilimiao.compose.pages.home.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.pages.home.HomePageState
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RegionInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RegionVideoInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RegionVideosRankInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.RegionStore
import com.a10miaomiao.bilimiao.comm.utils.TimeSelectUtil
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

private class HomeTimeSelectContentViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()
    private val fragment by instance<Fragment>()
    private val regionStore: RegionStore by instance()

    val regionList = regionStore.state.regions

    sealed class LoadState {
        data object Loading : LoadState()
        data class Success(val videos: List<RegionVideoInfo>) : LoadState()
        data class Error(val msg: String) : LoadState()
    }

    val loadState = MutableStateFlow<LoadState>(LoadState.Loading)

    private data class TimeSelectConfig(
        val timeFrom: String,
        val timeTo: String,
        val weights: Map<String, Int>,
        val selectedRegions: List<RegionInfo>,
        val pagesPerRegion: Int,
        val pageSize: Int,
        val minDuration: Int,
        val minPlayCount: Int,
        val originalOnly: Boolean,
    )

    init {
        loadData()
    }

    fun refresh() {
        loadState.value = LoadState.Loading
        loadData()
    }

    private fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val config = readConfig()
            if (config.selectedRegions.isEmpty()) {
                loadState.value = LoadState.Error("没有选择分区，请去设置中配置")
                return@launch
            }

            // 按权重最高的排序方式取数据
            val primaryOrder = config.weights.maxByOrNull { it.value }?.key ?: "click"

            // 收集所有分区的视频
            val allVideos = mutableListOf<Pair<RegionVideoInfo, Int>>() // video + regionId
            for (region in config.selectedRegions) {
                for (page in 1..config.pagesPerRegion) {
                    try {
                        val res = BiliApiService.regionAPI
                            .regionVideoList(
                                rid = region.tid,
                                rankOrder = primaryOrder,
                                pageNum = page,
                                pageSize = config.pageSize,
                                timeFrom = config.timeFrom,
                                timeTo = config.timeTo,
                            )
                            .awaitCall()
                            .json<ResultInfo<RegionVideosRankInfo>>()
                        if (res.code == 0) {
                            val result = res.data?.result
                            if (result != null) {
                                for (video in result) {
                                    allVideos.add(video to region.tid)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 单个分区/单页失败不影响其他
                    }
                }
            }

            // 去重（按 avid）
            val seen = mutableSetOf<Long>()
            val uniqueVideos = mutableListOf<RegionVideoInfo>()
            for ((video, _) in allVideos) {
                if (video.aid !in seen) {
                    seen.add(video.aid)
                    uniqueVideos.add(video)
                }
            }

            if (uniqueVideos.isEmpty()) {
                loadState.value = LoadState.Error("没有找到符合条件的视频")
                return@launch
            }

            // 应用过滤
            var filtered = uniqueVideos
            if (config.minDuration > 0) {
                filtered = filtered.filter { (it.duration ?: 0) >= config.minDuration }
            }
            if (config.minPlayCount > 0) {
                filtered = filtered.filter { (it.stat?.view ?: 0) >= config.minPlayCount }
            }

            if (filtered.isEmpty()) {
                loadState.value = LoadState.Error("过滤后没有符合条件的视频")
                return@launch
            }

            // 计算加权得分（先归一化）
            val scored = scoreAndSort(filtered, config.weights)

            loadState.value = LoadState.Success(scored)
        } catch (e: Exception) {
            loadState.value = LoadState.Error("加载失败: ${e.message}")
        }
    }

    private fun scoreAndSort(
        videos: List<RegionVideoInfo>,
        weights: Map<String, Int>,
    ): List<RegionVideoInfo> {
        if (videos.isEmpty()) return videos

        // 收集每个指标的 min/max
        var minCoin = Long.MAX_VALUE; var maxCoin = Long.MIN_VALUE
        var minFav = Long.MAX_VALUE; var maxFav = Long.MIN_VALUE
        var minClick = Long.MAX_VALUE; var maxClick = Long.MIN_VALUE
        var minDanmaku = Long.MAX_VALUE; var maxDanmaku = Long.MIN_VALUE
        var minReply = Long.MAX_VALUE; var maxReply = Long.MIN_VALUE

        for (v in videos) {
            val stat = v.stat ?: continue
            minCoin = minOf(minCoin, stat.coin ?: 0)
            maxCoin = maxOf(maxCoin, stat.coin ?: 0)
            minFav = minOf(minFav, stat.favorite ?: 0)
            maxFav = maxOf(maxFav, stat.favorite ?: 0)
            minClick = minOf(minClick, stat.view ?: 0)
            maxClick = maxOf(maxClick, stat.view ?: 0)
            minDanmaku = minOf(minDanmaku, stat.danmaku ?: 0)
            maxDanmaku = maxOf(maxDanmaku, stat.danmaku ?: 0)
            minReply = minOf(minReply, stat.reply ?: 0)
            maxReply = maxOf(maxReply, stat.reply ?: 0)
        }

        val coinW = (weights["coin"] ?: 0) / 100f
        val favW = (weights["favorite"] ?: 0) / 100f
        val clickW = (weights["click"] ?: 0) / 100f
        val danmakuW = (weights["danmaku"] ?: 0) / 100f
        val replyW = (weights["reply"] ?: 0) / 100f

        // 归一化 + 加权
        data class ScoredVideo(val video: RegionVideoInfo, val score: Float)
        val scored = videos.map { v ->
            val stat = v.stat
            val normCoin = if (maxCoin > minCoin)
                ((stat?.coin ?: 0) - minCoin).toFloat() / (maxCoin - minCoin).toFloat() else 0f
            val normFav = if (maxFav > minFav)
                ((stat?.favorite ?: 0) - minFav).toFloat() / (maxFav - minFav).toFloat() else 0f
            val normClick = if (maxClick > minClick)
                ((stat?.view ?: 0) - minClick).toFloat() / (maxClick - minClick).toFloat() else 0f
            val normDanmaku = if (maxDanmaku > minDanmaku)
                ((stat?.danmaku ?: 0) - minDanmaku).toFloat() / (maxDanmaku - minDanmaku).toFloat() else 0f
            val normReply = if (maxReply > minReply)
                ((stat?.reply ?: 0) - minReply).toFloat() / (maxReply - minReply).toFloat() else 0f

            val score = normCoin * coinW + normFav * favW + normClick * clickW
                    + normDanmaku * danmakuW + normReply * replyW

            ScoredVideo(v, score)
        }

        return scored.sortedByDescending { it.score }.map { it.video }
    }

    private fun readConfig(): TimeSelectConfig {
        val context = fragment.requireContext()
        val prefs = runBlocking {
            SettingPreferences.run {
                SettingPreferences.mapData(context) { it }
            }
        }

        val timeMode = prefs[SettingPreferences.TimeSelectTimeMode]
            ?: SettingConstants.TIME_SELECT_TIME_MODE_ALL
        val weightsStr = prefs[SettingPreferences.TimeSelectWeights]
            ?: SettingConstants.TIME_SELECT_DEFAULT_WEIGHTS
        val allRegions = prefs[SettingPreferences.TimeSelectAllRegions] ?: true
        val selectedRegionIds = prefs[SettingPreferences.TimeSelectSelectedRegions] ?: emptySet()
        val pagesPerRegion = prefs[SettingPreferences.TimeSelectPagesPerRegion]
            ?: SettingConstants.TIME_SELECT_DEFAULT_PAGES
        val minDuration = prefs[SettingPreferences.TimeSelectMinDuration] ?: 0
        val minPlayCount = prefs[SettingPreferences.TimeSelectMinPlayCount] ?: 0

        // 计算时间范围
        val (timeFrom, timeTo) = when (timeMode) {
            SettingConstants.TIME_SELECT_TIME_MODE_ALL -> {
                Pair("20090901", getTodayStr())
            }
            SettingConstants.TIME_SELECT_TIME_MODE_PAST -> {
                val pastDays = prefs[SettingPreferences.TimeSelectPastDays]
                    ?: SettingConstants.TIME_SELECT_DEFAULT_PAST_DAYS
                val excludeRecent = prefs[SettingPreferences.TimeSelectExcludeRecent] ?: 0
                val to = getDateDaysAgo(excludeRecent)
                val from = getDateDaysAgo(pastDays + excludeRecent)
                Pair(from, to)
            }
            SettingConstants.TIME_SELECT_TIME_MODE_CUSTOM -> {
                val from = prefs[SettingPreferences.TimeSelectCustomFrom] ?: "20090901"
                val to = prefs[SettingPreferences.TimeSelectCustomTo] ?: getTodayStr()
                Pair(from, to)
            }
            else -> Pair("20090901", getTodayStr())
        }

        // 选区列表
        val regions = if (allRegions) {
            regionStore.state.regions
        } else {
            regionStore.state.regions.filter {
                selectedRegionIds.contains(it.tid.toString())
            }
        }

        return TimeSelectConfig(
            timeFrom = timeFrom,
            timeTo = timeTo,
            weights = TimeSelectUtil.parseWeights(weightsStr),
            selectedRegions = regions,
            pagesPerRegion = pagesPerRegion,
            pageSize = SettingConstants.TIME_SELECT_DEFAULT_PAGE_SIZE,
            minDuration = minDuration,
            minPlayCount = minPlayCount,
            originalOnly = false,
        )
    }

    private fun getTodayStr(): String {
        val cal = java.util.Calendar.getInstance()
        return String.format("%04d%02d%02d", cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    private fun getDateDaysAgo(days: Int): String {
        if (days <= 0) return getTodayStr()
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
        return String.format("%04d%02d%02d", cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }
}

@Composable
internal fun HomeTimeSelectContent(
    pageState: HomePageState
) {
    val viewModel: HomeTimeSelectContentViewModel = diViewModel()
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val loadState by viewModel.loadState.collectAsState()

    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.HomeTimeSelect) {
                viewModel.refresh()
            }
        }
    }

    when (val state = loadState) {
        is HomeTimeSelectContentViewModel.LoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(windowInsets.toPaddingValues()),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        is HomeTimeSelectContentViewModel.LoadState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(windowInsets.toPaddingValues()),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = { viewModel.refresh() }) {
                    Text(state.msg)
                }
            }
        }
        is HomeTimeSelectContentViewModel.LoadState.Success -> {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(windowInsets.toPaddingValues()),
                columns = GridCells.Adaptive(300.dp),
            ) {
                items(state.videos, { it.aid }) { video ->
                    VideoItemBox(
                        videoInfo = video,
                    )
                }
            }
        }
    }
}
