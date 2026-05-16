package cn.a10miaomiao.bilimiao.compose.pages.home.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.pages.home.HomePageState
import cn.a10miaomiao.bilimiao.compose.pages.setting.TimeSelectSettingPage
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.region.RankingV2Response
import com.a10miaomiao.bilimiao.comm.entity.region.RankingV2VideoInfo
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

    fun toVideoDetail(video: RegionVideoInfo) {
        pageNavigation.navigateToVideoInfo(video.id)
    }

    fun toSetting() {
        pageNavigation.navigate(TimeSelectSettingPage)
    }

    /** 预读取配置（init 时加载一次） */
    private var cachedConfig: TimeSelectConfig? = null

    /** 待加载的分区索引 */
    private var pendingRegionIndex = 0
    private var pendingRegions: List<RegionInfo> = emptyList()

    val list = FlowPaginationInfo<RegionVideoInfo>()
    val isRefreshing = MutableStateFlow(false)

    private data class TimeSelectConfig(
        val timeFrom: String,
        val timeTo: String,
        val weights: Map<String, Int>,
        val selectedRegions: List<RegionInfo>,
        val pagesPerRegion: Int,
        val pageSize: Int,
        val minDuration: Int,
        val minPlayCount: Int,
    )

    init {
        loadData()
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        list.reset()
        pendingRegionIndex = 0
        cachedConfig = null
        loadData()
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadNextRegion()
        }
    }

    /** 初始加载：读配置 + 加载第一个分区 */
    private fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val config = readConfig()
            cachedConfig = config
            pendingRegions = config.selectedRegions
            pendingRegionIndex = 0

            if (pendingRegions.isEmpty()) {
                list.fail.value = "没有选择分区，请去设置中配置"
                isRefreshing.value = false
                return@launch
            }

            // 只加载第一个分区，后续 loadMore 时逐分区加载
            loadNextRegion(isFirst = true)
        } catch (e: Exception) {
            list.fail.value = "加载失败: ${e.message}"
            isRefreshing.value = false
        }
    }

    /** 加载下一个分区（防爬虫：每次只请求一个分区） */
    private fun loadNextRegion(isFirst: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        val config = cachedConfig ?: return@launch
        val regions = pendingRegions
        if (pendingRegionIndex >= regions.size) {
            list.finished.value = true
            isRefreshing.value = false
            return@launch
        }

        list.loading.value = true
        try {
            val region = regions[pendingRegionIndex]
            pendingRegionIndex++

            val res = BiliApiService.regionAPI
                .regionVideoRanking(rid = region.tid)
                .awaitCall()
                .json<ResultInfo<RankingV2Response>>()
            android.util.Log.d("TimeSelect", "API rid=${region.tid} code=${res.code} count=${res.data?.list?.size ?: 0}")

            if (res.code == 0) {
                val videoList = res.data?.list ?: emptyList()
                val timeFromLong = config.timeFrom.toLongOrNull() ?: 20090901L
                val timeToLong = config.timeTo.toLongOrNull() ?: 99999999L
                android.util.Log.d("TimeSelect", "rid=${region.tid} rawList=${videoList.size} timeFrom=$timeFromLong timeTo=$timeToLong")

                // 过滤 + 转换
                val newVideos = videoList.filter { video ->
                    val dateInt = pubdateToDateInt(video.pubdate)
                    dateInt in timeFromLong..timeToLong
                }.map { it.toRegionVideoInfo() }
                .filter { video ->
                    val passDuration = config.minDuration <= 0 ||
                        (video.duration.toIntOrNull() ?: 0) >= config.minDuration
                    val passPlayCount = config.minPlayCount <= 0 ||
                        (video.play.toLongOrNull() ?: 0L) >= config.minPlayCount
                    passDuration && passPlayCount
                }

                if (newVideos.isNotEmpty()) {
                    android.util.Log.d("TimeSelect", "rid=${region.tid} newVideos=${newVideos.size} after filtering")
                    // 去重后追加到列表，重新排序
                    val existingIds = list.data.value.map { it.id }.toSet()
                    val deduped = newVideos.filter { it.id !in existingIds }
                    if (deduped.isNotEmpty()) {
                        val merged = list.data.value.toMutableList()
                        merged.addAll(deduped)
                        list.data.value = scoreAndSort(merged, config.weights)
                    }
                }
            }

            // 如果最后一个分区或没更多了，标记完成
            if (pendingRegionIndex >= regions.size) {
                list.finished.value = true
            }
            if (isFirst && list.data.value.isEmpty()) {
                list.fail.value = if (pendingRegionIndex >= regions.size) "没有找到符合条件的视频" else "当前分区无数据，下拉加载更多"
            }
        } catch (e: Exception) {
            if (isFirst && list.data.value.isEmpty()) {
                list.fail.value = "加载失败: ${e.message}"
            }
            // 非首次失败不覆盖已有数据
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    /** Unix 时间戳秒 → YYYYMMDD Int */
    private fun pubdateToDateInt(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp * 1000
        return (cal.get(java.util.Calendar.YEAR) * 10000L +
                (cal.get(java.util.Calendar.MONTH) + 1) * 100L +
                cal.get(java.util.Calendar.DAY_OF_MONTH)).toLong()
    }

    private fun scoreAndSort(
        videos: List<RegionVideoInfo>,
        weights: Map<String, Int>,
    ): List<RegionVideoInfo> {
        if (videos.isEmpty()) return videos

        var minFav = Long.MAX_VALUE; var maxFav = Long.MIN_VALUE
        var minClick = Long.MAX_VALUE; var maxClick = Long.MIN_VALUE
        var minDanmaku = Long.MAX_VALUE; var maxDanmaku = Long.MIN_VALUE
        var minReply = Long.MAX_VALUE; var maxReply = Long.MIN_VALUE

        for (v in videos) {
            val fav = v.favorites.toLongOrNull() ?: 0L
            minFav = minOf(minFav, fav)
            maxFav = maxOf(maxFav, fav)

            val click = v.play.toLongOrNull() ?: 0L
            minClick = minOf(minClick, click)
            maxClick = maxOf(maxClick, click)

            val danmaku = v.video_review.toLongOrNull() ?: 0L
            minDanmaku = minOf(minDanmaku, danmaku)
            maxDanmaku = maxOf(maxDanmaku, danmaku)

            val reply = v.review.toLongOrNull() ?: 0L
            minReply = minOf(minReply, reply)
            maxReply = maxOf(maxReply, reply)
        }

        val coinW = 0f
        val favW = (weights["favorite"] ?: 35) / 100f
        val clickW = (weights["click"] ?: 15) / 100f
        val danmakuW = (weights["danmaku"] ?: 5) / 100f
        val replyW = (weights["reply"] ?: 5) / 100f

        data class ScoredVideo(val video: RegionVideoInfo, val score: Float)
        val scored = videos.map { v ->
            val fav = v.favorites.toLongOrNull() ?: 0L
            val click = v.play.toLongOrNull() ?: 0L
            val danmaku = v.video_review.toLongOrNull() ?: 0L
            val reply = v.review.toLongOrNull() ?: 0L

            val normFav = if (maxFav > minFav)
                (fav - minFav).toFloat() / (maxFav - minFav).toFloat() else 0f
            val normClick = if (maxClick > minClick)
                (click - minClick).toFloat() / (maxClick - minClick).toFloat() else 0f
            val normDanmaku = if (maxDanmaku > minDanmaku)
                (danmaku - minDanmaku).toFloat() / (maxDanmaku - minDanmaku).toFloat() else 0f
            val normReply = if (maxReply > minReply)
                (reply - minReply).toFloat() / (maxReply - minReply).toFloat() else 0f

            val score = normFav * favW + normClick * clickW
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

        val weightsStr = prefs[SettingPreferences.TimeSelectWeights]
            ?: SettingConstants.TIME_SELECT_DEFAULT_WEIGHTS
        val allRegions = prefs[SettingPreferences.TimeSelectAllRegions] ?: true
        val selectedRegionIds = prefs[SettingPreferences.TimeSelectSelectedRegions] ?: emptySet()
        val pagesPerRegion = prefs[SettingPreferences.TimeSelectPagesPerRegion]
            ?: SettingConstants.TIME_SELECT_DEFAULT_PAGES
        val minDuration = prefs[SettingPreferences.TimeSelectMinDuration] ?: 0
        val minPlayCount = prefs[SettingPreferences.TimeSelectMinPlayCount] ?: 0

        // 排除最近N天：N=0 表示全部时间
        val excludeDays = prefs[SettingPreferences.TimeSelectExcludeRecent] ?: 0
        val timeFrom = "20090901"
        val timeTo = if (excludeDays <= 0) getTodayStr() else getDateDaysAgo(excludeDays)

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

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.HomeTimeSelect) {
                viewModel.refresh()
            }
        }
    }

    SwipeToRefresh(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(windowInsets.toPaddingValues()),
            columns = GridCells.Adaptive(300.dp),
        ) {
            items(list, { it.id }) { video ->
                VideoItemBox(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    title = video.title,
                    pic = video.pic,
                    upperName = video.author,
                    playNum = video.play,
                    damukuNum = video.video_review,
                    duration = video.duration,
                    onClick = {
                        viewModel.toVideoDetail(video)
                    }
                )
            }
            item(
                span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }
            ) {
                // 居中的加载状态
                if (listLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (listFail.isNotEmpty() && list.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(listFail)
                        }
                        TextButton(onClick = { viewModel.toSetting() }) {
                            Text("前往设置")
                        }
                    }
                }
                if (!listLoading && listFail.isEmpty() && listFinished && list.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("没有符合条件的视频")
                        TextButton(onClick = { viewModel.toSetting() }) {
                            Text("前往设置")
                        }
                    }
                }
            }
        }
    }
}
