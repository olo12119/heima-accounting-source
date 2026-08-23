package com.heima.accounting.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRulesTest {
    @Test fun amountParsingUsesIntegerCents() {
        assertEquals(1_250L, FinanceRules.parseYuanToCents("12.50"))
        assertEquals(5L, FinanceRules.parseYuanToCents("0.05"))
        assertEquals(99_999_999_999L, FinanceRules.parseYuanToCents("999999999.99"))
        // Parsing and business validation are intentionally separate: the parser can
        // represent zero exactly, while RecordSheet/repository reject saving it.
        assertEquals(0L, FinanceRules.parseYuanToCents("0"))
        assertEquals(0L, FinanceRules.parseYuanToCents("0.00"))
        assertNull(FinanceRules.parseYuanToCents("12.345"))
        assertNull(FinanceRules.parseYuanToCents("-1"))
    }

    @Test fun amountKeyboardKeepsNaturalYuanInput() {
        var amount = ""
        listOf("1", "2", ".", "5", "0").forEach { amount = FinanceRules.appendAmount(amount, it) }
        assertEquals("12.50", amount)
        assertEquals("0.", FinanceRules.appendAmount("", "."))
        assertEquals("12.50", FinanceRules.appendAmount("12.50", "9"))
    }

    @Test fun weekAlwaysRunsMondayThroughSunday() {
        val range = FinanceRules.range(StatisticsPeriod.WEEK, LocalDate.of(2026, 8, 22))
        assertEquals(DayOfWeek.MONDAY, range.startInclusive.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, range.endInclusive.dayOfWeek)
    }

    @Test fun aggregationSeparatesIncomeExpenseAndCategory() {
        val zone = ZoneId.of("Asia/Shanghai")
        val time = LocalDate.of(2026, 8, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = listOf(
            Transaction(type = EntryType.EXPENSE, amountCents = 1200, categoryId = "expense_food", occurredAtEpochMillis = time),
            Transaction(type = EntryType.EXPENSE, amountCents = 800, categoryId = "expense_transport", occurredAtEpochMillis = time),
            Transaction(type = EntryType.INCOME, amountCents = 5000, categoryId = "income_salary", occurredAtEpochMillis = time),
        )
        val summary = FinanceRules.summarize(entries, DateRange(LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 22)), zone)
        assertEquals(2000L, summary.expenseCents)
        assertEquals(5000L, summary.incomeCents)
        assertEquals(1200L, summary.categoryTotals.first().amountCents)
        assertEquals(1, summary.dailyTotals.size)
    }

    @Test fun completeDefaultsContainBothLevelsAndBothTypes() {
        assertTrue(DefaultCategories.all.count { it.type == EntryType.EXPENSE && it.parentId == null } >= 16)
        assertTrue(DefaultCategories.all.count { it.type == EntryType.INCOME && it.parentId == null } >= 7)
        assertTrue(DefaultCategories.all.any { it.parentId == "expense_food" && it.name == "早餐" })
        assertTrue(DefaultCategories.all.any { it.parentId == "income_salary" && it.name == "基本工资" })
        assertEquals(DefaultCategories.all.size, DefaultCategories.all.map(Category::id).distinct().size)
        val byId = DefaultCategories.all.associateBy(Category::id)
        assertTrue(DefaultCategories.all.filter { it.parentId != null }.all { child ->
            byId[child.parentId]?.type == child.type && byId[child.parentId]?.parentId == null
        })
    }

    @Test fun tenThousandTransactionsAggregateWithoutAmountDrift() {
        val zone = ZoneId.of("Asia/Shanghai")
        val time = LocalDate.of(2026, 8, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = List(10_000) { index ->
            Transaction(
                type = if (index % 5 == 0) EntryType.INCOME else EntryType.EXPENSE,
                amountCents = 101L,
                categoryId = if (index % 5 == 0) "income_salary" else "expense_food",
                occurredAtEpochMillis = time,
            )
        }
        val summary = FinanceRules.summarize(
            entries,
            DateRange(LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 22)),
            zone,
        )
        assertEquals(808_000L, summary.expenseCents)
        assertEquals(202_000L, summary.incomeCents)
    }

    @Test fun donutKeepsTopFiveAndAggregatesEveryRemainingCategory() {
        val totals = (1..12).map { index ->
            CategoryTotal(
                categoryId = "category_$index",
                amountCents = (1_300L - index * 75L),
                ratio = if (index <= 5) .12f - index * .006f else .02f,
            )
        }

        val slices = FinanceRules.categoryChartSlices(totals)

        assertEquals(6, slices.size)
        assertEquals(5, slices.count { !it.isOther })
        assertTrue(slices.last().isOther)
        assertEquals(7, slices.last().sourceCategoryIds.size)
        assertEquals(totals.drop(5).sumOf(CategoryTotal::amountCents), slices.last().amountCents)
    }

    @Test fun customDateRangeContainsBothBoundaryDays() {
        val range = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15))
        assertTrue(LocalDate.of(2026, 8, 1) in range)
        assertTrue(LocalDate.of(2026, 8, 15) in range)
        assertTrue(LocalDate.of(2026, 8, 16) !in range)
    }

    @Test fun monthlyTrendKeepsAnEmptyLedgerAsAnEmptyState() {
        val range = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 23))
        assertTrue(FinanceRules.continuousDailyTotals(emptyList(), range).isEmpty())
    }

    @Test fun monthlyTrendFillsZeroDaysOnlyUntilToday() {
        val range = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 23))
        val result = FinanceRules.continuousDailyTotals(
            listOf(DailyTotal(LocalDate.of(2026, 8, 23), expenseCents = 2_200L, incomeCents = 0L)),
            range,
        )

        assertEquals(23, result.size)
        assertEquals(0L, result.first().expenseCents)
        assertEquals(2_200L, result.last().expenseCents)
        assertEquals(LocalDate.of(2026, 8, 23), result.last().date)
    }

    @Test fun monthlyTrendOnTheFirstDayProducesARealSinglePoint() {
        val day = LocalDate.of(2026, 9, 1)
        val result = FinanceRules.continuousDailyTotals(
            listOf(DailyTotal(day, expenseCents = 800L, incomeCents = 0L)),
            DateRange(day, day),
        )

        assertEquals(listOf(DailyTotal(day, 800L, 0L)), result)
    }

    @Test fun monthlyTrendKeepsTwoDaysAndDoesNotLeakAcrossMonthBoundary() {
        val range = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2))
        val result = FinanceRules.continuousDailyTotals(
            listOf(
                DailyTotal(LocalDate.of(2026, 7, 31), expenseCents = 9_900L, incomeCents = 0L),
                DailyTotal(LocalDate.of(2026, 8, 1), expenseCents = 300L, incomeCents = 0L),
                DailyTotal(LocalDate.of(2026, 8, 2), expenseCents = 600L, incomeCents = 0L),
            ),
            range,
        )

        assertEquals(listOf(300L, 600L), result.map(DailyTotal::expenseCents))
        assertEquals(listOf(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)), result.map(DailyTotal::date))
    }

    @Test fun monthlyTrendAggregatesSameDayAndKeepsMiddleZeroDates() {
        val zone = ZoneId.of("Asia/Shanghai")
        val transactions = listOf(
            Transaction(type = EntryType.EXPENSE, amountCents = 500L, categoryId = "expense_food", occurredAtEpochMillis = LocalDate.of(2026, 7, 2).atStartOfDay(zone).toInstant().toEpochMilli()),
            Transaction(type = EntryType.EXPENSE, amountCents = 700L, categoryId = "expense_food", occurredAtEpochMillis = LocalDate.of(2026, 7, 2).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()),
            Transaction(type = EntryType.EXPENSE, amountCents = 900L, categoryId = "expense_food", occurredAtEpochMillis = LocalDate.of(2026, 7, 4).atStartOfDay(zone).toInstant().toEpochMilli()),
        )
        val range = DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4))
        val summary = FinanceRules.summarize(transactions, range, zone)
        val result = FinanceRules.continuousDailyTotals(summary.dailyTotals, range)

        assertEquals(listOf(0L, 1_200L, 0L, 900L), result.map(DailyTotal::expenseCents))
    }

    @Test fun customStatisticsRangeRejectsFutureButAcceptsHistoricalCrossMonthDates() {
        val today = LocalDate.of(2026, 8, 23)
        assertNull(FinanceRules.historicalRangeOrNull(today.plusDays(1), today.plusDays(1), today))
        assertNull(FinanceRules.historicalRangeOrNull(today.minusDays(1), today.plusDays(1), today))
        assertNull(FinanceRules.historicalRangeOrNull(today, today.minusDays(1), today))
        assertEquals(DateRange(today, today), FinanceRules.historicalRangeOrNull(today, today, today))
        assertEquals(
            DateRange(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 2)),
            FinanceRules.historicalRangeOrNull(LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 2), today),
        )
    }
}
