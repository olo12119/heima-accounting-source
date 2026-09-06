package com.heima.accounting.designsystem

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

val LocalHeimaBackdrop: ProvidableCompositionLocal<Backdrop?> =
    staticCompositionLocalOf { null }

/**
 * 滚动感知（三期 3.1）：由各 LazyColumn 的 isScrollInProgress 经 derivedStateOf 驱动。
 * 默认 false = 现状静止行为；滚动中 GlassSurface 强制走静态渐变底色并跳过 rim 高光，
 * 图表生长动画暂停、停止后续播。滚动状态只能经这里读取，禁止各组件私自持有 LazyListState 判断。
 */
val LocalHeimaScrolling: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { false }

/**
 * 四期玻璃质感 helper（§3.4）：把 rim / sheen / 表面渐变收敛为唯一真相，
 * 页面禁止硬编码 alpha 值。浅深两档都写在这里。
 */

/** 玻璃表面渐变（backdrop 实时采样路径的 onDrawSurface 填充）。 */
internal fun glassSurfaceFill(
    palette: HeimaPalette,
    dark: Boolean,
    width: Float,
    height: Float,
): Brush = Brush.linearGradient(
    colors = if (dark) {
        listOf(
            palette.surface.copy(alpha = .34f),
            palette.brandSoft.copy(alpha = .18f),
            palette.surface.copy(alpha = .24f),
        )
    } else {
        listOf(
            palette.surface.copy(alpha = .22f),
            palette.brandSoft.copy(alpha = .16f),
            palette.surface.copy(alpha = .14f),
        )
    },
    start = Offset.Zero,
    end = Offset(width, height),
)

/** 玻璃 rim 渐变描边（顶部高光 → 侧边微光 → 品牌色 → 透明）。 */
internal fun glassRimBrush(
    palette: HeimaPalette,
    dark: Boolean,
    width: Float,
    height: Float,
): Brush = Brush.linearGradient(
    colors = listOf(
        palette.glassHighlight.copy(alpha = if (dark) 0.34f else 0.72f),
        palette.glassHighlight.copy(alpha = if (dark) 0.22f else 0.40f),
        palette.brand.copy(alpha = if (dark) 0.18f else 0.16f),
        Color.Transparent,
    ),
    start = Offset.Zero,
    end = Offset(width, height),
)

