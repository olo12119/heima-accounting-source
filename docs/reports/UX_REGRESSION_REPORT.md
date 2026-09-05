# 黑马记账 Android 1.1.0 UX 回归报告

| 验收项 | 结果 | 说明 |
| --- | :---: | --- |
| 统一 Material System | ✅ | 七种语义 Surface 和四档 Glass Quality 已接入公共组件 |
| 统一 Motion System | ✅ | 时长、Spring、按压、Sheet、Lens、图表和父子页面集中管理 |
| Card Soup | ✅ | 页面不再依赖同质大白卡，Hero/Metric/Chart/List 层级可区分 |
| Continuous Bottom Bar | ✅ | 点击、Pager Swipe、Lens 直接拖动同步，记账仍最醒目 |
| Quick Record | ✅ | Sheet 进度联动、背景降权、一级快速保存和可选二级分类保持 |
| Glass Calendar | ✅ | 中文、未来禁用、单日/区间、按钮翻月和月份 Swipe |
| Statistics | ✅ | 时间 Lens、Marker 拖动、Donut 交互、Top 5 + 其他 |
| Category Management | ✅ | 统一图标安全区、拖柄、抬升、阴影、排序和触觉边界 |
| Dark Mode | ✅ | 独立 Token，降低白雾与 Bloom，文字优先 |
| Glass OFF | ✅ | 保持布局和层级，降低实时材质成本 |
| Reduce Motion | ✅ | 8 种视觉组合均可渲染，新动效读取同一设置 |
| Update Check | ✅ | 用户主动检查、浏览器下载、无静默安装与后台轮询 |

## 视觉矩阵

- ✅ Light + Glass ON + Motion
- ✅ Light + Glass ON + Reduce Motion
- ✅ Light + Glass OFF + Motion
- ✅ Light + Glass OFF + Reduce Motion
- ✅ Dark + Glass ON + Motion
- ✅ Dark + Glass ON + Reduce Motion
- ✅ Dark + Glass OFF + Motion
- ✅ Dark + Glass OFF + Reduce Motion

上述 8 种组合由 Android UI 测试逐一切换并验证首页、底栏和主要操作仍可渲染；完整 Android 测试为 40/40。

## 手势矩阵

- ✅ 主页面 Swipe、底部 Lens 直接 Drag、半程返回和跨多个 Tab。
- ✅ 记账 Sheet Drag、关闭阈值、背景层级和快速重复开关。
- ✅ Calendar 按钮换月和 Android 坐标水平 Swipe。
- ✅ 分类排序 Drag 与跨位反馈。
- ✅ Chart 点击/拖动 Marker；不会错误切换 Pager。

## 55 项结论

- ✅ 53 项已通过代码、文档、模拟器或公开发布验收。
- ⚠️ 2 项依赖真实设备，详见 `UI_MOTION_MATERIAL_REPORT.md`。
- ❌ 0 项。

## 仍需用户真机体验

- ⚠️ 音效是否在你的手机扬声器上足够轻、清晰。
- ⚠️ 触觉是否符合你的手机马达和个人偏好。
- ⚠️ 高刷新率、长时间耗电与温升。

这些不是被隐藏的失败，而是模拟器无法替代的真实硬件验收。
