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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.ui.AnimatedBudgetGauge
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

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading("预算", "给生活留一点从容") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 30.dp, backdropBlur = true) {
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
                    PressableGlassSurface({ editing = true }, Modifier.fillMaxWidth().height(50.dp), 18.dp, backdropBlur = false) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text(if (budget == null) "设置本月预算" else "修改本月预算", color = palette.brand, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        item { SectionHeading("本月概览") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 24.dp, backdropBlur = false) {
                Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BudgetMetric("已支出", summary.expenseCents, amountsVisible)
                    BudgetMetric(if (remaining != null && remaining < 0) "已超出" else "还可使用", kotlin.math.abs(remaining ?: 0L), amountsVisible)
                }
            }
        }
        item { SectionHeading("温和提醒") }
        item {
            GlassSurface(Modifier.fillMaxWidth(), 22.dp, backdropBlur = false) {
                Text(
                    if (budget == null) "设置一个适合自己的月预算即可；黑马记账不会用频繁提醒制造焦虑。" else if (ratio >= .85f) "本月预算已接近上限，接下来的支出可以稍加留意。" else "当前消费节奏平稳，保持适合自己的生活方式。",
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
    var input by remember { mutableStateOf(current?.let { (it / 100).toString() }.orEmpty()) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text("本月预算") },
        text = {
            Column {
                OutlinedTextField(input, { input = it.filter { character -> character.isDigit() || character == '.' }.take(12); error = false }, label = { Text("金额（元）") }, singleLine = true)
                if (error) Text("请输入大于 0 的金额", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton({ val cents = FinanceRules.parseYuanToCents(input); if (cents != null && cents > 0) onResult(cents) else error = true }) { Text("保存") } },
        dismissButton = { TextButton({ onResult(null) }) { Text("取消") } },
    )
}
