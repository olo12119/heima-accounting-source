package com.heima.accounting.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.ui.AnimatedBudgetGauge
import com.heima.accounting.ui.GlassTextInputDialog
import com.heima.accounting.ui.SensitiveAmountText
import java.time.LocalDate

@Composable
fun BudgetScreen(snapshot: LedgerSnapshot, amountsVisible: Boolean, onSaveBudget: (String, Long) -> Unit) {
    val palette = HeimaTheme.palette
    val today = LocalDate.now()
    val month = FinanceRules.monthKey(today)
    val budget = snapshot.budgets.firstOrNull { it.month == month }
    val summary = remember(snapshot.transactions, month) { FinanceRules.summarize(snapshot.transactions, FinanceRules.range(StatisticsPeriod.MONTH, today)) }
    val ratio = if (budget?.amountCents != null && budget.amountCents > 0L) summary.expenseCents.toFloat() / budget.amountCents else 0f
    val remaining = budget?.let { it.amountCents - summary.expenseCents }
    var editing by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading("预算", "给生活留一点从容") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 30.dp, backdropBlur = true, role = HeimaSurfaceRole.HERO) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
                        AnimatedBudgetGauge(ratio, Modifier.matchParentSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${today.monthValue}月预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                            if (budget == null) Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                            else SensitiveAmountText(budget.amountCents, amountsVisible, MaterialTheme.typography.headlineSmall, palette.textPrimary)
                        }
                    }
                    Text(
                        when {
                            budget == null -> "设置后会计算剩余额度和使用进度"
                            remaining != null && remaining < 0 -> "已超出预算，请按实际需要调整"
                            else -> "本月已使用 ${(ratio * 100).toInt().coerceAtLeast(0)}%"
                        },
                        color = if (remaining != null && remaining < 0) palette.expense else palette.textSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    PressableGlassSurface({ editing = true }, Modifier.fillMaxWidth().height(50.dp), 18.dp, backdropBlur = false, role = HeimaSurfaceRole.INTERACTIVE) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text(if (budget == null) "设置本月预算" else "修改本月预算", color = palette.brand, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        item { SectionHeading("本月概览") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 24.dp, backdropBlur = false, role = HeimaSurfaceRole.INSIGHT) {
                Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BudgetMetric("已支出", summary.expenseCents, amountsVisible)
                    BudgetMetric(if (remaining != null && remaining < 0) "已超出" else "还可使用", kotlin.math.abs(remaining ?: 0L), amountsVisible)
                }
            }
        }
        item { SectionHeading("温和提醒") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 22.dp, backdropBlur = false, role = HeimaSurfaceRole.LIST) {
                Text(
                    when {
                        budget == null -> "设置预算后，这里会用真实支出计算使用比例。"
                        ratio > 1f -> "本月预算使用率为 ${(ratio * 100).toInt()}%，已超过设定额度。"
                        ratio >= .85f -> "本月预算使用率为 ${(ratio * 100).toInt()}%，已接近设定额度。"
                        else -> "本月预算使用率为 ${(ratio * 100).toInt().coerceAtLeast(0)}%，结果来自当前真实账单。"
                    },
                    Modifier.padding(20.dp), color = palette.textSecondary,
                )
            }
        }
    }
    if (editing) BudgetDialog(budget?.amountCents) { cents ->
        editing = false
        if (cents != null) onSaveBudget(month, cents)
    }
}

@Composable
private fun BudgetMetric(label: String, amount: Long, visible: Boolean) {
    val palette = HeimaTheme.palette
    Column { Text(label, color = palette.textTertiary); SensitiveAmountText(amount, visible, MaterialTheme.typography.titleLarge, palette.textPrimary) }
}

@Composable
private fun BudgetDialog(current: Long?, onResult: (Long?) -> Unit) {
    GlassTextInputDialog(
        title = "本月预算",
        initialValue = current?.let { (it / 100).toString() }.orEmpty(),
        placeholder = "金额（元）",
        confirmText = "保存",
        validator = { input ->
            if (FinanceRules.parseYuanToCents(input) == null || FinanceRules.parseYuanToCents(input)!! <= 0L) "请输入大于 0 的金额" else null
        },
        onDismiss = { onResult(null) },
        onConfirm = { input -> onResult(FinanceRules.parseYuanToCents(input)) },
    )
}
