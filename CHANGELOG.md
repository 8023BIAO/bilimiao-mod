# bilimiao2-mod 改版清单

> 对比原版 bilimiao2 v2.4.8.1 → 当前 mod 版本

## v8 (2026.05.08-01) — EULA删除 + 崩溃界面还原 + 百度统计移除 + 播放状态同步修复

### EULA 彻底删除
- 删除 `EulaScreen.kt` 弹窗文件及所有引用
- 删除 `SettingPreferences.EulaAccepted`、`EulaAcceptedVersion`
- 清理 `ComposeFragment.kt`、`MainActivity.kt` 中所有 EULA 注释代码和 `setBottomBarVisible()` 方法

### 崩溃界面恢复原版清爽风格
- `LogViewerActivity.kt` 恢复为原版：简单 `TextView`（可长按选择）包在 `ScrollView` 里
- 菜单仅保留"复制日志"按钮，删除全选/删除选中/复选框功能
- Toast 提示改回"程序崩溃了喵＞﹏＜"
- 删除 `item_log_line.xml` 布局文件

### 百度移动统计彻底移除
- 删除 `gradle/libs.versions.toml` 中的 `baidu-mobstat-sdk` 依赖定义
- 删除 `app/build.gradle.kts` 中的 `"fullImplementation"(libs.baidu.mobstat.sdk)`
- 删除 `app/src/full/.../BilimiaoStatService.kt` 和 `app/src/foss/.../BilimiaoStatService.kt`
- 清空 `app/src/full/AndroidManifest.xml` 中的百度统计 meta-data
- `MainActivity.kt` 移除 `BilimiaoStatService.start()`/`onResume()`/`onPause()` 调用
- `AppCrashHandler.kt` 移除 `BilimiaoStatService.recordException()` 调用

### 播放状态与通知栏同步修复
- `PlayerController.setStateAndUi()` 增加兜底同步：根据 `CURRENT_STATE_PLAYING`/`CURRENT_STATE_PAUSE`/`CURRENT_STATE_AUTO_COMPLETE` 自动调用 `PlaybackService.setPlaying()`
- `DanmakuVideoPlayer.clickStartIcon()` 移除冗余的 `PlaybackService` 调用，避免重复执行

---

## v7 (2026.05.07-02) — 底栏配色 + 通知栏修复 + UI 简化

### 通知栏播放器
- **完全重写**：MediaSessionService → 纯 Service + 手动构建 MediaStyle 通知
- **通知栏按钮失效修复**：startForegroundService 异步导致 startForeground 超时 → Handler.post 延迟绑定 + 仅通知栏开启时启动 Service
- **自动联播通知栏信息不更新**：loadPlayerSource 末尾刷新 updateMetadata + setPlaying
- **通知权限仅首次弹窗**：SharedPreferences 标记，不再每次启动都弹
- **播放/暂停双向同步**：DanmakuVideoPlayer.onVideoPause/onVideoResume → PlaybackService.setPlaying()
- 通知开关检查：PlayerNotification DataStore 设置控制通知栏显示
- 文件：PlaybackService.kt、PlayerDelegate2.kt、DanmakuVideoPlayer.kt、MainActivity.kt、AndroidManifest.xml

### 底栏配色（恢复原版）
- **选中 tab 高亮**：仅首页/动态当前选中项显示 themeColor
- **其余全部灰色**：未选中 tab + 视频详情底栏（返回/添加/下载/更多）+ 所有菜单项 → foregroundAlpha45Color
- 移除 MenuItemView 的 themeColor 属性（不再污染所有菜单项）
- AppBarVerticalUi.updateTheme() 仅对 MenuCheckableItemView 传色
- 文件：MenuItemView.kt、MenuCheckableItemView.kt、AppBarVerticalUi.kt

### 播放器 UI 简化
- **弹窗选中指示**：5 个弹窗文件改用 Material3 原生 isChecked，删除 GradientDrawable 自定义圆点代码（-115 行）
- **全屏状态栏**：全屏时强制显示状态栏
- 文件：QualityPopupMenu.kt、SpeedPopupMenu.kt、ScalePopupMenu.kt、MyPopupMenu.kt、CheckPopupMenu.kt、PlayerController.kt

---

## v6 (2026.05.07-01) — AV号隐藏 + 编译修复

