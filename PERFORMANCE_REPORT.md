# 黑马记账 Android 1.0.0 性能报告

## 测试环境

- 设备：Android Studio AVD `Heima_Android_16`
- 系统：Android 16 / API 36
- 分辨率：1080 × 2400
- 刷新率：60 Hz
- 构建：R8 压缩后的 `release` APK
- 采集：`adb shell dumpsys gfxinfo`、`am start -W`、`dumpsys meminfo` 和固定坐标压力脚本

这些数字只代表当前电脑模拟器，不能等同于用户安卓手机。

## 已测结果

| 场景 | Liquid Glass | 总帧 | 现代 Jank | P50/P90/P95/P99 | GPU P50/P90/P95/P99 |
| --- | --- | ---: | ---: | --- | --- |
| 连续 Tab 切换 | 开 | 756 | 21（2.78%） | 31/38/42/57 ms | 17/21/22/24 ms |
| 连续 Tab 切换 | 关 | 738 | 5（0.68%） | 21/28/31/42 ms | 17/21/22/24 ms |
| 首页上下滚动 | 开 | 541 | 2（0.37%） | 20/23/24/25 ms | 17/20/21/24 ms |

说明：Android 16 同时输出一套 `legacy` 卡顿口径，它在本模拟器上与现代 Frame Deadline 指标严重不一致，因此表格使用 Android 当前的截止帧指标作为主结论，并保留原始输出而不混算。

## 性能工具覆盖

| 工具/方法 | 最终状态 | 说明 |
| --- | --- | --- |
| `dumpsys gfxinfo` | ✅ 已使用 | 记录页面切换、首页滚动的帧耗时与 Jank。 |
| `am start -W` | ✅ 已使用 | 记录冷启动与已有任务恢复。 |
| `dumpsys meminfo` / CPU 采样 | ✅ 已使用 | 记录当前模拟器内存和瞬时 CPU。 |
| Android Studio Profiler | ⚠️ `NOT TESTED` | 需要用户在真机复测时连接 Profiler。 |
| Perfetto | ⚠️ `NOT TESTED` | 本轮没有生成可复核的 Perfetto Trace。 |
| Macrobenchmark | ⚠️ `NOT IMPLEMENTED` | 未建立独立 Benchmark 模块，不能声称取得官方 Macrobenchmark 数据。 |
| JankStats | ⚠️ `NOT IMPLEMENTED` | 当前使用系统 `gfxinfo`，没有把 JankStats 采集代码带入正式 App。 |
| 自定义 Baseline Profile | ⚠️ `NOT GENERATED` | Release 包含常规优化，但本轮没有生成设备采样的自定义 Baseline Profile。 |

这些缺口不会阻止记账功能运行，但意味着“不同真实手机上的完整性能结论”仍需后续真机实验。报告保留缺口，不用估算值代替。

## 启动、内存与处理器

- 冷启动观测范围：489～1506 ms；最终签名 APK 覆盖安装后的一次冷启动为 1506 ms。
- 已有任务恢复：约 42 ms。
- Glass 开启、Tab 压力后：总 PSS 44,537 KB，总 RSS 161,516 KB。
- Glass 关闭、重启后：总 PSS 33,604 KB，总 RSS 144,588 KB。
- 单次瞬时 CPU 采样：压力结束后 Glass 开约 3%，关闭重启后约 1%。这不是长时间平均值。
- GPU 利用率百分比：`NOT TESTED`；当前只取得每帧 GPU 时间分位数。
- 平均 FPS：`NOT TESTED`；屏幕目标为 60 Hz，但不能用刷新率冒充实际平均 FPS。

## 快速记账面板

最终实现采用：

- 3D 分类图集全 App 单次解码；
- 数字键盘使用轻量玻璃表面，不为十二个按键分别做实时 Backdrop；
- 面板打开时暂停被完全遮挡的底栏玻璃绘制；
- 大面积 Sheet 使用兼容玻璃材质，不执行逐帧全屏实时模糊；
- 入场缩短为 32dp / 90ms；Drag Handle 仍跟手，未达关闭阈值时 Spring 回位。

模拟器对复杂 Sheet 的首次绘制仍明显比首页滚动和 Tab 切换昂贵。最终复测的单次入场只产生少量动画帧，但这些帧仍有截止帧超时；因此该项结论为：

- ⚠️ 功能与手势通过，持续动画时间已经压短；
- ⚠️ 模拟器帧指标未达到与首页滚动相同的水平；
- ⚠️ 需要在用户主力安卓手机上确认实际触控手感，不能标为完全通过。

## 大数据量

- ✅ 100 条账单：事务替换与读回通过。
- ✅ 1000 条账单：事务替换与读回通过。
- ✅ 10000 条账单：事务替换与读回通过。
- ⚠️ 三种规模的独立耗时和滚动 FPS 没有可靠仪器数据，因此不填写虚假毫秒数。

## 功耗与温度

- 真机电量/小时：`NOT TESTED`
- 真机温升：`NOT TESTED`
- 真机 Thermal Throttling：`NOT TESTED`
- 模拟器电池数据不能代表真实手机，因此没有写入功耗数字。

代码层已确认：无网络轮询、无后台服务、无无限动画、无常驻 Timer；App 退到后台后 Compose 动画停止绘制。系统省电模式会关闭高成本 Blur/Lens，并使用更不透明的回退 Surface。

## 真机复测步骤

1. 安装正式 APK，重启手机后打开黑马记账。
2. 分别在 Liquid Glass 开/关状态连续切换 5 个页面 20 次。
3. 连续打开、下拉和关闭记账面板 20 次。
4. 导入或实际建立 1000 条账单，检查首页、统计和账单滚动。
5. 使用 Android Studio Profiler/Perfetto 记录 5 分钟 CPU、内存、帧和温度；再在系统电池页观察 30 分钟真实耗电。
6. 若主力手机出现明显掉帧，优先把视觉质量设为“自动”或关闭 Liquid Glass，不影响账本功能。
