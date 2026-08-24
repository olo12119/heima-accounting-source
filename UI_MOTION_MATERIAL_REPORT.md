# 黑马记账 UI / Motion / Material 专项升级报告

> 正式版本：Android 1.1.0（versionCode 110）
>
> 核验日期：2026-08-24
> 说明：`✅` 表示已经实现或完成研究决策；`⚠️` 表示代码和模拟器验证已完成，但真实硬件体验仍需真机确认；`❌` 表示未完成。本报告不把模拟器结果冒充真机数据。

## 结论

- ✅ 本轮不是换颜色或堆卡片，而是把玻璃材质、Surface 层级、动效时长、弹簧、按压反馈和性能降级集中为同一套 App 级设计系统。
- ✅ 保留现有账目、分类、预算、统计和 SQLite 架构；数据库 Schema 没有变化，不需要迁移，也不会删除用户账单。
- ✅ 底栏点击、页面滑动和 Lens 直接拖动继续共享同一个 `PagerState`；“记账”是主操作，不是 Pager 页面。
- ✅ 快速记账仍保留“金额 → 一级分类 → 保存”的最短路径，二级分类可选。
- ✅ 1.1.0 新增手动“检查更新”：只读取公开 GitHub Release 元数据并由系统浏览器下载，不静默安装、不上传账单。
- ⚠️ 模拟器完成逻辑、手势、构建和基础性能回归；真实扬声器、触觉、电池、温升、厂商 GPU 与 90/120Hz 为 `NEEDS REAL DEVICE VERIFICATION`。

## GitHub 源码研究与采用

| 项目 | 阅读的真实源码 | 最终采用 |
| --- | --- | --- |
| Kyant0/AndroidLiquidGlass | LiquidBottomTabs、LiquidButton、LiquidToggle、Backdrop、Lens、Highlight、Shadow、拖动与弹簧 | `DEPENDENCY`：继续使用 `backdrop:2.0.0`，由黑马设计系统统一封装 |
| chrisbanes/haze | GlassStyle、BlurStyle、Fallback、性能模式 | `REFERENCE_ONLY`：吸收质量分级与降级思路，不并行引入第二套 Backdrop 管线 |
| QmDeve/AndroidLiquidGlassView | Config、LiquidGlassView、Touch/Elastic Demo、AGSL 与缓存 | `REFERENCE_ONLY`：吸收折射参数和缓存边界，不混用 View Shader |
| skydoves/FlexibleBottomSheet | SheetState、visibilityProgress、Anchor、NestedScroll 与测试 | `PARTIAL_REIMPLEMENTATION`：用单一连续进度联动 Sheet、Scrim、背景和内容 |
| android/compose-samples | Jetsnack、JetLagged、Jetchat 的设计系统、Canvas、输入和 Back | `REFERENCE_ONLY`：采用官方组件分层和一次性事件组织方式 |
| kizitonwose/Calendar | 月份状态、单选、区间、边界与手势示例 | `REFERENCE_ONLY`：保留自有中文 Glass Calendar，补齐禁用未来日期和月份 Swipe |
| patrykandpatrick/vico | Marker、Scroll/Zoom、Cartesian Chart 示例 | `REFERENCE_ONLY`：当前轻量 Canvas 已实现触摸 Marker，不增加大型依赖 |
| Calvin-LL/Reorderable | 拖柄、自动让位、animateItem、Haptic | `REFERENCE_ONLY`：使用 Compose 原生拖动完成分类排序反馈 |
| fornewid/material-motion-compose | Shared Axis、Fade Through、分段时长 | `PARTIAL_REIMPLEMENTATION`：用现有 Compose 动画实现统一父子过渡 |
| zed-alpha/shadow-gadgets | Shadow Scope、Clipped Shadow、兼容节点 | `REFERENCE_ONLY`：建立自有 Shadow Token，避免再加渲染层 |

直接进入 APK 的第三方代码及许可证见 `THIRD_PARTY_NOTICES.md`。研究项目没有复制源码进工程。

## 最终系统

### Material

- `Hero / Metric / Insight / Chart / List / Interactive / Overlay` 七种 Surface 角色代替页面各自拼 Blur。
- `High / Balanced / Performance / Disabled` 四档质量统一决定 Blur、Refraction、Tint、Highlight、Shadow 和回退材质。
- 深色模式使用独立 Token；优先文字可读性，降低白雾、Bloom 和大面积高光。
- Glass OFF 只降低实时材质成本，不改变布局、圆角、信息层级和功能。

### Motion

