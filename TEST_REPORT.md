# 黑马记账 Android 1.0.2 测试报告

## 最终门禁

| 检查 | 结果 |
| --- | --- |
| Release Lint | ✅ No issues found |
| JVM 单元/规则测试 | ✅ 26/26，0 失败、0 跳过 |
| 模拟器数据库/UI/手势/截图测试 | ✅ 30/30，0 失败、0 跳过 |
| R8 Release 构建 | ✅ 通过 |
| zipalign / APK v3 签名 | ✅ 通过 |
| 正式包覆盖安装 | ✅ 版本号 102 / 1.0.2 |
| 正式包启动冒烟 | ✅ 冷启动 622ms，无 App FATAL/ANR |

执行命令：

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:lintRelease testDebugUnitTest
.\gradlew.bat :app:assembleRelease
```

## 功能与交互

### 导航

- ✅ 底栏点击：首页、统计、预算、我的正确切换。
- ✅ 页面明显横向手势由 `HorizontalPager` 切换；竖向滚动不误切页面。
- ✅ 记账不是 Pager 页面，滑过中间位置不会自动弹出 Sheet。
- ✅ 按住底栏 Lens 可连续拖动并吸附；快速跨过多个位置后页面、Lens、图标和文字保持同步。
- ✅ Reduced Motion 开启后缩短位移并减少 Morph，不影响功能。

### 删除与撤销

- ✅ 删除后立即从列表和统计移除，并显示“账单已删除 / 撤销”。
- ✅ 不操作时约 4 秒自动消失，不因 Compose 重组重新计时。
- ✅ 点击撤销后账单和统计恢复，提示立即结束。
- ✅ 连续删除采用最近一次事件；切页面不会产生永久 Snackbar。

### 设置

- ✅ Liquid Glass、操作音效、触觉反馈：`true = 右侧强调色 = 功能开启`。
- ✅ 减少动态效果：`true = 开关右侧 = 动画减少`，没有双重否定。
- ✅ OFF→ON、ON→OFF、页面返回、Activity 重建、重新读取本地设置均通过。
- ✅ 行为测试确认：Glass 会改变实际材质，Reduce Motion 会改变实际动效策略，Sound/Haptic 开关控制各自反馈门。
- ✅ Liquid Glass ON/OFF 截图采样存在显著像素差；自然主题 Light/Dark 也存在独立渲染差异。

### 音效、触觉和视觉反馈

- ✅ `SoundPool` 在启动后预加载项目自有的极短 UI Sound；保存、删除、撤销和重要确认按规则触发。
- ✅ Haptic 与 Sound 代码和设置完全分离；分类选择、日期选择、Lens 边界和保存使用轻反馈。
- ✅ 普通/Glass 控件具有即时按压缩放、Tint/Highlight 和 Spring 恢复。
- ⚠️ 模拟器只能验证 API 调用与状态门，真实声音大小和震动手感为 `NEEDS REAL DEVICE VERIFICATION`。

### 统计与日期

- ✅ 今日、本周、本月、今年的 Segmented Lens 与图表状态同步。
- ✅ 自定义单日和日期区间可打开、选日、切月、取消、确定、修改和重置。
- ✅ 日期选择器完整中文化，没有 August、Cancel、OK。
- ✅ SQLite 日期范围使用含起始、不含下一日零点的查询；最后一天不会漏算。
- ✅ Top 5 + 其他规则、3% 阈值、稳定排序和金额守恒测试通过。
- ✅ 点击“其他”可展开完整分类明细；点击普通扇区可展示对应账单。

### 数据与大规模

- ✅ 默认两级收支分类、CRUD、自定义分类、预算、CSV、备份校验和事务回滚。
- ✅ 二级分类必须属于当前一级分类；错误关系在数据层被拒绝。
- ✅ 100、1000、10000 条账单替换与读回通过。
- ✅ 正式版没有预置假账单；统计与洞察只来自用户真实数据。

## 视觉回归

- ✅ 检查首页、统计、预算、我的、设置、快速记账、日期、分类、Snackbar、Dialog、Bottom Sheet。
- ✅ 抽样覆盖澄澈蓝/自然治愈、Light/Dark、Liquid Glass ON/OFF。
- ✅ Dark Mode 无全屏黑色 Overlay、大片白雾、Glow 遮字或卡片边缘发白。
- ✅ Glass OFF 仍保持主题颜色、圆角、层级、Primary Record Button 和页面布局。

## 本轮真实失败与修复

1. ❌ 日历入口首次编译导入了错误的 `CornerRadius` 包；修正为 Compose Geometry 类型后，同一测试门禁通过。
2. ❌ 模拟器残留另一把 Debug 签名导致测试包无法覆盖；只卸载模拟器的 `.dev` 与 `.dev.test`，没有动正式包或手机数据。
3. ❌ 删除撤销测试最初滚动定位不稳定；改为按用户真实路径进入账单页后，自动消失与撤销均通过。
4. ❌ 性能 ON/OFF 脚本首次并行运行，互相重启同一个 App 并产生 0 帧无效结果；作废该结果并按顺序重测。
5. ❌ PowerShell 默认执行策略阻止性能脚本；只对单次命令使用 `-ExecutionPolicy Bypass`，未修改系统永久策略。
6. ❌ 第一次 APK 签名进程缺少 `JAVA_HOME`；仅在该进程指定 D 盘 JDK 后重新签名、验签成功。

## 仍需真机验证

- ⚠️ 扬声器实际响度和音色、静音/勿扰的厂商差异。
- ⚠️ 震动强弱、不同品牌触觉马达手感。
- ⚠️ 电池、温升、Thermal、低内存和 90/120Hz 表现。

以上项目没有被写成“已通过”，模拟器不冒充真实手机。
