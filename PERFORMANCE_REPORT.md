# 黑马记账 Android 1.0.2 性能报告

## 测试环境

- Android Studio AVD：`Heima_Android_16`
- Android 16 / API 36，1080×2400，模拟 60Hz
- Debug：自动手势和 `dumpsys gfxinfo`；R8 Release：最终冷启动与冒烟
- 采样脚本：`measure-tab-performance.ps1`、`measure-record-sheet-performance.ps1`、`measure-navigation-drag-performance.ps1`

模拟器适合发现趋势，不代表真实手机 GPU、120Hz、耗电或温升。本报告保留所有有效高压结果，不以“肉眼流畅”代替数据。

## 有效采样

| 场景 | Glass | 总帧 | 现代 Jank | P50/P90/P95/P99 | GPU P50/P90/P95 | 总 PSS |
| --- | --- | ---: | ---: | --- | --- | ---: |
| 快速记账打开/关闭 12 次 | ON | 312 | 49（15.71%） | 19/48/77/117ms | 16/20/24ms | 112,518 KB |
| 快速记账打开/关闭 12 次 | OFF | 307 | 42（13.68%） | 18/48/77/133ms | 15/19/20ms | 106,541 KB |
| 底栏 Lens 拖动与吸附 8 轮 | ON | 497 | 48（9.66%） | 22/42/53/150ms | 16/21/23ms | 123,124 KB |
| 底栏 Lens 拖动与吸附 8 轮 | OFF | 486 | 44（9.05%） | 18/31/38/101ms | 15/19/21ms | 106,180 KB |

说明：

- `dumpsys` 在拖动样本的 GPU P99 返回 4950ms 哨兵异常值，因此表中不采用 P99；P50/P90/P95 正常可用。
- 压力脚本连续操作，间隔短于普通用户使用节奏，数值用于版本回归而非承诺帧率。
- Glass OFF 的中位帧时间和内存均较低，证明关闭开关确实降低材质成本，而不是视觉反转。
- Glass ON 的高压 Jank 仍未达到理想目标，必须在主力真机上继续验收；功能层面可随时关闭 Glass。

## 启动、CPU 与内存

- ✅ 1.0.2 R8 正式包覆盖安装后的冷启动：622ms。
- ✅ Debug 高压脚本冷启动样本：716～854ms。
- ✅ 压力结束后的瞬时 CPU 样本为 0～0.1%；这只表示静止后 CPU 已回落，不代表动画峰值。
- ✅ App 没有网络轮询、后台服务、常驻 Timer 或无限图表动画；进入后台后 Compose 动画不再持续绘制。
- ✅ 100、1000、10000 条账单数据库替换与读回通过。

## 已完成的性能约束

- 主题、Glass、Sound、Haptic 由单一 StateFlow 驱动，避免 UI 与引擎重复状态和无效重组。
- Pager 使用官方 `HorizontalPager`；记账不作为隐藏页面参与 Pager 预组合。
- Bottom Lens 只维护单一动画位置，拖动跨过“记账”不会创建 Sheet。
- 统计日期范围直接在 SQLite 查询，避免先读取全部账单再在 UI 过滤。
- Donut 默认最多 6 个扇区，避免分类增多后大量 Path、颜色和图例。
- SoundPool 一次预加载，点击不重复创建 MediaPlayer。
- Glass OFF、Reduced Motion、系统省电和视觉质量可降低材质与动画成本。
- R8 和资源收缩启用；正式 APK 为 4,829,042 字节。

## 工具覆盖

| 方法 | 状态 |
| --- | --- |
| `dumpsys gfxinfo` | ✅ ON/OFF、Sheet、Tab、Lens Drag 已采样 |
| `dumpsys meminfo` / `cpuinfo` | ✅ 已采样进程快照 |
| `am start -W` | ✅ Release 冷启动 622ms |
| 30 项模拟器 UI/手势/截图测试 | ✅ 全部通过 |
| 100/1000/10000 数据库测试 | ✅ 通过 |
| Android Studio Profiler | ⚠️ NOT TESTED |
| Perfetto 长时间 Trace | ⚠️ NOT TESTED |
| 独立 Macrobenchmark 模块 | ⚠️ NOT IMPLEMENTED |
| 真机电池/温度/120Hz | ⚠️ NEEDS REAL DEVICE VERIFICATION |

## 真机复测建议

1. 安装 `手机安装包/黑马记账-Android-正式版-1.0.2.apk`。
2. 分别在 Glass ON/OFF 下拖动底栏、切页和打开记账面板各 20 次。
3. 用 Android Studio Profiler 或 Perfetto 记录 5 分钟 CPU、内存和帧时间。
4. 导入 1000 条账单后检查首页、统计和账单滚动。
5. 用系统电池页观察至少 30 分钟；若主力手机掉帧，先选“自动”或关闭 Liquid Glass，账本功能不受影响。
