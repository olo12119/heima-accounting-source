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
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

val LocalHeimaBackdrop: ProvidableCompositionLocal<Backdrop?> =
    staticCompositionLocalOf { null }

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
    val shape = RoundedCornerShape(cornerRadius)
    val spec = HeimaMaterialSystem.spec(role, backdropBlur)
    val blurEnabled = spec.backdropBlur && backdrop != null && material.expensiveGlassEnabled && Build.VERSION.SDK_INT >= 33
    val surfaceAlpha = spec.surfaceAlpha

    val opticalModifier = when {
        !material.liquidGlassEnabled -> Modifier.background(palette.solidSurface(role))
        blurEnabled -> {
        Modifier.drawBackdrop(
            backdrop = requireNotNull(backdrop),
            shape = { shape },
            effects = {
                vibrancy()
                blur(spec.blurRadius.toPx())
                lens(
                    refractionHeight = spec.refractionHeight.toPx(),
                    refractionAmount = spec.refractionAmount.toPx(),
                    chromaticAberration = false,
                )
            },
            highlight = { Highlight.Default.copy(alpha = spec.highlightAlpha) },
            shadow = { Shadow(alpha = if (material.darkTheme) .20f else .28f) },
            innerShadow = { InnerShadow(radius = 5.dp, alpha = spec.innerShadowAlpha) },
            onDrawSurface = {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = if (material.darkTheme) {
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
                        end = Offset(size.width, size.height),
                    ),
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
                ambientColor = palette.glassShadow.copy(alpha = if (material.darkTheme) .42f else .16f),
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
                val rim = Brush.linearGradient(
                    colors = listOf(
                        palette.glassHighlight.copy(alpha = if (material.darkTheme) 0.30f else 0.72f),
                        palette.glassHighlight.copy(alpha = if (material.darkTheme) 0.18f else 0.36f),
                        palette.brand.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                val sheen = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(
                            alpha = when {
                                !material.liquidGlassEnabled -> 0f
                                material.darkTheme -> 0.045f
                                quality == VisualQuality.POWER_SAVER -> 0.12f
                                else -> 0.32f
                            },
                        ),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.20f, size.height * 0.02f),
                    radius = size.maxDimension * 0.82f,
                )
                onDrawWithContent {
                    // The sheen belongs behind text and icons. Drawing it above the content
                    // was the root cause of washed-out labels in the old dark theme.
                    drawRoundRect(
                        brush = sheen,
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                    )
                    drawContent()
                    if (material.liquidGlassEnabled && material.expensiveGlassEnabled) {
                        drawRoundRect(
                            brush = rim,
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                            style = Stroke(width = 1.4.dp.toPx()),
                        )
                        drawLine(
                            color = palette.glassHighlight.copy(alpha = if (material.darkTheme) 0.30f else 0.38f),
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
                                    material.darkTheme && decorationsEnabled -> 0.24f
                                    decorationsEnabled -> 0.62f
                                    material.darkTheme -> 0.12f
                                    else -> 0.24f
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
                                    material.darkTheme && decorationsEnabled -> 0.18f
                                    decorationsEnabled -> 0.42f
                                    material.darkTheme -> 0.10f
                                    else -> 0.18f
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
                                    material.darkTheme && decorationsEnabled -> 0.15f
                                    decorationsEnabled -> 0.32f
                                    material.darkTheme -> 0.08f
                                    else -> 0.12f
                                }),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.78f, size.height * 0.60f),
                            radius = thirdRadius,
                        ),
                        radius = thirdRadius,
                        center = Offset(size.width * 0.78f, size.height * 0.60f),
                    )
                    if (decorationsEnabled) {
                        drawPath(
                            path = ribbon,
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = if (material.darkTheme) 0.07f else 0.25f), Color.Transparent),
                            ),
                            style = Stroke(width = 22.dp.toPx()),
                        )
                    }
                }
            },
    )
}