### 评论tab底栏AV号隐藏
- VideoReplyContent.kt pageTitle 清空，底栏不再显示 AV+BV 编号
- 文件：`VideoReplyContent.kt`

### 编译修复
- DynamicAllListContent/DynamicVideoListContent 补回 FilterStore import
- 上个会话改代码时被错误替换成 UserStore，导致 full 变体编译失败
- 文件：`DynamicAllListContent.kt`、`DynamicVideoListContent.kt`

### 默认编译变体
- 默认变体从 foss → full（含闭源库完整版）
- 编译命令改为 `assembleFullRelease`

---

## v5 (2026.05.06-02) — 弹幕空降 + 排序 + 崩溃修复

### 弹幕时间戳可点击
- 弹幕时间戳（00:00 / 00:00:00）显示蓝色，点击跳转
- 坐标转换修复：DanmakuVideoPlayer → DanmakuView 偏移
- 文件：DanmakuVideoPlayer.kt、PlayerController.kt

### 评论区时间戳修复
- 非 VideoDetailPage 上下文也能点击（改用 PlayerSeekBus 全局桥接）
- 中文冒号：和单数字秒兼容
- 文件：AnnotatedText.kt、ReplyItemBox.kt

### 弹幕触摸事件冲突修复
- DanmakuTouchHelper.onDown() 无可见弹幕时返回 false 放行
- 弹幕点击检测从 DanmakuView 移到 DanmakuVideoPlayer
- 文件：DanmakuTouchHelper.java、DanmakuVideoPlayer.kt、PlayerController.kt

### 个人主页排序
- 投稿页新增「最旧发布」排序
- 搜索投稿页新增排序功能
- 排序图标统一为 ic_sort_gray_24dp
- 文件：UserArchiveViewModel.kt、UserSpacePage.kt、UserSpaceViewModel.kt、UserSpaceSearchPage.kt、UserSearchArchiveContent.kt

### 播放器崩溃修复
- PlaybackService 秒退崩溃：懒初始化 MediaSession，不建空播放器
- videoPlayer 全链路空安全（43+32处）：scaffold fallback
- 文件：PlaybackService.kt、PlayerViews.kt、PlayerDelegate2.kt、PlayerController.kt

### 其他
- 后台播放入口默认值一致（关）
- 设置页「关于」入口 → GitHub 项目链接
- 仓库清理：删除 img/、fastlane/、Eclipse 残留

---

## v4 (2026.05.06-01) — 评论区时间戳 + 空降助手

### 评论区时间戳可点击
- 评论区时间戳（00:00 / 00:00:00）显示蓝色
- 点击后视频跳转到对应时间
- 实现：TimestampHelper → AnnotatedText (LinkAnnotation.Clickable) → PlayerSeekBus → seekTo
- 参考 [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus) 方案

