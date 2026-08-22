# 黑马记账构建说明

> Android 1.0.0 已完成，普通用户安装 `手机安装包/黑马记账-Android-正式版-1.0.0.apk`；Android Studio 和完整测试命令见 [Android 构建与测试](android/BUILD_AND_TEST.md)。下文保留 Windows 1.5.0 独立产品线的构建说明。

## 先分清两个项目

- Windows桌面版的完整Electron工程在 `apps/windows-desktop`。
- Android 手机版正式工程位于 `apps/android`，已经有 Gradle 工程、源码、测试和正式 APK。

下面其余命令只针对 Windows 桌面版。

## 当前验证环境

- Windows x64，系统内部版本26100
- Node.js 24.19.0、npm 11.17.0
- Electron 43.4.1（内置Node 24.18.1）
- Git 2.55.0、PowerShell 5.1
- Windows桌面版不需要Rust、Cargo、Flutter或Visual Studio C++ Build Tools

当前Windows源码版本为1.5.0预览版。目录分离后已在中文和空格路径下完成类型检查、测试、Electron启动和生产构建。

## 普通用户打开方式

不需要输入任何命令。在项目根目录双击：

```text
00-打开Windows桌面版-1.5.0.cmd
```

该入口会找到 `apps/windows-desktop/可直接打开-黑马记账-1.5.0预览版/win-unpacked/HeimaAccounting.exe`。这是免安装预览版，整个 `win-unpacked` 文件夹必须保留。

## 开发与检查

在项目根目录打开PowerShell，先进入Windows工程：

```powershell
Set-Location ".\apps\windows-desktop"
```

第一次或依赖缺失时安装程序零件：

```powershell
npm.cmd install
```

源码临时启动：

```powershell
npm.cmd run dev
```

这种方式出现的PowerShell窗口需要保持打开，直到应用关闭。

完整检查：

```powershell
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run test:e2e
npm.cmd run build
npm.cmd run test:packaged
```

`test:e2e` 使用独立的测试数据库，不会修改正式个人账本。

## Windows成品

### 当前已有：1.5.0免安装预览版

它不向Windows安装程序，用于在制作正式安装包前验收。如需重新生成：

```powershell
$env:ELECTRON_BUILDER_CACHE = "$PWD\.cache\electron-builder"
npm.cmd run build
npm.cmd exec electron-builder -- --dir --win --x64 "--config.directories.output=可直接打开-黑马记账-1.5.0预览版"
```

### 当前尚未制作：1.5.0安装版和单文件便携版

用户已决定等体验满意后再制作。`apps/windows-desktop/release` 中目前主要是旧1.0.0/1.1.0成品，不得称为当前1.5.0版。

将来获得用户同意后才执行：

```powershell
npm.cmd run dist:win
```

未购买代码签名证书，所以Windows SmartScreen可能显示“未知发布者”。

## macOS历史支持说明

Windows桌面版的Electron代码仍保留macOS构建配置，但只能在真实Mac上构建和验证DMG/ZIP。本项目没有在Windows上伪造macOS构建成功。这与新的Android开发方向互不影响。

## 图标位置

- 运行时应用图标：`apps/windows-desktop/src/renderer/public/logo-app-v3.png`
- Windows/macOS构建图标：`apps/windows-desktop/build/icon.png`
- 3D分类图标图集：`apps/windows-desktop/src/renderer/public/category-3d-atlas-v2.png`

这些是程序资源，不是缓存，由Git保存。
