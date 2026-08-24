# Android 正式版运行、构建与测试

## 新手查看界面

1. 双击项目根目录 `00-用Android Studio查看手机版.cmd`。
2. 等待同步完成，选择 `Heima_Android_16`。
3. 点击绿色三角形。
4. 检查完点击红色方块停止 App。

## 手机安装

当前唯一正式文件：

```text
手机安装包\黑马记账-Android-正式版-1.0.3.apk
```

它和 Android Studio 调试运行的区别：APK 用项目专用发布密钥签名，适合手机长期安装；Android Studio 运行的是 `.dev` 调试包，适合电脑开发检查。

## D 盘工具链

```powershell
$env:JAVA_HOME='D:\AndroidDev\Jdk\jdk-17.0.20.1+1'
$env:ANDROID_HOME='D:\AndroidDev\Sdk'
$env:GRADLE_USER_HOME='D:\AndroidDev\GradleHome'
Set-Location '.\apps\android'
```

这些变量只作用于当前命令窗口，不修改系统 PATH。

## 最终质量门禁

```powershell
.\gradlew.bat :app:lintRelease :core:domain:test :core:data:test :core:database:test :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

- `lintRelease`：以正式包配置检查 Android/Compose 错误和性能隐患。
- JVM 测试：金额、连续月趋势、日期、分类、CSV、反馈门和真实财务洞察等 34 项。
- `connectedDebugAndroidTest`：在已启动的模拟器执行 38 项数据库迁移、UI、手势和截图测试。
- `assembleRelease`：生成 R8 压缩的未签名 Release 中间产物。

最终签名使用忽略 Git 的 `.local-signing/heima-release.jks`。密钥丢失后无法给现有用户做覆盖升级，因此必须单独备份。

## 性能回归

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\measure-tab-performance.ps1" -PackageName "com.heima.accounting" -Cycles 12 -PauseMilliseconds 180
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\measure-record-sheet-performance.ps1" -PackageName "com.heima.accounting.dev" -MaterialMode ON -Cycles 12
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\measure-navigation-drag-performance.ps1" -PackageName "com.heima.accounting.dev" -MaterialMode ON -Cycles 8
```

脚本固定针对 1080×2400 的项目模拟器。模拟器可以发现卡顿趋势，但不能代替真机电池、温度和厂商 GPU 验收。如果模拟器自身出现 System UI ANR 或固定异常 GPU 值，必须把该轮结果标为无效，不能归因给 App。

## 报告位置

- Lint：`app/build/reports/lint-results-release.html`
- 单元测试：各模块 `build/reports/tests/testDebugUnitTest/index.html`
- 模拟器测试：`app/build/reports/androidTests/connected/debug/index.html`
- 正式总报告：`docs/reports/FINAL_RELEASE_REPORT.md`
