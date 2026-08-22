package com.heima.accounting.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = HeimaTheme.palette
    val quality = HeimaTheme.motion.quality
    val shape = RoundedCornerShape(cornerRadius)
    val lowerAlpha = if (quality == VisualQuality.POWER_SAVER) 0.96f else 1f

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = palette.brand.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.glassTop.copy(alpha = lowerAlpha),
                        palette.glassBottom.copy(alpha = lowerAlpha),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, palette.glassStroke), shape)
            .drawWithCache {
                val highlight = Brush.linearGradient(
                    colors = listOf(
                        palette.glassHighlight.copy(alpha = 0.42f),
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.72f, size.height * 0.82f),
                )
                onDrawWithContent {
                    drawContent()
                    if (quality != VisualQuality.POWER_SAVER) {
                        drawRect(
                            brush = highlight,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height * 0.44f),
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
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = HeimaTheme.motion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !motion.reduceMotion) 0.972f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "glass_press_scale",
    )

    GlassSurface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                Modifier.noRippleClick(
                    interactionSource = interactionSource,
                    onClick = onClick,
                ),
            ),
        cornerRadius = cornerRadius,
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
    val decorationsEnabled = HeimaTheme.motion.decorationsEnabled

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .drawWithCache {
                val firstRadius = size.minDimension * 0.58f
                val secondRadius = size.minDimension * 0.44f
                onDrawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.ambientOne.copy(alpha = if (decorationsEnabled) 0.42f else 0.18f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.82f, size.height * 0.04f),
                            radius = firstRadius,
                        ),
                        radius = firstRadius,
                        center = Offset(size.width * 0.82f, size.height * 0.04f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.ambientTwo.copy(alpha = if (decorationsEnabled) 0.28f else 0.12f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.08f, size.height * 0.70f),
                            radius = secondRadius,
                        ),
                        radius = secondRadius,
                        center = Offset(size.width * 0.08f, size.height * 0.70f),
                    )
                }
            },
    )
}
