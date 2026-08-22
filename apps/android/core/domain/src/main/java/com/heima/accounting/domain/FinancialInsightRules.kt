package com.heima.accounting.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class FinancialInsightLevel { INSUFFICIENT, STABLE, ATTENTION, HIGH_PRESSURE }

data class FinancialInsight(
    val level: FinancialInsightLevel,
    val title: String,
    val explanation: String,
    val transactionCount: Int,
)

/**
 * Transparent, deterministic rules derived only from the user's local ledger.
 * Thresholds deliberately live here rather than being scattered through UI text.
 */
object FinancialInsightRules {
    const val MINIMUM_CURRENT_TRANSACTIONS = 5
    const val HISTORY_MONTHS = 3
    const val HISTORY_CHANGE_ATTENTION = .20
    const val HISTORY_CHANGE_PRESSURE = .45
    const val CATEGORY_GROWTH_ATTENTION = .35
    const val CATEGORY_MINIMUM_DELTA_CENTS = 10_000L
    const val EXPENSE_INCOME_ATTENTION = .85
    const val EXPENSE_INCOME_PRESSURE = 1.00
    const val BUDGET_USAGE_ATTENTION = .85
    const val BUDGET_USAGE_PRESSURE = 1.10
    const val BUDGET_PACE_MARGIN = .20

    fun evaluate(
        snapshot: LedgerSnapshot,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): FinancialInsight {
        val currentMonth = YearMonth.from(today)
        val current = snapshot.transactions.filter {
            !it.excludedFromStatistics && YearMonth.from(it.localDate(zoneId)) == currentMonth && !it.localDate(zoneId).isAfter(today)
        }
        if (current.size < MINIMUM_CURRENT_TRANSACTIONS) {
            return FinancialInsight(
                FinancialInsightLevel.INSUFFICIENT,
                "数据还在积累",
                "继续记录几笔后，这里会形成更准确的财务分析。",
                current.size,
            )
        }

        val expense = current.filter { it.type == EntryType.EXPENSE }
        val income = current.filter { it.type == EntryType.INCOME }
        val expenseTotal = expense.sumOf(Transaction::amountCents)
        val incomeTotal = income.sumOf(Transaction::amountCents)
        val budget = snapshot.budgets.firstOrNull { it.month == FinanceRules.monthKey(today) }
        val highReasons = mutableListOf<String>()
        val attentionReasons = mutableListOf<String>()
        val stableReasons = mutableListOf<String>()
        var budgetExceeded = false

        if (incomeTotal > 0L) {
            val expenseIncomeRatio = expenseTotal.toDouble() / incomeTotal
            when {
                expenseIncomeRatio >= EXPENSE_INCOME_PRESSURE -> highReasons += "本月支出已达到收入的${percent(expenseIncomeRatio)}"
                expenseIncomeRatio >= EXPENSE_INCOME_ATTENTION -> attentionReasons += "本月支出已达到收入的${percent(expenseIncomeRatio)}"
                else -> stableReasons += "目前本月收入高于支出"
            }
        }

        if (budget != null && budget.amountCents > 0L) {
            val usage = expenseTotal.toDouble() / budget.amountCents
            val elapsed = today.dayOfMonth.toDouble() / currentMonth.lengthOfMonth()
            when {
                usage >= BUDGET_USAGE_PRESSURE -> {
                    budgetExceeded = true
                    highReasons += "本月预算已使用${percent(usage)}"
                }
                usage >= BUDGET_USAGE_ATTENTION || usage > elapsed + BUDGET_PACE_MARGIN ->
                    attentionReasons += "预算使用已到${percent(usage)}，快于本月时间进度"
                else -> stableReasons += "预算使用速度处于正常范围"
            }
        }

        val comparableHistory = (1..HISTORY_MONTHS).mapNotNull { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo.toLong())
            val endDay = minOf(today.dayOfMonth, month.lengthOfMonth())
            val values = snapshot.transactions.filter {
                !it.excludedFromStatistics &&
                    it.type == EntryType.EXPENSE &&
                    YearMonth.from(it.localDate(zoneId)) == month &&
                    it.localDate(zoneId).dayOfMonth <= endDay
            }
            values.takeIf { it.isNotEmpty() }
        }
        if (comparableHistory.isNotEmpty() && expenseTotal > 0L) {
            val average = comparableHistory.map { it.sumOf(Transaction::amountCents).toDouble() }.average()
            if (average > 0.0) {
                val change = expenseTotal / average - 1.0
                when {
                    change >= HISTORY_CHANGE_PRESSURE -> highReasons += "本月同期支出比过去${comparableHistory.size}个月平均高${percent(change)}"
                    change >= HISTORY_CHANGE_ATTENTION -> attentionReasons += "本月同期支出比过去${comparableHistory.size}个月平均高${percent(change)}"
                    abs(change) <= .15 -> stableReasons += "本月支出处于过去同期的正常区间"
                }

                val currentCategoryTotals = expense.groupBy(Transaction::categoryId)
                    .mapValues { (_, items) -> items.sumOf(Transaction::amountCents) }
                // Months with no spending in a category count as zero. Otherwise a
                // sporadic category would get an artificially high historical baseline.
                val historicalCategoryAverage = currentCategoryTotals.keys.associateWith { categoryId ->
                    comparableHistory.map { monthItems ->
                        monthItems.filter { it.categoryId == categoryId }.sumOf(Transaction::amountCents).toDouble()
                    }.average()
                }
                val anomaly = currentCategoryTotals
                    .mapNotNull { (categoryId, total) ->
                        val oldAverage = historicalCategoryAverage[categoryId] ?: return@mapNotNull null
                        val growth = if (oldAverage <= 0.0) 0.0 else total / oldAverage - 1.0
                        if (growth >= CATEGORY_GROWTH_ATTENTION && total - oldAverage >= CATEGORY_MINIMUM_DELTA_CENTS) {
                            Triple(categoryId, growth, total - oldAverage)
                        } else null
                    }
                    .maxByOrNull { it.third }
                if (anomaly != null) {
                    val category = snapshot.category(anomaly.first)?.name ?: "某一分类"
                    attentionReasons += "$category 支出比过去同期平均高${percent(anomaly.second)}"
                }
            }
        }

        val highPressure = budgetExceeded || highReasons.size >= 2
        val reasons = when {
            highPressure -> highReasons + attentionReasons
            highReasons.isNotEmpty() || attentionReasons.isNotEmpty() -> highReasons + attentionReasons
            stableReasons.isNotEmpty() -> stableReasons
            else -> listOf("已根据本月 ${current.size} 笔真实账单完成收支汇总")
        }
        val level = when {
            highPressure -> FinancialInsightLevel.HIGH_PRESSURE
            highReasons.isNotEmpty() || attentionReasons.isNotEmpty() -> FinancialInsightLevel.ATTENTION
            else -> FinancialInsightLevel.STABLE
        }
        val title = when (level) {
            FinancialInsightLevel.INSUFFICIENT -> "数据还在积累"
            FinancialInsightLevel.STABLE -> "本月收支平稳"
            FinancialInsightLevel.ATTENTION -> "有一项变化值得关注"
            FinancialInsightLevel.HIGH_PRESSURE -> "本月资金压力较高"
        }
        return FinancialInsight(level, title, reasons.take(2).joinToString("，") + "。", current.size)
    }

    private fun percent(value: Double): String = "${(value * 100).roundToInt()}%"
}
