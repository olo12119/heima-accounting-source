# 黑马记账 Android 文档索引

## 当前结论

Android 原生正式版 `1.0.0` 已完成。iCost、Apple 原生应用、visionOS 和 Liquid Glass 只作为信息布局、材质与动效参考；项目没有制作 iOS 应用。

当前技术为 Kotlin + Jetpack Compose + SQLite，默认人民币、本地优先、无需账号。正式 APK 位于 `../../手机安装包/黑马记账-Android-正式版-1.0.0.apk`。

## 阅读顺序

1. [最终发布报告](../../FINAL_RELEASE_REPORT.md)
2. [测试报告](../../TEST_REPORT.md)
3. [性能报告](../../PERFORMANCE_REPORT.md)
4. [产品定位与范围](PRODUCT.md)
5. [页面结构与核心流程](INFORMATION_ARCHITECTURE.md)
6. [Design System 与组件](DESIGN_SYSTEM.md)
7. [主题与字体](THEMES_AND_FONTS.md)
8. [动画、触觉与性能规范](MOTION_SPEC.md)
9. [技术与数据边界](TECH_AND_DATA_PLAN.md)
10. [运行、构建与测试](BUILD_AND_TEST.md)
11. [开发环境](ENVIRONMENT.md)

## 现在已经完成

- 首页、统计、记账、预算、我的和全部管理页面。
- 收入/支出、完整两级分类、自定义分类、SQLite 持久化。
- CSV、完整备份、校验恢复、恢复前安全副本。
- 两套主题、浅色/深色/跟随系统、Liquid Glass、音效、触觉、金额隐私。
- 严格 Lint、19 项单元测试、14 项模拟器集成/UI 测试、Release 构建和正式签名。

Windows 版仍保留为独立产品线；删除 Android App 不会修改 Windows 数据，反过来也一样。
