# 黑马记账 UI / Motion / Material 专项升级计划

> 目标版本：Android 1.1.0
>
> 制定日期：2026-08-24
> 原则：保留现有 Kotlin、Jetpack Compose、SQLite、SettingsRepository、PagerState 和业务规则；本轮重构的是设计系统与交互表达，不重写账目、分类、预算和统计数据层。

## 1. 当前工程审计结论

- 工程已经按 `app / core:designsystem / core:domain / core:database / core:data` 分层，适合继续演进。
- 已有两套主题（清透蓝、自然治愈）及各自独立的 Light/Dark Token，不采用“浅色加黑层”的错误做法。
- 已使用 `io.github.kyant0:backdrop:2.0.0`，底栏、玻璃 Surface 和部分选择控件具备真实 Backdrop、Lens、Blur、Highlight 能力。
- 主页面使用同一个 `PagerState`；底栏点击、页面 Swipe 和 Lens 直接拖动已经连通，记账仍是模态主操作而不是 Pager 页面。
- 快速记账已经支持手势关闭、一级/可选二级分类、中文自定义日期选择器、真实金额输入和保存。
- 当前主要问题不是“没有功能”，而是材质参数、Surface 层级、动效时长/弹簧、按压反馈和页面视觉语言仍分散在不同文件中，导致同类组件手感不完全一致，也容易出现 Card Soup。
- 本轮不改数据库 Schema，不需要 Migration，不会破坏现有账单。

## 2. GitHub 源码研究与采用决策

| Project | 实际阅读内容 | 借鉴能力 | 应用位置 | 采用方式 | 结论 |
| --- | --- | --- | --- | --- | --- |
| `Kyant0/AndroidLiquidGlass` | `LiquidBottomTabs`、`LiquidButton`、`LiquidToggle`、`LiquidSlider`、`DampedDragAnimation`、`InteractiveHighlight`、Backdrop/Lens/Highlight/Shadow | 连续 Lens、直接拖动、按压位置高光、速度形变、内外阴影 | 底栏、主记账按钮、分段选择、开关、重要按钮 | `DEPENDENCY` + 自有组件封装 | 保留现有 `backdrop:2.0.0`；不复制 Demo 布局和状态逻辑 |
| `chrisbanes/haze` | Haze 2 `GlassStyle`、`HazeBlurStyle`、Fallback delegate、Performance mode | 样式叠加、能力降级、一次定义多处复用、Fallback 仍保留层级 | `HeimaMaterialSystem`、质量分级、Glass OFF | `REFERENCE_ONLY` | Haze Glass 仍是实验 API 且发布状态变化快；本版不再增加第二套 Backdrop 管线 |
| `QmDeve/AndroidLiquidGlassView` | `Config`、`LiquidGlassView`、Touch/Elastic Demo、AGSL、RenderNode 缓存 | Refraction 参数边界、触点高光、Blur 缓存、Android 13+ 降级意识 | 高等级主操作、Hero、性能策略 | `REFERENCE_ONLY` | 这是 View + Android 13 RuntimeShader 路线；不与 Compose Backdrop 双重采样 |
| `skydoves/FlexibleBottomSheet` | `FlexibleSheetState`、`visibilityProgress`、Swipeable、NestedScroll、动态 Anchor 测试 | 单一连续进度驱动 Sheet、Scrim、背景、Handle；位置+速度结算 | 快速记账、日期、辅助 Sheet | `PARTIAL_REIMPLEMENTATION` | 现有 Sheet 已与业务深度集成；吸收状态模型，不增加整套库和 Material 外观 |
| `android/compose-samples` | Jetsnack 设计系统/Scaffold/删除反馈，JetLagged Canvas/Path，Jetchat 输入与 Back | 自定义组件分层、Canvas 图形、一次性事件、输入/Back 状态 | 页面结构、图表、反馈、导航 | `REFERENCE_ONLY` | 采用官方成熟组织方式，不复制产品视觉 |
| `kizitonwose/Calendar` | Compose `rememberCalendarState`、HorizontalCalendar、单选、区间、禁用边界 Sample | 日期边界、月份状态、Range 选择、手势优先级 | 记账日期与统计自定义日期 | `REFERENCE_ONLY` | 当前日历规模小且已有完整中文 UI；用单元测试补边界，避免为一个网格引入大依赖 |
| `patrykandpatrick/vico` | Cartesian Chart、Marker Controller、Scroll/Zoom State、Sample Marker | Marker、触摸定位、横向拖动、稳定数据模型 | 统计趋势图交互 | `REFERENCE_ONLY` | 当前数据图较轻；先用自有 Canvas 实现可触摸 Marker，避免 APK 和手势复杂度增长 |
| `Calvin-LL/Reorderable` | LazyList 状态、`draggableHandle`、`longPressDraggableHandle`、`animateItem`、Haptic Sample | 显式拖柄、自动让位、拖动抬升、无障碍移动 | 分类管理排序 | `REFERENCE_ONLY` | 当前分类数量有限；复用 Compose 原生拖动和 `animateItem`，不增加依赖 |
| `zed-alpha/shadow-gadgets` | Compose Shadow Scope、Clipped Shadow、兼容节点 | 阴影 Token、避免透明边缘伪影、只对需要处加阴影 | 分类图标、浮层、Modal | `REFERENCE_ONLY` | 现有最低 Android 29 和 Compose 能力足够；建立自有 Token 即可 |
| `fornewid/material-motion-compose` | Shared Axis X/Y/Z、Fade Through、时长分段 | 父子层级 Shared Axis、同层 Fade Through | 我的→管理、预算→编辑、Dialog | `PARTIAL_REIMPLEMENTATION` | 用现有 Compose Animation 写小型统一实现，避免仅为数个过渡增加依赖 |

