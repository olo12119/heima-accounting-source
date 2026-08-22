package com.heima.accounting.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme

@Composable
fun HeimaBottomBar(
    selected: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    onRecord: () -> Unit,
    recordPanelVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .padding(horizontal = 14.dp),
    ) {
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(76.dp),
            cornerRadius = 31.dp,
            elevation = 20.dp,
            backdropBlur = true,
        ) {
            BoxWithConstraints(Modifier.matchParentSize()) {
                val slotWidth = maxWidth / AppDestination.entries.size
                val indicatorWidth = 54.dp
                val targetX = slotWidth * selected.ordinal + (slotWidth - indicatorWidth) / 2
                val indicatorX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = if (motion.reduceMotion) {
                        spring(stiffness = Spring.StiffnessHigh)
                    } else {
                        spring(
                            dampingRatio = 0.90f,
                            stiffness = Spring.StiffnessMedium,
                        )
                    },
                    label = "bottom_bar_glass_lens",
                )

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(indicatorX.roundToPx(), 17.dp.roundToPx())
                        }
                        .width(indicatorWidth)
                        .height(38.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    palette.brandSoft.copy(alpha = 0.92f),
                                    palette.glassHighlight.copy(alpha = 0.78f),
                                ),
                            ),
                            RoundedCornerShape(20.dp),
                        ),
                )

                Row(
                    modifier = Modifier.matchParentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppDestination.entries.forEach { destination ->
                        if (destination == AppDestination.RECORD) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            BottomBarItem(
                                destination = destination,
                                selected = selected == destination,
                                onClick = { onDestinationSelected(destination) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        RecordAction(
            expanded = recordPanelVisible,
            reduceMotion = motion.reduceMotion,
            onClick = onRecord,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun BottomBarItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val color by animateColorAsState(
        targetValue = if (selected) palette.brand else palette.textTertiary,
        animationSpec = spring(
            dampingRatio = 0.90f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "nav_item_color",
    )
    val selection by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (motion.reduceMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessMedium)
        },
        label = "nav_item_selection",
    )

    Column(
        modifier = modifier
            .height(76.dp)
            .semantics { contentDescription = destination.accessibilityLabel }
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HeimaGlyph(
            destination = destination,
            color = color,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = 1f + selection * 0.08f
                    scaleY = 1f + selection * 0.08f
                    translationY = -selection * 1.5.dp.toPx()
                },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = destination.label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun RecordAction(
    expanded: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            reduceMotion -> 1f
            expanded -> 0.88f
            pressed -> 0.94f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "record_action_scale",
    )

    Column(
        modifier = modifier
            .semantics { contentDescription = AppDestination.RECORD.accessibilityLabel }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassSurface(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            cornerRadius = 36.dp,
            elevation = 22.dp,
            backdropBlur = true,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(5.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                palette.accent.copy(alpha = 0.94f),
                                palette.brand.copy(alpha = 0.98f),
                            ),
                            center = androidx.compose.ui.geometry.Offset(20f, 14f),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                HeimaGlyph(
                    destination = AppDestination.RECORD,
                    color = Color.White,
                    modifier = Modifier.size(31.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "记账",
            color = palette.brand,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
