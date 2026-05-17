# bilimiao2-mod 改动记录

> 基于 [10miaomiao/bilimiao2](https://github.com/10miaomiao/bilimiao2) 的 FOSS 修改版
> 版本：2026.05.14-06 | 内部版本号：17
> 上游原始：89,595 行 / 646 个文件
> MOD总计：93,866 行 / 658 个文件
> 净增：4,271 行 / 12 个文件

---

## 一、Phase 1：初始 FOSS 版

> 这是从原始上游 fork 后一次性导入的基础改动，占比约 **80%** 的代码量

### 1.1 移除：Google 服务依赖 🗑️
- 删除 `full` flavor 及相关代码（4 个文件）
  - `BiliGeetestUtilImpl.kt`（Google 安全验证）
  - `BilimiaoPlayerManager.kt`（ExoPlayer 管理器）
  - `Libgav1Media3ExoPlayerManager.kt`（AV1 解码器）
  - `BilimiaoStatService.kt`（统计服务）
- 所有 Geetest 验证走 FOSS 实现
- 纯开源依赖，无 Google Play Services

### 1.2 移除：非核心功能 🗑️
- **代理设置**（4 个页面 + 3 个组件）
  - `ProxySettingPage`
  - `AddProxyServerPage` / `EditProxyServerPage` / `SelectProxyServerPage`
  - `ProxyServerCard` / `ProxyServerForm`
- **`DensitySettingActivity`** — 密度设置
- **`AboutPage`** — 关于页面

### 1.3 新增：文章阅读器 📄
- `ArticleReaderPage` + `ArticleReaderContent` + `ArticleReaderViewModel`
- `ArticleData` 数据结构
- 搜索结果可显示文章卡片 `ArticleItemBox`

### 1.4 新增：私信/聊天系统 💬
- `ChatPage` — 聊天界面
- `PrivateMessageContent` — 私信内容列表
- `ChatMsgInfo` / `ChatSessionInfo` — 消息 & 会话数据类
- `MessageAPI` 大幅增强（+64 行）

### 1.5 新增：错误日志系统 🐛
- `ErrorLogCollector` — 异常采集器，捕获崩溃 & 异常
- `ErrorLogPage` — 错误日志浏览页
  - 日志列表 + 展开详情 + 堆栈跟踪
  - 多选删除、复制（单条/全部）
  - 浅色/深色模式全适配

### 1.6 新增：设置导出 ⚙️
- `ExportSettingPage` — 导出/导入所有偏好设置
- `SettingsExporter` — JSON 序列化/反序列化

### 1.7 新增：弹幕文字过滤 🔇
- `DanmakuTextFilter` — 按关键词过滤弹幕
- 弹幕设置页 `DanmakuDisplaySettingPage` 大幅增强（+189 行）
- 弹幕设置总体验重构（-74 行）

### 1.8 新增：粉丝 & 评论过滤 🧑‍🤝‍🧑
- `MyFollowerPage` — 我的粉丝列表
- `FilterCommentWordListContent` — 评论关键词过滤

### 1.9 播放器菜单增强 🎬
- `ScalePopupMenu` — 缩放比例菜单
- `PopupMenuExt` — 弹窗工具扩展
- `QualityPopupMenu` / `SpeedPopupMenu` / `ScalePopupMenu` 全链路主题修复
- 弹窗每次 `show()` 重建以跟随主题切换
- 隐藏长按倍速提示文字 `speed_tips`，恢复 `play_speed` 按钮

### 1.10 播放器 UI 改进 🎨
- 缩放菜单
- 画质菜单优化
- 倍速菜单优化
- 主题切换时的 `fgColor` 硬编码绕过缓存

### 1.11 设置页重构 🏗️
- `FlagsSettingPage` 大幅改造（+367 行）
- `SettingPage` 精简重构（-143 行）
- `HomePage` 重构（-154 行）
- `ContentBehavior` 简化（-326 行）
- `AppBarBehavior` 简化（-71 行）
- `PlayerBehaviorDelegate` 简化（-58 行）
- `MainActivity` 简化（-39 行）

### 1.12 其他新增文件 🆕
- `TimestampHelper` — 时间戳工具函数
- `TextIntPreference` — 整数输入偏好组件
- 4 个 Fragment 动画 XML
- 8 个 Material Icon SVG
- `Bilidownload.kt` — Compose Icon 定义

---

## 二、Phase 2：今日连续开发

> 2026.05.14 01:37 ~ 09:40 | 79 个文件 | +1,022 / -519 行 | 22 次提交

### 2.1 主题修复（01:37~05:01）⏱️
- `fix:` 隐藏长按倍速提示文字
- `fix:` 主题切换硬编码 `fgColor` 绕过 config 缓存
- `fix:` PopupMenu 主题跟随 — `popupContext()` 绕过 Activity 过期 Theme
- `fix:` 亮度手势加 `activityContext` 空检查
- `revert:` 回退实验性改动

**5 次提交 | ~1.5h**

### 2.2 下载视频续播（05:16~05:28）⏱️
- `feat:` 下载视频续播 — 本地缓存进度 + 静默上报 B 站
- `fix:` 秒→毫秒单位修复 + 云同步兜底
- `PlaybackService` / `PlayerDelegate2` / `DanmakuVideoPlayer` 联动

**2 次提交 | ~30min**

### 2.3 视频章节标记（05:28~07:26）⏱️
- `feat:` 启用进度条上方章节标记
- `fix:` `fetchChapters` 的 Referer 用 `finalAid` 不是 `aid`
- `fix:` 章节用 JSONObject 解析 + 触摸修复 + 定位
- `fix:` 分段标题修正 + 触摸对齐 + 字体加粗加大
- `fix:` 章节条紧贴进度条上方
- `fix:` DP 固定字体 + onDraw 正确应用 padding
- `fix:` `getGlobalVisibleRect` 精确对齐 SeekBar
- `feat:` 章节标记改为按钮弹窗 + 修复续播同步

**9 次提交 | ~2h**（迭代最密集的功能）

### 2.4 下载系统修复（07:56~09:02）⏱️
- `fix:` 合集下载用 `season_id` 分组
- `fix:` 合集下载目录结构（目录共享）
- `fix:` 底部灰色区域（LazyColumn weight）
- `fix:` 下载列表刷新修复（`toList()`）
- 收藏/订阅页移除误放的下载按钮

**2 次提交 | ~1h**

### 2.5 Toast 系统重构（09:02~09:20）⏱️
- `fix:` Snackbar 颜色跟随主题（`surfaceContainerHigh`）
- `fix:` 60dp padding 避免被底栏遮挡
- `refactor:` 全线切换到 `android.widget.Toast`
- 删除 Compose SnackbarHost
- `ToastManager` 重写为原生调用

**2 次提交 | ~20min**

### 2.6 暗黑模式修复 & 发布（09:25~09:40）⏱️
- `fix:` ErrorLogPage `Surface` 包裹，深浅模式均可见
- `R8 开启`（`isMinifyEnabled = true`）
- 版本号 `16→17`，`2026.05.14-05→06`
- Git 推送 + GitHub Release（`stable` tag）

**2 次提交 | ~15min**

---

## 三、总体统计

### 代码量

| 指标 | 数值 |
|---|---|
| 总代码行数 | **93,866 行**（658 个文件） |
| 相对上游净增 | **4,271 行**（12 个文件） |
| Phase 1（初始 Fork） | ~3,250 行 / 35+ 个文件 |
| Phase 2（今日开发） | +1,022 / -519 行（79 个文件） |

### 时间线

```
Phase 1 (初始Fork): ? → 2026.05.13
  └─ 移除 Google 依赖 / 新增功能 / 重构
  └─ 准备 FOSS-only 构建

Phase 2 (今日冲刺): 2026.05.14 01:37 → 09:40
  ├─ 主题修复        01:37~05:01    5 commits
  ├─ 下载续播        05:16~05:28    2 commits
  ├─ 章节标记        05:28~07:26    9 commits  ← 最密集
  ├─ 下载修复        07:56~09:02    2 commits
  ├─ Toast重构       09:02~09:20    2 commits  ← 263处替换
  └─ 发布            09:25~09:40    2 commits
```

### APK

| 指标 | 原版 | MOD |
|---|---|---|
| 大小 | ~26MB | **~24MB**（R8 压缩 -8%） |
| 压缩 | 无 | **R8 全量 + 资源压缩** |
| 签名 | debug | debug（FOSS） |

---

## 四、保留的原始行为

以下原版功能/特性**未做改动**：
- 视频播放核心（IJKPlayer/ExoPlayer）
- 番剧/影视模块
- 动态/推荐信息流
- 直播模块
- 用户空间/收藏
- 搜索系统
- 数据存储（DataStore / Room）
- 弹幕协议（Protobuf GRPC）
- 下载引擎（DownloadService）
- 主题系统基础架构（MaterialKolor）
- DI 框架（Kodein）

## 五、Phase 3：主题彻底修复 & 动态页UI修复 & 弹幕/PiP修复

> 2026.05.17 | 14 个文件 | +123 / -57 行

### 3.1 主题切换彻底修复 🎨
- `fix:` ViewConfig 改用 `resources.newTheme()` 绕过 `Activity.mTheme` 缓存（第2次切换后主题卡在第一次的问题）
- `fix:` 所有 PopupMenu 改用 `ContextThemeWrapper(activity, Theme_Bilimiao)` 确保弹窗颜色跟随主题
- `fix:` AppStore.setDarkMode 同步更新 stateFlow，消除 onConfigurationChanged 的 race condition

### 3.2 动态 UP tab 冗余去除 🗑️
- `fix:` 选中某个 UP 后，右侧帖子卡片不再重复显示 UP 头像和名字，保留发布时间
- `DynamicModuleAuthorBox` 新增 `showUserInfo` 参数控制

### 3.3 弹幕发送可见性修复 💬
- `fix:` 先 resume 再 addDanmaku，避免 `NOTIFY_RENDERING` 被 `start(position)` 的 `removeCallbacksAndMessages` 清除
- 使用实时 `currentPosition` 替代 API 请求前的过期 position

### 3.4 PiP 退出修复 📺
- `fix:` `closePlayer()` 中先退出画中画模式再释放播放器（API 36 移除了公开方法，用反射兼容）
