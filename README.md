# 黑马记账

黑马记账现在是一个包含两条产品线的项目：

- **Windows桌面版**：1.5.0免安装预览版，已实现完整收支记账功能，仍然可正常打开。
- **Android手机版**：已完成第一版完整UI/UX设计文档，尚未开始正式编码，目前没有APK。

两者已经分开放置，不会把Windows的Electron文件混入Android原生工程。iCost、Apple原生应用和Liquid Glass只是设计参考，黑马记账下一阶段只开发Android，不制作iOS应用。

## 你现在怎样打开

当前唯一可运行成品是Windows桌面版。请在项目根目录双击：

```text
00-打开Windows桌面版-1.5.0.cmd
```

它会打开1.5.0免安装预览版。旧的 `00-点我打开黑马记账-当前最新版.cmd` 仍然保留为兼容入口，但以后请优先使用名称更清楚的新入口。

Android现在只能阅读设计方案，还不能安装到手机。请从 [Android设计文档索引](docs/android/README.md) 开始查看。

## 项目地图

```text
黑马记账app/
├─ apps/
│  ├─ windows-desktop/   已完成的Windows桌面版源码、测试和预览成品
│  └─ android/           未来Android原生工程位置，当前只有说明
├─ design/android/         未来高保真原型和视觉稿位置
├─ docs/android/           Android UI/UX、动效、主题和技术方案
├─ docs/                   通用及Windows桌面版文档
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
