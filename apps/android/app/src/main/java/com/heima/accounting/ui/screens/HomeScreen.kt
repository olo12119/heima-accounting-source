package com.heima.accounting.ui.screens

import android.icu.util.Calendar
import android.icu.util.ULocale
import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaMotionTokens
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.LocalHeimaScrolling
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.BudgetMode
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.FinancialInsightLevel
import com.heima.accounting.domain.FinancialInsightRules
import com.heima.accounting.domain.HealthGrade
import com.heima.accounting.domain.HealthMetric
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.domain.Transaction
import com.heima.accounting.domain.formatYuan
import com.heima.accounting.ui.AnimatedAmount
import com.heima.accounting.ui.AnimatedBarChart
import com.heima.accounting.ui.AnimatedBudgetGauge
import com.heima.accounting.ui.AnimatedTrendChart
import com.heima.accounting.ui.CategoryIcon
import com.heima.accounting.ui.ChartSwitchTransition
import com.heima.accounting.ui.GlassPullToRefresh
import com.heima.accounting.ui.MonthCategoryPieChart
import com.heima.accounting.ui.REFRESH_MIN_DISPLAY_MS
import com.heima.accounting.ui.SensitiveAmountText
import com.heima.accounting.ui.StaggeredContent
import com.heima.accounting.ui.TransactionRow
import com.heima.accounting.ui.TrendChartSwitcher
import com.heima.accounting.ui.TrendChartType
import com.heima.accounting.ui.formatThousands
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onAmountsVisibleChange: (Boolean) -> Unit,
    onRecord: () -> Unit,
    onBudgetClick: () -> Unit,
    onOpenRecords: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit = {},
    onRefreshHome: suspend () -> Unit = {},
    onSelectionFeedback: () -> Unit = {},
    onAmountSettled: () -> Unit = {},
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    // 图表类型只活在本次会话：rememberSaveable 保证切 Tab/旋屏/后台回收不重置，不写盘。
    var chartType by rememberSaveable { mutableStateOf(TrendChartType.LINE) }
    val today = remember { LocalDate.now() }
    val weekday = remember(today) { today.format(DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE)) }
    val lunar = remember(today) { formatLunarDate(today) }
    val todaySummary = remember(snapshot.transactions, today) {
        FinanceRules.summarize(snapshot.transactions, FinanceRules.range(StatisticsPeriod.TODAY, today))
    }
    val monthSummary = remember(snapshot.transactions, today) {
        FinanceRules.summarize(snapshot.transactions, FinanceRules.range(StatisticsPeriod.MONTH, today))
    }
    val monthTrend = remember(monthSummary.dailyTotals, today) {
        FinanceRules.continuousDailyTotals(
            monthSummary.dailyTotals,
            com.heima.accounting.domain.DateRange(today.withDayOfMonth(1), today),
        )
    }
    val budget = snapshot.budgets.firstOrNull { it.month == FinanceRules.monthKey(today) }
    // 剩余预算卡口径适配（三期 3.3）：主金额语义一律经 budgetEvaluation 解释（共享约定 2）。
    val budgetEvaluation = remember(budget, monthSummary) {
        budget?.let { FinanceRules.budgetEvaluation(it, monthSummary) }
    }
    val budgetCard = when {
        budgetEvaluation == null -> null
        else -> when (budgetEvaluation.mode) {
            BudgetMode.SAVINGS_GOAL -> Triple(
                "剩余可花",
                ((budgetEvaluation.limitCents ?: 0L) - budgetEvaluation.spentCents).coerceAtLeast(0L),
                if (budgetEvaluation.overGoal) "先存后花：储蓄目标优先" else "本月可用",
            )
            BudgetMode.MONTHLY_CAP -> Triple(
                "剩余预算",
                ((budgetEvaluation.limitCents ?: 0L) - budgetEvaluation.spentCents).coerceAtLeast(0L),
                "本月可用",
            )
            BudgetMode.CATEGORY -> Triple(
                "分类预算剩余",
                (budgetEvaluation.categoryLimitTotalCents - budgetEvaluation.categoryRows.sumOf { it.spentCents }).coerceAtLeast(0L),
                "已设分类可用",
            )
        }
    }
    val budgetGaugeRatio = when {
        budgetEvaluation == null -> 0f
        budgetEvaluation.overGoal -> 1.25f
        else -> (budgetEvaluation.usageRatio ?: 0f).coerceIn(0f, 1.25f)
    }
    val recent = snapshot.transactions.take(6)
    // 财务体检（三期 3.2）：与统计页小结共用同一 evaluateHealth 口径（共享约定 5）。
    val healthReport = remember(snapshot.transactions, snapshot.budgets, today) {
        FinancialInsightRules.evaluateHealth(snapshot, YearMonth.from(today), today)
    }
    val reportColor = when (healthReport.level) {
        FinancialInsightLevel.INSUFFICIENT -> palette.brand
        FinancialInsightLevel.STABLE -> palette.income
        FinancialInsightLevel.ATTENTION -> palette.warning
        FinancialInsightLevel.HIGH_PRESSURE -> palette.expense
    }

    val listState = rememberLazyListState()
    // 滚动感知只经 LocalHeimaScrolling 下发（共享约定 6）：derivedStateOf 收敛，
    // 仅在滚动开始/结束各重组一次，玻璃降级与图表暂停都读它。
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    var refreshing by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalHeimaScrolling provides isScrolling) {
    GlassPullToRefresh(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            try {
                val start = SystemClock.elapsedRealtime()
                onRefreshHome()
                val elapsed = SystemClock.elapsedRealtime() - start
                if (elapsed < REFRESH_MIN_DISPLAY_MS) delay(REFRESH_MIN_DISPLAY_MS - elapsed)
            } finally {
                refreshing = false
            }
        },
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    StaggeredGreeting(dailyGreeting(today))
                    Spacer(Modifier.height(5.dp))
                    Text("${today.monthValue}月${today.dayOfMonth}日 $weekday", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${today.year}年 · $lunar", style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
                }
                PressableGlassSurface(
                    onClick = { onAmountsVisibleChange(!amountsVisible) },
                    modifier = Modifier.size(50.dp).semantics { contentDescription = if (amountsVisible) "隐藏所有金额" else "显示所有金额" },
                    cornerRadius = 25.dp,
                    backdropBlur = true,
                ) {
                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { PrivacyEyeIcon(amountsVisible, Modifier.size(26.dp)) }
                }
            }
        }

        item {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 31.dp,
                backdropBlur = true,
                role = HeimaSurfaceRole.HERO,
            ) {
                // 消费与收入平级等大：任一侧金额达 7 位（万元级）时两列同步降级字号，保证始终一致。
                val amountStyle = if (maxOf(todaySummary.expenseCents, todaySummary.incomeCents) >= 1_000_000_00L) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.headlineLarge
                }
                val balanceColor by animateColorAsState(
                    targetValue = when {
                        todaySummary.balanceCents > 0L -> palette.income
                        todaySummary.balanceCents < 0L -> palette.expense
                        else -> palette.textSecondary
                    },
                    label = "balance_color",
                )
                val balanceScale = remember { Animatable(1f) }
                LaunchedEffect(todaySummary.balanceCents, reduceMotion) {
                    if (!reduceMotion) {
                        balanceScale.snapTo(1.08f)
                        balanceScale.animateTo(1f, HeimaMotionTokens.bounce(reduceMotion))
                    }
                }
                Column(Modifier.padding(horizontal = 22.dp, vertical = 21.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("今日消费", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                            Spacer(Modifier.height(4.dp))
                            AnimatedAmount(
                                targetCents = todaySummary.expenseCents,
                                visible = amountsVisible,
                                style = amountStyle,
                                color = palette.textPrimary,
                                format = { it.formatThousands() },
                                onSettled = onAmountSettled,
                            )
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("今日收入", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                            Spacer(Modifier.height(4.dp))
                            AnimatedAmount(
                                targetCents = todaySummary.incomeCents,
                                visible = amountsVisible,
                                style = amountStyle,
                                color = palette.income,
                                format = { it.formatThousands() },
                                onSettled = onAmountSettled,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AnimatedAmount(
                            targetCents = todaySummary.balanceCents,
                            visible = amountsVisible,
                            style = MaterialTheme.typography.titleMedium,
                            color = balanceColor,
                            prefix = "今日结余  ",
                            signed = true,
                            modifier = Modifier.graphicsLayer {
                                scaleX = balanceScale.value
                                scaleY = balanceScale.value
                            },
                        )
                    }
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth(), cornerRadius = 26.dp, backdropBlur = false, role = HeimaSurfaceRole.CHART) {
                Column(Modifier.padding(17.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("本月趋势", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        TrendChartSwitcher(
                            selected = chartType,
                            onSelect = { selected ->
                                onSelectionFeedback()
                                chartType = selected
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // 图表区高度恒定：切换只做粒子/淡入过渡，卡片不跳高、不闪白。
                    Box(Modifier.fillMaxWidth().height(150.dp)) {
                        if (monthSummary.expenseCents == 0L) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("本月暂无消费", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            // B3：图表切换粒子过渡（旧图淡出+微粒上飘，新图淡入生长）；reduceMotion 降级为纯淡入淡出。
                            ChartSwitchTransition(
                                targetState = chartType,
                                reduceMotion = reduceMotion,
                                modifier = Modifier.fillMaxSize(),
                            ) { type ->
                                when (type) {
                                    TrendChartType.LINE -> AnimatedTrendChart(monthTrend, Modifier.fillMaxSize())
                                    TrendChartType.BAR -> AnimatedBarChart(monthTrend, Modifier.fillMaxSize())
                                    TrendChartType.PIE -> MonthCategoryPieChart(monthSummary.categoryTotals, snapshot, Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(if (monthSummary.expenseCents == 0L) "等待第一笔账" else "本月 ${monthSummary.expenseCents.formatYuan()}", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            PressableGlassSurface(
                onClick = onBudgetClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .semantics { contentDescription = "查看本月预算" },
                cornerRadius = 26.dp,
                backdropBlur = true,
                role = HeimaSurfaceRole.METRIC,
            ) {
                Box(Modifier.matchParentSize().padding(17.dp)) {
                    Column {
                        Text(budgetCard?.first ?: "剩余预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        if (budgetCard == null) {
                            Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium)
                            Text("在预算页设置", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                        } else {
                            SensitiveAmountText(budgetCard.second, amountsVisible, MaterialTheme.typography.headlineMedium, palette.textPrimary)
                            Text(budgetCard.third, color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (budgetEvaluation != null) {
                        AnimatedBudgetGauge(budgetGaugeRatio, Modifier.size(62.dp).align(Alignment.BottomEnd))
                    }
                }
            }
        }

        item {
            // 财务体检卡（三期 3.2）：4 行指标 + 迷你进度条，去掉旧"财务状态"固定高度与圆圈。
            GlassSurface(Modifier.fillMaxWidth(), cornerRadius = 28.dp, backdropBlur = true, role = HeimaSurfaceRole.INSIGHT) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("财务体检", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Text(
                            healthReport.title,
                            color = reportColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val healthMetrics = buildList {
                        healthReport.savingRate?.let { add("结余率" to it) }
                        healthReport.concentration?.let { add("支出集中度" to it) }
                        healthReport.savingsProgress?.let { add("储蓄进度" to it) }
                        healthReport.monthOverMonth?.let { add("与上月对比" to it) }
                    }
                    StaggeredContent(healthMetrics.size, baseDelayMs = 60) { index ->
                        val (label, metric) = healthMetrics[index]
                        HealthMetricRow(label, metric)
                    }
                }
            }
        }

        item { SectionHeading("分类支出洞察") }
        if (monthSummary.categoryTotals.isEmpty()) {
            item { EntityCard(Modifier.fillMaxWidth()) { EmptyIllustration("有账目后，这里会展示花得最多的分类", Modifier.fillMaxWidth().padding(vertical = 8.dp)) } }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(monthSummary.categoryTotals.take(6), key = { it.categoryId }) { total ->
                        val category = snapshot.category(total.categoryId)
                        GlassSurface(Modifier.size(width = 132.dp, height = 142.dp), cornerRadius = 24.dp, backdropBlur = false, role = HeimaSurfaceRole.LIST) {
                            Column(Modifier.padding(15.dp)) {
                                CategoryIcon(category?.iconKey ?: "other", selected = false, size = 54.dp)
                                Spacer(Modifier.height(3.dp))
                                Text(category?.name ?: "未分类", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                                SensitiveAmountText(total.amountCents, amountsVisible, MaterialTheme.typography.labelLarge, palette.textSecondary)
                                Text("${(total.ratio * 100).toInt()}%", color = palette.brand, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeading("最近账单", action = "查看全部", onAction = onOpenRecords) }
        if (recent.isEmpty()) {
            item {
                PressableGlassSurface(onRecord, Modifier.fillMaxWidth(), cornerRadius = 24.dp, backdropBlur = true, role = HeimaSurfaceRole.INTERACTIVE) {
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                        Text("还没有账单", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(5.dp))
                        Text("点击这里或底部记账按钮，开始记录真实收支", color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            item {
                GlassSurface(Modifier.fillMaxWidth(), cornerRadius = 25.dp, backdropBlur = false, role = HeimaSurfaceRole.LIST) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        StaggeredContent(recent.size, baseDelayMs = 40) { index ->
                            SwipeableTransactionRow(
                                transaction = recent[index],
                                snapshot = snapshot,
                                amountsVisible = amountsVisible,
                                onClick = { onTransactionClick(recent[index].id) },
                                onDelete = { onDeleteTransaction(recent[index].id) },
                            )
                        }
                    }
                }
            }
        }
    }
    }
    }
}

/** B2：问候逐字淡入 + 底部辉光横条扫过（每日首次触发）。 */
@Composable
private fun StaggeredGreeting(greeting: String) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    Row(Modifier.fillMaxWidth()) {
        greeting.forEachIndexed { index, ch ->
            val visible = remember(greeting, index) { Animatable(if (reduceMotion) 1f else 0f) }
            LaunchedEffect(greeting, index, reduceMotion) {
                if (reduceMotion) {
                    visible.snapTo(1f)
                } else {
                    delay((index * 30).toLong())
                    visible.animateTo(1f, tween(160))
                }
            }
            Text(
                ch.toString(),
                color = palette.textSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.graphicsLayer { alpha = visible.value },
            )
        }
    }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(greeting, reduceMotion) {
        if (!reduceMotion) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        val bandWidth = size.width * 0.42f
        val x = sweep.value * (size.width + bandWidth) - bandWidth
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, palette.brand.copy(alpha = .5f), Color.Transparent),
            ),
            topLeft = Offset(x, 0f),
            size = Size(bandWidth, size.height),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    transaction: Transaction,
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.expense.copy(alpha = .92f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("删除", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 18.dp))
            }
        },
    ) {
        TransactionRow(transaction, snapshot, amountsVisible, onClick)
    }
}

/** 体检卡单行指标：数值按分档着色，进度条与数值共用同一 ratio（C6）。 */
@Composable
private fun HealthMetricRow(label: String, metric: HealthMetric) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val gradeColor = when (metric.grade) {
        HealthGrade.GOOD -> palette.income
        HealthGrade.MEDIUM -> palette.textPrimary
        HealthGrade.POOR -> palette.expense
        HealthGrade.N_A -> palette.textMuted
    }
    val animatedProgress by animateFloatAsState(
        targetValue = metric.progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) snap() else tween(420, easing = FastOutSlowInEasing),
        label = "health_metric_progress",
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
            Text(
                metric.displayValue,
                color = gradeColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).background(palette.surfaceVariant, RoundedCornerShape(3.dp))) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(5.dp)
                    .background(gradeColor, RoundedCornerShape(3.dp)),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(metric.text, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PrivacyEyeIcon(visible: Boolean, modifier: Modifier = Modifier) {
    val palette = HeimaTheme.palette
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.50f)
            cubicTo(size.width * 0.28f, size.height * 0.17f, size.width * 0.72f, size.height * 0.17f, size.width * 0.92f, size.height * 0.50f)
            cubicTo(size.width * 0.72f, size.height * 0.83f, size.width * 0.28f, size.height * 0.83f, size.width * 0.08f, size.height * 0.50f)
        }
        drawPath(path, palette.textSecondary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(palette.textSecondary, radius = size.minDimension * 0.12f)
        if (!visible) drawLine(palette.textSecondary, Offset(size.width * 0.18f, size.height * 0.16f), Offset(size.width * 0.82f, size.height * 0.84f), 2.3.dp.toPx(), StrokeCap.Round)
    }
}

internal fun dailyGreeting(date: LocalDate): String {
    val greetings = listOf("每一笔，都是生活留下的脚印", "把今天的收支，轻轻放进账本", "认真记录，也是在照顾未来的自己", "看清钱的方向，生活更从容", "今天也留一点时间给自己的账本", "小小一笔，慢慢拼出生活全貌", "让每一份所得与付出都有迹可循")
    return greetings[Math.floorMod(date.toEpochDay().toInt(), greetings.size)]
}

internal fun formatLunarDate(date: LocalDate): String {
    val calendar = Calendar.getInstance(ULocale("zh_CN@calendar=chinese"))
    calendar.timeInMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val months = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
    val days = listOf("初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十", "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十", "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十")
    return "农历${months[calendar.get(Calendar.MONTH).coerceIn(0, 11)]}${days[(calendar.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, 29)]}"
}
