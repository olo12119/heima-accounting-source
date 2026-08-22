# 黑马记账 Android 设计文档索引

## 当前结论

黑马记账下一阶段只开发Android应用。iCost、Apple原生应用、visionOS和Liquid Glass仅作为信息层级、材质和动效参考，不制作iOS版本。

第一版高保真视觉方向已经确认，当前源码进入 `0.3.0-performance-motion` 性能与动效基础版。确认图仍可从 [Android视觉原型目录](../../design/android/README.md) 查看；当前成果通过Android Studio中的 `Heima_Android_16` 虚拟手机验收，本轮不生成独立APK。

推荐技术是Kotlin + Jetpack Compose。产品继续保持本地优先、无需账号、人民币为默认货币，并通过经过校验的完整备份从Windows桌面版迁移数据。

## 阅读顺序

1. [产品定位与范围](PRODUCT.md)
2. [页面结构与核心流程](INFORMATION_ARCHITECTURE.md)
3. [Design System与组件库](DESIGN_SYSTEM.md)
4. [两套主题与字体商店](THEMES_AND_FONTS.md)
5. [动画、触觉与性能规范](MOTION_SPEC.md)
6. [技术、数据迁移与工程边界](TECH_AND_DATA_PLAN.md)
7. [Android命令行开发环境](ENVIRONMENT.md)
8. [原型计划与逐页验收](PROTOTYPE_AND_ACCEPTANCE.md)
9. [构建、测试与APK说明](BUILD_AND_TEST.md)

## 阶段边界

- 已完成：设计文档、Windows/Android目录分离、高保真确认图、D盘命令行环境、Kotlin/Compose工程、第一阶段视觉体验APK和第一轮真机反馈修正。
- 当前：0.3已完成页签性能、记账金额输入和Liquid Glass重点区域的基础修正；仍没有正式数据持久化。
- 下一阶段：在0.3稳定基础上继续设计分类图标等视觉元素，再接入本地数据库、真实收支、账单、统计和预算业务。
- Android备份导入与真机测试完成前：不删除Windows版。