- 时长：Instant 90ms、Fast 150ms、Standard 220ms、Emphasized 320ms。
- 弹簧：Responsive、Snap、Soft、Shared Axis X，分别服务按压、Lens、Sheet 和父子页面。
- Micro、Component、Container、Navigation 四级动效使用统一 Token。
- Reduce Motion 开启后保留必要位置反馈，缩短或移除 Morph、Overshoot 和装饰运动。

### 性能

- 同一页面共享 Backdrop，避免数字键、Row 和文字层层实时 Blur。
- 图表聚合、颜色映射和手势状态稳定化；Chart 拖动只更新必要状态。
- 节电模式、严重温度状态、旧系统或用户关闭 Glass 时自动使用较低成本材质。
- 没有新增后台轮询、常驻 Timer 或无限装饰动画；更新检查只在用户点击时联网。

## 55 项逐条自查

| # | 原始目标 | 结果 | 自查说明 |
| ---: | --- | :---: | --- |
| 1 | 修改前研究指定 GitHub 项目 | ✅ | 已拉取并阅读真实源码，研究目录留在 Git 忽略的临时区 |
| 2 | 深入 Kyant AndroidLiquidGlass | ✅ | 研究 BottomTabs、Button、Toggle、Lens、Backdrop、拖动、弹簧和阴影 |
| 3 | 深入 Haze | ✅ | 研究 GlassStyle、Fallback 与性能等级；决定不引入第二套采样管线 |
| 4 | 深入 Qm LiquidGlassView | ✅ | 研究 Config、Touch、Elastic、AGSL 与缓存边界 |
| 5 | 深入 FlexibleBottomSheet | ✅ | 用连续 `visibilityProgress` 重构记账 Sheet 联动 |
| 6 | 深入 compose-samples | ✅ | 研究 Jetsnack、JetLagged、Jetchat 并吸收官方组织方式 |
| 7 | 解决 Card Soup | ✅ | 页面改由语义 Surface 角色表达层级，列表不再每行套重玻璃 |
| 8 | HeroSurface | ✅ | 首页主收支与财务状态使用 Hero 角色，信息第一眼可见 |
| 9 | MetricSurface | ✅ | 趋势、预算和紧凑指标统一 Metric 角色 |
| 10 | InsightSurface | ✅ | 真实规则洞察使用 Insight 角色，不制造虚假分析 |
| 11 | ChartSurface | ✅ | 统计图表使用安静背景、弱边缘和更大数据空间 |
| 12 | ListSurface | ✅ | 账单与实体列表统一轻量分组材质 |
| 13 | 统一 Motion Design System | ✅ | 时长、缓动、弹簧均集中在 `MotionMaterial.kt` |
| 14 | 统一 Spring 体系 | ✅ | Responsive、Snap、Soft、Shared Axis 分工明确 |
| 15 | Micro Interaction | ✅ | Button、Toggle、Tab、分类、日期、图表 Marker 均有即时反馈 |
| 16 | Component Motion | ✅ | 二级分类、筛选、Chip、排序和图表更新使用统一过渡 |
| 17 | Container Transform | ✅ | 主记账按钮与 Sheet 使用连续因果动画，日期采用自有 Glass Overlay |
| 18 | Navigation Motion | ✅ | 主 Pager 跟手；次级页使用统一 Shared Axis X |
| 19 | 页面层级动画逻辑 | ✅ | 同层切换与父子前进/返回采用不同语义，不一律淡入淡出 |
| 20 | 底栏继续按第 41 条直接拖动 | ✅ | 超过正常触摸阈值即拖动，不要求长按，经过记账不误弹 |
| 21 | 深化底部 Lens 材质 | ✅ | 单一连续 Lens、局部高光、Tint、边缘和克制速度形变 |
| 22 | 记账按钮按第 42 条升级 | ✅ | 保持连续底栏，同时提高图标、Tint、高光和按压优先级 |
| 23 | Record Button → Sheet | ✅ | 按压、Highlight、背景降权与 Sheet 入场由同一进度衔接 |
| 24 | 记账 Sheet 背景层级 | ✅ | Scrim、Dim、Sheet 和内容分层，减少 Glass-on-Glass 污染 |
| 25 | 日期组件专项 | ✅ | 统一中文日期、边界、选中态和可访问说明 |
| 26 | Liquid Glass Calendar | ✅ | 单日/区间、未来禁用、中文、Lens、月份按钮与左右 Swipe 完成 |
| 27 | 统计图表专项 | ✅ | 评估 Vico 后保留轻量 Canvas，新增触摸 Marker 和数据反馈 |
| 28 | 统计时间切换动画 | ✅ | Lens 与数据内容同步过渡，Reduce Motion 有降级 |
| 29 | Donut 动效 | ✅ | 数值/Arc 平滑更新，扇区可选，Top 5 + 其他保持清晰 |
| 30 | 图表颜色系统 | ✅ | 稳定分类语义映射，并为 Light/Dark 调整亮度 |
| 31 | 分类管理专项 | ✅ | 拖柄、抬升、缩放、阴影、跨位触觉和自动让位完成 |
| 32 | 阴影材质专项 | ✅ | None/Soft/Float/Modal Token 化，避免透明边缘黑边和暗色白雾 |
| 33 | Dark Mode 独立材质 | ✅ | 独立颜色与 Glass Token，不采用 Light 加黑色 Overlay |
| 34 | Typography 材质 | ✅ | 金额、标题、正文、说明和数据标签角色集中管理 |
| 35 | 图标系统 | ✅ | 功能线性图标与 3D 分类插画分层；分类保持统一安全区 |
| 36 | Interaction Feedback 统一 | ✅ | 视觉、音效、触觉三套门独立；按压反馈统一且即时 |
| 37 | Reduce Motion 统一接入 | ✅ | 新增 Sheet、Lens、图表、月份和页面过渡均读取同一设置 |
| 38 | Liquid Glass OFF 统一接入 | ✅ | 所有语义 Surface 使用同一质量解析与静态材质回退 |
| 39 | 不一次引入所有依赖 | ✅ | 只保留实际必要的 Backdrop；其他项目研究或局部重写 |
| 40 | 依赖选择原则 | ✅ | 以功能必要性、兼容、APK、维护和许可证共同决策 |
| 41 | 许可证 | ✅ | 真正依赖保留 Apache-2.0；研究项目与依赖明确区分 |
| 42 | UI 升级不牺牲性能 | ✅ | 控制实时 Blur 数量，稳定图表/手势状态，保留完整回退 |
| 43 | Glass 性能专项 | ⚠️ | 已做 ON/OFF 模拟器压力采样；真机 GPU/高刷仍需验证 |
| 44 | 无操作时停止动态效果 | ✅ | 无常驻无限动效；页面离开、后台和降级状态不持续重绘 |
| 45 | 最终页面 P0–P3 改造 | ✅ | 底栏/记账、首页/统计、日期/设置/分类、预算/次级页已覆盖 |
| 46 | 首页最终感觉 | ✅ | 主收支优先，记账入口明确，指标、洞察和列表层级拉开 |
| 47 | 统计最终感觉 | ✅ | 时间 Lens、Marker、Donut 交互、真实数据和 Top 5 + 其他完成 |
| 48 | 快速记账最终感觉 | ✅ | 保留 3 秒路径；高级材质没有增加必填步骤 |
| 49 | 最终设计关键词 | ✅ | 克制、通透、直接、连续、可读、可降级贯穿公共组件 |
| 50 | 最终 8 组合验证矩阵 | ✅ | Light/Dark × Glass ON/OFF × Reduce Motion ON/OFF 已自动渲染验证 |
| 51 | 手势测试 | ✅ | Pager、Lens、Sheet、Calendar、Category、Chart 均完成自动或 Android 坐标验证 |
| 52 | 真实设备体验 | ⚠️ | APK 可供真机验收；音色、震感、耗电、温升和 120Hz 不伪造 |
| 53 | 专项报告 | ✅ | 本报告包含 Research、Integration、Dependencies、Material、Motion、组件、前后差异、性能与遗留项 |
| 54 | 禁止“假升级” | ✅ | 变更落在公共设计系统和交互根因，并通过构建与测试，不是截图补丁 |
| 55 | 最终目标 | ✅ | 主要动作来自同一成熟系统，手指、Lens、页面和 Sheet 建立直接联系 |

## 已知边界

- ⚠️ 无真实 Android 手机接入测试环境，因此真实音效、触觉、耗电、温升和高刷新率体验不能宣称通过。
- ⚠️ 当前 CI/本机没有独立 Macrobenchmark 模块和长时间 Perfetto 采样；保留现有性能脚本与后续真机步骤。
- ✅ 公开 GitHub Release `v1.1.0` 已上线；APK 位于专用公开发布仓库，源码仓库继续保持私有，签名文件永不上传。

结论：55 项中 53 项达到代码/文档/模拟器验收，2 项因必须依赖真实硬件标记为 `⚠️`，没有 `❌`。这些警告不是隐藏失败，而是无法用模拟器替代的真实验收边界。
