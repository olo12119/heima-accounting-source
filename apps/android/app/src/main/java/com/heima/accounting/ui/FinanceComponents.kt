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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.domain.CategoryChartSlice
import com.heima.accounting.domain.CategoryTotal
import com.heima.accounting.domain.DailyTotal
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.FinanceRules
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
    var selectedIndex by remember(totals, showIncome) { mutableIntStateOf(-1) }
    Box(modifier.semantics { contentDescription = "消费趋势图，共${totals.size}天数据" }) {
    Canvas(
        Modifier
            .matchParentSize()
            .pointerInput(totals) {
                detectTapGestures { tap ->
                    if (totals.isNotEmpty()) {
                        selectedIndex = ((tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, .9999f) * totals.size).toInt()
                    }
                }
            }
            .pointerInput(totals) {
                detectDragGestures(
                    onDragStart = { start ->
                        if (totals.isNotEmpty()) selectedIndex = ((start.x / size.width.coerceAtLeast(1)).coerceIn(0f, .9999f) * totals.size).toInt()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (totals.isNotEmpty()) selectedIndex = ((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, .9999f) * totals.size).toInt()
                    },
                )
            },
    ) {
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
        points.getOrNull(selectedIndex)?.let { selected ->
            drawLine(
                color = palette.outline.copy(alpha = .72f),
                start = Offset(selected.x, 0f),
                end = Offset(selected.x, baselineY),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(lineColor.copy(alpha = .18f), radius = 9.dp.toPx(), center = selected)
            drawCircle(lineColor, radius = 4.dp.toPx(), center = selected)
        }
    }
    totals.getOrNull(selectedIndex)?.let { selected ->
        val value = if (showIncome) selected.incomeCents else selected.expenseCents
        Text(
            text = "${selected.date.format(DateTimeFormatter.ofPattern("M月d日"))}  ${value.formatYuan()}",
            color = palette.textPrimary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.TopEnd),
        )
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

/** 本月趋势卡支持的三种图表类型。选中态只留在会话级 rememberSaveable，不写盘。 */
enum class TrendChartType { LINE, BAR, PIE }

/** 柱状图：镜像 AnimatedTrendChart 的动画/交互（生长动画 + 点按/拖动选中 + 右上浮层数值）。 */
@Composable
fun AnimatedBarChart(
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
    var selectedIndex by remember(totals, showIncome) { mutableIntStateOf(-1) }
    Box(modifier.semantics { contentDescription = "每日收支柱状图，共${totals.size}天数据" }) {
    Canvas(
        Modifier
            .matchParentSize()
            .pointerInput(totals) {
                detectTapGestures { tap ->
                    if (totals.isNotEmpty()) {
                        selectedIndex = ((tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, .9999f) * totals.size).toInt()
                    }
                }
            }
            .pointerInput(totals) {
                detectDragGestures(
                    onDragStart = { start ->
                        if (totals.isNotEmpty()) selectedIndex = ((start.x / size.width.coerceAtLeast(1)).coerceIn(0f, .9999f) * totals.size).toInt()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (totals.isNotEmpty()) selectedIndex = ((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, .9999f) * totals.size).toInt()
                    },
                )
            },
    ) {
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
        val slotWidth = size.width / totals.size
        val barWidth = slotWidth * .56f
        val usableHeight = size.height - 9.dp.toPx()
        val barColor = if (showIncome) palette.income else palette.brand
        values.forEachIndexed { index, value ->
            // Bars grow out of the baseline as the entrance animation progresses.
            val barHeight = value.toFloat() / maxValue * usableHeight * progress.value
            if (barHeight <= 0f) return@forEachIndexed
            val centerX = slotWidth * (index + .5f)
            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            val top = baselineY - barHeight
            val corner = 3.dp.toPx().coerceAtMost(barWidth / 2f).coerceAtMost(barHeight)
            val color = if (selectedIndex < 0 || selectedIndex == index) barColor else barColor.copy(alpha = .55f)
            val bar = Path().apply {
                moveTo(left, baselineY)
                lineTo(left, top + corner)
                quadraticTo(left, top, left + corner, top)
                lineTo(right - corner, top)
                quadraticTo(right, top, right, top + corner)
                lineTo(right, baselineY)
                close()
            }
            drawPath(bar, color)
        }
    }
    totals.getOrNull(selectedIndex)?.let { selected ->
        val value = if (showIncome) selected.incomeCents else selected.expenseCents
        Text(
            text = "${selected.date.format(DateTimeFormatter.ofPattern("M月d日"))}  ${value.formatYuan()}",
            color = palette.textPrimary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
    }
}

/** 饼图：包装 AnimatedDonutChart，自带选中态与"分类名 ¥金额 · 占比"标签。 */
@Composable
fun MonthCategoryPieChart(
    totals: List<CategoryTotal>,
    snapshot: LedgerSnapshot,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    val slices = remember(totals) { FinanceRules.categoryChartSlices(totals) }
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    Box(modifier.semantics { contentDescription = "本月分类支出占比饼图，点按扇区查看详情" }) {
        AnimatedDonutChart(
            slices = slices,
            colors = palette.chartColors,
            selectedIndex = selectedIndex,
            onSelected = { index -> selectedIndex = if (selectedIndex == index) null else index },
            modifier = Modifier.align(Alignment.Center).size(118.dp),
            centerTitle = "本月支出",
            centerSubtitle = slices.sumOf(CategoryChartSlice::amountCents).formatYuan(),
        )
        slices.getOrNull(selectedIndex ?: -1)?.let { slice ->
            val name = slice.categoryId?.let { snapshot.category(it)?.name ?: "未分类" } ?: "其他"
            Text(
                text = "$name  ${slice.amountCents.formatYuan()} · ${(slice.ratio * 100).toInt()}%",
                color = palette.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** 三图切换器：三个 32.dp 图标位，选中项主色高亮 + 品牌色浅底圆。 */
@Composable
fun TrendChartSwitcher(
    selected: TrendChartType,
    onSelect: (TrendChartType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HeimaTheme.palette
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        TrendChartType.entries.forEach { type ->
            val isSelected = type == selected
            val label = when (type) {
                TrendChartType.LINE -> "折线图"
                TrendChartType.BAR -> "柱状图"
                TrendChartType.PIE -> "饼图"
            }
            Box(
                Modifier
                    .size(32.dp)
                    .semantics { contentDescription = "切换到$label${if (isSelected) "，当前显示" else ""}" }
                    .clip(CircleShape)
                    .background(if (isSelected) palette.brandSoft.copy(alpha = .85f) else Color.Transparent)
                    .clickable(enabled = !isSelected) { onSelect(type) },
                contentAlignment = Alignment.Center,
            ) {
                TrendChartGlyph(type, if (isSelected) palette.brand else palette.textTertiary)
            }
        }
    }
}

@Composable
private fun TrendChartGlyph(type: TrendChartType, color: Color) {
    Canvas(Modifier.size(17.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        when (type) {
            TrendChartType.LINE -> {
                val points = listOf(
                    Offset(size.width * .12f, size.height * .72f),
                    Offset(size.width * .40f, size.height * .42f),
                    Offset(size.width * .62f, size.height * .58f),
                    Offset(size.width * .88f, size.height * .22f),
                )
                val line = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(line, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                points.forEach { drawCircle(color, radius = strokeWidth * .85f, center = it) }
            }
            TrendChartType.BAR -> {
                val barWidth = size.width * .16f
                val gap = size.width * .10f
                var left = size.width * .14f
                listOf(.48f, .78f, .60f).forEach { ratio ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, size.height * (1f - ratio)),
                        size = Size(barWidth, size.height * ratio),
                        cornerRadius = CornerRadius(barWidth * .32f),
                    )
                    left += barWidth + gap
                }
            }
            TrendChartType.PIE -> {
                val radius = size.minDimension / 2f - strokeWidth / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color, radius = radius, center = center, style = Stroke(strokeWidth))
                drawLine(color, center, Offset(center.x, center.y - radius), strokeWidth)
                drawLine(color, center, Offset(center.x + radius * .87f, center.y + radius * .5f), strokeWidth)
            }
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
