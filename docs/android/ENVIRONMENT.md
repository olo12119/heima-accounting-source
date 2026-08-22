# Android 命令行开发环境

## 当前状态

Android命令行环境已在Windows 11电脑上准备并用于真实编译。没有安装Android Studio、模拟器、Flutter、Visual Studio或iOS工具，也没有修改系统 `PATH`。

大型工具、缓存和下载内容均位于D盘。项目已生成可安装的Android视觉体验APK。

## 工具位置与当前版本

| 工具 | 通俗作用 | 当前版本 | D盘位置 |
| --- | --- | --- | --- |
| Microsoft OpenJDK | 运行构建工具的“发动机” | 17.0.20.1 LTS | `D:\AndroidDev\Jdk\jdk-17.0.20.1+1` |
| Android Command-line Tools | 下载和管理Android官方组件 | 15859902；另有Android CLI 1.0.15985488 | `D:\AndroidDev\Sdk\cmdline-tools` |
| Android SDK Platform | 编译Android 17应用使用的标准 | API 37.0，修订版2 | `D:\AndroidDev\Sdk\platforms\android-37.0` |
| Android SDK Build-Tools | 资源处理、APK打包和签名 | 36.0.0 | `D:\AndroidDev\Sdk\build-tools\36.0.0` |
| Android Platform-Tools | ADB手机连接工具 | 37.0.1 | `D:\AndroidDev\Sdk\platform-tools` |
| Gradle Wrapper | 按项目锁定的APK装配工具 | 9.7.1 | 首次下载后位于 `D:\AndroidDev\GradleCache` |
| Android Gradle Plugin | Android工程构建规则 | 9.3.1 | Gradle缓存内 |
| Kotlin / Compose Compiler | 编译Kotlin和Compose界面 | 2.4.10 | Gradle缓存内 |

Compose依赖使用BOM `2026.08.00`，最低系统为Android 10（API 29），目标和编译标准为Android 17（API 37）。

## 文件完整性

- Microsoft OpenJDK ZIP SHA-256：`3d9006956fc8af5601cd24ffc4f468bef48279c7ebd8171b9bdf90d0aabfbf1f`。
- Android Command-line Tools ZIP SHA-256：`90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a`。
- 初始独立Gradle 9.4.1 ZIP SHA-256：`2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb`。
- 项目Gradle Wrapper 9.7.1 SHA-256：`acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a`，已写入Wrapper配置，下载时自动核验。

## 中文路径处理

项目仍保留在原有中文和空格路径，没有搬家。Android打包在原路径可成功，但Gradle单元测试运行器复用中文绝对路径缓存时无法加载已经编译好的测试类。

项目构建脚本会把同一个文件夹临时映射为 `H:` 英文入口，并关闭该次配置缓存；这只是同一批文件的临时别名，不复制、不移动、不删除源码。脚本结束后会移除由脚本创建的映射。

## 进程级环境

构建脚本只在自己的运行过程中设置：

```text
JAVA_HOME=D:\AndroidDev\Jdk\jdk-17.0.20.1+1
ANDROID_HOME=D:\AndroidDev\Sdk
ANDROID_SDK_ROOT=D:\AndroidDev\Sdk
ANDROID_USER_HOME=D:\AndroidDev\AndroidUserHome
GRADLE_USER_HOME=D:\AndroidDev\GradleCache
```

命令结束后不会改变Windows的全局设置。

## 已知工具问题

- 旧 `sdkmanager` 无法列出API 37.0，因此改用同一官方工具包内的新Android CLI安装。
- 新Android CLI下载API 37.0到100%并写入完整平台后，以Windows异常码 `-1073740791` 退出。随后验证 `android.jar`、`source.properties` 和已安装列表均正确，真实Android工程也已用API 37成功编译；该工具退出问题不影响当前构建，但已保留记录。
- `android.overridePathCheck=true` 是Android官方提供的中文路径兼容开关，Gradle会显示实验性提示；项目没有因此搬家。
