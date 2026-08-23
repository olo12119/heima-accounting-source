package com.heima.accounting.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.heima.accounting.designsystem.GlassSegmentedControl
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.CategoryChartSlice
import com.heima.accounting.domain.CategoryTotal
import com.heima.accounting.domain.DateRange
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.domain.StatisticsResult
import com.heima.accounting.domain.formatYuan
import com.heima.accounting.ui.AnimatedDonutChart
import com.heima.accounting.ui.AnimatedTrendChart
import com.heima.accounting.ui.LiquidGlassDateRangePicker
import com.heima.accounting.ui.SensitiveAmountText
import com.heima.accounting.ui.TransactionRow
import com.heima.accounting.ui.categoryColorFromArgb
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatisticsScreen(
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    loadStatistics: suspend (DateRange) -> StatisticsResult,
    onSelectionFeedback: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    var period by rememberSaveable { mutableStateOf(StatisticsPeriod.TODAY) }
    var customStartIso by rememberSaveable { mutableStateOf<String?>(null) }
    var customEndIso by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf(StatisticsResult()) }
    var selectedSliceIndex by remember { mutableIntStateOf(-1) }
    var showOtherCategories by remember { mutableStateOf(false) }
    val customRange = remember(customStartIso, customEndIso) {
        val start = customStartIso?.let(LocalDate::parse)
        val end = customEndIso?.let(LocalDate::parse)
        if (start != null && end != null) DateRange(start, end) else null
    }
    val range = customRange ?: FinanceRules.range(period, LocalDate.now())
    val summary = result.summary
    val periods = listOf(
        StatisticsPeriod.TODAY to "今日",
        StatisticsPeriod.WEEK to "本周",
        StatisticsPeriod.MONTH to "本月",
        StatisticsPeriod.YEAR to "今年",
    )

    LaunchedEffect(range, snapshot.transactions) {
        result = runCatching { loadStatistics(range) }.getOrDefault(StatisticsResult())
        selectedSliceIndex = -1
    }

    val slices = remember(summary.categoryTotals) {
        FinanceRules.categoryChartSlices(summary.categoryTotals)
    }
    val sliceColors = slices.mapIndexed { index, slice ->
        val base = if (slice.isOther) {
            palette.chartColors.last()
        } else {
            snapshot.category(slice.categoryId)?.colorArgb?.let(::categoryColorFromArgb)
                ?: palette.chartColors[index % palette.chartColors.size]
        }
        if (motion.darkTheme) lerp(base, Color.White, .16f) else base
    }
    val selectedSlice = slices.getOrNull(selectedSliceIndex)
    val selectedCategoryIds = selectedSlice?.sourceCategoryIds.orEmpty()
    val selectedTransactions = remember(result.transactions, selectedCategoryIds) {
        if (selectedCategoryIds.isEmpty()) emptyList()
        else result.transactions.filter { it.categoryId in selectedCategoryIds }
    }

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading("统计", "读懂每一笔真实收支") }
        item {
            if (customRange == null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        GlassSegmentedControl(
                            options = periods,
                            selected = period,
                            onSelected = { period = it },
                            accessibilityLabel = "统计时间范围",
                        )
                    }
                    CalendarFilterButton { showDatePicker = true }
                }
            } else {
                CustomRangeHeader(
                    range = range,
                    onEdit = { showDatePicker = true },
                    onReset = { customStartIso = null; customEndIso = null },
                )
            }
        }
        item {
            AnimatedContent(
                targetState = summary,
                transitionSpec = {
                    fadeIn(tween(if (motion.reduceMotion) 70 else 180)) togetherWith
                        fadeOut(tween(if (motion.reduceMotion) 60 else 120))
                },
                label = "statistics_summary",
            ) { visibleSummary ->
                GlassSurface(Modifier.fillMaxWidth(), 28.dp, backdropBlur = true) {
                    Column(Modifier.padding(22.dp)) {
                        Text("支出总额", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(7.dp))
                        SensitiveAmountText(
                            visibleSummary.expenseCents,
                            amountsVisible,
                            MaterialTheme.typography.displayMedium,
                            palette.textPrimary,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text("收入", color = palette.textTertiary)
                                SensitiveAmountText(
                                    visibleSummary.incomeCents,
                                    amountsVisible,
                                    MaterialTheme.typography.titleMedium,
                                    palette.income,
                                )
                            }
                            Column {
                                Text("结余", color = palette.textTertiary)
                                SensitiveAmountText(
                                    visibleSummary.balanceCents,
                                    amountsVisible,
                                    MaterialTheme.typography.titleMedium,
                                    if (visibleSummary.balanceCents >= 0) palette.income else palette.expense,
                                    signed = true,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeading("收支结构") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 27.dp, backdropBlur = false) {
                if (slices.isEmpty()) {
                    EmptyIllustration("还没有可统计的支出", Modifier.fillMaxWidth().padding(vertical = 22.dp))
                } else {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            val title = when {
                                selectedSlice == null -> "支出分类"
                                selectedSlice.isOther -> "其他"
                                else -> snapshot.category(selectedSlice.categoryId)?.name ?: "未分类"
                            }
                            val subtitle = selectedSlice?.let {
                                if (amountsVisible) "${it.amountCents.formatYuan()} · ${(it.ratio * 100).toInt()}%"
                                else "¥••••"
                            }.orEmpty()
                            AnimatedDonutChart(
                                slices = slices,
                                colors = sliceColors,
                                selectedIndex = selectedSliceIndex.takeIf { it >= 0 },
                                onSelected = {
                                    selectedSliceIndex = it
                                    if (slices[it].isOther) showOtherCategories = true
                                },
                                modifier = Modifier.size(132.dp),
                                centerTitle = title,
                                centerSubtitle = subtitle,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                slices.forEachIndexed { index, slice ->
                                    val label = if (slice.isOther) "其他" else snapshot.category(slice.categoryId)?.name ?: "未分类"
                                    LegendRow(
                                        label = label,
                                        amountCents = slice.amountCents,
                                        ratio = slice.ratio,
                                        color = sliceColors[index],
                                        amountsVisible = amountsVisible,
                                        selected = index == selectedSliceIndex,
                                        onClick = {
                                            selectedSliceIndex = index
                                            if (slice.isOther) showOtherCategories = true
                                        },
                                    )
                                }
                            }
                        }
                        if (slices.any(CategoryChartSlice::isOther)) {
                            Text(
                                "查看全部分类",
                                color = palette.brand,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clickable { showOtherCategories = true }
                                    .padding(6.dp),
                            )
                        }
                    }
                }
            }
        }
        if (selectedTransactions.isNotEmpty()) {
            item { SectionHeading("所选分类账单") }
            items(selectedTransactions.take(8), key = { "selected-${it.id}" }) { transaction ->
                GlassSurface(Modifier.fillMaxWidth(), 19.dp, backdropBlur = false) {
                    TransactionRow(
                        transaction = transaction,
                        snapshot = snapshot,
                        amountsVisible = amountsVisible,
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    )
                }
            }
        }
        item { SectionHeading("消费趋势") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 25.dp, backdropBlur = false) {
                Column(Modifier.padding(20.dp)) {
                    AnimatedTrendChart(summary.dailyTotals, Modifier.fillMaxWidth().height(136.dp))
                    if (summary.dailyTotals.isEmpty()) {
                        Text(
                            "记账后会看到消费随时间的变化",
                            color = palette.textTertiary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (summary.categoryTotals.isNotEmpty()) {
            item { SectionHeading("分类排行") }
            items(summary.categoryTotals, key = CategoryTotal::categoryId) { total ->
                val index = summary.categoryTotals.indexOf(total)
                val category = snapshot.category(total.categoryId)
                val categoryColor = category?.colorArgb?.let(::categoryColorFromArgb) ?: palette.brand
                GlassSurface(Modifier.fillMaxWidth(), 19.dp, backdropBlur = false) {
                    Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. ${category?.name ?: "未分类"}", color = palette.textPrimary)
                            SensitiveAmountText(
                                total.amountCents,
                                amountsVisible,
                                MaterialTheme.typography.labelLarge,
                                palette.textSecondary,
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Box(Modifier.fillMaxWidth().height(6.dp).background(palette.surfaceMuted, RoundedCornerShape(3.dp))) {
                            Box(
                                Modifier
                                    .fillMaxWidth(total.ratio.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .background(categoryColor, RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        LiquidGlassDateRangePicker(
            initialRange = customRange ?: range,
            onDismiss = { showDatePicker = false },
            onSelectionFeedback = onSelectionFeedback,
            onConfirm = {
                customStartIso = it.startInclusive.toString()
                customEndIso = it.endInclusive.toString()
                showDatePicker = false
            },
        )
    }
    if (showOtherCategories) {
        OtherCategoriesSheet(
            totals = summary.categoryTotals.filter { total ->
                slices.firstOrNull(CategoryChartSlice::isOther)?.sourceCategoryIds?.contains(total.categoryId) == true
            },
            snapshot = snapshot,
            amountsVisible = amountsVisible,
            onDismiss = { showOtherCategories = false },
        )
    }
}

@Composable
private fun CalendarFilterButton(onClick: () -> Unit) {
    PressableGlassSurface(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = "自定义统计日期" },
        cornerRadius = 17.dp,
        backdropBlur = false,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val color = HeimaTheme.palette.brand
            Canvas(Modifier.size(21.dp)) {
                val strokeWidth = 1.8.dp.toPx()
                val top = 3.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(1.5.dp.toPx(), top),
                    size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(strokeWidth),
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 8.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(19.dp.toPx(), 8.dp.toPx()),
                    strokeWidth = strokeWidth,
                )
                listOf(7f, 14f).forEach { x ->
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(x.dp.toPx(), 1.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(x.dp.toPx(), 5.5.dp.toPx()),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomRangeHeader(range: DateRange, onEdit: () -> Unit, onReset: () -> Unit) {
    val palette = HeimaTheme.palette
    val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)
    val label = if (range.startInclusive == range.endInclusive) {
        range.startInclusive.format(formatter)
    } else {
        "${range.startInclusive.format(formatter)} - ${range.endInclusive.format(formatter)}"
    }
    GlassSurface(Modifier.fillMaxWidth(), 18.dp, backdropBlur = false) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("自定义：$label", Modifier.weight(1f), color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            Text("修改", color = palette.brand, modifier = Modifier.clickable(onClick = onEdit).padding(5.dp))
            Text("重置", color = palette.textSecondary, modifier = Modifier.clickable(onClick = onReset).padding(5.dp))
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    amountCents: Long,
    ratio: Float,
    color: Color,
    amountsVisible: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = HeimaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.brandSoft.copy(.48f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(label, color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
            Text(
                if (amountsVisible) amountCents.formatYuan() else "¥••••",
                color = palette.textTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            "${(ratio * 100).toInt()}%",
            color = palette.textPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OtherCategoriesSheet(
    totals: List<CategoryTotal>,
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onDismiss: () -> Unit,
) {
    val palette = HeimaTheme.palette
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (HeimaTheme.motion.darkTheme) .54f else .24f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            GlassSurface(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(onClick = {}),
                cornerRadius = 28.dp,
                elevation = 18.dp,
                backdropBlur = false,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).size(38.dp, 4.dp).background(palette.outline, CircleShape))
                    Text("其他分类明细", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
                    totals.forEach { total ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(snapshot.category(total.categoryId)?.name ?: "未分类", color = palette.textSecondary)
                            Text(
                                if (amountsVisible) "${total.amountCents.formatYuan()} · ${(total.ratio * 100).toInt()}%" else "¥••••",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        "完成",
                        color = palette.brand,
                        modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
