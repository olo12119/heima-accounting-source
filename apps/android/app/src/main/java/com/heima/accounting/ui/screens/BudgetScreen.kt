package com.heima.accounting.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heima.accounting.designsystem.GlassFieldSurface
import com.heima.accounting.designsystem.GlassSegmentedControl
import com.heima.accounting.designsystem.GlassSurface
import com.heima.accounting.designsystem.HeimaSurfaceRole
import com.heima.accounting.designsystem.HeimaTheme
import com.heima.accounting.designsystem.PressableGlassSurface
import com.heima.accounting.domain.BudgetMode
import com.heima.accounting.domain.BudgetReminder
import com.heima.accounting.domain.BudgetEvaluation
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.FinanceSummary
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.StatisticsPeriod
import com.heima.accounting.domain.formatYuan
import com.heima.accounting.ui.AnimatedBudgetGauge
import com.heima.accounting.ui.CategoryIcon
import com.heima.accounting.ui.GlassTextInputDialog
import com.heima.accounting.ui.HeimaDialogFrame
import com.heima.accounting.ui.SensitiveAmountText
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 预算页 · 三模式（三期 3.3）：先存后花（默认）/ 整月上限 / 分类预算。
 * 跨模式金额计算一律走 FinanceRules.budgetEvaluation()（共享约定 2）；
 * 提醒阈值取 FinanceRules.BUDGET_REMINDER_*（共享约定 4，UI 不自写阈值）。
 */
