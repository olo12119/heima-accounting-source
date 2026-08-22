package com.heima.accounting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.domain.CategoryTotal
import com.heima.accounting.domain.DailyTotal
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.Transaction
import com.heima.accounting.domain.formatYuan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun SensitiveAmountText(
    amountCents: Long,
    visible: Boolean,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "",
    signed: Boolean = false,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            fadeIn(tween(110, delayMillis = 80)) togetherWith fadeOut(tween(90))
        },
        label = "private_amount",
        modifier = modifier,
    ) { isVisible ->
        val value = if (isVisible) {
            val sign = if (signed && amountCents > 0L) "+" else ""
            "$prefix$sign${amountCents.formatYuan()}"
        } else {
            "$prefix¥••••"
        }
        Text(value, style = style, color = color, maxLines = 1)
    }
}

@Composable
fun AnimatedTrendChart(
    totals: List<DailyTotal>,
    modifier: Modifier = Modifier,
    showIncome: Boolean = false,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val key = remember(totals, showIncome) { totals.hashCode() * 31 + showIncome.hashCode() }
    val progress = remember(key) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(key, reduceMotion) {
        if (!reduceMotion) progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        else progress.snapTo(1f)
    }
    Canvas(modifier.semantics { contentDescription = "消费趋势图，共${totals.size}天数据" }) {
        val baselineY = size.height - 4.dp.toPx()
        drawLine(
            palette.divider,
            Offset(0f, baselineY),
            Offset(size.width, baselineY),
            1.dp.toPx(),
        )
        if (totals.isEmpty()) return@Canvas
        val values = totals.map { if (showIncome) it.incomeCents else it.expenseCents }
        val maxValue = max(1L, values.maxOrNull() ?: 1L)
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) size.width / 2f else index.toFloat() / (values.size - 1) * size.width
            val y = baselineY - value.toFloat() / maxValue * (size.height - 9.dp.toPx())
            Offset(x, y)
        }
        val visibleX = size.width * progress.value
        val path = Path()
        points.forEachIndexed { index, point ->
            if (point.x <= visibleX) {
                if (index == 0) path.moveTo(point.x, point.y) else {
                    val previous = points[index - 1]
                    val midX = (previous.x + point.x) / 2f
                    path.cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
                }
            }
        }
        drawPath(
            path,
            color = if (showIncome) palette.income else palette.brand,
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
fun AnimatedDonutChart(
    totals: List<CategoryTotal>,
    snapshot: LedgerSnapshot,
    modifier: Modifier = Modifier,
    centerLabel: String,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val key = remember(totals) { totals.hashCode() }
    val progress = remember(key) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(key, reduceMotion) {
        if (!reduceMotion) progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        else progress.snapTo(1f)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 16.dp.toPx()
            drawCircle(palette.surfaceMuted, style = Stroke(stroke))
            var start = -90f
            totals.take(8).forEach { total ->
                val category = snapshot.category(total.categoryId)
                val color = category?.colorArgb?.let(::Color) ?: palette.brand
                val sweep = 360f * total.ratio * progress.value
                drawArc(color, start, sweep.coerceAtLeast(0f), false, style = Stroke(stroke, cap = StrokeCap.Round))
                start += sweep
            }
        }
        Text(centerLabel, color = palette.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AnimatedBudgetGauge(
    ratio: Float,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val target = ratio.coerceIn(0f, 1.25f)
    val progress = remember(target) { Animatable(if (reduceMotion) target else 0f) }
    LaunchedEffect(target, reduceMotion) {
        if (!reduceMotion) progress.animateTo(target, tween(520, easing = FastOutSlowInEasing))
        else progress.snapTo(target)
    }
    Canvas(modifier.semantics { contentDescription = "预算已使用${(ratio * 100).toInt()}%" }) {
        val stroke = 12.dp.toPx()
        val color = when {
            ratio > 1f -> palette.expense
            ratio >= 0.85f -> palette.warning
            else -> palette.brand
        }
        drawArc(palette.surfaceMuted, 140f, 260f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, 140f, 260f * progress.value.coerceAtMost(1f), false, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val primary = snapshot.category(transaction.categoryId)
    val secondary = snapshot.category(transaction.subcategoryId)
    val dateTime = Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(ZoneId.systemDefault())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(primary?.colorArgb ?: 0xFF8A96A8).copy(alpha = 0.13f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CategoryArtwork(primary?.iconKey ?: "other", Modifier.size(43.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                secondary?.name ?: primary?.name ?: "未分类",
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            val note = transaction.note.ifBlank { primary?.name.orEmpty() }
            Text(
                "${dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.SIMPLIFIED_CHINESE))} · $note",
                color = palette.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
        SensitiveAmountText(
            amountCents = transaction.amountCents,
            visible = amountsVisible,
            prefix = if (transaction.type == EntryType.EXPENSE) "−" else "+",
            style = MaterialTheme.typography.titleMedium,
            color = if (transaction.type == EntryType.EXPENSE) palette.textPrimary else palette.income,
        )
    }
}
