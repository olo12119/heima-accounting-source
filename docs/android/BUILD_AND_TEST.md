# Android Studio运行、构建与测试说明

## 普通用户唯一推荐入口

1. 在项目根目录双击 `00-用Android Studio查看手机版.cmd`。
2. 等待Android Studio底部的同步和索引结束；第一次可能需要几分钟。
3. 顶部设备列表选择 `Heima_Android_16`。
4. 点击绿色三角形“运行”。虚拟手机会自动安装并打开当前源码。
5. 检查结束后，点击顶部红色方块停止App；再关闭虚拟手机或Android Studio。

当前源码版本是 `0.3.0-performance-motion`。本轮按产品负责人要求不复制独立APK，因此 `手机安装包` 中的0.2.0只是历史对照，不能代表当前代码。

## 容易混淆的版本

| 名称 | 当前状态 | 是否代表当前代码 |
| --- | --- | --- |
| Android Studio源码运行 `0.3.0-performance-motion` | 当前推荐 | 是 |
| 历史APK `0.2.0-visual` | 保留用于对比 | 否 |
| 历史APK `0.1.0-visual` | 保留用于对比 | 否 |
| 正式发布APK/AAB | 尚未制作 | 否 |
| Windows免安装版 `1.5.0` | 独立产品线 | 否 |

## 不复制APK的检查入口

开发者运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\apps\android\scripts\verify-studio.ps1"
```

它使用D盘JDK、SDK与Gradle缓存，并执行：

```text
:app:assembleDebug
:app:testDebugUnitTest
:app:lintDebug
```

Gradle在 `apps/android/app/build` 内生成内部调试文件是Android Studio运行所必需的，但脚本不会把APK复制到 `手机安装包`，也不会把它当作用户交付成品。

虚拟手机界面测试：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\apps\android\scripts\verify-studio.ps1" -Tasks ":app:connectedDebugAndroidTest"
```

页签性能回归：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\apps\android\scripts\measure-tab-performance.ps1" -PauseMilliseconds 300
```

性能脚本固定使用1080×2400的 `Heima_Android_16`，每300毫秒切换一次，共32次；它会先强制停止旧任务、从首页启动并清空旧帧统计，避免把打开的记账面板或上一次数据混入结果。

## 0.3阶段验证标准

- Kotlin/Compose编译通过。
- 金额单元测试覆盖直接元输入、小数点、最多两位小数和整数分转换。
- 严格Android Lint零错误，不建立基线隐藏问题。
- 虚拟手机界面测试覆盖四个真实页面、`12.50`原样显示、收入/支出切换和对应常用分类。
- 32次页签性能记录帧分位数、GPU分位数、慢UI线程、慢绘制与截止帧超时。
- 目视检查首页和记账面板：中央按钮不被系统手势条遮挡，选中镜片不盖住图标，记账键盘不留下大块无用空白。

## 性能与耗电边界

- Liquid Glass只在底栏、重点卡片和记账面板等有限区域使用，不做全屏持续模糊。
- 页面使用短距离、高阻尼Spring交接；不用整页普通淡入叠加。
- 金额输入立即更新，不使用逐位滚动动画。
- 系统省电模式自动关闭昂贵模糊并提高表面不透明度。
- 没有无限动画、后台服务、网络轮询或定时任务。

模拟器能发现布局和明显帧问题，但不能等同于用户安卓手机的温度、电量和厂商系统表现；正式发布前仍需真机验收。
