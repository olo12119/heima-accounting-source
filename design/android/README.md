# 黑马记账 Android 视觉原型

## 当前确认入口

请按下面顺序查看：

1. [首页与分类入口逻辑](01-home-and-category-flow.png)
2. [统计页](02-statistics.png)
3. [预算页](03-budget.png)
4. [我的、主题与字体](04-profile-and-settings.png)
5. [完整账单与账目详情](05-records-and-detail.png)
6. [隐私、备份与数据恢复](06-privacy-backup-recovery.png)

这些图片用于确认视觉方向和页面逻辑，不是可点击的 App，也不代表正式 Android 代码已经完成。

第一版主题正式缩减为Liquid Glass与自然治愈。Mechanical Fantasy和Cyber Finance不再进入正式Android产品范围。

分类交互遵循：启动直接进入首页且不弹窗；点击“记账”后才显示常用分类、全部分类和快速添加；完整分类整理位于“我的 → 记账设置 → 分类管理”。

确认图主要验证Android手势安全区、白底立体应用图标、收入与支出、精致3D分类图标、两套主题、主要页面和危险操作保护。

图片中的少量说明文字由生图模型生成，可能出现错字；正式 App 文字由 Android 代码排版，不使用图片中的文字。

## 阶段边界

- 当前完成：六张第一版高保真视觉确认图。
- 用户确认后：建立 Kotlin + Jetpack Compose Android 工程，并先实现 Design System、底部导航和记账入口。
- 暂不执行：真实数据库、Windows 工程删除、iOS 开发和正式安装包发布。
