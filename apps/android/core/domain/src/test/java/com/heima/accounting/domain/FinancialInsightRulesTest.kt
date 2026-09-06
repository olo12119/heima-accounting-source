package com.heima.accounting.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialInsightRulesTest {
    private val today = LocalDate.of(2026, 8, 22)
    private val month = YearMonth.of(2026, 8)

    @Test
    fun `too few real records produce insufficient report with guidance text`() {
        val snapshot = snapshot(
            listOf(
                expense(1, 1_200),
                expense(2, 2_000),
                income(3, 50_000),
                expense(4, 900),
            ),
        )

        val report = FinancialInsightRules.evaluateHealth(snapshot, month, today)

        assertEquals(FinancialInsightLevel.INSUFFICIENT, report.level)
        assertNull(report.savingRate)
        assertNull(report.concentration)
        assertNull(report.monthOverMonth)
        assertTrue(report.summaryText.contains("记几笔账"))
    }

    @Test
    fun `high saving rate is graded good and summary mentions it`() {
        // 收入 1000 元、支出 200 元 → 结余率 80%（GOOD）；支出全部集中在一个分类（POOR）。
        val records = listOf(
            income(1, 60_000),
            expense(2, 5_000),
            expense(3, 5_000),
            expense(4, 4_000),
            expense(5, 3_000),
            expense(6, 3_000),
        )
        val report = FinancialInsightRules.evaluateHealth(snapshot(records), month, today)

        val savingRate = assertNotNull(report.savingRate)
        assertEquals(HealthGrade.GOOD, savingRate.grade)
        assertEquals("67%", savingRate.displayValue)
        assertEquals(2.0 / 3, savingRate.progress.toDouble(), 1e-6)
        assertTrue(savingRate.text.contains("存下了收入的67%"))
        val concentration = assertNotNull(report.concentration)
        assertEquals(HealthGrade.POOR, concentration.grade)
        assertEquals("100%", concentration.displayValue)
        assertTrue(concentration.text.contains("开销很集中"))
    }

    @Test
    fun `savings progress uses the savings goal when budget mode is savings goal`() {
        // 收入 1000 元、支出 500 元 → 结余 500 元；储蓄目标 500 元 → 完成 100%（GOOD）。
        val records = listOf(
            income(1, 100_000),
            expense(2, 10_000),
            expense(3, 10_000),
            expense(4, 10_000),
            expense(5, 10_000),
            expense(6, 10_000),
        )
        val report = FinancialInsightRules.evaluateHealth(
            snapshot(records, budget = MonthlyBudget("2026-08", 50_000L, BudgetMode.SAVINGS_GOAL, savingsGoalCents = 50_000L)),
            month,
            today,
        )

        val progress = assertNotNull(report.savingsProgress)
        assertEquals(HealthGrade.GOOD, progress.grade)
        assertEquals("100%", progress.displayValue)
        assertTrue(progress.text.contains("储蓄目标已完成100%"))
        assertEquals(1f, progress.progress)
    }

    @Test
    fun `missing savings goal degrades to saving rate reference row`() {
        val records = listOf(
            income(1, 100_000),
            expense(2, 25_000),
            expense(3, 25_000),
            expense(4, 20_000),
            expense(5, 20_000),
            expense(6, 5_000),
        )
        // 整月上限模式：无储蓄目标 → 退化行复用结余率口径（结余率 5% → POOR）。
        val report = FinancialInsightRules.evaluateHealth(
            snapshot(records, budget = MonthlyBudget("2026-08", 100_000L)),
            month,
            today,
        )

        val progress = assertNotNull(report.savingsProgress)
        assertEquals(HealthMetricKey.SAVINGS_PROGRESS, progress.key)
        assertEquals("结余率", progress.displayValue)
        assertTrue(progress.text.contains("未设储蓄目标"))
        val savingRate = assertNotNull(report.savingRate)
        assertEquals(savingRate.grade, progress.grade)
        assertEquals(savingRate.progress, progress.progress)
    }

    @Test
    fun `zero income yields not available saving rate without nan`() {
        val records = (1L..5L).map { expense(it, 5_000) }
        val report = FinancialInsightRules.evaluateHealth(snapshot(records), month, today)

        val savingRate = assertNotNull(report.savingRate)
        assertEquals(HealthGrade.N_A, savingRate.grade)
        assertEquals("—", savingRate.displayValue)
        assertEquals(0f, savingRate.progress)
        assertTrue(savingRate.text.contains("暂时算不出结余率"))
    }

    @Test
    fun `month over month compares against the same day of previous month`() {
        // 本月 1~5 日各花 100 元（截至 22 日共 500 元）；上月 1~5 日各花 200 元（同期 1000 元）→ 少花 50%。
        val current = (1L..5L).map { expense(it, 100, today.minusDays(it - 1)) }
        val history = (6L..10L).map { expense(it, 200, LocalDate.of(2026, 7, 1).plusDays(it - 6L)) }
        val report = FinancialInsightRules.evaluateHealth(snapshot(current + history), month, today)

        val monthOverMonth = assertNotNull(report.monthOverMonth)
        assertEquals(HealthGrade.GOOD, monthOverMonth.grade)
        assertTrue(monthOverMonth.text.contains("少花"))
        assertTrue(monthOverMonth.displayValue.contains("-"))
    }

    @Test
    fun `no previous month expense is reported as not available`() {
        val records = (1L..5L).map { expense(it, 10_000) }
        val report = FinancialInsightRules.evaluateHealth(snapshot(records), month, today)

        val monthOverMonth = assertNotNull(report.monthOverMonth)
        assertEquals(HealthGrade.N_A, monthOverMonth.grade)
        assertTrue(monthOverMonth.text.contains("没有支出记录"))
    }

    @Test
    fun `big spending spike versus last month raises attention`() {
        val current = (1L..5L).map { expense(it, 20_000, today.minusDays(it - 1)) }
        val history = (6L..10L).map { expense(it, 4_000, LocalDate.of(2026, 7, 10 + (it - 6L).toInt())) }
        val report = FinancialInsightRules.evaluateHealth(snapshot(current + history), month, today)

        val monthOverMonth = assertNotNull(report.monthOverMonth)
        assertEquals(HealthGrade.POOR, monthOverMonth.grade)
        assertEquals("+400%", monthOverMonth.displayValue)
        // 集中度（100% 单分类）与环比两个维度均为 POOR → HIGH_PRESSURE。
        assertEquals(FinancialInsightLevel.HIGH_PRESSURE, report.level)
    }

    @Test
    fun `saving rate grade boundaries match the architecture table`() {
        // 架构阈值表：>30% 好 / 10%~30% 中 / <10%（含负）差，逐分验证边界
        fun report(expense: Long): FinanceHealthReport = FinancialInsightRules.evaluateHealth(
            snapshot(listOf(income(99L, 100_000)) + expensesExact(expense)),
            month,
            today,
        )
        assertEquals(HealthGrade.MEDIUM, report(70_000).savingRate?.grade) // 恰 30% → 中
        assertEquals(HealthGrade.GOOD, report(69_999).savingRate?.grade)   // 30%+1分 → 好
        assertEquals(HealthGrade.MEDIUM, report(90_000).savingRate?.grade) // 恰 10% → 中
        assertEquals(HealthGrade.POOR, report(90_001).savingRate?.grade)   // 10%−1分 → 差
    }

    @Test
    fun `concentration grade boundaries match the architecture table`() {
        // 架构阈值表：<40% 好（分散）/ 40%~60% 中 / >60% 差，逐分验证边界
        val categories = listOf(
            Category("c_food", EntryType.EXPENSE, "餐饮", "meal", 1L),
            Category("c_transport", EntryType.EXPENSE, "交通", "car", 2L),
            Category("c_play", EntryType.EXPENSE, "娱乐", "game", 3L),
        )
        fun report(top: Long, otherA: Long, otherB: Long): FinanceHealthReport =
            FinancialInsightRules.evaluateHealth(
                LedgerSnapshot(categories, mixedExpenses("c_food" to top, "c_transport" to otherA, "c_play" to otherB)),
                month,
                today,
            )
        assertEquals(HealthGrade.MEDIUM, report(40_000, 30_000, 30_000).concentration?.grade) // 恰 40% → 中
        assertEquals(HealthGrade.GOOD, report(39_999, 30_000, 30_000).concentration?.grade)   // 40%−1分 → 好
        assertEquals(HealthGrade.MEDIUM, report(60_000, 20_000, 20_000).concentration?.grade) // 恰 60% → 中
        assertEquals(HealthGrade.POOR, report(60_001, 20_000, 20_000).concentration?.grade)   // 60%+1分 → 差
    }

    @Test
    fun `savings progress grade boundaries match the architecture table`() {
        // 架构阈值表：≥100% 好 / 50%~100% 中 / <50%（含负）差；结余 = 100_000 − 支出
        fun report(expense: Long, goal: Long): FinanceHealthReport = FinancialInsightRules.evaluateHealth(
            snapshot(
                listOf(income(99L, 100_000)) + expensesExact(expense),
                budget = MonthlyBudget("2026-08", goal, BudgetMode.SAVINGS_GOAL, savingsGoalCents = goal),
            ),
            month,
            today,
        )
        assertEquals(HealthGrade.MEDIUM, report(75_000, 50_000).savingsProgress?.grade) // 完成 50% 恰 → 中
        assertEquals(HealthGrade.POOR, report(75_001, 50_000).savingsProgress?.grade)   // 50%−1分 → 差
        assertEquals(HealthGrade.POOR, report(120_000, 50_000).savingsProgress?.grade)  // 负完成度 → 差
        assertTrue(report(120_000, 50_000).savingsProgress?.text?.contains("还没存下钱") == true)
        assertEquals(HealthGrade.GOOD, report(50_000, 50_000).savingsProgress?.grade)   // 完成 100% → 好
    }

    @Test
    fun `month over month grade boundaries match the architecture table`() {
        // 架构阈值表：≤0% 好（持平或下降）/ 0~20% 中 / >20% 差，逐分验证边界
        val previous = (6L..10L).map { expense(it, 10_000, LocalDate.of(2026, 7, 1).plusDays(it - 6L)) } // 上月同期 50_000
        fun current(vararg dailyCents: Long) = dailyCents.mapIndexed { index, cents ->
            expense(100L + index, cents, today.minusDays(dailyCents.size - 1L - index))
        }
        assertEquals(
            HealthGrade.GOOD,
            FinancialInsightRules.evaluateHealth(snapshot(current(10_000, 10_000, 10_000, 10_000, 10_000) + previous), month, today).monthOverMonth?.grade, // 持平 0%
        )
        assertEquals(
            HealthGrade.MEDIUM,
            FinancialInsightRules.evaluateHealth(snapshot(current(12_000, 12_000, 12_000, 12_000, 12_000) + previous), month, today).monthOverMonth?.grade, // +20% 恰 → 中
        )
        assertEquals(
            HealthGrade.POOR,
            FinancialInsightRules.evaluateHealth(snapshot(current(12_001, 12_000, 12_000, 12_000, 12_000) + previous), month, today).monthOverMonth?.grade, // +20%+1分 → 差
        )
        assertEquals(
            HealthGrade.GOOD,
            FinancialInsightRules.evaluateHealth(snapshot(current(5_000, 5_000, 5_000, 5_000, 5_000) + previous), month, today).monthOverMonth?.grade, // 下降
        )
    }

    private fun snapshot(transactions: List<Transaction>, budget: MonthlyBudget? = null): LedgerSnapshot = LedgerSnapshot(
        categories = listOf(Category("expense_food", EntryType.EXPENSE, "餐饮", "meal", 0xFFF2A65A)),
        transactions = transactions,
        budgets = budget?.let(::listOf).orEmpty(),
    )

    private fun expense(id: Long, cents: Long, date: LocalDate = today): Transaction = transaction(id, EntryType.EXPENSE, cents, date)

    private fun income(id: Long, cents: Long, date: LocalDate = today): Transaction = transaction(id, EntryType.INCOME, cents, date)

    /** 把 total 分成 count 笔、合计严格等于 total（便于验证差 1 分的阈值边界）。 */
    private fun expensesExact(total: Long, count: Int = 5): List<Transaction> {
        val base = total / count
        val remainder = total % count
        return (1L..count).map { expense(it, base + if (it <= remainder) 1L else 0L) }
    }

    /** 每个分类各拆 2 笔、合计严格等于给定额度，用于集中度边界测试。 */
    private fun mixedExpenses(vararg categoryTotals: Pair<String, Long>): List<Transaction> {
        var id = 100L
        return categoryTotals.flatMap { (categoryId, total) ->
            val base = total / 2
            val remainder = total % 2
            (1..2).map {
                Transaction(
                    id = id++,
                    type = EntryType.EXPENSE,
                    amountCents = base + if (it <= remainder) 1L else 0L,
                    categoryId = categoryId,
                    occurredAtEpochMillis = today.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            }
        }
    }

    private fun transaction(id: Long, type: EntryType, cents: Long, date: LocalDate): Transaction = Transaction(
        id = id,
        type = type,
        amountCents = cents,
        categoryId = "expense_food",
        occurredAtEpochMillis = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
}

/** 与 Kotlin stdlib 的 assertNotNull 不同：这里直接返回非空值，简化断言书写。 */
private fun <T> assertNotNull(value: T?): T {
    org.junit.Assert.assertNotNull(value)
    return value!!
}
