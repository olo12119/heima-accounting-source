# 黑马记账 Android 1.0.3 性能报告

## 测试环境与边界

- Android Studio AVD：`Heima_Android_16`，Android 16 / API 36，1080×2400，模拟 60Hz。
- Debug 用自动手势、`dumpsys gfxinfo`、`meminfo` 和启动计时；R8 Release 用于最终冷启动与冒烟。
- 模拟器适合发现版本间趋势，不代表真实手机 GPU、90/120Hz、耗电或温升。

## 高压场景采样

| 场景 | Glass | 总帧 | 现代 Jank | P50/P90/P95/P99 | GPU P50/P90/P95 | 总 PSS |
| --- | --- | ---: | ---: | --- | --- | ---: |
| 快速记账打开/关闭 12 次 | ON | 327 | 44（13.46%） | 21/34/93/117ms | 15/21/24ms | 107,479 KB |
| 快速记账打开/关闭 12 次 | OFF | 320 | 42（13.12%） | 18/34/81/117ms | 14/19/22ms | 103,075 KB |
| 底栏 Lens 直接拖动 8 轮 | ON | 702 | 84（11.97%） | 34/48/81/150ms | 17/22/23ms | 119,780 KB |
| 底栏 Lens 直接拖动 8 轮 | OFF | 720 | 50（6.94%） | 25/38/57/117ms | 17/21/22ms | 118,223 KB |

说明：

- 脚本连续快速操作，压力高于普通用户节奏；数值用于回归，不承诺真机帧率。
- 模拟器 GPU P99 偶发返回 4950ms 哨兵异常值，因此表中只采用 P50/P90/P95。
- Glass OFF 的底栏拖动 Jank 明显较低，证明开关确实减少实时材质成本，不存在开关反转。
- Glass ON 的底栏拖动仍未达到理想目标；产品保留自动质量、省电和完全关闭 Glass 的降级路线，并必须在主力真机继续验收。

## 启动、CPU 与数据规模

- ✅ 1.0.3 正式包安装后首次冷启动：1,445ms。
- ✅ 随后 3 次冷启动：1,103ms、1,162ms、1,202ms，中位数 1,162ms。
- ✅ 启动后进程存活，冒烟窗口无 App FATAL/ANR。
- ✅ 压力结束后的瞬时 CPU 样本约 0%～0.1%，只说明静止后已回落，不代表动画峰值。
- ✅ App 没有网络轮询、后台服务、常驻 Timer 或无限图表动画；进入后台后 Compose 动画不会继续主动绘制。
- ✅ 100 笔账单迁移保留测试通过；原有 100/1,000/10,000 条数据读写压力路径继续保留。

## 已实施的性能约束

- Pager 的 `beyondViewportPageCount=0`，只组合当前所需页面；记账不是隐藏 Pager 页面。
- 底栏拖动只保存最新绝对位置，使用 conflated command，避免指针采样堆积造成 Lens 落后。
- Glass Lens 只有一个连续实例；拖过“记账”不会创建 Sheet。
- 月趋势先在规则层补零并汇总，UI 只绘制最终点集。
- 统计日期范围和分类查询在 SQLite 执行，不在 UI 读取全部账单后过滤。
- 分类颜色使用 `mutableLongStateOf`，最终 Lint 无装箱性能提示。
- 分类图集复用一张压缩资源，不为每个分类重复解码大图。

## 工具覆盖

| 方法 | 状态 |
| --- | --- |
| `dumpsys gfxinfo` | ✅ Glass ON/OFF、Sheet、Lens Drag 已采样 |
| `dumpsys meminfo` / `cpuinfo` | ✅ 已采样进程快照 |
| `am start -W` | ✅ 1.0.3 Release 冷启动 4 次 |
| 38 项模拟器 UI/手势/截图测试 | ✅ 全部通过 |
| Release Lint / R8 / 资源收缩 | ✅ 通过 |
| Android Studio Profiler | ⚠️ NOT TESTED |
| Perfetto 长时间 Trace | ⚠️ NOT TESTED |
| 独立 Macrobenchmark 模块 | ⚠️ NOT IMPLEMENTED |
| 真机电池/温度/90/120Hz | ⚠️ NEEDS REAL DEVICE VERIFICATION |

## 真机复测建议

1. 安装 `手机安装包/黑马记账-Android-正式版-1.0.3.apk`。
2. 分别在 Glass ON/OFF 下拖动底栏、切页和打开快速记账各 20 次。
3. 在 Android Studio Profiler 或 Perfetto 记录 5 分钟 CPU、内存和帧时间。
4. 用系统电池页观察至少 30 分钟；如果主力手机掉帧，优先选“自动”或关闭 Liquid Glass，记账功能不受影响。
