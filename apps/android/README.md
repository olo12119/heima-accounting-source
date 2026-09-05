# 黑马记账 Android 1.1.0

这是黑马记账的 Android 原生正式工程。

## 技术栈

- Kotlin、Jetpack Compose、Material 3
- 模块化 `app / core:designsystem / core:domain / core:data / core:database`
- Android SQLiteOpenHelper、WAL、外键、索引、事务和迁移版本
- Kyant Backdrop 2.0.0 Liquid Glass
- AndroidX 测试、Compose UI Test、严格 Android Lint、R8

## 功能

- 收入与支出、自然小数金额、一级快速分类、可选二级精细分类。
- 16 个支出一级分类、完整收入/二级分类和全功能分类编辑器。
- 首页、统计、预算、账单、分类、数据管理和设置。
- CSV、带 SHA-256 的完整 JSON 备份和事务恢复。
- 两套主题、Light/Dark/System、Liquid Glass、音效、触觉、隐私金额。
- 无虚假账单、无后台服务；仅手动“检查更新”使用网络，不上传账单。
- 统一 Material / Motion Token、语义 Surface、触摸图表、中文 Glass Calendar 和 8 组合视觉回归。

## 打开与构建

普通用户安装根目录 `手机安装包/黑马记账-Android-正式版-1.1.0.apk`。

开发者双击 `scripts/用Android-Studio查看手机版.cmd`，或按 `../../docs/android/BUILD_AND_TEST.md` 使用 D 盘工具链构建。

## 重要边界

- 本地签名密钥位于忽略 Git 的 `.local-signing`，必须单独备份。
- APK、构建目录、Gradle 缓存、模拟器和用户数据库不进入 Git。
- 模拟器不能代表真机耗电和温度；真实性能边界见 `../../docs/reports/PERFORMANCE_REPORT.md`。
