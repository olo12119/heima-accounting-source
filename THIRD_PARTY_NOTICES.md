# 第三方软件、素材与许可证说明

本文件区分“最终 APK 真正依赖的代码”和“只用于产品研究的项目”。只看过页面或实现思路，不等于复制或使用其源码。

## 最终 APK 使用

### AndroidLiquidGlass / Backdrop

- 项目：https://github.com/Kyant0/AndroidLiquidGlass
- 依赖：`io.github.kyant0:backdrop:2.0.0`
- 许可证：Apache License 2.0
- Copyright 2025 Kyant
- 用途：Backdrop、Glass Lens、Blur、Vibrancy、Highlight、Shadow 和 InnerShadow。
- 集成方式：通过公开 Maven 依赖调用 API；黑马记账自行定义参数、组件、兼容降级、底栏布局和手势逻辑，没有复制并改名上游 Demo。
- 完整许可证：`apps/android/licenses/AndroidLiquidGlass-APACHE-2.0.txt`

### AndroidX / Jetpack Compose

- 项目：https://developer.android.com/jetpack/androidx
- 许可证：Apache License 2.0
- 用途：Activity、Lifecycle、Compose UI/Foundation/Material 3、HorizontalPager、动画、测试和 Profile Installer。

### Kotlin / kotlinx.coroutines

- 项目：https://github.com/JetBrains/kotlin、https://github.com/Kotlin/kotlinx.coroutines
- 许可证：Apache License 2.0
- 用途：Kotlin 语言和结构化并发。

## 仅研究，未复制源码或引入依赖

- `QmDeve/AndroidLiquidGlassView`（MIT）：研究 Refraction、Touch、Elastic、Draggable 和缓存边界。
- `android/nowinandroid`（Apache-2.0）：研究独立 Light/Dark Tokens、设置单向数据流和 Snackbar 事件。
- `androidx/androidx`（Apache-2.0）：研究 HorizontalPager、PagerState、手势和触觉接口。
- `android/performance-samples`（Apache-2.0）：研究 Jank、启动和 Macrobenchmark 边界。
- `enrique-lozano/Monekin`（AGPL-3.0）：研究本地优先、分类、预算、备份和财务信息层级。
- `jameskokoska/Cashew`（GPL-3.0）：研究首页趋势、分类数据结构和移动端预算信息组织。
- `Ivy-Apps/ivy-wallet`（GPL-3.0）：研究 Compose 记账产品的简洁信息架构与自定义体验。
- `mtotschnig/myexpenses`（GPL-3.0）：研究长期账单、分类、筛选和本地数据能力边界。

以上研究项目没有源文件复制进黑马记账，也没有新增对应 Gradle 依赖。因此其 AGPL/GPL 代码不构成黑马记账 APK 的派生代码来源。

## 项目自有素材

`category_3d_atlas_v2.png`、应用图标和 Compose 导航图形来自黑马记账项目此前确认或绘制的素材。原始图集保存在 `apps/android/tools/source-assets`，分类组件通过统一安全区、裁剪和光学对齐复用该图集。
