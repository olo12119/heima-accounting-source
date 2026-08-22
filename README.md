# 黑马记账

黑马记账现在包含两条彼此独立的产品线：

- Android 正式版 `1.0.0`：本项目当前重点，已完成本地收支、分类、预算、统计、备份恢复、主题和 Liquid Glass。
- Windows 桌面版 `1.5.0`：保留在 `apps/windows-desktop`，没有被 Android 工程覆盖。

iCost、Apple 原生应用和 Liquid Glass 只作为设计思路参考；本项目当前只发布 Android App，不制作 iOS 版本。

## 普通用户唯一推荐入口

把这个文件复制到安卓手机并点击安装：

```text
手机安装包\黑马记账-Android-正式版-1.0.0.apk
```

这是已经签名的正式安装文件，代表当前最终源码。`手机安装包\旧版本-请勿安装` 中的 0.1 和 0.2 仅用于历史对比。

如果想在电脑模拟器查看当前源码，双击：

```text
00-用Android Studio查看手机版.cmd
```

等待同步结束，顶部选择 `Heima_Android_16`，点击绿色三角形。Android Studio 运行的是源码开发版；APK 是给手机直接安装的正式版，两者功能代码相同，但签名和用途不同。

## 项目地图

```text
黑马记账app/
├─ apps/android/              Android 原生源码与测试
├─ apps/windows-desktop/      旧 Windows 产品线
├─ docs/android/              Android 产品、设计、构建文档
├─ 手机安装包/                当前正式 APK 与归档旧版
├─ FINAL_RELEASE_REPORT.md    最终功能和架构
├─ TEST_REPORT.md             功能、异常和自动测试
├─ PERFORMANCE_REPORT.md      模拟器性能与真机边界
└─ THIRD_PARTY_NOTICES.md     开源项目和许可证
```

## 技术结构

Android 使用 Kotlin + Jetpack Compose + SQLite。账目金额以整数“分”保存；数据库、导出与恢复在数据层完成，页面不能直接绕过校验修改数据库。Liquid Glass 使用 Kyant Backdrop，并对旧系统、省电模式和用户关闭效果提供一致回退。

## Git“游戏存档”

双击 `00-查看Git存档点.cmd` 可以只读查看源码存档。Git 不会自动备份手机中的账目、APK 和本地签名密钥：账目请用 App 内“完整备份”，签名密钥请另行备份 `apps/android/.local-signing`。
