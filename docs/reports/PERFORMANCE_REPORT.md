# 黑马记账 Android 1.1.0 性能报告

## 环境与结论边界

- Android Studio AVD：`Heima_Android_16`，Android 16 / API 36。
- 最终发布包使用 R8 与资源收缩；模拟器可检查回归和明显阻塞，但不能代表真实手机 GPU、电池、温升和高刷新率。
- 本轮把实时 Glass 收敛到共享 Backdrop 和少量高等级交互表面；普通 Row、数字键和正文不重复建立全尺寸 Blur。

## 实测结果

| 项目 | 结果 | 解释 |
| --- | --- | --- |
| 正式 1.1.0 冷启动 | ✅ 971ms | `am start -W`，启动日志无 App FATAL/ANR |
| 最终 APK 大小 | ✅ 4,894,578 字节 | R8 Release + v3 正式签名 |
| JVM 测试 | ✅ 38/38 | 规则、版本、材质质量解析 |
| Android 模拟器测试 | ✅ 40/40 | 数据库、UI、视觉组合和手势 |
| Headless SwiftShader Sheet 压测 | ⚠️ 164 帧，144 Jank（87.80%） | P50/P90/P95/P99 为 133/200/250/600ms；该模拟器渲染器过载，只能作为无崩溃压力哨兵，不能代表手机 |
| 压测内存快照 | ⚠️ 112,702 KB | 同一 Headless 场景，只记录不做真机承诺 |
| 导航并行压测 | ⚠️ INVALID | 脚本竞争并导致模拟器离线，结果已废弃 |
| 真机 GPU/电池/温升/120Hz | ⚠️ NOT TESTED | 必须真实设备验证 |

## 已实施约束

- `HeimaMaterialSystem` 集中解析 High/Balanced/Performance/Disabled，避免每个页面自行叠 Glass。
- 同一页面共享 Backdrop；Glass OFF、节电、严重温度状态和旧系统使用低成本回退。
- Sheet 使用单一 `visibilityProgress`，避免多套动画状态互相追赶和重复重组。
- Chart 聚合与颜色映射保持稳定；拖动只更新 Marker 所需状态。
- Pager、Bottom Lens 和页面选中态共享同一个 `PagerState`，不维护三套可能错位的状态。
- 没有后台更新轮询、后台服务、无限 Timer 或常驻装饰动画；检查更新只在用户点击时执行。

## 未伪造的数据

以下项目当前明确为 `NEEDS REAL DEVICE VERIFICATION`：

- FPS/Jank 的真实手机数值和 90/120Hz 表现。
- GPU、CPU 峰值、稳定内存和厂商系统差异。
- 30 分钟以上耗电、温升与 Thermal 降级。
- 触觉手感和扬声器音色。
- Android Studio Profiler、长时间 Perfetto 和独立 Macrobenchmark 尚未形成可靠最终数据。

## 真机验收步骤

1. 安装 `手机安装包/黑马记账-Android-正式版-1.1.0.apk`。
2. 分别在 Glass ON/OFF 下连续拖动底栏、打开记账、切换统计和月份各 20 次。
3. 打开“减少动态效果”后复测，确认反馈仍即时但 Morph 明显减少。
4. 在 Android Studio Profiler 或 Perfetto 记录至少 5 分钟；再用系统电池页观察 30 分钟以上。
