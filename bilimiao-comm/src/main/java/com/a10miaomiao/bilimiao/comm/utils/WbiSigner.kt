package com.a10miaomiao.bilimiao.comm.utils

import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.miao.MiaoJson
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * B站 Web API WBI 签名
 * 从 /x/web-interface/nav 获取 img_key + sub_key，混合后 MD5 签名
 */
object WbiSigner {

    /** Mixin 查找表（32位） */
    private val MIXIN_TABLE = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13
    )

    private var mixKey: String? = null
    private var lastFetchDay: Int = -1

    private val REGEX_FILTER = Regex("[!'()*]")

    /** 获取 mix_key（每天刷新一次） */
    suspend fun getMixKey(): String {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        if (mixKey != null && lastFetchDay == today) {
            return mixKey!!
        }
        return fetchAndCacheMixKey().also { mixKey = it; lastFetchDay = today }
    }

    private suspend fun fetchAndCacheMixKey(): String {
        try {
            val response = MiaoHttp.request {
                url = "https://api.bilibili.com/x/web-interface/nav"
            }.awaitCall()
            val navRes = response.body?.string() ?: return ""
            val navData = MiaoJson.fromJson<Map<String, Any>>(navRes)
            val data = navData["data"] as? Map<*, *> ?: return ""
            val wbiImg = data["wbi_img"] as? Map<*, *> ?: return ""
            val imgUrl = wbiImg["img_url"] as? String ?: return ""
            val subUrl = wbiImg["sub_url"] as? String ?: return ""
            val imgKey = imgUrl.substringAfterLast("/").substringBefore(".")
            val subKey = subUrl.substringAfterLast("/").substringBefore(".")
            val rawKey = imgKey + subKey
            return getMixinKey(rawKey)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun getMixinKey(raw: String): String {
        val sb = StringBuilder()
        for (i in MIXIN_TABLE) {
            if (i < raw.length) sb.append(raw[i])
        }
        return sb.toString()
    }

    /** 对 URL 追加 WBI 签名参数（w_rid + wts） */
    suspend fun signUrl(rawUrl: String): String {
        val mixKey = getMixKey()
        if (mixKey.isEmpty()) return rawUrl

        // 解析已有参数
        val qIndex = rawUrl.indexOf('?')
        if (qIndex < 0) return rawUrl

        val base = rawUrl.substring(0, qIndex)
        val query = rawUrl.substring(qIndex + 1)
        val params = mutableMapOf<String, String>()

        query.split("&").forEach { pair ->
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                val value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                params[key] = value
            }
        }

        // 加 wts
        val wts = (System.currentTimeMillis() / 1000).toString()
        params["wts"] = wts

        // 排序 + 过滤特殊字符 + 构建签名字符串
        val sortedKeys = params.keys.sorted()
        val queryParts = sortedKeys.map { key ->
            val value = REGEX_FILTER.replace(params[key] ?: "", "")
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val signStr = queryParts.joinToString("&")

        // MD5
        val md5 = MessageDigest.getInstance("MD5")
        val wRid = md5.digest((signStr + mixKey).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        return "$base?$signStr&w_rid=$wRid"
    }

    /** 同步版（用于非协程环境，谨慎使用） */
    fun signUrlBlocking(rawUrl: String): String {
        return runBlocking { signUrl(rawUrl) }
    }
}
