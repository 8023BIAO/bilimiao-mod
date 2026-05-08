# bilimiao2-mod 改版清单

> 基于原版 bilimiao2 v2.4.8.1 的第三方修改版

## 项目基础

- **包名**：`com.a10miaomiao.bilimiao` → `com.a10miaomiao.bilimiao.mod`（可与原版共存）
- **版本号**：改用日期版本（如 `2026.05.08-01`）
- **Gradle**：8.10.2 → 8.13，AGP 同步升级
- **ProGuard**：精简规则，禁用混淆，解决 R8 编译崩溃

## 移除的组件

- **EULA 弹窗**：删除首次启动全屏协议弹窗及相关代码
- **百度移动统计**：删除 SDK、依赖、Manifest 配置及所有调用点
- **空降助手**：删除 SponsorBlock 相关全部代码
- **AboutPage**：删除原关于页面（530 行）
- **GitHub Actions CI**：删除上游自动构建工作流

## 新增功能

### 播放器
- 后台小窗播放（PIP）开关支持
- 底栏锁定：防止滑动时隐藏底部导航
- 合集排序：支持按 aid 升序/降序/热度排序
- 自动连播：下一 P → 下一番剧 → 播放列表 → 推荐 → 循环
- 定时关闭快捷入口（播放器更多菜单）
- 隐藏相关推荐 / 隐藏封面
- 弹幕发送时自动暂停

### 弹幕
- 弹幕关键词过滤（黑名单匹配）
- 重复弹幕合并
- 弹幕过滤设置界面（3 个 tab 统一管理）

### 评论
- 评论关键词屏蔽（增删管理 + 列表过滤）
- 评论区时间戳可点击跳转（00:00 / 00:00:00）
- IP 属地显示简化（去掉"IP属地："前缀）

### 设置
- 设置导入/导出（JSON 格式，覆盖 DataStore/SQLite/SharedPreferences/JSON 四层存储）
- 身份导入导出（跨设备迁移登录态）
- 首页新增"动态"导航入口
- DPI 设置、图片缓存等移到"实用功能"分类

### 其他
- 存储权限优化：下载目录改为公共 `Download/BiliMiao/`，移除运行时存储权限请求
- 首页推荐/热门 URI 去重（翻页时自动过滤已显示视频）
- 下载按钮从底栏移到"更多"菜单

## 稳定性修复

- R8 编译崩溃（Kodein DI TypeReference 被擦除）
- MaterialToolbar 反射构造异常
- DynamicPage 序列化崩溃
- DanmakuVideoPlayer 空指针（currentDanmakuMode / setWindowInsets）
- 收藏夹 mid 为空导致加载失败
- 播放器 Surface 丢失黑屏
- DownloadService JSON 解析崩溃
- LocalPlayerSource 兼容旧版 index.json 字段
- 消息页未登录反序列化崩溃
- PlaybackService 秒退崩溃（MediaSession 懒初始化）

## UI 改进

- 底栏图标统一配色（修复浅色主题下白色图标看不见）
- 选中 tab 高亮，其余统一灰色
- 视频详情页合集左右增加边距
- 动态页视频卡片底部增加间距
- 播放器弹窗改用 Material3 原生选中样式
- 启动图标背景改 B站粉（区分原版）
- 排序菜单带图标

## 播放器通知栏

- MediaSessionService 改为纯 Service + 手动 MediaStyle 通知
- 修复通知栏按钮点击失效
- 修复自动联播通知信息不更新
- 播放/暂停状态双向同步
- 通知权限仅首次弹窗