### 空降助手
- 基于 [空降助手](https://github.com/hanydd/BilibiliSponsorBlock) bsbsb.top API
- 自动跳过赞助广告、开场动画等片段
- AV/BV转换、SHA-256 hash、MiaoHttp 请求
- 设置页开关（实验功能）

### README更新
- 添加致谢链接与声明

---
> 统计：100+ 个文件改动，+3500 / -3500 行，50+ 个 commit（去掉备份/checkpoint/README 后约 40 个有效 commit）
> 改动周期：2026-04-22 → 2026-05-05（约 14 天）

---

## 一、🏗️ 项目基础

### 1. 包名（应用共存）
- **applicationId**: `com.a10miaomiao.bilimiao` → `com.a10miaomiao.bilimiao.mod`
- 与原版可同时安装，互不冲突
- 文件：`app/build.gradle.kts`

### 2. 版本号
- `versionCode=117` → `versionCode=1`
- `versionName="2.4.8.1"` → `versionName="2026.05.04-01"`
- 文件：`app/build.gradle.kts`

### 3. ProGuard/R8 规则
- 从零散规则 → 精简整理为 94 行
- 保留 Kodein TypeReference（解决 R8 擦除崩溃）
- 保留 Material3/Splitties/Kotlin 反射
- 禁用混淆（`-dontobfuscate`）
- 禁用资源收缩（`shrinkResources=false`）
- 文件：`app/proguard-rules.pro`

### 4. Gradle 升级
- Gradle wrapper：8.10.2 → 8.13
- AGP：8.7.3 → 8.13（隐含在 libs.versions.toml）
- 文件：`gradle/wrapper/gradle-wrapper.properties`

### 5. GitHub Actions
- 删除上游 CI 工作流（`.github/workflows/android.yml`），不走自动构建

---

## 二、🛠️ 稳定性修复（崩溃/Bug修复）

### 1. R8 编译崩溃
- **症状**：Kodein DI 的 `TypeReference` 泛型参数被 R8 擦除 → runtime 崩溃
- **修复**：添加 keep 规则 `-keep class org.kodein.type.** { *; }`
- 文件：`app/proguard-rules.pro`

### 2. MaterialToolbar 构造异常
- **症状**：`view<MaterialToolbar>()` 反射构造 → `NoSuchMethodException`
- **修复**：改用 `MaterialToolbar(activity)` 直接构造器
- 涉及的 Activity：`DensitySettingActivity.kt`、`LogViewerActivity.kt`

### 3. DynamicPage 序列化崩溃
- **症状**：`class DynamicPage` 未正确序列化
- **修复**：`class` → `object`
- 文件：`bilimiao-compose/.../DynamicPage.kt`

### 4. DanmakuVideoPlayer 空指针
- **症状**：`currentDanmakuMode` 和 `setWindowInsets` 空指针
- **修复**：安全调用保护（`?.`、`?:`）
- 文件：`DanmakuVideoPlayer.kt`

### 5. 收藏夹 mid 为空
- **症状**：进入收藏夹时没传 mid → 加载失败
- **修复**：mid 为空时从 userStore 取当前用户 ID 兜底
- 文件：`UserFavouriteViewModel.kt`

### 6. 播放器 Surface 丢失
- **症状**：页面切换后 Surface 丢失，画面黑屏
- **修复**：`PlayerDelegate2.onStart` 添加状态恢复逻辑
- 文件：`PlayerDelegate2.kt`

### 7. DownloadService JSON 崩溃
- **症状**：`readDownloadDirectory` 解析 JSON 遇到未转义字符 → `JsonDecodingException`
- **修复**：添加 try-catch，解析异常时跳过损坏文件
- 文件：`DownloadService.kt`

### 8. LocalPlayerSource 兼容旧数据
- **症状**：旧版 `index.json` 字段名不同（`evideo`/`audiol` 等）→ 解析失败
- **修复**：添加 `parseType2()` 回退逻辑，兼容旧字段名
- 文件：`LocalPlayerSource.kt`

### 9. 内容区布局大重构
- **症状**：原版 ContentBehavior 约 380 行，管理方式复杂
- **修改**：大幅简化 ContentBehavior、PlayerBehaviorDelegate、AppBarHorizontalUi
  - ContentBehavior：382 行 → 精简到 ~50 行（实际是大量代码被删除或简化）
  - AppBarHorizontalUi：整个 320 行文件被删除
  - PlayerBehaviorDelegate：99 行 → 精简

---

## 三、🎮 播放器功能

### 1. 小窗/画中画（PIP）
- 后台小窗播放开关（`PlayerPipOnBackground` DataStore 设置项）
- `onUserLeaveHint` + `onPause` 触发 `tryEnterPipOnBackground()`
- 小窗全屏切换支持
- 文件：`PlayerDelegate2.kt`、`PlayerController.kt`

### 2. 底栏锁定
- **功能**：锁定底部导航栏，防止滑动时隐藏
- **实现**：
  - `AppBarBehavior` 内维护独立 `locked` 字段 + 全局 `globalLock`（跨实例持久化）
  - `onStartNestedScroll`、`onNestedScroll` 锁定时拦截
  - `onLayoutChild` 中锁定时强制重置 `translationY=0`
  - `MainActivity` 锁定时主动 `slideUp` 兜底
- **UI**：首页 → 实用功能 → 锁定底栏开关
- 文件：`AppBarBehavior.kt`、`ScaffoldView.kt`、`MainActivity.kt`、`FlagsSettingPage.kt`

### 3. 弹幕暂停/恢复
- 发弹幕按钮同时触发暂停
- 非全屏时发送弹幕也应暂停（统一行为）
- 修复了暂停后恢复的逻辑
- 文件：`PlayerController.kt`

### 4. 合集排序
- 合集详情页支持三种排序：按 aid 降序（默认）/ 升序 / 热度（stat.view）
- 排序菜单带图标
- 文件：`UserSeasonDetailContent.kt`

### 5. 自动连播
- 优先级：下一 P → 下一番剧 → 下一播放列表项 → 推荐 → 循环 → 完成弹窗
- 播放列表检查支持
- 文件：`PlayerController.kt`、`VideoDetailContent.kt`

### 6. 定时关闭快捷入口
- 播放器更多菜单新增"定时关闭"选项
- 文件：`player_top_more.xml`（含 menu-v26 版本）、`PlayerController.kt`

### 7. 隐藏相关推荐
- 视频详情页可隐藏"相关推荐"列表
- DataStore 设置项：`VideoHideRelates`
- 文件：`VideoSettingPage.kt`、`VideoDetailContent.kt`

### 8. 隐藏封面
- 视频封面隐藏，首页/动态/播放器均生效
- 文件：`MiniVideoItemBox.kt`、`VideoItemBox.kt`

---

## 四、💬 弹幕过滤

### 1. DanmakuTextFilter（全新文件）
- **关键词过滤**：匹配弹幕文本中包含指定关键词的弹幕
- **重复合并**：按时间间隔合并重复弹幕
- 过滤引擎独立类，可复用
- 文件：`DanmakuTextFilter.kt`（新建，75 行）

### 2. 弹幕过滤设置界面
- `DanmakuDisplaySettingPage` 重构，新增 3 个 tab：
  - 默认显示设置
  - 全屏模式显示设置
  - **弹幕过滤**（新增）
- 弹幕过滤设置包含：
  - 启用/禁用开关
  - 弹幕过滤关键词列表管理弹窗（增、删、清空）
  - 屏蔽重复弹幕开关
- 文件：`DanmakuDisplaySettingPage.kt`、`SettingPreferences.kt`

### 3. DanmakuSettingPage 精简
- 原版 4 个独立显示设置入口（默认/小屏/全屏/小窗）→ 合并为 1 个"弹幕显示设置"
- 弹幕过滤入口移除（由 DanmakuDisplaySettingPage tab 统一管理）
- 文件：`DanmakuSettingPage.kt`

---

## 五、🔇 评论屏蔽

### 1. FilterCommentWordListContent（全新文件）
- 完整评论关键词屏蔽管理页面
- 支持：添加关键词、单条删除、多选删除
- 基于 DataStore 的 `CommentBlockedWords` key
- 文件：`FilterCommentWordListContent.kt`（新建，261 行）

### 2. 评论列表过滤
- `ReplyListContent` 在渲染前过滤评论
- 匹配 `reply.content.message` 中包含屏蔽词的评论
- 文件：`ReplyListContent.kt`

### 3. 设置页面入口
- `FilterSettingPage` 新增"评论屏蔽词"入口
- 删除屏蔽页底部"可进设置帮助查看使用方法"文字
- 文件：`FilterSettingPage.kt`

---

## 六、📤 设置导入/导出

### 1. SettingsExporter（全新文件）
- 完整覆盖 **4 层存储**的导出/导入：
  - DataStore `settings`：约 80 个键（播放、弹幕、主题、首页等）
  - SQLite `filter_db`：屏蔽关键字、屏蔽 UP 主、屏蔽标签
  - SharedPreferences：时光姬时间设置、代理 UPOS、DPI、字体缩放、下载画质
  - JSON 文件：代理服务器列表
- 导出格式：JSON，支持全量导入
- 文件：`SettingsExporter.kt`（新建，225 行）

### 2. ExportSettingPage（全新文件）
- 导出/导入设置 UI 页面
- 使用 SAF（存储访问框架）文件选择
- 导入后自动 `killProcess` 重启应用
- 文件：`ExportSettingPage.kt`（新建，227 行）

---

## 七、🎨 设置 UI 改进

### 1. 首页入口
- 首页导航栏新增"动态"选项
- 文件：`HomePage.kt`

### 2. 设置页重构（多次迭代）
- **第一次尝试**（被否决）：分为 6 大类（常规/播放/弹幕/过滤/实用/实验）
- **最终状态**：维持原版分级导航，仅增删特定选项
- **成功保留**：DPI/图片缓存移到"实用功能"分类
- 文件：`SettingPage.kt`（176 行变更）

### 3. DPI 设置
- 改用显式 Intent 替代 `Class.forName`（避免 R8 反射问题）
- 文件：`DensitySettingActivity.kt`

### 4. IP 属地简化
- 原版：`IP属地：广东` → 简化：`广东`
- 去掉"IP属地："前缀和冒号
- 文件：`ReplyItemBox.kt`（仅改 1 行）

### 5. 下载按钮移动
- 从底栏移除，放到"更多"菜单中
- 文件：`VideoDetailContent.kt`、`AppBarView.kt`

### 6. 排序菜单带图标
- 合集排序菜单加了图标（用户强迫症需求）
- 文件：`UserSeasonDetailContent.kt`

### 7. 主页设置项
- `HomeSettingPage` 新增若干设置项
- 文件：`HomeSettingPage.kt`

### 8. 播放器视频设置
- `VideoSettingPage` 新增设置项（底栏锁定、隐藏相关推荐等）
- 文件：`VideoSettingPage.kt`

---

## 八、📱 封面/弹窗

### 1. 封面保存弹窗
- **问题**：DialogX PopTip 白色背景不跟随暗色主题
- **尝试**：`ThemeDelegate` → `getDefaultNightMode()` → DataStore 读取 → 自定义 overlay
- **备注**：最终也未找到完美的暗色适配方案，白色背景仅在暗色主题下短暂闪现后可接受
- 文件：`CoverActivity.kt`、`build.gradle.kts（datastore 依赖）`

### 2. 封面保存 Activity
- `CoverActivity.kt`：62 行变更，重构保存提示弹窗

---

## 九、⏪ 尝试后回退的功能

| 功能 | 尝试方式 | 回退原因 |
|------|---------|----------|
| 时光精选首页 | newlist_rank API 完整实现 | API 已废弃 |
| WebView 暗色适配 | CSS 注入 → FORCE_DARK → 最终放弃 | FORCE_DARK 在哔哩喵内无效 |
| 视频详情 tag 折叠 | 最多显示 5 个，多余隐藏 | 用户不喜欢，回退到全部显示 |
| 设置页 6 大类重构 | 完整分类 | 用户否决，回退到原始结构 |

---

## 十、🗑️ 删除的文件

| 文件 | 说明 |
|------|------|
| `.github/workflows/android.yml` | 上游 CI，fork 不用 |
| `项目说明.md` | 私有文档，移入 gitignore |
| `AboutPage.kt` | 完全删除（530 行），用户移除 |
| `AppBarHorizontalUi.kt` | 内容区布局大幅简化，320 行删除 |

---

## 十一、📊 改动统计总览

### 按 commit 类型
- **feat（新功能）**：7 个
- **fix（修复）**：10 个
- **checkpoint（备份）**：3 个
- **chore/docs（杂项）**：6 个
- **revert（回退）**：3 个

### 按功能大小
- **设置导入导出**：452 行（2 个文件新建）
- **评论屏蔽**：276 行（1 个文件新建）
- **弹幕过滤**：272 行（1 个文件新建 + 多处修改）
- **播放器功能**：~200 行（PIP + 底栏锁定 + 弹幕暂停 + 合集排序）
- **设置 UI**：~300 行（多处页面修改）

### 影响最大的文件（按修改行数）
| 文件 | 类型 | 行数 |
|------|------|------|
| `ContentBehavior.kt` | 大幅简化 | -382 |
| `AppBarHorizontalUi.kt` | **删除** | -320 |
| `AboutPage.kt` | **删除** | -530 |
| `HomePage.kt` | 重构首页 | -198 |
| `SettingPage.kt` | 多次迭代 | -176 |
| `MainActivity.kt` | 底栏/功能/空间 | -157 |
| `FlagsSettingPage.kt` | 设置项 | -147 |
| `PlayerController.kt` | 播放器功能 | +117 |
| `TextIntPreference.kt` | 新增组件 | +106 |
| `ScaffoldView.kt` | 底栏/布局 | -106 |

---

## 十二、🛡️ 免责与合规 (2026-05-05)

### 1. EULA 首次启动弹窗
- **功能**：首次启动 / 版本更新时全屏覆盖层
- **逻辑**：10 秒倒计时，拒绝则退出 APP，接受后写 DataStore 永久记录
- **版本检测**：`EulaAcceptedVersion` 比较当前 versionName，版本不同则重新弹出
- 文件：`EulaScreen.kt`（新建）、`ComposeFragment.kt`、`SettingPreferences.kt`

### 2. EULA 免费声明（条款 0）
- 明确标注：免费、非盈利、非商用
- 付费获得 → 被骗，应立即举报退款
- 文件：`EulaScreen.kt`

### 3. 关于弹窗
- 设置页"关于"入口，展示原版链接（可点击超链接）+ 使用声明
- 声明：仅供学习、禁止商业、B站API随时失效、风险自担
- 文件：`SettingPage.kt`

### 4. GitHub 仓库
- 原公开仓库已删除（怕版权风险）→ 改为私有仓库备份

---

## 十三、🏠 首页去重 (2026-05-05)

### URI 去重机制
- 推荐/热门页面维护已显示视频 URI 集合
- 不刷新时翻页自动过滤已出现视频（对抗 B站重复推送）
- 刷新时清空集合
- 文件：`HomeRecommendContent.kt`、`HomePopularContent.kt`

---

## 十四、🔐 身份导入 (2026-05-05)

### 导入身份信息
- 从剪贴板读取 JSON → 重建 LoginInfo → 加密写入 auth_hd
- 支持跨设备迁移登录态（不用反复手机号登录）
- 从"导出身份信息"中复制，到另一台设备"导入身份信息"粘贴
- 文件：`FlagsSettingPage.kt`

---

## 十五、📥 存储权限优化 (2026-05-04)

### 1. 下载目录
- 从沙盒 `Android/data/` 改为公共 `Download/BiliMiao/`
- 写入失败静默回退沙盒（不崩不弹权限）
- 移除 `READ/WRITE_EXTERNAL_STORAGE` 权限声明
- 保留 `requestLegacyExternalStorage` 兼容 MIUI 等系统
- 文件：`DownloadService.kt`、`AndroidManifest.xml`

### 2. 图片/封面保存
- MediaStore 优先，失败回退私有目录
- 移除所有存储权限运行时请求
- 文件：`ImageSaveUtil.kt`、`CoverActivity.kt`

### 3. BiliDownOut 引用
- 删除下载列表中 BiliDownOut 外部工具跳转提示
- 文件：`DownloadListPage.kt`

---

## 十六、📱 UI 细节 (2026-05-05)

### 1. 图标改色
- 启动图标背景 `#FFFFFF` → `#FFFB7299`（B站粉），区分原版
- 文件：`ic_launcher_background.xml`

### 2. 复制 Cookie
- 描述从"用于脚本自动化" → "复制Cookie和Token到剪贴板"
- 文件：`FlagsSettingPage.kt`

### 3. 标签屏蔽
- 删除"此功能为实验性功能……"红色提示文字
- 文件：`FilterTagListContent.kt`

## 十七、🐛 崩溃修复 (2026-05-05)

### 1. 消息页未登录崩溃
- **根因**：`ResultInfo.data` 为非空类型，未登录 API 返回无 data 字段 → JSON 反序列化失败
- **修复**：`data: T` → `data: T? = null`
- 消息页各 tab 统一显示「未登录账号或加载失败」
- 影响：`LikeMessageContent`、`ReplyMessageContent`、`TimeRegionDetailListContent`、`UserMedialistDetailContent`
- 文件：`ResultInfo.kt` + 上述 4 个

### 2. EULA 底栏遮挡
- EulaScreen 全屏覆盖时 MainActivity 底栏仍可见，遮挡按钮
- ComposeFragment 中 EULA 显示时通知 MainActivity 隐藏底栏
- 文件：`ComposeFragment.kt`、`MainActivity.kt`

---

## 十八、⏳ 待办（已知问题）

1. ~~**底栏锁定**：锁定时若底栏已隐藏，偶尔不自动弹出（效果待验证）~~
2. ~~**视频亮度调节无效**：GSYVideoPlayer 手势调节无效~~
3. **图片缓存无上限**：Glide 未限制
4. **更深度的弹幕过滤**（按发送者屏蔽等）：因数据源限制未实现

---

> 生成时间：2026-05-07