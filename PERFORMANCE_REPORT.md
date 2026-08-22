# 黑马记账 Android 1.0.1 性能报告

## 测试环境与诚实结论

- 设备：Android Studio AVD `Heima_Android_16`
- 系统：Android 16 / API 36
- 分辨率：1080 × 2400，60Hz
- 图形：Android Emulator OpenGL ES Translator / NVIDIA RTX 4060
- 构建：1.0.1 Debug 用于自动交互，R8 Release 用于最终启动冒烟

本轮确实执行了 Liquid Glass ON/OFF、记账 Sheet、普通 Tab、启动、CPU、GPU 和内存采集。但采集期间模拟器弹出 `System UI isn't responding`；重启虚拟系统后，系统仍在所有场景把 GPU P50/P90/P95/P99 固定报告为不可能的 `4950ms`。因此：

- ⚠️ FPS：`NOT VALID`，不能从异常模拟器数据计算。
- ⚠️ Jank：`NOT VALID`，系统 UI 自身 ANR，ON/OFF 和普通 Tab 均同样异常。
- ⚠️ GPU 帧时间：`NOT VALID`，固定 `4950ms` 是损坏数据。
- ⚠️ 启动性能基线：`NOT VALID`，系统 UI 异常时冷启动为 2.6～3.0 秒。
- ✅ 功能交互仍完成 21/21 自动化测试，无 App FATAL/ANR。

没有用这些失真的高卡顿数字宣称 App 性能变差，也没有用旧数字冒充本轮结果。

## 本轮原始异常样本

| 场景 | 帧 | 系统报告 Jank | GPU 分位数 | 瞬时 CPU | 总 PSS / RSS |
| --- | ---: | ---: | --- | ---: | --- |
| 记账 Sheet 12 次，Glass ON | 136 | 129（94.85%） | 4950/4950/4950/4950ms（无效） | 0%（结束后瞬时） | 100,560 / 216,472 KB |
| 记账 Sheet 12 次，Glass OFF | 111 | 106（95.50%） | 4950/4950/4950/4950ms（无效） | 0%（结束后瞬时） | 104,513 / 220,152 KB |
| 普通 Tab，低频操作 | 109 | 106（97.25%） | 4950/4950/4950/4950ms（无效） | 未单独采样 | 未单独采样 |

Glass OFF 并没有改善异常数字，普通 Tab 也同样异常；结合系统 UI ANR，可以确认这组数据不能用于 ON/OFF 产品比较。内存是当时进程快照，不代表峰值或长期平均；CPU 只是压力结束后的瞬时值。

## 仍然完成的代码性能修正

- ✅ 分类 3D 图集在 App 层只解码一次，不为每个分类重复解码整图。
- ✅ 图标预处理统一裁掉透明噪点并只把正式图集放进 APK；旧 1.59MB 源图移到工具目录，不再重复打包。
- ✅ 记账 Sheet 自身使用一块足够不透明的模态材质，数字按键和分类不再层层执行实时 Backdrop。
- ✅ 删除 Sheet 移动期间的全屏 `RenderEffect` Blur，改为背景降对比 + 单层 Dim；视觉回归确认仍无文字穿透。
- ✅ 统计计算使用 `Dispatchers.Default`，不让数据库汇总卡住选中 Lens 动画。
- ✅ 动画只在数据变化时播放；没有常驻 Timer、网络轮询、后台服务或无限图表动画。
- ✅ Reduced Motion、系统省电和 Liquid Glass OFF 都能降低动画/材质成本，布局和功能不变。
- ✅ R8 代码压缩和资源收缩后，APK 从 1.0.0 的 5,238,642 字节降为 4,747,122 字节。

## 历史可用基线（仅供对照，不是 1.0.1 复测）

在模拟器图形统计尚正常的 1.0.0 测试中曾取得：

| 旧版本场景 | Glass | 总帧 | Jank | GPU P50/P90/P95/P99 |
| --- | --- | ---: | ---: | --- |
| 连续 Tab 切换 | 开 | 756 | 21（2.78%） | 17/21/22/24ms |
| 连续 Tab 切换 | 关 | 738 | 5（0.68%） | 17/21/22/24ms |
| 首页滚动 | 开 | 541 | 2（0.37%） | 17/20/21/24ms |

这些数字只证明此前同一 AVD 曾能正确输出合理 GPU 时间；不能替代 1.0.1 真机复测。

## 工具覆盖

| 方法 | 状态 |
| --- | --- |
| `dumpsys gfxinfo` | ⚠️ 已执行，但本轮结果因系统 UI ANR 无效 |
| `dumpsys meminfo` / `cpuinfo` | ✅ 已采集进程快照 |
| `am start -W` | ⚠️ 已执行，但本轮启动时间不构成有效基线 |
| 21 项模拟器功能/UI 测试 | ✅ 全部通过 |
| Android Studio Profiler | ⚠️ `NOT TESTED` |
| Perfetto Trace | ⚠️ `NOT TESTED` |
| Macrobenchmark | ⚠️ `NOT IMPLEMENTED` |
| JankStats | ⚠️ `NOT IMPLEMENTED` |
| 自定义 Baseline Profile | ⚠️ `NOT GENERATED` |

## 大数据量、功耗与温度

- ✅ 100、1000、10000 条账单的事务替换与读回通过。
- ⚠️ 大数据滚动 FPS：`NOT VALID`，本轮模拟器图形统计异常。
- ⚠️ 真机电量/小时：`NOT TESTED`。
- ⚠️ 真机温升与 Thermal Throttling：`NOT TESTED`。
- ⚠️ 真机 60/90/120Hz：`NOT TESTED`。

模拟器电池和温度不能代表用户手机，因此没有编造数字。

## 推荐真机复测

1. 安装 `手机安装包/黑马记账-Android-正式版-1.0.1.apk`。
2. 分别在 Liquid Glass ON/OFF 下连续切换 5 个页面 20 次。
3. 连续打开、拖动、关闭记账 Sheet 20 次。
4. 导入或建立 1000 条账单，检查首页、统计和账单滚动。
5. 连接 Android Studio Profiler/Perfetto，记录至少 5 分钟 CPU、内存、帧、GPU 和温度。
6. 再用系统电池页观察 30 分钟真实耗电；若主力手机掉帧，优先将视觉质量设为“自动”或关闭 Liquid Glass，账本功能不受影响。
