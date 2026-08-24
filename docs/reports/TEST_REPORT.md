# 黑马记账 Android 1.1.0 测试报告

## 最终门禁

| 检查 | 最终结果 |
| --- | --- |
| JVM 单元/规则测试 | ✅ 38/38，0 失败、0 错误、0 跳过 |
| Android 16 模拟器测试 | ✅ 40/40，0 失败、0 跳过 |
| 8 种视觉组合渲染 | ✅ Light/Dark × Glass ON/OFF × Reduce Motion ON/OFF |
| Release Lint | ✅ 通过 |
| R8 Release 构建 | ✅ 通过 |
| APK 正式签名 | ✅ v3，证书与 1.0.3 一致 |
| 覆盖安装 | ✅ `versionCode=110` / `versionName=1.1.0` |
| 正式包启动 | ✅ 971ms，无 App FATAL/ANR |

最终命令：

```powershell
.\gradlew.bat test lintRelease :app:assembleRelease --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

## 功能、交互与视觉

- ✅ 收入/支出、自然小数、一级快速保存、可选二级精细保存、编辑、删除、撤销、日期和备注原有测试继续通过。
- ✅ 首页、统计、预算、分类、备份恢复和设置只读取真实数据，不创建假账。
- ✅ 底栏点击、页面左右 Swipe 和 Lens 直接拖动同步；经过“记账”位置不会误弹 Sheet。
- ✅ 快速记账 Sheet 的背景、Scrim、主表面、Handle 和内容由同一连续进度联动。
- ✅ 中文日历单选/区间、未来日期禁用、月份按钮和 Android 坐标 Swipe 通过。
- ✅ 趋势图点击/拖动 Marker 不会误切主 Pager；Donut 的 Top 5 + 其他和选中反馈正常。
- ✅ 分类排序的拖柄、抬升反馈和数据顺序保持正常。
- ✅ Liquid Glass ON/OFF 和 Reduce Motion ON/OFF 均保持功能与布局一致，昂贵效果按设置降级。
- ✅ 更新版本比较覆盖相等、较新、较旧和不同长度版本号；检查只由用户触发。

## 真实失败、根因与处理

1. ❌ 第一次研究 Shadow Gadgets 使用了错误仓库地址；确认 404 后改为真实 `zed-alpha/shadow-gadgets`，失败目录未提交。
2. ❌ 模拟器曾残留旧 `.dev` / `.dev.test` 签名而阻止测试包安装；只清理模拟器测试外壳，未删除正式账本，再跑 40/40 通过。
3. ❌ Compose 测试注入的横滑事件无法穿过 Dialog 子节点验证月份 Swipe；未保留这个不可靠测试，改用真实 Android `adb` 坐标手势验证 `2026年8月 → 2026年7月`。
4. ❌ 第一次最终构建在受限环境无法写 D 盘 Gradle 锁文件；获准使用现有 D 盘工具链后同一命令成功，不修改系统 PATH。
5. ⚠️ 一次并行运行性能脚本互相 `force-stop`，结果无效并废弃；正式报告不引用这组数字。

## 真机边界

- ⚠️ 扬声器实际音色与音量、静音/勿扰和不同厂商差异。
- ⚠️ 触觉强弱、触摸延迟和 90/120Hz 手感。
- ⚠️ 电池、温升、Thermal、低内存和真实 GPU 表现。

这些项目为 `NEEDS REAL DEVICE VERIFICATION`，模拟器不冒充真机。