@Composable
fun BudgetScreen(
    snapshot: LedgerSnapshot,
    amountsVisible: Boolean,
    onSaveBudget: (MonthlyBudget) -> Unit,
) {
    val palette = HeimaTheme.palette
    val reduceMotion = HeimaTheme.motion.reduceMotion
    val today = LocalDate.now()
    val month = FinanceRules.monthKey(today)
    val budget = snapshot.budgets.firstOrNull { it.month == month }
    val summary = remember(snapshot.transactions, month) {
        FinanceRules.summarize(snapshot.transactions, FinanceRules.range(StatisticsPeriod.MONTH, today))
    }

    // 模式切换（D2/D3）：无预算行或新模式尚无数据时只做 UI 局部态（ARCH 八.2，不落库、不报错）；
    // 能落库的模式切换立即 upsert 当月行（保留旧字段互不覆盖，D12）。
    var pendingMode by remember { mutableStateOf<BudgetMode?>(null) }
    val effectiveMode = pendingMode ?: budget?.mode ?: BudgetMode.SAVINGS_GOAL
    LaunchedEffect(budget?.mode) { pendingMode = null }

    var editingGoal by remember { mutableStateOf(false) }
    var editingCap by remember { mutableStateOf(false) }
    var editingCategories by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 50.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeading("预算", "给生活留一点从容") }
        item {
            GlassSegmentedControl(
                options = listOf(
                    BudgetMode.SAVINGS_GOAL to "先存后花",
                    BudgetMode.MONTHLY_CAP to "整月上限",
                    BudgetMode.CATEGORY to "分类预算",
                ),
                selected = effectiveMode,
                onSelected = { mode ->
                    if (mode == effectiveMode) return@GlassSegmentedControl
                    val current = budget
                    val canPersist = when (mode) {
                        BudgetMode.MONTHLY_CAP -> current != null
                        BudgetMode.SAVINGS_GOAL -> (current?.savingsGoalCents ?: 0L) > 0L
                        BudgetMode.CATEGORY -> !current?.categoryBudgets.isNullOrEmpty()
                    }
                    if (current != null && canPersist) {
                        if (current.mode == mode) {
                            pendingMode = null
                        } else {
                            pendingMode = mode
                            onSaveBudget(current.copy(mode = mode))
                        }
                    } else {
                        pendingMode = mode
                    }
                },
                accessibilityLabel = "预算模式",
            )
        }
        item {
            // 模式区切换用 fade（150ms/90ms），不做位移（共享约定 9：避免与指标刷新竞态）。
            AnimatedContent(
                targetState = effectiveMode,
                transitionSpec = {
                    val duration = if (reduceMotion) 90 else 150
                    fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
                },
                label = "budget_mode",
            ) { mode ->
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    when (mode) {
                        BudgetMode.SAVINGS_GOAL -> SavingsGoalHero(budget, summary, amountsVisible) { editingGoal = true }
                        BudgetMode.MONTHLY_CAP -> MonthlyCapHero(budget, "${today.monthValue}月", summary, amountsVisible) { editingCap = true }
                        BudgetMode.CATEGORY -> CategoryBudgetHero(snapshot, budget, summary, amountsVisible) { editingCategories = true }
                    }
                }
            }
        }
        item { SectionHeading("温和提醒") }
        item {
            val evaluation = budget?.takeIf { it.mode == effectiveMode }?.let { FinanceRules.budgetEvaluation(it, summary) }
            GlassSurface(Modifier.fillMaxWidth(), 22.dp, backdropBlur = false, role = HeimaSurfaceRole.LIST) {
                Text(
                    budgetReminderText(evaluation),
                    Modifier.padding(20.dp),
                    color = palette.textSecondary,
                )
            }
        }
    }

    if (editingGoal) {
        GlassTextInputDialog(
            title = "每月储蓄目标",
            initialValue = budget?.savingsGoalCents?.takeIf { it > 0L }?.toYuanInput().orEmpty(),
            placeholder = "金额（元）",
            confirmText = "保存",
            validator = { input ->
                val cents = FinanceRules.parseYuanToCents(input)
                if (cents == null || cents <= 0L) "请输入大于 0 的金额" else null
            },
            onDismiss = { editingGoal = false },
            onConfirm = { input ->
                editingGoal = false
                val cents = FinanceRules.parseYuanToCents(input) ?: return@GlassTextInputDialog
                if (cents > 0L) {
                    // 模式 A：主金额冗余存储蓄目标（语义映射表），计算一律读 savingsGoalCents。
                    val base = budget ?: MonthlyBudget(month, cents)
                    onSaveBudget(base.copy(mode = BudgetMode.SAVINGS_GOAL, savingsGoalCents = cents, amountCents = cents))
                }
            },
        )
    }
    if (editingCap) {
        GlassTextInputDialog(
            title = "本月上限",
            initialValue = budget?.amountCents?.takeIf { it > 0L }?.toYuanInput().orEmpty(),
            placeholder = "金额（元）",
            confirmText = "保存",
            validator = { input ->
                val cents = FinanceRules.parseYuanToCents(input)
                if (cents == null || cents <= 0L) "请输入大于 0 的金额" else null
            },
            onDismiss = { editingCap = false },
            onConfirm = { input ->
                editingCap = false
                val cents = FinanceRules.parseYuanToCents(input) ?: return@GlassTextInputDialog
                if (cents > 0L) {
                    val base = budget ?: MonthlyBudget(month, cents)
                    onSaveBudget(base.copy(mode = BudgetMode.MONTHLY_CAP, amountCents = cents))
                }
            },
        )
    }
    if (editingCategories) {
        CategoryBudgetsDialog(
            categories = snapshot.topLevelCategories(EntryType.EXPENSE),
            current = budget?.categoryBudgets.orEmpty(),
            onDismiss = { editingCategories = false },
            onConfirm = { categoryBudgets ->
                editingCategories = false
                val total = categoryBudgets.values.sum()
                // 模式 C：主金额冗余存"已设分类额度合计"（语义映射表），计算一律读 categoryBudgets。
                val base = budget ?: MonthlyBudget(month, total)
                onSaveBudget(base.copy(mode = BudgetMode.CATEGORY, categoryBudgets = categoryBudgets, amountCents = total))
            },
        )
    }
}

