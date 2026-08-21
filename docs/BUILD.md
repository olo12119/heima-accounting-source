# 黑马记账构建说明

## 当前验证环境

- Windows x64，系统内部版本 26100
- Node.js 24.19.0、npm 11.17.0
- Electron 43.4.1（内置 Node 24.18.1）
- Git 2.55.0、PowerShell 5.1
- 不需要 Rust、Cargo、Flutter 或 Visual Studio C++ Build Tools

项目路径可以包含中文和空格，当前项目已经在该路径完成类型检查、测试、Electron 启动和构建。

## 安装依赖

在项目根目录打开 PowerShell：

```powershell
npm install
```

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

## Windows 构建

```powershell
npm run dist:win
```

产物位于 `release`：

- `HeimaAccounting-Setup-1.1.0-x64.exe`：带安装向导的 NSIS 安装包。
- `HeimaAccounting-Portable-1.1.0-x64.exe`：无需安装的便携版。
- `win-unpacked\HeimaAccounting.exe`：解压运行目录中的主程序。

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
