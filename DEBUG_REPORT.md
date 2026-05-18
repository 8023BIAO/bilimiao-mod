# bilimiao2-mod 全面 Debug 报告

生成日期: 2026-05-18
扫描范围: 539 个 Kotlin 源文件 + 64 个 protobuf 定义 + 构建配置

---

## 🔴 严重等级说明

| 等级 | 含义 |
|------|------|
| 🔴 CRITICAL | 必定触发崩溃/功能完全不可用 |
| 🟠 HIGH | 特定条件下崩溃或严重逻辑错误 |
| 🟡 MEDIUM | 功能异常但不崩溃，或潜在风险 |
| 🔵 LOW | 代码质量问题，可优化 |

---

## 一、🔴 CRITICAL — 必须马上修

### 1. gRPC 帧长度编码完全错误

**文件:** `BiliGRPCHttp.kt:63`
**影响:** 所有 gRPC 请求（热门、动态、空间等）的 protobuf 载荷超过 255 字节时，服务器收到的消息长度错误

```kotlin
// 当前错误代码 (line 63):
val stateBytes = byteArrayOf(0, 0, 0, 0, messageBytes.size.toByte())

// 正确应该是（Big-Endian 4字节长度）:
// [0 (compression)] [len >> 24] [len >> 16] [len >> 8] [len]
```

`messageBytes.size.toByte()` 只取 Int 最低 8 位。300 字节的消息 → length=44，256 字节 → length=0。**大部分 gRPC 请求都超过 255 字节，所以大部分 gRPC 功能实际上都坏的。** 解析侧同样有问题（`inputStream.skip(5L)` 直接丢弃长度信息，硬编码偏移读取）。

**修复:** 用 `ByteBuffer.allocate(5).put(0).putInt(messageBytes.size).array()` 替代。解析侧也需按实际长度读取。

### 2. gRPC HTTP 错误码不检查

**文件:** `BiliGRPCHttp.kt` — `onResponse` 方法
**影响:** 服务端返回 401/403/500 时没有检查 `response.isSuccessful`，直接走 protobuf 解码路径，用户看到的是"protobuf 解码异常"的崩溃，不是可读的错误信息。

**修复:** 在 `parseResponse` 入口加 `if (!res.isSuccessful) throw HttpException(res.code, res.message)`。

### 3. gRPC 响应体不关闭（内存泄漏）

**文件:** `BiliGRPCHttp.kt` — `parseResponse()` 和 `awaitCall()`
**影响:** OkHttp Response body 从未关闭。每次 gRPC 请求泄漏一个连接池条目，多次调用后连接池耗尽，所有 gRPC 请求卡死。

**修复:** 用 `use {}` 或 `try/finally` 确保 `response.body.close()`。

### 4. gRPC 缺少必需请求头

**文件:** `BiliGRPCHttp.kt` — `addHeaders()` 方法
**影响:** 缺少 `x-bili-trace-id`、`x-bili-aurora-eid`、`x-bili-aurora-zone`、`x-bili-exps-bin` 四个 proto 文档标注为 **必需** 的头。服务端可能因此拒绝请求或错误路由。

**修复:** 按 `protobuf/readme.md` 规范补全。

---

## 二、🟠 HIGH — 尽快修

### 5. 全项目 89 处 `!!` 强制非空断言

**数量:** 89 处，其中 8 处 CRITICAL 级别、17 处 HIGH

**高频危险模式：**
- 网络响应体 `response.body!!.string()` — 至少 5 处（服务器返回空响应时崩溃）
- API JSON 字段 `data["key"]!!` — 至少 8 处（API 格式变更时崩溃）
- 导航参数 `uri!!`、`params["id"]!!` — 至少 4 处（导航到错误页面时崩溃）
- `lateinit var` 未初始化前访问 — `MainActivity.kt:98` `mainUi!!`（异步初始化竞态）

**典型例子（`PlayerController.kt:941`）：**
```kotlin
response.body!!.string()  // body 可能为 null
```
改成:
```kotlin
response.body?.string() ?: ""
```

### 6. MiaoHttp 可重入/线程安全问题

**文件:** `MiaoHttp.kt`
- **`url!!` 强制解包** — `buildRequest()` 中 `Request.Builder().url(url!!)`，如果 `url` 未设置就调用 `get()`/`post()` 直接 NPE
- **`requestBuilder` 是实例字段** — 同个 `MiaoHttp` 实例复用 `Request.Builder`，累计上次请求的头，导致头膨胀
- **`headers[key]!!` 强制解包** — 如果设了 `headers["X-Foo"] = null`（绕过了类型系统），解包时崩溃

### 7. 登录缺失：MyFollowPage 完全无登录拦截

**文件:** `MyFollowPage.kt`
**影响:** 未登录用户可以进入关注页面，看到空内容，调用 API 失败，没有任何提示。`isLogin()` 变量被计算但**从未使用**。

**修复:** 同 `DynamicPage.kt` 的模式，入口处判断 `if (!isLogin) { Text("请先登录") }`。

### 8. 登录缺失：ChatPage 完全无登录拦截

**文件:** `ChatPage.kt`
**影响:** 未登录用户进入私信页面，看到空 UI，`myUid` 返回 0L 发送给 API。发消息时只有 CSRF 校验没有前置提示。

### 9. ReplyDetailContent 发送回复按钮无登录检查

**文件:** `ReplyDetailContent.kt` — `openReplyDialog()` 方法
**影响:** 未登录用户也能点开回复输入框，输入内容后提交才报错。对比 `likeReplyAt()` 已有正确的 `if (!isLogin) { toast() }` 模式，不一致。

