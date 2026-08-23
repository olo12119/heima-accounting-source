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
        assertTrue(DefaultCategories.all.count { it.type == EntryType.EXPENSE && it.parentId == null } >= 10)
        assertTrue(DefaultCategories.all.count { it.type == EntryType.INCOME && it.parentId == null } >= 7)
        assertTrue(DefaultCategories.all.any { it.parentId == "expense_food" && it.name == "早餐" })
        assertTrue(DefaultCategories.all.any { it.parentId == "income_salary" && it.name == "基本工资" })
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
}
