# 黑马记账

黑马记账是一款本地优先的个人桌面收支记账软件。它不需要注册、不需要登录，也不依赖网络；账目默认只保存在你自己的电脑上。

## 已实现功能

- 快速新增、编辑和二次确认删除收入或支出
- 收支类型、金额、专属两级分类、日期、时间和备注
- 今日、本周、本月和全部账单，可筛选全部/收入/支出
- 今日与月度收支、月结余、分类占比、分类排行和每日趋势
- 18个一级分类、83个二级分类和独立彩色矢量图标
- UTF-8 CSV 导出
- 带版本和 SHA-256 校验的完整备份与事务恢复
- 浅色、深色、跟随系统三种外观
- Windows 安装版和便携版构建；共用代码支持 macOS

## 普通用户

请直接阅读 [用户指南](docs/USER_GUIDE.md)。Windows 构建完成后，安装包和便携版位于 `release` 目录。

如果你只是想知道项目里这些文件分别做什么、哪些不能删除，请看 [项目文件地图](docs/FILE_GUIDE.md)。

## 开发命令

```powershell
npm install
npm run dev
```

常用检查：

```powershell
npm run typecheck
npm run lint
npm test
npm run test:e2e
npm run build
npm run dist:win
```

完整环境、Windows/macOS 打包方法见 [构建说明](docs/BUILD.md)，产品和架构说明见 [产品文档](docs/PRODUCT.md) 与 [架构说明](docs/ARCHITECTURE.md)。
