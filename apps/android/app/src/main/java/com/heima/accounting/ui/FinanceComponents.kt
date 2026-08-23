package com.heima.accounting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.domain.CategoryChartSlice
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
import kotlin.math.atan2
import kotlin.math.hypot

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
    val reduceMotion = HeimaTheme.motion.reduceMotion
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(55)) togetherWith fadeOut(tween(45))
            } else {
                fadeIn(tween(110, delayMillis = 70)) togetherWith fadeOut(tween(90))
            }
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
        val visiblePoints = points.filter { it.x <= visibleX + .5f }
        if (visiblePoints.isEmpty()) return@Canvas
        val lineColor = if (showIncome) palette.income else palette.brand
        if (visiblePoints.size == 1) {
            val point = visiblePoints.first()
            drawCircle(lineColor.copy(alpha = .15f), radius = 8.dp.toPx(), center = point)
            drawCircle(lineColor, radius = 3.5.dp.toPx(), center = point)
            return@Canvas
        }
        val path = Path()
        visiblePoints.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else {
                val previous = visiblePoints[index - 1]
                val midX = (previous.x + point.x) / 2f
                path.cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
            }
        }
        val area = Path().apply {
            addPath(path)
            lineTo(visiblePoints.last().x, baselineY)
            lineTo(visiblePoints.first().x, baselineY)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = .18f), lineColor.copy(alpha = .015f)),
                endY = baselineY,
            ),
        )
        drawPath(
            path,
            color = lineColor,
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
        )
        if (progress.value >= .99f) {
            val latest = points.last()
            drawCircle(lineColor.copy(alpha = .14f), radius = 7.dp.toPx(), center = latest)
            drawCircle(lineColor, radius = 3.dp.toPx(), center = latest)
        }
    }
}

@Composable
fun AnimatedDonutChart(
    slices: List<CategoryChartSlice>,
    colors: List<Color>,
    selectedIndex: Int?,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    centerTitle: String,
    centerSubtitle: String,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val key = remember(slices) { slices.hashCode() }
    val progress = remember(key) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(key, reduceMotion) {
        if (!reduceMotion) progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        else progress.snapTo(1f)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .matchParentSize()
                .semantics { contentDescription = "可交互的收支分类环形图" }
                .pointerInput(slices) {
                    detectTapGestures { tap ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = hypot(tap.x - centerX, tap.y - centerY)
                        val outer = minOf(size.width, size.height) / 2f
                        if (radius !in outer * .54f..outer) return@detectTapGestures
                        val degrees = Math.toDegrees(
                            atan2(tap.y - centerY, tap.x - centerX).toDouble(),
                        ).toFloat()
                        val clockwiseFromTop = (degrees + 450f) % 360f
                        var cursor = 0f
                        slices.forEachIndexed { index, slice ->
                            val end = cursor + 360f * slice.ratio
                            if (clockwiseFromTop in cursor..end) {
                                onSelected(index)
                                return@detectTapGestures
                            }
                            cursor = end
                        }
                    }
                },
        ) {
            val stroke = 16.dp.toPx()
            drawCircle(palette.surfaceMuted, style = Stroke(stroke))
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val color = colors.getOrElse(index) { palette.brand }
                val sweep = 360f * slice.ratio * progress.value
                val selected = selectedIndex == index
                drawArc(
                    color = color,
                    startAngle = start + if (selected) .8f else 1.2f,
                    sweepAngle = (sweep - if (selected) 1.6f else 2.4f).coerceAtLeast(0f),
                    useCenter = false,
                    style = Stroke(
                        width = if (selected) 20.dp.toPx() else stroke,
                        cap = StrokeCap.Round,
                    ),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerTitle,
                color = palette.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                centerSubtitle,
                color = palette.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
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
        CategoryIcon(primary?.iconKey ?: "other", selected = false, size = 48.dp)
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
