# 黑马记账 Android 设计文档索引

## 当前结论

黑马记账下一阶段只开发Android应用。iCost、Apple原生应用、visionOS和Liquid Glass仅作为信息层级、材质和动效参考，不制作iOS版本。

第一版高保真视觉方向已经生成，可从 [Android视觉原型目录](../../design/android/README.md) 查看。该图片用于确认布局、材质和主题方向，不是可点击的App；确认后再开始正式Android代码。

推荐技术是Kotlin + Jetpack Compose。产品继续保持本地优先、无需账号、人民币为默认货币，并通过经过校验的完整备份从Windows桌面版迁移数据。

## 阅读顺序

1. [产品定位与范围](PRODUCT.md)
2. [页面结构与核心流程](INFORMATION_ARCHITECTURE.md)
3. [Design System与组件库](DESIGN_SYSTEM.md)
4. [两套主题与字体商店](THEMES_AND_FONTS.md)
5. [动画、触觉与性能规范](MOTION_SPEC.md)
6. [技术、数据迁移与工程边界](TECH_AND_DATA_PLAN.md)
7. [原型计划与逐页验收](PROTOTYPE_AND_ACCEPTANCE.md)

## 阶段边界

- 当前：完成设计文档和Windows/Android目录分离。
- 下一阶段：制作不连接真实数据库的高保真原型。
- 原型确认后：才建立正式Android工程。
- Android备份导入与真机测试完成前：不删除Windows版。
