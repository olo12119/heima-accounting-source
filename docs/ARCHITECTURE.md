# 黑马记账架构说明（Android）

> Android 正式版采用 `app + core:designsystem + core:domain + core:data + core:database` 的模块结构。界面、业务规则与 SQLite 保持分层；详细结论见 [最终发布报告](reports/FINAL_RELEASE_REPORT.md)。

## 用普通话解释 App 的组成

可以把 App 理解为三层：

1. **你看到的界面**：首页、统计、记账面板、预算、我的、账单、分类管理等页面，位于 `apps/android/app`，用 Jetpack Compose 编写。
2. **业务规则**：金额怎么算、分类怎么约束、退款怎么抵扣、预算怎么提醒，位于 `apps/android/core/domain`。
3. **本地账本与数据**：SQLite 文件保存分类、收支、预算和设置，读写封装在 `apps/android/core/data` 与 `apps/android/core/database`。关闭 App 后数据仍在。

```text
用户点击保存
  → Compose 界面先检查
  → domain 层校验业务规则
  → data/database 层用事务写入 SQLite
  → 界面重新读取首页、账单和统计
```

## 主要模块

- `apps/android/app`：应用入口、各页面 UI、资源和应用级组装。
- `apps/android/core/designsystem`：统一材质层级、主题、字体、动效 Token 和组件。
- `apps/android/core/domain`：纯业务规则，不直接碰数据库。
- `apps/android/core/data`：Repository，把数据库操作暴露给上层。
- `apps/android/core/database`：SQLite 建表、迁移、查询和事务。

## 数据放在哪里

账目保存在手机 App 的私有数据库中。SQLite 可能在数据库旁短暂保留 `-wal` 和 `-shm` 文件，它们是安全写入的一部分，不应在 App 运行时单独删除或复制。

## 数据安全策略

- 金额以整数“分”保存，12.50 元保存为 1250 分，避免小数误差。
- 二级分类必须属于选定的一级分类，并匹配收入或支出类型。
- 退款/报销必须关联有效原支出，统计时抵扣原支出和原分类，不虚增普通收入。
- 隐私密码保存随机盐和 `scrypt` 校验值，不保存明文；它防随手查看，不等于数据库加密。
- 有历史账目引用的自定义分类只停用，不物理删除。
- 恢复备份前先保存当前账本；整个替换要么全部成功，要么完全回滚。
- CSV 不适合无损表达退款与原支出关系，此类数据应用完整备份迁移。
- 数据库检查失败时保留原文件，不用空数据库覆盖。

## 界面与动效

- 底栏为固定五个入口（首页、统计、记账、预算、我的），中央“记账”是独立主操作。
- 统一材质层级 Hero / Metric / Insight / Chart / List / Interactive / Overlay；动效时长、Spring、Glass 质量和性能回退由公共设计系统管理。
- `prefers-reduced-motion` 会减少位移和弹性效果，保证易眩晕用户可用。
