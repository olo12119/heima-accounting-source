# 黑马记账

黑马记账现在是一个包含两条产品线的项目：

- **Windows桌面版**：1.5.0免安装预览版，已实现完整收支记账功能，仍然可正常打开。
- **Android手机版**：已完成原生工程和第一轮真机反馈修正，当前可安装版本为 `0.2.0-visual`。

两者已经分开放置，不会把Windows的Electron文件混入Android原生工程。iCost、Apple原生应用和Liquid Glass只是设计参考，黑马记账下一阶段只开发Android，不制作iOS应用。

## 你现在怎样打开或安装

当前项目重点是Android。请把下面这个文件复制到安卓手机并安装：

```text
手机安装包/黑马记账-Android-视觉修正版-0.2.0.apk
```

逐步说明在同文件夹的 `手机安装说明.txt`。它是视觉修正版，暂时不会永久保存账目。

如需打开旧的Windows桌面版，请在项目根目录双击：

```text
00-打开Windows桌面版-1.5.0.cmd
```

它会打开1.5.0免安装预览版。旧的 `00-点我打开黑马记账-当前最新版.cmd` 仍然保留为兼容入口。Windows版和Android版是两条独立产品线，不要把Windows的EXE复制到手机。

## 项目地图

```text
黑马记账app/
├─ apps/
│  ├─ windows-desktop/   已完成的Windows桌面版源码、测试和预览成品
│  └─ android/           Android原生源码、测试和构建脚本
├─ design/android/         Android高保真原型和视觉稿
├─ docs/android/           Android UI/UX、动效、主题和技术方案
├─ docs/                   通用及Windows桌面版文档
├─ 手机安装包/             当前与历史Android APK及安装说明
└─ shared/contracts/       未来两端备份格式的共用说明
```

更详细的小白说明见 [请先看这里](00-请先看这里.md) 和 [项目文件地图](docs/FILE_GUIDE.md)。

## Windows开发检查

所有Windows命令现在都需要先进入它的独立目录：

```powershell
Set-Location ".\apps\windows-desktop"
npm.cmd run dev
```

类型检查、代码规范、38项自动测试、Electron端到端测试、生产构建和免安装版冒烟测试已在目录分离后全部通过。完整命令见 [构建说明](docs/BUILD.md)。

## Git“游戏存档”

双击 `00-查看Git存档点.cmd` 可以只读查看存档，不会修改文件。请注意：Git保护的是源码和文档，不会自动保护你在软件中记录的个人账目。
