package com.heima.accounting.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    val motion = HeimaTheme.motion

    GlassSurface(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(76.dp),
        cornerRadius = 30.dp,
        elevation = 18.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppDestination.entries.forEach { destination ->
                if (destination == AppDestination.RECORD) {
                    RecordAction(
                        expanded = recordPanelVisible,
                        reduceMotion = motion.reduceMotion,
                        onClick = onRecord,
                    )
                } else {
                    BottomBarItem(
                        destination = destination,
                        selected = selected == destination,
                        onClick = { onDestinationSelected(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val color by animateColorAsState(
        targetValue = if (selected) palette.brand else palette.textTertiary,
        label = "nav_item_color",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 48.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_indicator_width",
    )

    Column(
        modifier = Modifier
            .widthIn(min = 58.dp)
            .height(68.dp)
            .semantics { contentDescription = destination.accessibilityLabel }
            .then(Modifier.simpleClick(onClick)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .height(30.dp)
                    .background(palette.brandSoft.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
            )
            HeimaGlyph(
                destination = destination,
                color = color,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            color = color,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun RecordAction(
    expanded: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val targetScale = if (expanded && !reduceMotion) 0.88f else 1f
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "record_action_scale",
    )

    Column(
        modifier = Modifier
            .widthIn(min = 70.dp)
            .offset(y = (-12).dp)
            .semantics { contentDescription = AppDestination.RECORD.accessibilityLabel }
            .then(Modifier.simpleClick(onClick)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = palette.brand.copy(alpha = 0.30f),
                    spotColor = palette.brand.copy(alpha = 0.34f),
                )
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(palette.accent, palette.brand),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            HeimaGlyph(
                destination = AppDestination.RECORD,
                color = Color.White,
                modifier = Modifier.size(27.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "记账",
            color = palette.brand,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun Modifier.simpleClick(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
