# Android Liquid Glass 研究与最终使用记录

## 最终运行依赖

### Kyant0/AndroidLiquidGlass

- 仓库：https://github.com/Kyant0/AndroidLiquidGlass
- 固定研究提交：`b18eb0ff12c616546a68c72e7d0097f1ab286c87`
- 最终依赖：`io.github.kyant0:backdrop:2.0.0`
- 许可证：Apache-2.0
- 实际用途：Backdrop、Lens、Blur、Vibrancy、Highlight、Shadow、InnerShadow。
- 重点阅读：README、LiquidButton、LiquidToggle、LiquidSlider、LiquidBottomTabs、Backdrop、lens/blur/vibrancy/highlight/shadow 和拖动/Spring 实现。

## 仅研究，未复制或链接

### QmDeve/AndroidLiquidGlassView

- 仓库：https://github.com/QmDeve/AndroidLiquidGlassView
- 固定研究提交：`3c7b2d046726afdc9263b56cb8224e029ff1f924`
- 许可证：MIT
- 重点阅读：README、core、LiquidGlassView、Config、TouchEffectActivity、ElasticLiquidGlassViewActivity、Draggable、Blur/Tint/Refraction/Dispersion Shader 和背景绑定。
- 最终状态：没有复制源文件、没有 Gradle 依赖；只吸收参数分层、缓存和兼容降级思路。

正式第三方通知位于根目录 `THIRD_PARTY_NOTICES.md`，完整 Apache 许可证位于 `licenses/AndroidLiquidGlass-APACHE-2.0.txt`。
