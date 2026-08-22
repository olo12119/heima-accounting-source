package com.heima.accounting.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.ui.AnimatedDonutChart
import com.heima.accounting.ui.AnimatedTrendChart
import com.heima.accounting.ui.SensitiveAmountText
import java.time.LocalDate

@Composable
fun StatisticsScreen(snapshot: LedgerSnapshot, amountsVisible: Boolean) {
    val palette = HeimaTheme.palette
    var period by remember { mutableStateOf(StatisticsPeriod.MONTH) }
    val summary = remember(snapshot.transactions, period) {
        FinanceRules.summarize(snapshot.transactions, FinanceRules.range(period, LocalDate.now()))
    }
    val periods = listOf(StatisticsPeriod.TODAY to "今日", StatisticsPeriod.WEEK to "本周", StatisticsPeriod.MONTH to "本月", StatisticsPeriod.YEAR to "今年")

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading("统计", "读懂每一笔真实收支") }
        item {
            Row(Modifier.fillMaxWidth().background(palette.surfaceMuted.copy(alpha = .72f), RoundedCornerShape(18.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                periods.forEach { (value, label) ->
                    PressableGlassSurface({ period = value }, Modifier.weight(1f).height(42.dp), 15.dp, backdropBlur = false) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Text(label, color = if (period == value) palette.brand else palette.textSecondary, fontWeight = if (period == value) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 28.dp, backdropBlur = true) {
                Column(Modifier.padding(22.dp)) {
                    Text("支出总额", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(7.dp))
                    SensitiveAmountText(summary.expenseCents, amountsVisible, MaterialTheme.typography.displayMedium, palette.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column { Text("收入", color = palette.textTertiary); SensitiveAmountText(summary.incomeCents, amountsVisible, MaterialTheme.typography.titleMedium, palette.income) }
                        Column { Text("结余", color = palette.textTertiary); SensitiveAmountText(summary.balanceCents, amountsVisible, MaterialTheme.typography.titleMedium, if (summary.balanceCents >= 0) palette.income else palette.expense, signed = true) }
                    }
                }
            }
        }
        item { SectionHeading("收支结构") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 27.dp, backdropBlur = false) {
                if (summary.categoryTotals.isEmpty()) {
                    EmptyIllustration("还没有可统计的支出", Modifier.fillMaxWidth().padding(vertical = 22.dp))
                } else {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        AnimatedDonutChart(summary.categoryTotals, snapshot, Modifier.size(126.dp), "支出分类")
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            summary.categoryTotals.take(4).forEach { total ->
                                val category = snapshot.category(total.categoryId)
                                LegendRow(category?.name ?: "未分类", total.ratio, category?.colorArgb?.let(::Color) ?: palette.brand)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeading("消费趋势") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 25.dp, backdropBlur = false) {
                Column(Modifier.padding(20.dp)) {
                    AnimatedTrendChart(summary.dailyTotals, Modifier.fillMaxWidth().height(136.dp))
                    if (summary.dailyTotals.isEmpty()) Text("记账后会看到消费随时间的变化", color = palette.textTertiary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (summary.categoryTotals.isNotEmpty()) {
            item { SectionHeading("分类排行") }
            items(summary.categoryTotals.size) { index ->
                val total = summary.categoryTotals[index]
                val category = snapshot.category(total.categoryId)
                GlassSurface(Modifier.fillMaxWidth(), 19.dp, backdropBlur = false) {
                    Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. ${category?.name ?: "未分类"}", color = palette.textPrimary)
                            SensitiveAmountText(total.amountCents, amountsVisible, MaterialTheme.typography.labelLarge, palette.textSecondary)
                        }
                        Spacer(Modifier.height(7.dp))
                        Box(Modifier.fillMaxWidth().height(6.dp).background(palette.surfaceMuted, RoundedCornerShape(3.dp))) {
                            Box(Modifier.fillMaxWidth(total.ratio.coerceIn(0f, 1f)).height(6.dp).background(category?.colorArgb?.let(::Color) ?: palette.brand, RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, ratio: Float, color: Color) {
    val palette = HeimaTheme.palette
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(5.dp)))
        Text(label, Modifier.weight(1f), color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
        Text("${(ratio * 100).toInt()}%", color = palette.textPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
