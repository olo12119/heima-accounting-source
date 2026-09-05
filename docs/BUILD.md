# 黑马记账构建说明

> Android 正式版 1.1.0 已完成，普通用户安装 `手机安装包/黑马记账-Android-正式版-1.1.0.apk`；Android Studio 和完整测试命令见 [Android 构建与测试](android/BUILD_AND_TEST.md)。

## 项目范围

本项目当前只维护 Android 手机版，正式工程位于 `apps/android`，包含 Gradle 工程、源码、测试和正式 APK。

- 普通用户安装：`手机安装包/黑马记账-Android-正式版-1.1.0.apk`
- 开发者构建与测试：见 [docs/android/BUILD_AND_TEST.md](android/BUILD_AND_TEST.md)

## 开发与检查

在项目根目录进入 Android 工程：

```text
apps/android
```

具体构建、测试、签名和 APK 输出命令，以 [docs/android/BUILD_AND_TEST.md](android/BUILD_AND_TEST.md) 为准。

## 图标位置

- 应用图标：`apps/android/app/src/main/res/mipmap-*`
- 分类图标图集：`apps/android/app/src/main/res/drawable-nodpi`

这些是程序资源，不是缓存，由 Git 保存。
