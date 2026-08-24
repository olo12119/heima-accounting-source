package com.heima.accounting.ui.screens

import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaType
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.FinancialInsightLevel
import com.heima.accounting.domain.FinancialInsightRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.domain.formatYuan
import com.heima.accounting.ui.AnimatedBudgetGauge
import com.heima.accounting.ui.AnimatedTrendChart
import com.heima.accounting.ui.CategoryIcon
import com.heima.accounting.ui.SensitiveAmountText
import com.heima.accounting.ui.TransactionRow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onAmountsVisibleChange: (Boolean) -> Unit,
    onRecord: () -> Unit,
    onBudgetClick: () -> Unit,
    onOpenRecords: () -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    val palette = HeimaTheme.palette
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
    val remainingBudget = budget?.let { (it.amountCents - monthSummary.expenseCents).coerceAtLeast(0L) }
    val recent = snapshot.transactions.take(6)
    val insight = remember(snapshot.transactions, snapshot.budgets, snapshot.categories, today) {
        FinancialInsightRules.evaluate(snapshot, today)
    }
    val insightColor = when (insight.level) {
        FinancialInsightLevel.INSUFFICIENT -> palette.brand
        FinancialInsightLevel.STABLE -> palette.income
        FinancialInsightLevel.ATTENTION -> palette.warning
        FinancialInsightLevel.HIGH_PRESSURE -> palette.expense
    }

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(dailyGreeting(today), style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
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
                Column(Modifier.padding(horizontal = 22.dp, vertical = 21.dp)) {
                    Text("今日消费", style = MaterialTheme.typography.titleMedium, color = palette.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    SensitiveAmountText(todaySummary.expenseCents, amountsVisible, HeimaType.displayAmount, palette.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    SensitiveAmountText(todaySummary.incomeCents, amountsVisible, MaterialTheme.typography.bodyLarge, palette.textSecondary, prefix = "今日收入  ")
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSurface(Modifier.weight(1f).height(150.dp), cornerRadius = 26.dp, backdropBlur = false, role = HeimaSurfaceRole.METRIC) {
                    Column(Modifier.padding(17.dp)) {
                        Text("本月趋势", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(10.dp))
                        AnimatedTrendChart(monthTrend, Modifier.fillMaxWidth().height(56.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(if (monthSummary.expenseCents == 0L) "等待第一笔账" else "本月 ${monthSummary.expenseCents.formatYuan()}", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                PressableGlassSurface(
                    onClick = onBudgetClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .semantics { contentDescription = "查看本月预算" },
                    cornerRadius = 26.dp,
                    backdropBlur = true,
                    role = HeimaSurfaceRole.METRIC,
                ) {
                    Box(Modifier.matchParentSize().padding(17.dp)) {
                        Column {
                            Text("剩余预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            if (remainingBudget == null) {
                                Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium)
                                Text("在预算页设置", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                            } else {
                                SensitiveAmountText(remainingBudget, amountsVisible, MaterialTheme.typography.headlineMedium, palette.textPrimary)
                                Text("本月可用", color = palette.textTertiary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (budget != null) AnimatedBudgetGauge(monthSummary.expenseCents.toFloat() / budget.amountCents, Modifier.size(62.dp).align(Alignment.BottomEnd))
                    }
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth().height(145.dp), cornerRadius = 28.dp, backdropBlur = true, role = HeimaSurfaceRole.INSIGHT) {
                Row(Modifier.matchParentSize().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("财务状态", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(insight.title, color = insightColor, style = MaterialTheme.typography.headlineMedium)
                        Text(insight.explanation, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Canvas(Modifier.size(74.dp)) {
                        drawCircle(insightColor.copy(alpha = 0.16f))
                        drawCircle(insightColor, radius = size.minDimension * 0.30f, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
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
                        recent.forEach { transaction -> TransactionRow(transaction, snapshot, amountsVisible, { onTransactionClick(transaction.id) }) }
                    }
                }
            }
        }
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
