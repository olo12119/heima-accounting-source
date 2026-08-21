# 黑马记账构建说明

## 当前验证环境

- Windows x64，系统内部版本 26100
- Node.js 24.19.0、npm 11.17.0
- Electron 43.4.1（内置 Node 24.18.1）
- Git 2.55.0、PowerShell 5.1
- 不需要 Rust、Cargo、Flutter 或 Visual Studio C++ Build Tools

项目路径可以包含中文和空格，当前项目已经在该路径完成类型检查、测试、Electron 启动和构建。

当前源码版本为1.3.0预览版。用户决定先体验功能再制作安装包，因此本轮没有生成1.3.0 Windows安装版或便携版，`release` 中仍是已验证的1.1.0产物。

## 安装依赖

在项目根目录打开 PowerShell：

```powershell
npm install
```

如果PowerShell提示禁止运行 `npm.ps1`，不需要修改系统安全策略，把命令中的 `npm` 换成 `npm.cmd` 即可，例如 `npm.cmd run dev`。

依赖安装在项目的 `node_modules`，npm 缓存位于 `.cache/npm`。Electron 43 会在第一次运行时下载官方运行时。如果下载中断，可重试：

```powershell
npm exec install-electron -- --no
```

## 开发与检查

```powershell
npm run dev
npm run typecheck
npm run lint
npm test
npm run test:e2e
npm run build
npm run test:packaged
```

`test:e2e` 会启动 Electron 并使用系统临时目录中的独立测试数据库，不会修改正式账本。
当前测试环境的Electron子进程需要在Playwright启动参数中关闭GPU和测试沙箱；这些参数只存在于测试文件，正式App仍保持渲染进程沙箱、上下文隔离和硬件加速。

## Windows 构建

```powershell
npm run dist:win
```

执行打包后，新产物会位于 `release`。当前实际存在的是1.1.0产物：

- `HeimaAccounting-Setup-1.1.0-x64.exe`：带安装向导的 NSIS 安装包。
- `HeimaAccounting-Portable-1.1.0-x64.exe`：无需安装的便携版。
- `win-unpacked\HeimaAccounting.exe`：解压运行目录中的主程序。

在用户确认1.3.0预览版之前不要把上述文件误称为1.3.0。确认后再执行 `npm run dist:win`，预期生成带1.3.0版本号的新文件。

当前产物没有代码签名证书，Windows SmartScreen 可能显示“未知发布者”。这是签名限制，不是程序损坏。

## macOS 构建

必须在真实 Mac 上安装 Node.js 24 和 Xcode Command Line Tools，然后从同一项目代码执行：

```bash
npm ci
npm run typecheck
npm test
npm run build
npm run dist:mac -- --arm64
npm run dist:mac -- --x64
```

产物是对应架构的 DMG 和 ZIP，位于 `release`。没有 Apple Developer 账号时可以本机测试未签名 App，但对外发布前应完成 Apple 签名和公证。本项目没有在 Windows 上伪造 macOS 构建成功。

## 图标

SVG 源文件在 `assets/logo.svg`。Windows 上可重新生成512像素安装图标：

```powershell
npm run icons
```

界面中的立体分类图标位于 `src/renderer/public/category-3d-atlas.png`。这是运行时资源，必须保留并由Git管理；它不是构建缓存，不应在清理项目时删除。
