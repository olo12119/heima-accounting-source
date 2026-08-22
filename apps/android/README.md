# 黑马记账 Android 1.0.0

这是与 Windows 桌面版分开的 Android 原生正式工程。

## 技术栈

- Kotlin、Jetpack Compose、Material 3
- 模块化 `app / core:designsystem / core:domain / core:data / core:database`
- Android SQLiteOpenHelper、WAL、外键、索引、事务和迁移版本
- Kyant Backdrop 2.0.0 Liquid Glass
- AndroidX 测试、Compose UI Test、严格 Android Lint、R8

## 功能

- 收入与支出、自然小数金额、一级快速分类、可选二级精细分类。
- 完整默认分类和用户自定义分类。
- 首页、统计、预算、账单、分类、数据管理和设置。
- CSV、带 SHA-256 的完整 JSON 备份和事务恢复。
- 两套主题、Light/Dark/System、Liquid Glass、音效、触觉、隐私金额。
- 无虚假账单、无网络权限、无后台服务。

## 打开与构建

普通用户安装根目录 `手机安装包/黑马记账-Android-正式版-1.0.0.apk`。

开发者从根目录双击 `00-用Android Studio查看手机版.cmd`，或按 `../../docs/android/BUILD_AND_TEST.md` 使用 D 盘工具链构建。

## 重要边界

- 本地签名密钥位于忽略 Git 的 `.local-signing`，必须单独备份。
- APK、构建目录、Gradle 缓存、模拟器和用户数据库不进入 Git。
- 模拟器不能代表真机耗电和温度；真实性能边界见 `../../PERFORMANCE_REPORT.md`。
