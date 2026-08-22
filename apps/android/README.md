# 黑马记账 Android App

## 当前状态

这里是Android原生App的独立区域。产品负责人已经确认第一轮界面、两套主题和分类入口逻辑，下一步开始建立Kotlin、Gradle与Jetpack Compose工程。当前仍然没有APK。

当前可运行成品仍是 `../windows-desktop` 中的Windows桌面版1.5.0。Android代码、缓存和APK必须与Windows工程分开，不得删除或覆盖桌面版。

## 已确定方向

- 只开发Android，不制作iOS应用。
- 使用Kotlin与Jetpack Compose原生开发。
- 借鉴iCost的信息组织、Apple精品应用的留白和层级、Liquid Glass的材质思想，但遵守Android手势、返回、字体缩放和设备性能规则。
- 本地优先，不要求登录，不擅自加入云服务。
- 高保真视觉确认图位于 `../../design/android`；正式代码以确认图与 `docs/android` 共同作为依据。
- 不安装Android Studio；使用D盘中的JDK、Android命令行SDK和Gradle Wrapper构建。
- 产品负责人通过安装调试APK在自己的安卓手机上检查，不要求阅读代码。

## 未来可运行入口

完成首个可运行阶段后，项目将提供明确标注版本号的调试APK和面向新手的手机安装说明。不存在APK时必须直说，不用网页预览冒充真实Android App。
