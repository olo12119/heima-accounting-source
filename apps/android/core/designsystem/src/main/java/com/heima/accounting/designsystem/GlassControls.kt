package com.heima.accounting.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** A single sliding selection surface shared by filters and two-way choices. */
@Composable
fun <T> GlassSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = "选项",
    height: Dp = 48.dp,
) {
    require(options.isNotEmpty())
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val shape = RoundedCornerShape(18.dp)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(palette.surfaceMuted.copy(alpha = if (motion.darkTheme) .78f else .66f))
            .border(1.dp, palette.glassStroke.copy(alpha = if (motion.darkTheme) .24f else .60f), shape)
            .padding(4.dp),
    ) {
        val slotWidth = maxWidth / options.size
        val targetX = slotWidth * selectedIndex
        val lensX by animateDpAsState(
            targetValue = targetX,
            animationSpec = if (motion.reduceMotion) tween(80) else spring(dampingRatio = .88f, stiffness = 520f),
            label = "glass_segment_lens",
        )
        val lensShape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .offset { IntOffset(lensX.roundToPx(), 0) }
                .width(slotWidth)
                .fillMaxHeight()
                .clip(lensShape)
                .background(
                    if (motion.liquidGlassEnabled) {
                        Brush.horizontalGradient(
                            listOf(
                                palette.glassHighlight.copy(alpha = if (motion.darkTheme) .12f else .58f),
                                palette.brandSoft.copy(alpha = if (motion.darkTheme) .38f else .82f),
                                palette.glassTop.copy(alpha = if (motion.darkTheme) .18f else .70f),
                            ),
                        )
                    } else {
                        Brush.horizontalGradient(listOf(palette.brandSoft, palette.brandSoft))
                    },
                )
                .border(1.dp, palette.glassStroke.copy(alpha = if (motion.darkTheme) .32f else .80f), lensShape),
        )

        Row(Modifier.fillMaxSize()) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val interaction = remember(value) { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed && !motion.reduceMotion) .97f else 1f,
                    animationSpec = if (motion.reduceMotion) tween(50) else spring(dampingRatio = .88f, stiffness = 760f),
                    label = "glass_segment_press",
                )
                val color by animateColorAsState(
                    if (isSelected) palette.brand else palette.textSecondary,
                    animationSpec = tween(if (motion.reduceMotion) 70 else 170),
                    label = "glass_segment_text",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                        .semantics {
                            contentDescription = "$accessibilityLabel：$label"
                            stateDescription = if (isSelected) "已选中" else "未选中"
                            this.selected = isSelected
                        }
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                        ) { onSelected(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * A semantic switch whose right/accent state always means true. Keeping the mapping
 * here prevents individual settings rows from accidentally reversing their logic.
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val thumbX by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        animationSpec = if (motion.reduceMotion) tween(70) else spring(dampingRatio = .88f, stiffness = 620f),
        label = "glass_toggle_thumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.brand else palette.surfaceMuted,
        animationSpec = tween(if (motion.reduceMotion) 70 else 160),
        label = "glass_toggle_track",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .width(52.dp)
            .height(30.dp)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (checked) "已开启" else "已关闭"
            }
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = onCheckedChange,
            )
            .clip(CircleShape)
            .background(trackColor.copy(alpha = if (checked) .96f else if (motion.darkTheme) .82f else .92f))
            .border(1.dp, if (checked) palette.accent.copy(alpha = .55f) else palette.divider, CircleShape),
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), 3.dp.roundToPx()) }
                .width(25.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (motion.darkTheme) {
                            listOf(palette.textSecondary, palette.surfaceElevated)
                        } else {
                            listOf(Color.White, if (checked) Color(0xFFEAF2FF) else palette.glassBottom)
                        },
                    ),
                )
                .border(
                    1.dp,
                    if (motion.darkTheme) palette.glassOutline.copy(alpha = .42f) else Color.White.copy(alpha = .72f),
                    CircleShape,
                ),
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !motion.reduceMotion) .97f else 1f,
        animationSpec = if (motion.reduceMotion) tween(50) else spring(dampingRatio = .88f, stiffness = 760f),
        label = "glass_chip_press",
    )
    Box(
        modifier
            .semantics {
                contentDescription = "选择${text}细分"
                stateDescription = if (selected) "已选中" else "未选中"
            }
            .clip(shape)
            .background(
                if (selected) palette.brandSoft.copy(alpha = if (motion.darkTheme) .72f else .90f)
                else palette.surfaceMuted.copy(alpha = if (motion.darkTheme) .74f else .66f),
            )
            .border(1.dp, if (selected) palette.brand.copy(.46f) else palette.divider, shape)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) palette.brand else palette.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** A consistent 54dp input/control surface for compact form rows. */
@Composable
fun GlassFieldSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val shape = RoundedCornerShape(16.dp)
    val clickModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Box(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(palette.surfaceMuted.copy(alpha = if (motion.darkTheme) .76f else .62f))
            .border(1.dp, palette.glassStroke.copy(alpha = if (motion.darkTheme) .27f else .66f), shape)
            .then(clickModifier)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
        content = content,
    )
}

@Composable
fun GlassModalScrim(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val scrim = if (motion.darkTheme) {
        Color.Black.copy(alpha = .54f)
    } else {
        palette.background.copy(alpha = .74f)
    }
    Box(
        modifier
            .fillMaxSize()
            .background(scrim)
            .clickable(onClick = onDismiss),
    )
}
