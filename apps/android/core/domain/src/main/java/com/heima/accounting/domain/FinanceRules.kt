package com.heima.accounting.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object FinanceRules {
    const val MAX_VISIBLE_CATEGORIES = 5
    const val SMALL_CATEGORY_RATIO = 0.03f

    /** 预算提醒阈值唯一来源（拍板 1）：80% 注意 / 100% 超限。 */
    const val BUDGET_REMINDER_NOTICE = 0.80f
    const val BUDGET_REMINDER_EXCEEDED = 1.00f
    fun parseYuanToCents(input: String): Long? {
        val normalized = input.trim().trimEnd('.')
        if (!normalized.matches(Regex("\\d{1,9}(\\.\\d{1,2})?"))) return null
        return runCatching {
            BigDecimal(normalized)
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact()
        }.getOrNull()
    }

    fun appendAmount(current: String, key: String, maxIntegerDigits: Int = 9): String {
        if (key == ".") return when {
            current.contains('.') -> current
            current.isEmpty() -> "0."
            else -> "$current."
        }
        if (key.length != 1 || !key[0].isDigit()) return current
        val decimalIndex = current.indexOf('.')
        if (decimalIndex >= 0) {
            return if (current.length - decimalIndex - 1 < 2) current + key else current
        }
        if (current == "0") return if (key == "0") current else key
        return if (current.length < maxIntegerDigits) current + key else current
    }

    fun range(period: StatisticsPeriod, today: LocalDate): DateRange = when (period) {
        StatisticsPeriod.TODAY -> DateRange(today, today)
        StatisticsPeriod.WEEK -> DateRange(
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
        )
        StatisticsPeriod.MONTH -> YearMonth.from(today).let { DateRange(it.atDay(1), it.atEndOfMonth()) }
        StatisticsPeriod.YEAR -> DateRange(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()))
        StatisticsPeriod.ALL -> DateRange(LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))
    }

    fun summarize(
        transactions: List<Transaction>,
        range: DateRange,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): FinanceSummary {
        val included = transactions.filter { !it.excludedFromStatistics && it.localDate(zoneId) in range }
        val expense = included.filter { it.type == EntryType.EXPENSE }
        val income = included.filter { it.type == EntryType.INCOME }
        val expenseTotal = expense.sumOf(Transaction::amountCents)
        val categories = expense.groupBy(Transaction::categoryId)
            .map { (id, values) ->
                val amount = values.sumOf(Transaction::amountCents)
                CategoryTotal(id, amount, if (expenseTotal == 0L) 0f else amount.toFloat() / expenseTotal)
            }
            .sortedByDescending(CategoryTotal::amountCents)
        val daily = included.groupBy { it.localDate(zoneId) }
            .map { (date, values) ->
                DailyTotal(
                    date,
                    values.filter { it.type == EntryType.EXPENSE }.sumOf(Transaction::amountCents),
                    values.filter { it.type == EntryType.INCOME }.sumOf(Transaction::amountCents),
                )
            }
            .sortedBy(DailyTotal::date)
        return FinanceSummary(expenseTotal, income.sumOf(Transaction::amountCents), categories, daily)
    }

    /**
     * Builds an honest day-by-day series without inventing transactions. Empty
     * days are represented by zero, while a completely empty ledger remains an
     * empty state. Callers choose the end date, so home can stop at today instead
     * of drawing future days from the rest of the month.
     */
    fun continuousDailyTotals(
        totals: List<DailyTotal>,
        range: DateRange,
    ): List<DailyTotal> {
        if (totals.isEmpty()) return emptyList()
        val byDate = totals.associateBy(DailyTotal::date)
        return buildList {
            var date = range.startInclusive
            while (!date.isAfter(range.endInclusive)) {
                add(byDate[date] ?: DailyTotal(date, expenseCents = 0L, incomeCents = 0L))
                date = date.plusDays(1)
            }
        }
    }

    fun monthKey(date: LocalDate): String = "%04d-%02d".format(date.year, date.monthValue)

    /** Validation shared by the custom statistics picker and its confirm path. */
    fun historicalRangeOrNull(
        startInclusive: LocalDate,
        endInclusive: LocalDate,
        today: LocalDate,
    ): DateRange? = if (
        startInclusive.isAfter(today) ||
        endInclusive.isAfter(today) ||
        endInclusive.isBefore(startInclusive)
    ) {
        null
    } else {
        DateRange(startInclusive, endInclusive)
    }

    /**
     * 返回 null 表示合法；否则返回面向用户的错误文案。
     * 规则与 AccountingRepository.saveCategory 保持一致（1~20 字、同级不重名、忽略大小写），
     * 存储层的 require 仍是最终防线。
     */
    fun validateCategoryName(name: String?, existingSiblingNames: List<String> = emptyList()): String? {
        val normalized = name.orEmpty().trim()
        if (normalized.isEmpty()) return "请输入名称"
        if (normalized.length > 20) return "名称最长 20 个字"
        if (existingSiblingNames.any { it.trim().equals(normalized, ignoreCase = true) }) return "该细分已存在"
        return null
    }

    /** Keeps a mobile donut legible while preserving every category in Other. */
    fun categoryChartSlices(
        totals: List<CategoryTotal>,
        maxVisible: Int = MAX_VISIBLE_CATEGORIES,
        smallRatio: Float = SMALL_CATEGORY_RATIO,
    ): List<CategoryChartSlice> {
        require(maxVisible > 0)
        val sorted = totals.sortedByDescending(CategoryTotal::amountCents)
        val visible = mutableListOf<CategoryTotal>()
        val other = mutableListOf<CategoryTotal>()
        sorted.forEach { total ->
            if (visible.size < maxVisible && total.ratio >= smallRatio) visible += total else other += total
        }
        return buildList {
            visible.forEach { total ->
                add(CategoryChartSlice(total.categoryId, total.amountCents, total.ratio, setOf(total.categoryId)))
            }
            if (other.isNotEmpty()) {
                add(
                    CategoryChartSlice(
                        categoryId = null,
                        amountCents = other.sumOf(CategoryTotal::amountCents),
                        ratio = other.sumOf { it.ratio.toDouble() }.toFloat(),
                        sourceCategoryIds = other.mapTo(linkedSetOf(), CategoryTotal::categoryId),
                    ),
                )
            }
        }
    }

    /**
     * 预算三模式统一计算（三期 3.3）。主金额 amountCents 的语义按 mode 解释，
     * 任何新代码不得直接读 amountCents 做跨模式计算——一律先走本函数（共享约定 2）。
     *
     * 模式 A（先存后花）：limit = income − savingsGoal（可为负）；limit ≤ 0 时
     *   usageRatio = null 且 overGoal = true（D6 边界，UI 显示引导语不显示负数）。
     * 模式 B（整月上限）：limit = amountCents；gauge 的 .85 提示文案由 UI 层处理。
     * 模式 C（分类预算）：rows 按额度降序；已花从 summary.categoryTotals 取
     *   （summarize 按一级 categoryId 分组，天然含该分类下所有细分账单，D8）；
     *   总进度 = Σspent(已设额度分类) / Σlimit，提醒用同一 80%/100% 阈值（D9）。
     */
    fun budgetEvaluation(budget: MonthlyBudget, monthSummary: FinanceSummary): BudgetEvaluation {
        val income = monthSummary.incomeCents
        val expense = monthSummary.expenseCents
        val balance = monthSummary.balanceCents
        return when (budget.mode) {
            BudgetMode.SAVINGS_GOAL -> {
                val limit = income - budget.savingsGoalCents
                val overGoal = limit <= 0L
                val usage = if (overGoal) null else expense.toFloat() / limit
                BudgetEvaluation(
                    mode = BudgetMode.SAVINGS_GOAL,
                    limitCents = limit,
                    spentCents = expense,
                    usageRatio = usage,
                    reminderLevel = reminderLevel(usage),
                    categoryRows = emptyList(),
                    incomeCents = income,
                    expenseCents = expense,
                    balanceCents = balance,
                    overGoal = overGoal,
                )
            }
            BudgetMode.MONTHLY_CAP -> {
                val usage = expense.toFloat() / budget.amountCents
                BudgetEvaluation(
                    mode = BudgetMode.MONTHLY_CAP,
                    limitCents = budget.amountCents,
                    spentCents = expense,
                    usageRatio = usage,
                    reminderLevel = reminderLevel(usage),
                    categoryRows = emptyList(),
                    incomeCents = income,
                    expenseCents = expense,
                    balanceCents = balance,
                    overGoal = false,
                )
            }
            BudgetMode.CATEGORY -> {
                val rows = budget.categoryBudgets
                    .map { (categoryId, limitCents) ->
                        val spent = monthSummary.categoryTotals
                            .firstOrNull { it.categoryId == categoryId }?.amountCents ?: 0L
                        CategoryBudgetRow(
                            categoryId = categoryId,
                            spentCents = spent,
                            limitCents = limitCents,
                            ratio = if (limitCents <= 0L) 0f else spent.toFloat() / limitCents,
                        )
                    }
                    .sortedByDescending(CategoryBudgetRow::limitCents)
                val limitTotal = rows.sumOf(CategoryBudgetRow::limitCents)
                val spentTotal = rows.sumOf(CategoryBudgetRow::spentCents)
                val usage = if (limitTotal <= 0L) null else spentTotal.toFloat() / limitTotal
                BudgetEvaluation(
                    mode = BudgetMode.CATEGORY,
                    limitCents = null,
                    spentCents = expense,
                    usageRatio = usage,
                    reminderLevel = reminderLevel(usage),
                    categoryRows = rows,
                    incomeCents = income,
                    expenseCents = expense,
                    balanceCents = balance,
                    overGoal = false,
                )
            }
        }
    }

    private fun reminderLevel(usageRatio: Float?): BudgetReminder = when {
        usageRatio == null -> BudgetReminder.NONE
        usageRatio >= BUDGET_REMINDER_EXCEEDED -> BudgetReminder.EXCEEDED
        usageRatio >= BUDGET_REMINDER_NOTICE -> BudgetReminder.NOTICE
        else -> BudgetReminder.NONE
    }

}

/** 预算提醒两档（拍板 1）：唯一阈值来源在 FinanceRules，UI 不得自写。 */
enum class BudgetReminder { NONE, NOTICE, EXCEEDED }

/** 分类预算行：spent 从 FinanceSummary.categoryTotals 取（一级分类口径，与统计页一致，D8）。 */
data class CategoryBudgetRow(
    val categoryId: String,
    val spentCents: Long,
    val limitCents: Long,
    val ratio: Float,
)

data class BudgetEvaluation(
    val mode: BudgetMode,
    val limitCents: Long?,
    val spentCents: Long,
    val usageRatio: Float?,
    val reminderLevel: BudgetReminder,
    val categoryRows: List<CategoryBudgetRow>,
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long,
    val overGoal: Boolean,
) {
    /** 已设额度的分类的额度合计（仅模式 C 有意义）。 */
    val categoryLimitTotalCents: Long get() = categoryRows.sumOf(CategoryBudgetRow::limitCents)
}

fun Long.formatYuan(showSymbol: Boolean = true): String {
    val value = BigDecimal(this).movePointLeft(2).setScale(2)
    return (if (showSymbol) "¥" else "") + value.toPlainString()
}
