# 黑马记账架构说明

> Android 1.0.1 的正式结构为 `app + core:designsystem + core:domain + core:data + core:database`，详细结论见 [最终发布报告](../FINAL_RELEASE_REPORT.md)。Windows Electron 工程位于 `apps/windows-desktop`；下文继续解释独立的桌面版架构。

## 用普通话解释App的组成

可以把Windows版理解为四层：

1. **你看到的界面**：首页、记账表单、账单/日历、统计、分类管理、预算与计划、设置和密码锁，位于 `apps/windows-desktop/src/renderer`。
2. **安全传话层**：界面不能直接碰电脑文件，只能通过 `apps/windows-desktop/src/preload` 提出固定请求。
3. **应用管家**：`apps/windows-desktop/src/main` 再次检查数据、读写数据库、弹出文件选择框，并创建桌面窗口。
4. **本地账本**：SQLite文件保存分类、收支、预算、模板和设置。关闭App后数据仍在。

```text
用户点击保存
  → React表单先检查
  → preload只传递允许的字段
  → Electron主进程用Zod再检查
  → SQLite事务写入
  → 界面重新读取首页、账单和统计
```

## 数据放在哪里

- Windows：`%APPDATA%\HeimaAccounting\data\heima-accounting.sqlite3`
- macOS：`~/Library/Application Support/HeimaAccounting/data/heima-accounting.sqlite3`

SQLite可能在数据库旁短暂保留 `-wal` 和 `-shm` 文件，它们是安全写入的一部分，不应在App运行时单独删除或复制。

## 主要模块

- `apps/windows-desktop/src/shared`：数据类型、分类、金额、日期和校验规则。
- `apps/windows-desktop/src/main/database.ts`：建表、迁移、分类初始化、收支查询、退款抵扣、预算、模板、密码锁与统计。
- `apps/windows-desktop/src/main/data-formats.ts`：CSV导入/导出、备份和稳定校验值。
- `apps/windows-desktop/src/main/portability.ts`：文件选择、原子写入、安全备份与恢复。
- `apps/windows-desktop/src/main/ipc.ts`：允许的请求清单和来源验证。
- `apps/windows-desktop/src/renderer/src/components`：导航、记账表单、确认框和主题选择器等可复用界面。
- `apps/windows-desktop/src/renderer/src/pages`：首页、账单、统计、分类、计划和数据设置页。
- `apps/windows-desktop/src/renderer/public/logo-app-v3.png`：白底账本、笔与人民币徽章的应用图标。
- `apps/windows-desktop/src/renderer/public/category-3d-atlas-v2.png`：28格透明背景3D分类图标图集。
- `apps/windows-desktop/tests`：纯逻辑、SQLite、React交互和真实Electron操作测试。

## 界面与动效

- Windows桌面使用侧边导航；窄窗口使用底部导航。这只是桌面响应式布局，不等于Android代码可直接复用。
- 桌面版保留黑马经典、暖阳活力、云朵治愈和深海专注主题；Android将按新Design System重做四套完整主题。
- `motion`负责页面、弹窗、金额、列表和主题过渡；CSS负责微动效；Recharts负责图表。
- `prefers-reduced-motion` 会减少位移和弹性效果，保证易眩晕用户可用。

## 数据安全策略

- 金额以整数“分”保存，12.50元保存为1250分，避免小数误差。
- 二级分类必须属于选定的一级分类，并匹配收入或支出类型。
- 退款/报销必须关联有效原支出，统计时抵扣原支出和原分类，不虚增普通收入。
- 隐私密码保存随机盐和 `scrypt` 校验值，不保存明文；它防随手查看，不等于数据库加密。
- 有历史账目引用的自定义分类只停用，不物理删除。
- 恢复备份前先保存当前账本；整个替换要么全部成功，要么完全回滚。
- CSV不适合无损表达退款与原支出关系，此类数据应用完整备份迁移。
- 数据库检查失败时保留原文件，不用空数据库覆盖。
