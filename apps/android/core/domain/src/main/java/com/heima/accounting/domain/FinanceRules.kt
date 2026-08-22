package com.heima.accounting.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object FinanceRules {
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

    fun monthKey(date: LocalDate): String = "%04d-%02d".format(date.year, date.monthValue)

}

fun Long.formatYuan(showSymbol: Boolean = true): String {
    val value = BigDecimal(this).movePointLeft(2).setScale(2)
    return (if (showSymbol) "¥" else "") + value.toPlainString()
}