/** 模式 A · 先存后花：可花额度 = 本月收入 − 储蓄目标（D4/D5/D6）。 */
@Composable
private fun SavingsGoalHero(
    budget: MonthlyBudget?,
    summary: FinanceSummary,
    amountsVisible: Boolean,
    onEdit: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val goal = budget?.savingsGoalCents ?: 0L
    val evaluation = budget?.takeIf { goal > 0L }?.let { FinanceRules.budgetEvaluation(it, summary) }
    GlassSurface(Modifier.fillMaxWidth(), 30.dp, backdropBlur = true, role = HeimaSurfaceRole.HERO) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
                val gaugeRatio = when {
                    evaluation == null -> 0f
                    evaluation.overGoal -> 1.25f // 进度条满红（D6），不显示负数
                    else -> (evaluation.usageRatio ?: 0f).coerceIn(0f, 1.25f)
                }
                AnimatedBudgetGauge(gaugeRatio, Modifier.matchParentSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("本月可花", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                    if (evaluation == null) {
                        Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    } else {
                        SensitiveAmountText(
                            evaluation.limitCents!!.coerceAtLeast(0L),
                            amountsVisible,
                            MaterialTheme.typography.headlineSmall,
                            palette.textPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    evaluation == null -> "设置储蓄目标后，自动算出本月可花额度"
                    evaluation.overGoal -> "储蓄目标已超过本月收入，请调整储蓄目标"
                    evaluation.reminderLevel == BudgetReminder.EXCEEDED -> "本月可花额度已用完，再花就存不够了"
                    evaluation.reminderLevel == BudgetReminder.NOTICE ->
                        "已用可花额度的${percentInt(evaluation.usageRatio)}%，注意节奏"
                    else -> "先存后花，剩下的安心花"
                },
                color = when {
                    evaluation?.overGoal == true || evaluation?.reminderLevel == BudgetReminder.EXCEEDED -> palette.expense
                    evaluation?.reminderLevel == BudgetReminder.NOTICE -> palette.warning
                    else -> palette.textSecondary
                },
            )
            if (evaluation != null) {
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BudgetMetricRow("本月收入", evaluation.incomeCents, amountsVisible)
                    BudgetMetricRow("储蓄目标", goal, amountsVisible)
                    BudgetMetricRow("本月已花", evaluation.spentCents, amountsVisible)
                    BudgetMetricRow("剩余可花", (evaluation.limitCents!! - evaluation.spentCents).coerceAtLeast(0L), amountsVisible)
                }
            }
            Spacer(Modifier.height(18.dp))
            PressableGlassSurface(onEdit, Modifier.fillMaxWidth().height(50.dp), 18.dp, backdropBlur = false, role = HeimaSurfaceRole.INTERACTIVE) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (evaluation == null) "设置储蓄目标" else "修改储蓄目标",
                        color = palette.brand,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** 模式 B · 整月上限：指标区三行 收入/支出/结余（D7），进度 = 已花 ÷ 上限。 */
@Composable
private fun MonthlyCapHero(
    budget: MonthlyBudget?,
    monthLabel: String,
    summary: FinanceSummary,
    amountsVisible: Boolean,
    onEdit: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val evaluation = budget?.let { FinanceRules.budgetEvaluation(it, summary) }
    val ratio = evaluation?.usageRatio ?: 0f
    GlassSurface(Modifier.fillMaxWidth(), 30.dp, backdropBlur = true, role = HeimaSurfaceRole.HERO) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
                AnimatedBudgetGauge(ratio.coerceIn(0f, 1.25f), Modifier.matchParentSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${monthLabel}预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                    if (budget == null) {
                        Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    } else {
                        SensitiveAmountText(budget.amountCents, amountsVisible, MaterialTheme.typography.headlineSmall, palette.textPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    budget == null -> "设置本月最多花多少，超支前心里有数"
                    ratio > 1f -> "已超出本月上限，请按实际需要调整"
                    else -> "本月已使用 ${percentInt(ratio)}%"
                },
                color = if (ratio > 1f) palette.expense else palette.textSecondary,
            )
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BudgetMetricRow("本月收入", evaluation?.incomeCents ?: summary.incomeCents, amountsVisible)
                BudgetMetricRow("本月支出", evaluation?.expenseCents ?: summary.expenseCents, amountsVisible)
                BudgetMetricRow("本月结余", evaluation?.balanceCents ?: summary.balanceCents, amountsVisible)
            }
            Spacer(Modifier.height(18.dp))
            PressableGlassSurface(onEdit, Modifier.fillMaxWidth().height(50.dp), 18.dp, backdropBlur = false, role = HeimaSurfaceRole.INTERACTIVE) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (budget == null) "设置本月上限" else "修改本月上限",
                        color = palette.brand,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** 模式 C · 分类预算：总进度（Σspent/Σlimit，D9）+ 各分类"已花/额度"行（D8）。 */
@Composable
private fun CategoryBudgetHero(
    snapshot: LedgerSnapshot,
    budget: MonthlyBudget?,
    summary: FinanceSummary,
    amountsVisible: Boolean,
    onEdit: () -> Unit,
) {
    val palette = HeimaTheme.palette
    val evaluation = budget?.let { FinanceRules.budgetEvaluation(it, summary) }
    val rows = evaluation?.categoryRows.orEmpty()
    val ratio = evaluation?.usageRatio ?: 0f
    GlassSurface(Modifier.fillMaxWidth(), 30.dp, backdropBlur = true, role = HeimaSurfaceRole.HERO) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) {
                AnimatedBudgetGauge(ratio.coerceIn(0f, 1.25f), Modifier.matchParentSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分类预算", color = palette.textSecondary, style = MaterialTheme.typography.labelLarge)
                    if (rows.isEmpty()) {
                        Text("未设置", color = palette.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    } else {
                        SensitiveAmountText(
                            rows.sumOf { it.spentCents },
                            amountsVisible,
                            MaterialTheme.typography.headlineSmall,
                            palette.textPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (rows.isEmpty()) {
                    "给最想控制的几个支出分类分别设额度"
                } else {
                    "已设 ${rows.size} 个分类额度，总进度 ${percentInt(ratio)}%"
                },
                color = when {
                    evaluation?.reminderLevel == BudgetReminder.EXCEEDED -> palette.expense
                    evaluation?.reminderLevel == BudgetReminder.NOTICE -> palette.warning
                    else -> palette.textSecondary
                },
            )
            Spacer(Modifier.height(18.dp))
            PressableGlassSurface(onEdit, Modifier.fillMaxWidth().height(50.dp), 18.dp, backdropBlur = false, role = HeimaSurfaceRole.INTERACTIVE) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (rows.isEmpty()) "设置分类额度" else "修改分类额度",
                        color = palette.brand,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
    if (rows.isNotEmpty()) {
        // 未设额度的分类不显示行（D8）；额度来自 categoryBudgets，已花与统计页同口径。
        GlassSurface(Modifier.fillMaxWidth(), 24.dp, backdropBlur = false, role = HeimaSurfaceRole.INSIGHT) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEach { row ->
                    val category = snapshot.category(row.categoryId)
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CategoryIcon(category?.iconKey ?: "other", selected = false, size = 42.dp)
                                Text(category?.name ?: "未分类", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "${row.spentCents.formatYuan()} / ${row.limitCents.formatYuan()}",
                                color = palette.textSecondary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        val barColor = when {
                            row.ratio >= FinanceRules.BUDGET_REMINDER_EXCEEDED -> palette.expense
                            row.ratio >= FinanceRules.BUDGET_REMINDER_NOTICE -> palette.warning
                            else -> palette.brand
                        }
                        Box(Modifier.fillMaxWidth().height(6.dp).background(palette.surfaceVariant, RoundedCornerShape(3.dp))) {
                            Box(
                                Modifier
                                    .fillMaxWidth(row.ratio.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .background(barColor, RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 分类额度编辑弹层：列出全部支出一级分类，每行一个金额输入（拍板 5）。 */
@Composable
private fun CategoryBudgetsDialog(
    categories: List<Category>,
    current: Map<String, Long>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Long>) -> Unit,
) {
    val palette = HeimaTheme.palette
    var inputs by remember { mutableStateOf(current.mapValues { (_, cents) -> cents.toYuanInput() }) }
    var error by remember { mutableStateOf<String?>(null) }
    HeimaDialogFrame(onDismiss = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("设置分类额度", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
                Text("仅支出一级分类；留空的分类不设额度", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            items(categories, key = Category::id) { category ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryIcon(category.iconKey, selected = false, size = 40.dp)
                    Text(category.name, Modifier.weight(1f), color = palette.textPrimary)
                    GlassFieldSurface(Modifier.size(width = 110.dp, height = 44.dp)) {
                        BasicTextField(
                            value = inputs[category.id].orEmpty(),
                            onValueChange = { value ->
                                if (value.isEmpty() || FinanceRules.parseYuanToCents(value) != null) {
                                    inputs = inputs + (category.id to value)
                                    error = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = palette.textPrimary,
                                textAlign = TextAlign.End,
                            ),
                        )
                    }
                }
            }
            error?.let { message -> item { Text(message, color = palette.expense, style = MaterialTheme.typography.bodyMedium) } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PressableGlassSurface(onDismiss, Modifier.weight(1f).height(48.dp), 16.dp) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { Text("取消", color = palette.textSecondary) }
                    }
                    PressableGlassSurface(
                        {
                            val parsed = linkedMapOf<String, Long>()
                            var invalid = false
                            inputs.forEach { (categoryId, text) ->
                                if (text.isBlank()) return@forEach
                                val cents = FinanceRules.parseYuanToCents(text)
                                if (cents == null || cents <= 0L) {
                                    invalid = true
                                } else {
                                    parsed[categoryId] = cents
                                }
                            }
                            when {
                                invalid -> error = "有额度格式不正确，请检查"
                                parsed.isEmpty() -> error = "至少给一个分类设置额度"
                                else -> onConfirm(parsed)
                            }
                        },
                        Modifier.weight(1f).height(48.dp),
                        16.dp,
                    ) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Text("保存", color = palette.brand, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetMetricRow(label: String, amountCents: Long, visible: Boolean) {
    val palette = HeimaTheme.palette
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
        SensitiveAmountText(amountCents, visible, MaterialTheme.typography.titleMedium, palette.textPrimary)
    }
}

/** "温和提醒"卡文案：按模式与 reminderLevel 三态（D5，阈值取 FinanceRules 常量）。 */
private fun budgetReminderText(evaluation: BudgetEvaluation?): String = when {
    evaluation == null -> "设置好储蓄目标、整月上限或分类额度后，这里会按真实支出给出温和提醒。"
    else -> when (evaluation.mode) {
        BudgetMode.SAVINGS_GOAL -> when (evaluation.reminderLevel) {
            BudgetReminder.EXCEEDED -> "本月可花额度已用完，再花就要动到储蓄目标了。"
            BudgetReminder.NOTICE -> "已用掉本月可花额度的${percentInt(evaluation.usageRatio)}%，再花就存不够了，注意节奏。"
            BudgetReminder.NONE -> "本月已花${evaluation.expenseCents.formatYuan()}，距离可花额度还有余地，节奏不错。"
        }
        BudgetMode.MONTHLY_CAP -> when {
            (evaluation.usageRatio ?: 0f) > 1f -> "本月预算使用率为${percentInt(evaluation.usageRatio)}%，已超过设定额度。"
            (evaluation.usageRatio ?: 0f) >= 0.85f -> "本月预算使用率为${percentInt(evaluation.usageRatio)}%，已接近设定额度。"
            else -> "本月预算使用率为${percentInt(evaluation.usageRatio)}%，结果来自当前真实账单。"
        }
        BudgetMode.CATEGORY -> when (evaluation.reminderLevel) {
            BudgetReminder.EXCEEDED -> "已设额度的分类合计使用${percentInt(evaluation.usageRatio)}%，已超出总额度。"
            BudgetReminder.NOTICE -> "已设额度的分类合计使用${percentInt(evaluation.usageRatio)}%，接近总额度，注意节奏。"
            BudgetReminder.NONE -> "各分类额度使用${percentInt(evaluation.usageRatio)}%，按分类控制花销更从容。"
        }
    }
}

private fun percentInt(ratio: Float?): Int = ((ratio ?: 0f) * 100).roundToInt().coerceAtLeast(0)

/** 分 → 元输入框回显（100000 → "1000"；100050 → "1000.5"）。 */
private fun Long.toYuanInput(): String =
    BigDecimal(this).movePointLeft(2).stripTrailingZeros().toPlainString()
