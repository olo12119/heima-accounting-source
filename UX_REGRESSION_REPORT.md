# 黑马记账 Android 1.0.2 UX 回归报告

| 验收项 | 结果 | 说明 |
| --- | --- | --- |
| 1. Dark Mode 重构 | ✅ | 独立 Light/Dark Tokens；澄澈蓝与自然治愈分别适配，不使用全屏黑 Overlay |
| 2. Snackbar Undo | ✅ | 一次性事件，约 4 秒自动消失；撤销恢复；重组不重计时 |
| 3. Swipe Navigation | ✅ | 首页↔统计↔预算↔我的使用 `HorizontalPager`；记账不是 Pager 页面 |
| 4. Bottom Bar Drag | ✅ | Lens 跟手、跨页、吸附；经过记账不误开，释放在记账才打开 |
| 5. Switch 逻辑审计 | ✅ | Glass/Sound/Haptic 正向语义；Reduce Motion 负向文案但正向变量；单一状态源 |
| 6. Sound | ✅ / ⚠️ | SoundPool 加载和行为门测试通过；真实响度需真机验证 |
| 7. Haptic | ✅ / ⚠️ | 选择/吸附/确认调用和开关门通过；实际手感需真机验证 |
| 8. Custom Date | ✅ | 单日、区间、中文自有日历、修改/重置和 SQLite 范围查询完成 |
| 9. Many Categories | ✅ | Top 5 + 其他、3% 归并、稳定颜色、其他明细和扇区交互完成 |
| 10. 仍然存在的问题 | ⚠️ | 模拟器压力 Jank 尚高；真机电池、温升、120Hz、音效和触觉未验收 |

## 视觉矩阵

- ✅ 澄澈蓝：Light/Glass ON、Light/Glass OFF、Dark/Glass ON、Dark/Glass OFF。
- ✅ 自然治愈：Light/Dark 独立截图差异测试；Glass ON/OFF 使用同一降级规则。
- ✅ 首页、统计、预算、我的、设置、日期、快速记账、Snackbar、Dialog、Bottom Sheet 可读。
- ✅ Dark Glass 降低白雾和 Bloom；Glass OFF 保留一致布局和视觉性格。

## 手势与状态矩阵

- ✅ 慢拖、快速 Fling、拖动后吸附、连续切换均由 Pager/Lens 单一状态同步。
- ✅ 页内竖向列表、分类横向列表和 Bottom Sheet 不会被手写全局手势粗暴拦截。
- ✅ 所有新动画尊重 Reduce Motion。
- ✅ 删除提示、设置和自定义日期均不会因重组丢失或无限存在。

## 结论

代码、数据库、交互、截图和发布构建门禁已通过。剩余项不是已知功能错误，而是必须在用户真实 Android 手机上判断的硬件体验；报告中统一标为 `NEEDS REAL DEVICE VERIFICATION`。
