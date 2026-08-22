package com.heima.accounting.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialInsightRulesTest {
    private val today = LocalDate.of(2026, 8, 22)

    @Test
    fun `too few real records produces no judgement`() {
        val snapshot = snapshot(
            listOf(
                expense(1, 1_200),
                expense(2, 2_000),
                income(3, 50_000),
                expense(4, 900),
            ),
        )

        val insight = FinancialInsightRules.evaluate(snapshot, today)

        assertEquals(FinancialInsightLevel.INSUFFICIENT, insight.level)
        assertTrue(insight.explanation.contains("继续记录几笔"))
    }

    @Test
    fun `positive cash flow and normal budget are explained as stable`() {
        val records = listOf(
            income(1, 100_000),
            expense(2, 5_000),
            expense(3, 6_000),
            expense(4, 7_000),
            expense(5, 4_000),
            expense(6, 3_000),
        )
        val snapshot = snapshot(records, budget = 100_000)

        val insight = FinancialInsightRules.evaluate(snapshot, today)

        assertEquals(FinancialInsightLevel.STABLE, insight.level)
        assertTrue(insight.explanation.contains("收入高于支出"))
        assertTrue(insight.explanation.contains("预算使用速度"))
    }

    @Test
    fun `large month over month increase identifies the real category`() {
        val current = (1L..5L).map { expense(it, 20_000, today.minusDays(it - 1)) }
        val history = (6L..10L).map { expense(it, 4_000, LocalDate.of(2026, 7, 10 + (it - 6).toInt())) }
        val insight = FinancialInsightRules.evaluate(snapshot(current + history), today)

        assertEquals(FinancialInsightLevel.ATTENTION, insight.level)
        assertTrue(insight.explanation.contains("过去1个月平均高"))
    }

    @Test
    fun `serious budget overrun is high pressure`() {
        val records = listOf(
            income(1, 60_000),
            expense(2, 30_000),
            expense(3, 25_000),
            expense(4, 20_000),
            expense(5, 15_000),
            expense(6, 10_000),
        )
        val insight = FinancialInsightRules.evaluate(snapshot(records, budget = 80_000), today)

        assertEquals(FinancialInsightLevel.HIGH_PRESSURE, insight.level)
        assertTrue(insight.explanation.contains("预算已使用125%"))
    }

    private fun snapshot(transactions: List<Transaction>, budget: Long? = null): LedgerSnapshot = LedgerSnapshot(
        categories = listOf(Category("expense_food", EntryType.EXPENSE, "餐饮", "meal", 0xFFF2A65A)),
        transactions = transactions,
        budgets = budget?.let { listOf(MonthlyBudget("2026-08", it)) }.orEmpty(),
    )

    private fun expense(id: Long, cents: Long, date: LocalDate = today): Transaction = transaction(id, EntryType.EXPENSE, cents, date)

    private fun income(id: Long, cents: Long, date: LocalDate = today): Transaction = transaction(id, EntryType.INCOME, cents, date)

    private fun transaction(id: Long, type: EntryType, cents: Long, date: LocalDate): Transaction = Transaction(
        id = id,
        type = type,
        amountCents = cents,
        categoryId = "expense_food",
        occurredAtEpochMillis = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
}