### 许可证与维护判断

- 真正打进 APK 的 Kyant Backdrop 为 Apache-2.0，继续保留许可证与归属说明。
- 研究项目为 Apache-2.0 或 MIT；本轮不直接复制其源码文件。
- `THIRD_PARTY_NOTICES.md` 仍严格区分“真正依赖”和“只研究”。
- 研究快照显示上述项目在 2026 年仍有维护记录；但“仍维护”不代表必须引入，最终以架构成本、性能和必要性决定。
- 研究期间第一次使用了错误的 Shadow Gadgets 仓库地址，确认 404/克隆失败后改为真实仓库 `zed-alpha/shadow-gadgets`；不会把失败目录或临时源码提交进项目。

## 3. 统一设计系统

### 3.1 Heima Material System

建立统一入口，页面只描述“它是什么层级”，不各自拼 Blur：

- `HeroSurface`：首页今日收支与财务状态，空间最大、玻璃克制、金额最清晰。
- `MetricSurface`：趋势、预算、结余等紧凑指标，较低 Blur/Refraction。
- `InsightSurface`：真实规则生成的洞察，可带局部 Orb / Mesh，但不做常驻高成本动画。
- `ChartSurface`：弱边框、安静背景、更大内部空间，突出数据本身。
- `ListSurface`：轻量分组容器，账单 Row 不再一条一个巨大 Card。
- `InteractiveSurface`：按钮、Chip、开关、选择 Lens，Touch Down 即刻反馈。
- `OverlaySurface`：Bottom Sheet、Dialog、Snackbar，保证前景清晰并控制 Glass-on-Glass。

### 3.2 Glass Quality

统一映射为：

- `High`：Backdrop + Blur + Lens + Vibrancy + 克制 Highlight；仅 P0 与少量 Hero。
- `Balanced`：共享 Backdrop + 中低 Blur + Tint + Highlight；默认等级。
- `Performance`：静态 Tint + Border + Highlight，无实时重采样。
- `Disabled`：主题化 Solid/Translucent Surface；布局、圆角、层级和交互不变。

实际等级由用户质量设置、Glass 开关、Android 版本、节电状态和 Reduce Motion 共同决定；切换只在稳定状态点发生，避免运行中闪烁。

### 3.3 Shadow 与 Typography

- 阴影分为 `None / Soft / Float / Modal`，深色模式主要靠 Surface 色阶与细边缘，不使用大片白 Glow。
- 字体角色分为 `DisplayAmount / HeroNumber / SectionTitle / CardTitle / Body / Caption / DataLabel`。
- 艺术字体只允许出现在品牌与少量大标题；金额、日期、比例和说明使用高可读系统字体。

## 4. 统一 Motion Design System

### 时长

- `Instant`：90ms，Touch Down、极短状态响应。
- `Fast`：150ms，按钮释放、Chip、Eye、轻量反馈。
- `Standard`：220ms，Segmented Lens、列表变化、图表更新。
- `Emphasized`：320ms，Sheet、父子层级、重要容器变换。

### Spring

- `Responsive`：按钮与局部反馈，较高刚度，无明显弹跳。
- `Snap`：底栏 Lens、Segmented Lens，跟手并快速吸附。
- `Soft`：Bottom Sheet、展开区域，有重量但不拖沓。
- `Emphasized`：少量重要容器连续变换。

### 动效等级