/** 玻璃内高光 sheen（左上角柔光，深色已由 .045 → .07 提亮）。 */
internal fun glassSheenBrush(
    palette: HeimaPalette,
    material: HeimaMotion,
    quality: VisualQuality,
    width: Float,
    height: Float,
): Brush = Brush.radialGradient(
    colors = listOf(
        Color.White.copy(
            alpha = when {
                !material.liquidGlassEnabled -> 0f
                material.darkTheme -> 0.07f
                quality == VisualQuality.POWER_SAVER -> 0.12f
                else -> 0.32f
            },
        ),
        Color.Transparent,
    ),
    center = Offset(width * 0.20f, height * 0.02f),
    radius = maxOf(width, height) * 0.82f,
)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 14.dp,
    backdropBlur: Boolean = false,
    role: HeimaSurfaceRole = HeimaSurfaceRole.METRIC,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = HeimaTheme.palette
    val quality = HeimaTheme.motion.quality
    val material = HeimaTheme.motion
    val backdrop = LocalHeimaBackdrop.current
    val scrolling = LocalHeimaScrolling.current
    val shape = RoundedCornerShape(cornerRadius)
    val spec = HeimaMaterialSystem.spec(role, backdropBlur)
    // P1：滚动中禁实时背景模糊，落到下方静态渐变路径（同一套 Token 与透明度公式，观感对齐）。
    val blurEnabled = spec.backdropBlur && backdrop != null && material.expensiveGlassEnabled &&
        Build.VERSION.SDK_INT >= 33 && !scrolling
    val surfaceAlpha = spec.surfaceAlpha

    val opticalModifier = when {
        !material.liquidGlassEnabled -> Modifier.background(palette.solidSurface(role))
        blurEnabled -> {
        Modifier.drawBackdrop(
            backdrop = requireNotNull(backdrop),
            shape = { shape },
            effects = {
                colorControls(saturation = 1.8f)
                blur(spec.blurRadius.toPx())
                lens(
                    refractionHeight = spec.refractionHeight.toPx(),
                    refractionAmount = spec.refractionAmount.toPx(),
                    chromaticAberration = false,
                )
            },
            highlight = { Highlight.Default.copy(alpha = spec.highlightAlpha) },
            shadow = { Shadow(alpha = if (material.darkTheme) .24f else .30f) },
            innerShadow = { InnerShadow(radius = 5.dp, alpha = spec.innerShadowAlpha) },
            onDrawSurface = {
                drawRect(
                    brush = glassSurfaceFill(palette, material.darkTheme, size.width, size.height),
                )
            },
        )
        }
        else -> Modifier.background(
            brush = Brush.verticalGradient(
                listOf(
                    palette.glassTop.copy(alpha = surfaceAlpha),
                    palette.glassBottom.copy(alpha = surfaceAlpha * .82f),
                ),
            ),
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevation == 14.dp) spec.shadowLevel.elevation() else elevation,
                shape = shape,
                ambientColor = palette.glassShadow.copy(alpha = if (material.darkTheme) .48f else .22f),
                spotColor = palette.glassShadow,
            )
            .clip(shape)
            .then(opticalModifier)
            .border(
                BorderStroke(
                    1.dp,
                    if (material.liquidGlassEnabled) palette.glassOutline else palette.outline.copy(alpha = .56f),
                ),
                shape,
            )
            .drawWithCache {
                val radiusPx = cornerRadius.toPx()
                val rim = glassRimBrush(palette, material.darkTheme, size.width, size.height)
                val sheen = glassSheenBrush(palette, material, quality, size.width, size.height)
                onDrawWithContent {
                    // The sheen belongs behind text and icons. Drawing it above the content
                    // was the root cause of washed-out labels in the old dark theme.
                    drawRoundRect(
                        brush = sheen,
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                    )
                    drawContent()
                    // P3：滚动中跳过 rim 渐变描边与顶边高光（2 次 stroke 采样省掉）；
                    // sheen（1 次 fill）便宜且消失肉眼可见，保留。
                    if (material.liquidGlassEnabled && material.expensiveGlassEnabled && !scrolling) {
                        drawRoundRect(
                            brush = rim,
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                            style = Stroke(width = 1.4.dp.toPx()),
                        )
                        drawLine(
                            color = palette.glassHighlight.copy(alpha = if (material.darkTheme) 0.50f else 0.55f),
                            start = Offset(radiusPx * 0.72f, 1.4.dp.toPx()),
                            end = Offset(size.width - radiusPx * 0.72f, 1.4.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx(),
                        )
                    }
                }
            },
        content = content,
    )
}

