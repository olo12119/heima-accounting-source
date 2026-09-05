# 黑马记账

黑马记账是一个 Android 记账应用，当前正式版本为 `1.1.0`，已完成本地收支、完整分类编辑、预算与统计，以及统一 Liquid Glass Material / Motion 设计系统和手动在线更新检查。

iCost、Apple 原生应用和 Liquid Glass 只作为设计思路参考；本项目当前只发布 Android App，不制作 iOS 版本。

这是一个公开源码仓库：Git 用来保存代码和文档历史，不保存用户账单、发布签名密钥或 Android Studio 的个人配置。

## 快速导航

- 当前正式版本：[Android 1.1.0](CHANGELOG.md)
- [Android 产品、设计和构建文档](docs/android/README.md)
- [正式报告索引](docs/reports/README.md)
- [Git 存档教学](docs/GIT_GUIDE.md)
- [第三方开源说明](THIRD_PARTY_NOTICES.md)
- [Android 公开更新与下载页](https://github.com/olo12119/heima-accounting-releases/releases/latest)

## 普通用户唯一推荐入口

把这个文件复制到安卓手机并点击安装：

```text
手机安装包\黑马记账-Android-正式版-1.1.0.apk
```

这是已经签名的正式安装文件，代表当前最终源码。1.0.3 及更早 APK 都是历史版本，不再用于验收。

如果想在电脑模拟器查看当前源码，双击：

```text
scripts\用Android-Studio查看手机版.cmd
```

等待同步结束，顶部选择 `Heima_Android_16`，点击绿色三角形。Android Studio 运行的是源码开发版；APK 是给手机直接安装的正式版，两者功能代码相同，但签名和用途不同。

## 项目地图

```text
黑马记账app/
├─ apps/android/              Android 原生源码与测试
├─ docs/android/              Android 产品、设计、构建文档
├─ docs/reports/              正式发布、测试、性能与 UX 报告
├─ scripts/                   快捷启动脚本
├─ design/                    设计稿与界面截图
├─ shared/                    跨模块共享契约
├─ 手机安装包/                当前正式 APK 与归档旧版
├─ CHANGELOG.md               正式版本更新记录
└─ THIRD_PARTY_NOTICES.md     开源项目和许可证
```

## 技术结构

Android 使用 Kotlin + Jetpack Compose + SQLite。账目金额以整数“分”保存；数据库、导出与恢复在数据层完成，页面不能直接绕过校验修改数据库。Liquid Glass 使用 Kyant Backdrop，并对旧系统、省电模式和用户关闭效果提供一致回退。1.1.0 的完整 55 项核验见 [UI / Motion / Material 专项报告](docs/reports/UI_MOTION_MATERIAL_REPORT.md)。

## Git“游戏存档”

双击 `scripts\查看Git存档点.cmd` 可以只读查看源码存档。Git 不会自动备份手机中的账目、APK 和本地签名密钥：账目请用 App 内“完整备份”，签名密钥请另行备份 `apps/android/.local-signing`。
