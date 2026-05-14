package com.a10miaomiao.bilimiao.comm.store

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.view.v1.ViewGRPC
import bilibili.app.view.v1.ViewReq
import com.a10miaomiao.bilimiao.comm.db.FilterTagDB
import com.a10miaomiao.bilimiao.comm.db.FilterUpperDB
import com.a10miaomiao.bilimiao.comm.db.FilterWordDB
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.base.BaseStore
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class FilterStore(override val di: DI) :
    ViewModel(), BaseStore<FilterStore.State> {

    data class State (
        var filterWordList: MutableList<String> = mutableListOf(),
        var filterUpperList: MutableList<FilterUpperDB.Upper> = mutableListOf(),
        var filterTagList: MutableList<String> = mutableListOf()
    )

    override val stateFlow = MutableStateFlow(State())
    override fun copyState() = state.copy()

    private val activity: AppCompatActivity by instance()

    private val filterWordDB = FilterWordDB(activity)

    private val filterUpperDB = FilterUpperDB(activity)

    private val filterTagDB = FilterTagDB(activity)

    val filterWordCount get() = state.filterWordList.size

    val filterUpperCount get() = state.filterUpperList.size

    val filterTagCount get() = state.filterTagList.size

    // 视频最小过滤时长(秒)，0表示不过滤
    var videoMinDuration: Int = 0
    // 视频最小播放量过滤(个)，0表示不过滤
    var videoMinPlayCount: Int = 0

    init {
        // 从设置中加载视频过滤时长
        viewModelScope.launch {
            SettingPreferences.run {
                activity.dataStore.data.collect {
                    videoMinDuration = it[VideoMinDuration] ?: SettingConstants.VIDEO_MIN_DURATION_DEFAULT
                    videoMinPlayCount = it[VideoMinPlayCount] ?: SettingConstants.VIDEO_MIN_PLAY_COUNT_DEFAULT
                }
            }
        }
        queryFilterWord()
        queryFilterUpper()
        queryFilterTag()
    }

    fun queryFilterWord() {
        val list = filterWordDB.queryAll()
        setState {
            filterWordList = list
        }
    }

    fun queryFilterUpper() {
        val list = filterUpperDB.queryAll()
        setState {
            filterUpperList = list
        }
    }

    fun queryFilterTag() {
        val list = filterTagDB.queryAll()
        setState {
            filterTagList = list
        }
    }

    fun filterWord(text: String): Boolean {
        state.filterWordList.forEach {
            if (it.length > 2 && it.startsWith('/')  && it.endsWith('/')) {
                val regEx = it.substring(1, it.length - 1).toRegex()
                if (regEx.containsMatchIn(text))
                    return false
            } else {
                if (it in text)
                    return false
            }
        }
        return true
    }

    fun addWord(keyword: String) {
        filterWordDB.insert(keyword)
        queryFilterWord()
        toast("添加成功")
    }

    fun setWord(oldWord: String, newWord: String) {
        filterWordDB.updateKeyword(oldWord, newWord)
        queryFilterWord()
    }

    fun deleteWord(index: Int) {
        val keyword = state.filterWordList[index]
        filterWordDB.deleteByKeyword(keyword)
        queryFilterWord()
        toast("删除成功")
    }

    fun deleteWord(keywordList: List<String>) {
        keywordList.forEach {
            filterWordDB.deleteByKeyword(it)
        }
        queryFilterWord()
        toast("删除成功")
    }

    fun addUpper(mid: Long, name: String) {
        filterUpperDB.insert(mid, name)
        queryFilterUpper()
        toast("已添加屏蔽")
    }

    fun deleteUpper(mid: Long) {
        filterUpperDB.deleteByMid(mid)
        queryFilterUpper()
        toast("已取消屏蔽")
    }

    fun deleteUpper(midList: List<Long>) {
        midList.forEach {
            filterUpperDB.deleteByMid(it)
        }
        queryFilterUpper()
        toast("删除成功")
    }

    fun filterUpper(mid: String) = filterUpper(mid.toLong())
    fun filterUpper(mid: Long): Boolean {
        state.filterUpperList.forEach {
            if (it.mid == mid)
                return false
        }
        return true
    }

    fun filterUpperName(name: String): Boolean {
        // TODO: 筛选UP主昵称
        return true
    }

    fun filterTag(text: List<String>): Boolean {
        text.forEach {
            if (state.filterTagList.contains(it)) {
                return false
            }
        }
        return true
    }

    /**
     * 根据av号或bv号筛选视频标签
     */
    suspend fun filterTag(
        id: String, // av号或bv号
    ): Boolean {
        if (state.filterTagList.isEmpty()) {
            return true
        }
        val req = if (id.startsWith("BV")) {
            ViewReq(
                bvid = id,
            )
        } else {
            ViewReq(
                aid = id.toLong(),
            )
        }
        val res = BiliGRPCHttp.request {
            ViewGRPC.view(req)
        }.awaitCall()
        val tags = res.tag.map { it.name }
        return filterTag(tags)
    }

    fun addTag(name: String) {
        filterTagDB.insert(name)
        queryFilterTag()
        toast("添加成功")
    }

    fun setTag(old: String, new: String) {
        filterTagDB.updateTagName(old, new)
        queryFilterWord()
    }

    fun deleteTag(index: Int) {
        val name = state.filterTagList[index]
        filterTagDB.deleteByTagName(name)
        queryFilterTag()
        toast("删除成功")
    }

    fun deleteTag(tagList: List<String>) {
        tagList.forEach {
            filterTagDB.deleteByTagName(it)
        }
        queryFilterTag()
        toast("删除成功")
    }

    fun filterTagListIsEmpty() = state.filterTagList.isEmpty()

    /**
     * 过滤视频时长（文字版）
     * @param durationText 时长文本，如 "03:24"
     * @return true 表示通过，false 表示被过滤
     */
    fun filterDuration(durationText: String?): Boolean {
        if (videoMinDuration <= 0 || durationText.isNullOrBlank()) {
            return true
        }
        // 解析时长文本 "03:24" -> 秒数
        val seconds = parseDurationToSeconds(durationText)
        return seconds >= videoMinDuration
    }

    /**
     * 将时长文本解析为秒数
     * 支持格式: "03:24" (分:秒), "1:23:45" (时:分:秒)
     */
    /**
     * 过滤视频时长（秒数版）
     * @param durationSeconds 时长秒数
     * @return true 表示通过，false 表示被过滤
     */
    fun filterDuration(durationSeconds: Int): Boolean {
        if (videoMinDuration <= 0) {
            return true
        }
        return durationSeconds >= videoMinDuration
    }

    /** 过滤播放量（文本格式） */
    fun filterPlayCount(playText: String?): Boolean {
        if (videoMinPlayCount <= 0) return true
        if (playText.isNullOrBlank()) return true
        val count = NumberUtil.parseToLong(playText)
        return count >= videoMinPlayCount
    }

    private fun parseDurationToSeconds(text: String): Int {
        val parts = text.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]  // 时:分:秒
            2 -> parts[0] * 60 + parts[1]                      // 分:秒
            1 -> parts[0]                                      // 秒
            else -> 0
        }
    }
}