@Composable
fun PressableGlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    backdropBlur: Boolean = false,
    role: HeimaSurfaceRole = HeimaSurfaceRole.INTERACTIVE,
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = HeimaTheme.motion
    val palette = HeimaTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var pressPosition by remember { mutableStateOf(Offset.Unspecified) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) pressPosition = interaction.pressPosition
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (isPressed && !motion.reduceMotion) 1f else 0f,
        animationSpec = HeimaMotionTokens.responsive(motion.reduceMotion),
        label = "glass_press_progress",
    )

    GlassSurface(
        modifier = modifier
            .graphicsLayer {
                val scale = 1f - progress * 0.032f
                scaleX = scale
                scaleY = scale
                translationY = progress * 1.5.dp.toPx()
            }
            .drawWithCache {
                val center = if (pressPosition != Offset.Unspecified) {
                    pressPosition
                } else {
                    Offset(size.width / 2f, size.height / 2f)
                }
                val highlight = Brush.radialGradient(
                    colors = listOf(
                        palette.glassHighlight.copy(alpha = progress * if (motion.darkTheme) .10f else .28f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.maxDimension * .72f,
                )
                onDrawWithContent {
                    drawContent()
                    if (progress > 0f) drawRect(highlight)
                }
            }
            .then(
                Modifier.noRippleClick(
                    interactionSource = interactionSource,
                    onClick = onClick,
                ),
            ),
        cornerRadius = cornerRadius,
        elevation = if (backdropBlur) 14.dp else 5.dp,
        backdropBlur = backdropBlur,
        role = role,
        content = content,
    )
}

private fun Modifier.noRippleClick(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    onClick = onClick,
)

@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    val material = HeimaTheme.motion
    val decorationsEnabled = material.decorationsEnabled

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.background, palette.backgroundSecondary, palette.background),
                ),
            )
            .drawWithCache {
                val firstRadius = size.minDimension * 0.66f
                val secondRadius = size.minDimension * 0.54f
                val thirdRadius = size.minDimension * 0.42f
                val fourthRadius = size.minDimension * 0.52f
                val fifthRadius = size.minDimension * 0.70f
                val ribbon = Path().apply {
                    moveTo(-size.width * 0.12f, size.height * 0.32f)
                    cubicTo(
                        size.width * 0.22f,
                        size.height * 0.20f,
                        size.width * 0.62f,
                        size.height * 0.48f,
                        size.width * 1.10f,
                        size.height * 0.26f,
                    )
                }
                onDrawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.ambientOne.copy(alpha = when {
                                    material.darkTheme && decorationsEnabled -> 0.40f
                                    decorationsEnabled -> 0.85f
                                    material.darkTheme -> 0.18f
                                    else -> 0.30f
                                }),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.86f, size.height * 0.04f),
                            radius = firstRadius,
                        ),
                        radius = firstRadius,
                        center = Offset(size.width * 0.86f, size.height * 0.04f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.ambientTwo.copy(alpha = when {
                                    material.darkTheme && decorationsEnabled -> 0.32f
                                    decorationsEnabled -> 0.60f
                                    material.darkTheme -> 0.14f
                                    else -> 0.24f
                                }),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.04f, size.height * 0.72f),
                            radius = secondRadius,
                        ),
                        radius = secondRadius,
                        center = Offset(size.width * 0.04f, size.height * 0.72f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.brandSoft.copy(alpha = when {
                                    material.darkTheme && decorationsEnabled -> 0.22f
                                    decorationsEnabled -> 0.45f
                                    material.darkTheme -> 0.10f
                                    else -> 0.18f
                                }),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.78f, size.height * 0.60f),
                            radius = thirdRadius,
                        ),
                        radius = thirdRadius,
                        center = Offset(size.width * 0.78f, size.height * 0.60f),
                    )
                    // 四期新增色斑 4（accent，左上，更大更亮）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = when {
                                    material.darkTheme && decorationsEnabled -> 0.28f
                                    decorationsEnabled -> 0.50f
                                    material.darkTheme -> 0.12f
                                    else -> 0.20f
                                }),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.10f),
                            radius = fourthRadius,
                        ),
                        radius = fourthRadius,
                        center = Offset(size.width * 0.12f, size.height * 0.10f),
                    )
                    // 四期新增色斑 5（brand，右下，面积最大）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.brand.copy(alpha = when {
                                    material.darkTheme && decorationsEnabled -> 0.20f
                                    decorationsEnabled -> 0.38f
                                    material.darkTheme -> 0.09f
                                    else -> 0.15f
                                }),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.92f, size.height * 0.82f),
                            radius = fifthRadius,
                        ),
                        radius = fifthRadius,
                        center = Offset(size.width * 0.92f, size.height * 0.82f),
                    )
                    if (decorationsEnabled) {
                        drawPath(
                            path = ribbon,
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = if (material.darkTheme) 0.14f else 0.45f), Color.Transparent),
                            ),
                            style = Stroke(width = 30.dp.toPx()),
                        )
                    }
                }
            },
    )
}
