package com.heima.accounting.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class EntryType { EXPENSE, INCOME }

data class Category(
    val id: String,
    val type: EntryType,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val parentId: String? = null,
    val isCustom: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
)

data class Transaction(
    val id: Long = 0L,
    val type: EntryType,
    val amountCents: Long,
    val categoryId: String,
    val subcategoryId: String? = null,
    val note: String = "",
    val occurredAtEpochMillis: Long,
    val excludedFromStatistics: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    fun localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(occurredAtEpochMillis).atZone(zoneId).toLocalDate()
}

/**
 * 预算三模式（三期 3.3）：
 * SAVINGS_GOAL = 先存后花（默认）；MONTHLY_CAP = 整月上限；CATEGORY = 分类预算。
 * [MonthlyBudget.amountCents] 是受 DB CHECK(>0) 约束的"主金额"，按 mode 解释：
 * MONTHLY_CAP → 整月上限；SAVINGS_GOAL → 冗余存 savingsGoalCents；
 * CATEGORY → 冗余存"已设分类额度合计"。跨模式计算一律走 FinanceRules.budgetEvaluation()。
 */
enum class BudgetMode { SAVINGS_GOAL, MONTHLY_CAP, CATEGORY }

data class MonthlyBudget(
    val month: String,
    val amountCents: Long,
    val mode: BudgetMode = BudgetMode.MONTHLY_CAP,
    val savingsGoalCents: Long = 0L,
    val categoryBudgets: Map<String, Long> = emptyMap(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

data class LedgerSnapshot(
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<MonthlyBudget> = emptyList(),
) {
    fun topLevelCategories(type: EntryType): List<Category> = categories
        .filter { it.type == type && it.parentId == null && it.isActive }
        .sortedBy(Category::sortOrder)

    fun childCategories(parentId: String): List<Category> = categories
        .filter { it.parentId == parentId && it.isActive }
        .sortedBy(Category::sortOrder)

    fun category(id: String?): Category? = categories.firstOrNull { it.id == id }
}

data class CategoryTotal(
    val categoryId: String,
    val amountCents: Long,
    val ratio: Float,
)

data class DailyTotal(
    val date: LocalDate,
    val expenseCents: Long,
    val incomeCents: Long,
)

data class FinanceSummary(
    val expenseCents: Long = 0L,
    val incomeCents: Long = 0L,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val dailyTotals: List<DailyTotal> = emptyList(),
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class StatisticsResult(
    val summary: FinanceSummary = FinanceSummary(),
    val transactions: List<Transaction> = emptyList(),
)

data class CategoryChartSlice(
    val categoryId: String?,
    val amountCents: Long,
    val ratio: Float,
    val sourceCategoryIds: Set<String>,
) {
    val isOther: Boolean get() = categoryId == null
}

enum class StatisticsPeriod { TODAY, WEEK, MONTH, YEAR, ALL }

data class DateRange(
    val startInclusive: LocalDate,
    val endInclusive: LocalDate,
) {
    init { require(!endInclusive.isBefore(startInclusive)) }
    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(startInclusive) && !date.isAfter(endInclusive)
}
