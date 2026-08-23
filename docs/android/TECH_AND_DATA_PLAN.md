# Android 1.0 技术与数据方案

## 已采用技术

| 技术 | 通俗解释 | 实际职责 |
| --- | --- | --- |
| Kotlin | Android 原生编程语言 | 业务、状态和数据规则 |
| Jetpack Compose | 用代码搭建手机界面 | 页面、主题、Glass 和动画 |
| SQLiteOpenHelper | 手机里的本地电子账本 | 迁移、分类、账单和预算 |
| SettingsRepository + SharedPreferences | 本机体验设置柜 | 主题、Glass、音效、触觉、动效和金额隐私的单一状态源 |
| ViewModel + Flow | 页面数据管理员 | 后台读写后把最新数据送回页面 |
| Coroutines | 不堵住界面的任务管道 | 数据库、导入和导出 |
| Kyant Backdrop | 局部背景采样和玻璃 Lens | 底栏与重要交互表面 |
| Gradle | 自动装配与检查工具 | 依赖、测试、压缩和 APK 构建 |

没有使用 Room、DataStore、WorkManager、BiometricPrompt 或云服务；文档不再把未实现的计划当作已完成能力。

## 分层结构

```text
app
├─ UI / ViewModel / InteractionFeedback
├─ core:designsystem  主题、Glass、Motion
├─ core:domain        模型、金额规则、默认分类
├─ core:data          Repository、设置状态、CSV、备份校验
└─ core:database      SQLite、Migration、事务、quick_check
```

页面不能直接拼 SQL。金额、分类关系和备份结构分别在 Domain/Data/Database 层再次校验。

## 数据规则

- 金额以整数分保存，`12.50` 保存为 `1250`，不持久化浮点数。
- 账目记录收支类型、一级分类、可选二级分类、备注、发生时间和创建时间。
- 二级分类必须属于正确的一级分类并匹配收支类型。
- 有历史账目的自定义分类只停用，不破坏旧账单关系。
- 预算按月份保存。
- 体验设置采用正向语义并由 `SettingsRepository` 单向流向 ViewModel 和 UI。
- 默认只建立分类和设置，不建立虚假账单。

## 数据库安全

- 数据库位于 Android App 私有目录。
- 启用外键、WAL、索引和 `quick_check`。
- 所有完整替换都在一个事务中执行；错误自动回滚。
- 数据库无法打开或完整性失败时进入保护界面，不自动删除、覆盖或重建原文件。
- 后续结构变化必须提高数据库版本并编写 Migration，不能清空用户数据解决升级问题。

## 导出与恢复

- CSV：UTF-8 BOM、中文表头、人民币元和标准转义，适合 Excel 查看。
- 完整备份：版本化 JSON、导出时间、分类/账单/预算和 SHA-256。
- 恢复：先校验结构、金额和分类关系，再生成恢复前安全副本，最后事务替换。
- 当前 Android 备份格式以 Android 1.0 数据模型为准；没有声称可无损导入 Windows 的高级退款/模板数据。

## 兼容与性能

- Android 13 及以上且性能允许时启用完整 Backdrop；旧系统使用视觉一致的半透明 Surface。
- 系统省电模式自动降低 Blur/Lens 成本。
- Liquid Glass 关闭后只改变材质成本，不改变页面结构与功能。
- 无后台服务、网络轮询、常驻 Timer 或无限动画。

详细验证见根目录 `TEST_REPORT.md` 和 `PERFORMANCE_REPORT.md`。