1. Micro：按钮、Toggle、Tab、分类、Eye、日期、Marker、保存。
2. Component：二级分类、筛选、Chip、增删、预算详情。
3. Container：记账按钮→Sheet、自定义日期→Picker。
4. Navigation：主 Pager 手势；父子页面 Shared Axis / Slide + Fade；Modal 用 Sheet 或 Scale/Fade。

Reduce Motion 开启后保留位置、拖动、Pager 与必要状态反馈，只移除夸张 Morph、Overshoot、装饰运动和长过渡。

## 5. 分阶段实现

### P0 高频主流程

1. 重构 `HeimaBottomBar`：保留直接拖动，共享 PagerState；加强连续底材、Lens 内外边缘、速度 Morph 与主记账入口，但不制造巨大悬浮蓝球。
2. 建立按触点高光与统一 Press Surface；记账 Touch Down、释放、Sheet 入场形成同一条因果动画。
3. 快速记账 Sheet 改为单一 `visibilityProgress` 驱动位移、Scrim、背景权重、Handle 和内容；避免 Sheet 内每个数字键再次实时 Blur。
4. 保持“金额→一级分类→保存”的 3 秒路径；二级分类仍然可选。

### P1 首页与统计

1. 首页按 Hero / Metric / Insight / List 重排视觉层级，首眼真实财务状态，第二眼记账，第三眼趋势与预算。
2. 统计保留真实查询和 Top 5 + Other；增加图表 Marker/拖动、Donut 选中 Arc、数值与路径更新过渡。
3. 图表颜色使用稳定分类语义映射，并提供独立 Light/Dark 亮度。

### P2 日期、设置、分类

1. 日历统一单选、区间、未来日期禁用、中文星期、月份 Swipe；选中为 Glass Lens，Range 为连续轻量连接。
2. Toggle 使用统一组件但 SettingsRepository 仍是唯一真相来源，防止开关反转。
3. 分类管理加入明确拖柄、拖动抬升、自动让位和结束吸附；分类 3D 插画继续与功能线性图标分层。

### P3 预算、次级页与弹窗

1. 替换 Card Soup，使用相应 Surface 层级。
2. 父子页面过渡使用 Shared Axis / Slide + Fade；Dialog 使用克制 Scale/Fade 或材质 Reveal。
3. Snackbar、确认框、输入框统一 Overlay Surface，保持浅色/深色/Glass ON/OFF 一致。

## 6. 性能与耗电策略

- 同一页面尽量共享一个 Backdrop Source；正文 Row、数字键、文字和普通小按钮不做独立实时 Blur。
- 不新增常驻无限动画；所有装饰动效在页面离开、应用后台或 Reduce Motion 开启时停止。
- Path、颜色映射和图表聚合使用 `remember`/稳定数据；手势过程尽量更新 Layer/Draw 参数而不是重组整页。
- 高成本 Glass 只用于 P0、Hero 和重要 Overlay；其余使用 Balanced/Performance Surface。
- 模拟器记录基础帧、Jank、内存、启动和手势回归；耗电、触觉手感、扬声器音色、120Hz 和真机 GPU 明确标注 `NEEDS REAL DEVICE VERIFICATION`，不伪造。

## 7. 验证矩阵与完成标准

### 8 种核心视觉组合

- Light / Dark
- Glass ON / OFF
- Reduce Motion ON / OFF

每种组合检查 Button、Toggle、Bottom Navigation、Quick Record Sheet、Calendar、Chart、Dialog。

### 手势矩阵

- 页面 Swipe、Bottom Lens Drag、Sheet Drag、Calendar Swipe、Category Reorder、Chart Drag。
- 验证图表横拖不会误切 Pager、Sheet 拖动不会扰乱内部滚动、日历横滑不会误切主页面。

### 工程检查

- Unit Test、Android Instrumented/UI Test、Lint、Debug/Release 构建。
- 模拟器安装与启动、核心流程、日志 FATAL/ANR 检查。
- 生成 `UI_MOTION_MATERIAL_REPORT.md`，逐条核对本指令 55 项，并更新正式发布、测试、性能、第三方与开发日志文档。

## 8. 版本与发布

- 本次为明显的大型 UI/交互升级，目标版本定为 `1.1.0`，`versionCode` 必须高于 103。
- 最终 APK 使用现有正式签名生成，保证可以覆盖 1.0.3；签名密钥和口令绝不进入 Git/GitHub。
- 在线更新采用“GitHub Releases 检查新版本并引导系统浏览器下载”的免费路线；不申请危险的静默安装权限，不上传用户账单。首次安装 1.1.0 仍由用户手动完成，以后版本可在 App 内检查更新。
- 本轮属于重大更新：全部测试通过后自动创建 Git 存档并推送私有源码仓库；公开发布只放 APK、版本说明和校验值，不公开私有源码与签名材料。
