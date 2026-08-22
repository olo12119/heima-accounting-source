# 黑马记账 Android App

## 当前状态

这里是未来Android原生App的独立区域。目前处于“设计文档阶段”，**尚未创建Kotlin、Gradle或Jetpack Compose工程，也没有APK**。

在产品负责人确认高保真UI原型以前，不在此目录开始正式编码。当前可运行成品仍是 `../windows-desktop` 中的Windows桌面版1.5.0。

## 已确定方向

- 只开发Android，不制作iOS应用。
- 使用Kotlin与Jetpack Compose原生开发。
- 借鉴iCost的信息组织、Apple精品应用的留白和层级、Liquid Glass的材质思想，但遵守Android手势、返回、字体缩放和设备性能规则。
- 本地优先，不要求登录，不擅自加入云服务。
- 第一阶段先完成 `docs/android` 中的设计文档；第二阶段制作高保真原型；原型确认后才开始正式App代码。

## 未来可运行入口

正式进入原型阶段后，项目根目录会生成唯一的 `00-打开Android原型.cmd`。进入真机测试阶段后，才会生成明确标注版本号的APK和安装说明。