### 10. VideoDetailContent 下载按钮无登录检查

**文件:** `VideoDetailContent.kt` — `openDownloadDialog()`
**影响:** 未登录用户可以打开下载对话框（可能需要鉴权的 API 请求），而点赞/投币/收藏按钮都有 `checkLogin()`。

### 11. MiaoHttp CookieManager 异常被吞

**文件:** `MiaoHttp.kt` — `cookieManager` 懒初始化
**影响:** `CookieManager.getInstance()` 首次异常时 `catch` 返回 null，后续所有请求 cookie 为空。B站 API 对无 cookie 的请求静默拒绝或返回不同数据。没有日志、没有提示。

### 12. HomeRecommendContent 分页刷新的竞态

**文件:** `HomeRecommendContent.kt` — `loadData()` 和 `loadMore()`
**影响:** 快速下拉刷新时，`loadData(0)` 和 `loadMore()` 可能同时运行，导致：
- 旧请求覆盖新请求的结果
- 同一个 `loadData` 内对 `list.data.value` 多次赋值（tag 过滤分支每次匹配都赋值一次），触发多次重组

---

## 三、🟡 MEDIUM — 建议修

### 13. FlagsSettingPage 单次快照而非响应式

**文件:** `FlagsSettingPage.kt:320`
```kotlin
var loginInfoState by remember { mutableStateOf(BilimiaoCommApp.commApp.loginInfo) }
```
只在组合时取一次快照。如果在其他页面登录/登出，此页面显示过期状态。

### 14. FlagsSettingPage 乐观更新无确认

**文件:** `FlagsSettingPage.kt:406-410`
切换访客模式时，`loginInfoState = null` 在 `toggleGuestMode()` 成功前就设置了。如果操作失败，UI 永久卡在"返回登录"状态。

### 15. Guest Mode 使用 SharedPreferences commit() 在主线程

**文件:** `FlagsSettingPage.kt:69`
```kotlin
prefs.edit().putString(...).commit()  // 同步磁盘 I/O，阻塞主线程
```
改成 `.apply()` 或使用 DataStore。

### 16. DPI 设置使用不同存储机制

**文件:** `FlagsSettingPage.kt:600-604`
DPI 存储在 `PreferenceManager.getDefaultSharedPreferences()` 而非 DataStore。其他设置全用 DataStore，不一致。

### 17. HomeRecommendContent 字符串拼接 bug

**文件:** `HomeRecommendContent.kt` catch 块
```kotlin
toast("加载失败:" + e.message ?: e.toString())
```
运算符优先级问题：`+` 先于 `?:`，`e.message` 为 null 时输出 `"加载失败:null"`，不会 fallback 到 `e.toString()`。

### 18. ApiHelper.getSing() 可能错误截取 URL

**文件:** `ApiHelper.kt`
```kotlin
var str = url.substring(url.indexOf("?", 4) + 1)
```
如果 URL 前 4 个字符内有 `?` 或没有 `?`，`indexOf` 返回 -1 → `substring(0)` 拿整个 URL 去签名 → 签名错误。

### 19. 分区过滤接口缺失

**文件:** `HomeRecommendContent.kt` / 首页推荐
首页推荐 API 响应不含 `tid`（分区 ID）。要按分区过滤必须对每条视频单独发请求，一页 20 条 = 20 次额外请求 → 触发风控 404。参考 `references/homepage-api-field-availability.md`。

### 20. 下载模块多处 `!!` 风险

**文件:** `bilimiao-download/` 下 `BiliPalyUrlHelper.kt`、`DownloadService.kt`、`DownloadManager.kt`
合计 7 处 `!!` 对 `avid`、`season_id`、`body`、`headers[key]` 做强制解包。下载流程中任何数据缺失都直接崩溃，没有降级。

---

## 四、🔵 LOW — 可优化

### 21. `ApiHelper.getSing` 名称拼写错误
应该是 `getSign` 不是 `getSing`。不影响运行但影响可读性。

### 22. ViewModel 未清理的上游协程
多个 ViewModel 使用 `viewModelScope.launch` 启动的协程在 scope 取消时自然取消，但未显式处理 `CancellationException`，导致 `catch (e: Exception)` 吞掉取消信号。

### 23. 硬编码 API 域名
`BiliApiService.kt` 中 `api.bilibili.com`、`app.bilibili.com` 等域名硬编码。如果 B 站 CDN 切换或需要代理调试时需改多处。

### 24. `toast` 工具在协程外可能有问题
自定义扩展 `Context.toast()` / `Fragment.toast()` 可能在非主线程调用时报错（参考 toast-notification-system.md 的架构）。

---

## 五、总体统计

| 类别 | 数量 |
|------|------|
| 🔴 CRITICAL 级问题 | 4 |
| 🟠 HIGH 级问题 | 16 |
| 🟡 MEDIUM 级问题 | 8 |
| 🔵 LOW 级问题 | 4 |
| **合计** | **32** |

### 最优先修复的三个

1. **gRPC 帧长度编码** — 使 gRPC 功能大部分不可用，影响热门、动态、空间等核心页面
2. **MyFollowPage 缺登录门** — 未登录崩溃/无提示，入口暴露在导航栏
3. **gRPC 响应体不关闭** — 内存泄漏，长期运行必出问题

---

*报告由 AI 静态分析生成，建议逐条验证后再修复。详细的参考文件见 skill 的 `references/` 目录。*
