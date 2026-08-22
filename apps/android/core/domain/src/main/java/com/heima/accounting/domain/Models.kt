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

data class MonthlyBudget(
    val month: String,
    val amountCents: Long,
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

enum class StatisticsPeriod { TODAY, WEEK, MONTH, YEAR, ALL }

data class DateRange(
    val startInclusive: LocalDate,
    val endInclusive: LocalDate,
) {
    init { require(!endInclusive.isBefore(startInclusive)) }
    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(startInclusive) && !date.isAfter(endInclusive)
}

