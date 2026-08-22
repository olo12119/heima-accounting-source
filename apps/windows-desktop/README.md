# 黑马记账Windows桌面版

这里是已经可运行的1.5.0 Electron桌面工程，包含源码、自动测试、开发配置和本机预览成品。

普通用户不要在此目录寻找EXE，请回到总项目根目录，双击 `00-打开Windows桌面版-1.5.0.cmd`。

开发命令必须在本目录中执行：

```powershell
npm.cmd run dev
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run test:e2e
npm.cmd run build
```

Android版是另一个独立工程，不要把Kotlin或Gradle文件放进这里。
