package com.heima.accounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme

@Composable
internal fun ScreenHeading(
    title: String,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    color = HeimaTheme.palette.textSecondary,
                )
                Spacer(Modifier.height(5.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = HeimaTheme.palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        trailing?.invoke()
    }
}

@Composable
internal fun SectionHeading(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = HeimaTheme.palette.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = HeimaTheme.palette.brand,
                modifier = Modifier
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun EntityCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = HeimaTheme.palette
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = palette.surface.copy(alpha = 0.96f),
        shadowElevation = 5.dp,
        tonalElevation = 0.dp,
        content = { Box(Modifier.padding(20.dp)) { content() } },
    )
}

@Composable
internal fun EmptyIllustration(
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(palette.brandSoft, palette.surfaceMuted),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(palette.glassHighlight, RoundedCornerShape(9.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
