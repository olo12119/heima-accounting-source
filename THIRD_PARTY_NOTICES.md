# 第三方软件与许可证说明

本文件只记录最终源码或 APK 真正使用的第三方项目。项目仅用于调研而没有复制/链接代码时，会单独注明，不把“看过”误写成“使用”。

## AndroidLiquidGlass / Backdrop

- 项目：https://github.com/Kyant0/AndroidLiquidGlass
- 最终依赖：`io.github.kyant0:backdrop:2.0.0`
- 许可证：Apache License 2.0
- Copyright 2025 Kyant
- 用途：连续背景采样、Glass Lens、Blur、Vibrancy、Highlight、Shadow 和 InnerShadow。
- 本项目修改情况：没有把上游源码复制后改名；通过公开 Maven 依赖调用 API，并在黑马记账自己的设计系统中重新定义参数、回退和组件结构。
- 完整许可证：`apps/android/licenses/AndroidLiquidGlass-APACHE-2.0.txt`

## AndroidX / Jetpack Compose

- 项目：https://developer.android.com/jetpack/androidx
- 许可证：Apache License 2.0
- 用途：Activity、Lifecycle、Compose UI、Foundation、Material 3、Animation、测试和 Profile Installer。

## Kotlin 与 kotlinx.coroutines

- 项目：https://github.com/JetBrains/kotlin 与 https://github.com/Kotlin/kotlinx.coroutines
- 许可证：Apache License 2.0
- 用途：Kotlin 语言运行支持和结构化并发。

## 仅研究、未进入最终 APK

- `QmDeve/AndroidLiquidGlassView`（MIT，Copyright © 2025 Donny Yale）：研究 Refraction、Dispersion、Touch、Elastic、Draggable 和缓存边界。
- `android/nowinandroid`（Apache License 2.0）：研究独立 Light/Dark ColorScheme、设置单向数据流和 Snackbar 事件。
- `android/performance-samples`（Apache License 2.0）：研究 Jank、启动和 Macrobenchmark 测试边界。

以上研究项目没有源文件复制进黑马记账，也没有新增对应 Gradle 依赖。AndroidX `HorizontalPager` 来自已经声明的 AndroidX/Compose 依赖。

## 项目自有素材

`category_3d_atlas_v2.png`、应用图标和 Compose 导航图形来自黑马记账项目此前已经确认或绘制的素材，不标记为上述开源项目的作品。原始分类图集保存在 `apps/android/tools/source-assets`，通过项目内确定性脚本规范化透明边与视觉重心后生成最终资源。
