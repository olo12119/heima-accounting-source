# Android技术、数据迁移与工程边界

## 技术选择

只开发Android后，推荐Kotlin + Jetpack Compose，而不是SwiftUI或Flutter。原因是原生方案能直接使用Android返回手势、输入法、通知、生物识别、无障碍和性能工具；跨平台框架的主要收益在当前范围内不存在。

## 技术组成与通俗解释

| 技术 | 通俗解释 | 职责 |
| --- | --- | --- |
| Kotlin | Android官方主要编程语言 | 产品与业务逻辑 |
| Jetpack Compose | Android官方界面搭建工具 | 页面、主题、动画和自适应 |
| Room + SQLite | 手机中的本地电子账本 | 账目、分类、预算和模板 |
| ViewModel | 页面数据管理员 | 页面重建后保持状态 |
| Coroutines + Flow | 自动传送最新数据的管道 | 后台查询和界面同步更新 |
| Navigation Compose | App内部导航地图 | 页面、返回和深层链接 |
| DataStore | 小型设置保险盒 | 主题、字体和用户偏好 |
| WorkManager | 受系统管理的定时助手 | 周期账目到期提醒 |
| BiometricPrompt | Android生物识别入口 | 指纹或设备支持的人脸解锁 |
| Android Keystore | 系统安全柜 | 敏感校验材料 |
| Kotlin Serialization | 备份文件翻译器 | JSON导入、导出和校验 |
| Gradle | 自动装配工具 | 依赖、测试和APK构建 |

Material 3只作为Android行为、无障碍和基础控件参考，视觉表面由黑马记账Design System控制，不做默认Material模板拼装。

## 开发工具与验收方式

- 不安装Android Studio。
- JDK、Android命令行SDK、平台工具、构建工具、Gradle缓存和下载缓存优先放在 `D:\AndroidDev`。
- 不修改系统PATH；项目脚本使用明确路径调用工具。
- 只从官方来源下载并核对校验值，不登录Google账号，不使用云端构建服务。
- 开发者通过命令行执行编译、测试和APK打包。
- 产品负责人不需要阅读代码，唯一推荐验收入口是把调试APK复制到自己的安卓手机并安装体验。
- 页面截图和动效录屏只作辅助证据，不能替代真机APK。
- Windows可能在C盘保留少量系统临时文件、注册信息或快捷方式；大型SDK、构建缓存和模拟器数据不得默认放在C盘。

## 分层架构

```text
Compose页面
  → ViewModel页面状态
  → 业务用例
  → Repository数据入口
  → Room数据库 / 备份文件 / Android系统能力
```

- 页面不能直接执行SQL或读写文件。
- 业务规则不依赖某一个页面，便于测试和未来重用。
- 数据库与文件操作在后台线程执行。
- 页面只观察不可变状态，避免切页或旋转后数据混乱。

## 建议模块

```text
apps/android/
├─ app/                 启动、导航和依赖组合
├─ core/designsystem/   颜色、字体、材质和组件
├─ core/database/       Room数据库和迁移
├─ core/data/           Repository与备份
├─ core/domain/         金额、日期、统计和预算规则
├─ feature/home/        首页
├─ feature/records/     账单与日历
├─ feature/entry/       记账
├─ feature/statistics/  统计
├─ feature/budget/      预算与模板
├─ feature/settings/    设置、主题和字体
└─ benchmark/           性能与启动测试
```

正式编码前只保留规划，不提前生成这些模块。

## 数据模型原则

- 金额继续用整数分保存，12.50元存为1250。
- 日期和时间按用户本地时区解释。
- 收入、支出、退款和报销使用统一账目模型。
- 二级分类必须属于选中的一级分类，并匹配收支类型。
- 删除有历史账目的自定义分类时改为停用。
- 统计排除标记和退款冲减规则与桌面版一致。
- 数据库迁移必须逐版本执行并有回滚测试。

## Android本地数据

- 数据库保存在App私有目录，其他普通App不能直接读取。
- 用户设置使用DataStore；账目使用Room/SQLite。
- 不在公共下载目录直接运行数据库。
- 导出备份时通过Android系统文件选择器让用户选择位置。
- 数据库无法打开或完整性检查失败时保留原文件，进入恢复界面。

## Windows到Android迁移

不直接复制Windows SQLite文件，使用完整备份作为安全桥梁：

1. Windows导出 `.heima-backup.json`。
2. Android通过系统文件选择器读取。
3. 校验备份版本、SHA-256、金额、日期和分类关系。
4. 自动导出Android当前账本作为导入前备份。
5. 在单一数据库事务中导入。
6. 失败时回滚，不覆盖手机原账本。
7. 导入后比较账目笔数、收入合计、支出合计、分类和模板数量。

共享规则保存在 `shared/contracts`，只描述数据合同，不试图让Kotlin直接运行TypeScript代码。

## 隐私与安全

- 第一版不要求账号，不发送账目到网络。
- 生物识别只负责解锁入口；不能把指纹本身保存进App。
- 隐私密码不保存明文，校验材料放入Android Keystore保护范围。
- 截图遮挡、后台任务预览保护和导出确认在原型阶段定义。
- 恢复和删除是高风险动作，必须明确说明影响并二次确认。

## Android版本与效果降级

- 建议最低支持Android 10，正式决定前核对用户主力手机版本。
- Android 12及以上提供完整玻璃模糊和更丰富材质。
- Android 10至11使用静态或较轻的玻璃替代。
- 系统“减少动画”和App节能模式可以进一步降级。

## 测试策略

- 单元测试：金额、日期、统计、预算、备份校验和迁移。
- Room集成测试：迁移、增删改查、事务回滚和重启持久化。
- Compose交互测试：记账、主题、字体、搜索、删除和恢复确认。
- 截图基准测试：两套主题、深浅模式、大字体和降低透明度。
- 真机测试：至少一台用户主力Android手机和一台中等性能设备。
- 性能测试：启动、首页滚动、记账展开、图表和主题切换。
- APK冒烟测试：卸载、首次安装、升级安装、备份导入和重启。

## Windows与Android工程边界

- `apps/windows-desktop`：冻结的Electron桌面版，只修复数据安全或迁移相关问题。
- `apps/android`：未来Android原生工程，原型确认前不写正式代码。
- `docs/android`：Android产品与设计规范。
- `design/android`：高保真原型与动效演示。
- `shared/contracts`：跨版本备份格式和固定业务规则。

Android成功导入桌面备份并稳定使用前，不删除Windows工程。
