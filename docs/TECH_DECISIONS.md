# 黑马记账技术决策

## 收入与支出共用账目模型

收入和支出拥有相同的金额、日期、备注与两级分类结构，因此继续使用同一张账目表，并增加受约束的 `entry_type` 字段，而不是复制两套表和接口。分类同样标记适用类型，主进程会校验“账目类型、一级分类、二级分类”三者一致。这样可以统一编辑、导出和备份，也能在首页可靠计算收入减支出的结余。数据库第2版迁移将旧数据明确标记为支出，避免破坏现有账本。

## 1. 桌面框架

| 方案 | 优点 | 缺点 | 本机落地情况 |
| --- | --- | --- | --- |
| Tauri 2 | 体积小、内存较低、系统 WebView | 需要 Rust、Cargo、Microsoft C++ Build Tools；Rust/前端双栈维护 | 当前电脑缺少 Rust、Cargo、C++ 工具，需新增大型环境 |
| Electron 43 | Windows/macOS 共用代码；React、打包和 AI 辅助生态成熟；SQLite 与系统文件能力直接 | 安装包和内存占用大于 Tauri；需持续更新 Chromium | 当前已有 Node.js 24，实际开发、启动与打包路径最短 |
| Flutter Desktop | 原生渲染一致、跨平台 UI 完整 | 需 Flutter SDK、Dart 和 Visual Studio C++；桌面安装器要额外配置 | 当前电脑缺少 Flutter 和 C++ 工具 |

最终选择 Electron 43.4.1。对1.0而言，“能在当前电脑真实完成、测试和打包”比最小体积更重要。Electron 43 自带 Node 24.18.1 与 Chromium，Windows/macOS 共用 React 界面和绝大部分主进程代码。未来可以迁移到 Tauri，但需要重写系统能力层和 SQLite 调用，UI 可较大程度复用。

## 2. 前端与构建

- React 19 + TypeScript：组件和类型生态成熟，便于长期维护。
- electron-vite 5 + Vite 7：分别构建主进程、CommonJS preload 和 renderer，开发反馈快。没有使用 Electron Forge 的实验性 Vite 插件。
- React Router：使用 HashRouter，打包后的本地文件页面无需服务器路由。
- TanStack Query：统一异步 IPC 查询、缓存失效和保存后的刷新。
- React Hook Form + Zod：表单体验与主进程二次校验共用明确规则。
- Recharts：只实现分类、排行、趋势三种与产品问题直接相关的图表。
- 自定义 CSS 变量 + Lucide：避免大型后台组件库，支持完整主题与品牌风格。

## 3. 本地数据

| 方案 | 优点 | 缺点 |
| --- | --- | --- |
| JSON 文件 | 简单、容易查看 | 并发与部分写入风险高；筛选统计、迁移和约束能力弱 |
| IndexedDB | 浏览器原生、无需原生依赖 | 数据位置和人工备份不直观；桌面主进程访问不自然 |
| SQLite | 事务、索引、约束、统计查询和跨平台格式成熟 | 需要 SQLite 绑定和迁移管理 |

最终选择 SQLite + `better-sqlite3` 13。金额以整数分保存，开启外键、WAL、合理同步级别和忙等待；启动执行 `quick_check`。`better-sqlite3` 13 自带 Windows/macOS 的 Node-API 预编译文件，因此设置 `npmRebuild: false`，避免在包含空格的路径下无意义地调用 node-gyp。未来可继续用 SQL 迁移扩展表结构。

## 4. 备份与恢复

- CSV 只负责通用表格导出，不作为无损恢复格式。
- 完整备份使用版本化 JSON，包含账目和设置，并对规范化数据计算 SHA-256。
- 恢复前校验结构、金额、日期、分类关系、重复编号和校验值。
- 正式替换前自动保存当前数据，使用单个 SQLite 事务完成替换，错误自动回滚。
- 不采用“合并恢复”，因为没有账号或设备身份时无法可靠判断重复账目。

## 5. 安全边界

- renderer 启用沙箱、上下文隔离和 Web 安全，关闭 Node 集成。
- preload 只暴露类型化的最小 API；主进程验证 IPC 来源和所有参数。
- 只加载打包内本地资源，设置 CSP，拒绝权限请求、外部导航和新窗口。
- 数据库损坏时进入保护状态，不自动删除、重命名或重建原数据库。

## 6. 打包

使用 electron-builder 26：Windows 生成 x64 NSIS 安装包、便携版和解压目录；macOS 配置 DMG/ZIP，并需在 Mac 上分别构建 arm64/x64。当前没有购买签名证书，也不使用开发者账号，因此产物未签名；这不会影响功能，但系统可能显示安全警告。
