package com.heima.accounting.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun categoryNameRejectsBlankAndWhitespaceOnlyInput() {
        assertEquals("请输入名称", FinanceRules.validateCategoryName(null))
        assertEquals("请输入名称", FinanceRules.validateCategoryName(""))
        assertEquals("请输入名称", FinanceRules.validateCategoryName("   "))
        assertEquals("请输入名称", FinanceRules.validateCategoryName("　　　"))
        // 首尾空格不计入有效内容：纯空格即使超过 20 个字符也优先报空
        assertEquals("请输入名称", FinanceRules.validateCategoryName(" ".repeat(25)))
    }

    @Test fun categoryNameRejectsMoreThanTwentyCharactersAfterTrim() {
        assertEquals("名称最长 20 个字", FinanceRules.validateCategoryName("一".repeat(21)))
        assertEquals("名称最长 20 个字", FinanceRules.validateCategoryName("abcdefghijklmnopqrstu"))
        // 边界：恰好 20 字合法，且首尾空格不计入长度
        assertNull(FinanceRules.validateCategoryName("一".repeat(20)))
        assertNull(FinanceRules.validateCategoryName("  " + "一".repeat(20) + "  "))
    }

    @Test fun categoryNameRejectsDuplicateSiblingIgnoringCaseAndSurroundingSpaces() {
        val siblings = listOf("早餐", "Taxi", "  地铁  ")
        assertEquals("该细分已存在", FinanceRules.validateCategoryName("早餐", siblings))
        assertEquals("该细分已存在", FinanceRules.validateCategoryName("taxi", siblings))
        assertEquals("该细分已存在", FinanceRules.validateCategoryName("TAXI", siblings))
        assertEquals("该细分已存在", FinanceRules.validateCategoryName(" 地铁 ", siblings))
        // 新名称自身带首尾空格时按 trim 后比较
        assertEquals("该细分已存在", FinanceRules.validateCategoryName("  Taxi  ", siblings))
    }

    @Test fun categoryNameAcceptsValidUniqueNames() {
        assertNull(FinanceRules.validateCategoryName("午餐"))
        assertNull(FinanceRules.validateCategoryName("午餐", listOf("早餐", "晚餐")))
        // 仅大小写不同不算合法——只有真正不同的名字才通过
        assertNull(FinanceRules.validateCategoryName("Metro", listOf("Taxi")))
        assertNull(FinanceRules.validateCategoryName("  网购  ", listOf("外卖")))
    }

    @Test fun categoryNameChecksEmptinessBeforeLengthBeforeDuplicate() {
        val longDuplicate = "一".repeat(21)
        assertEquals("名称最长 20 个字", FinanceRules.validateCategoryName(longDuplicate, listOf(longDuplicate)))
    }

    @Test fun monthlyCapUsageFollowsEightyAndHundredPercentThresholds() {
        val budget = MonthlyBudget("2026-08", 50_000L)
        fun evaluation(expense: Long) = FinanceRules.budgetEvaluation(
            budget,
            FinanceSummary(expenseCents = expense, incomeCents = 100_000L),
        )
        // 差 1 分边界：80% 注意 / 100% 超限（拍板 1）
        assertEquals(BudgetReminder.NONE, evaluation(39_999L).reminderLevel)
        assertEquals(BudgetReminder.NOTICE, evaluation(40_000L).reminderLevel)
        assertEquals(BudgetReminder.NOTICE, evaluation(49_999L).reminderLevel)
        assertEquals(BudgetReminder.EXCEEDED, evaluation(50_000L).reminderLevel)
        assertEquals(BudgetReminder.NONE, evaluation(0L).reminderLevel)
        val at80 = evaluation(40_000L)
        assertEquals(BudgetMode.MONTHLY_CAP, at80.mode)
        assertEquals(50_000L, at80.limitCents)
        assertEquals(40_000L, at80.spentCents)
        assertEquals(0.8f, at80.usageRatio!!, 1e-5f)
        assertEquals(60_000L, at80.balanceCents)
        assertFalse(at80.overGoal)
        assertTrue(at80.categoryRows.isEmpty())
    }

    @Test fun savingsGoalLimitIsIncomeMinusGoalNotTheStoredMainAmount() {
        // 主金额语义映射表：SAVINGS_GOAL 的 amountCents 仅为满足 DB CHECK 的冗余，
        // 计算一律读 savingsGoalCents——这里故意让两者不同来验证。
        val budget = MonthlyBudget("2026-08", 30_000L, BudgetMode.SAVINGS_GOAL, savingsGoalCents = 30_000L)
        fun evaluation(income: Long, expense: Long) = FinanceRules.budgetEvaluation(
            budget,
            FinanceSummary(expenseCents = expense, incomeCents = income),
        )
        val mid = evaluation(100_000L, 56_000L)
        assertEquals(70_000L, mid.limitCents) // 100_000 − 30_000，而非 amountCents 的 30_000
        assertEquals(0.8f, mid.usageRatio!!, 1e-5f)
        assertEquals(BudgetReminder.NOTICE, mid.reminderLevel)
        assertEquals(BudgetReminder.EXCEEDED, evaluation(100_000L, 70_000L).reminderLevel)
        assertEquals(BudgetReminder.NOTICE, evaluation(100_000L, 69_999L).reminderLevel) // 99.998% 仍在注意档
        assertEquals(BudgetReminder.NONE, evaluation(100_000L, 55_999L).reminderLevel)   // 79.998% 恰低于 80% 档
        val basic = evaluation(100_000L, 56_000L)
        assertTrue(basic.categoryRows.isEmpty())
        assertFalse(basic.overGoal)
    }

    @Test fun savingsGoalIncomeAtOrBelowGoalMarksOverGoalWithoutNegativeUsage() {
        fun evaluation(goal: Long, income: Long, expense: Long = 10_000L) = FinanceRules.budgetEvaluation(
            MonthlyBudget("2026-08", goal, BudgetMode.SAVINGS_GOAL, savingsGoalCents = goal),
            FinanceSummary(expenseCents = expense, incomeCents = income),
        )
        // 收入 < 储蓄目标（D6）：limitCents 为负值保留真实账，但 usage=null、overGoal=true、reminder=NONE
        val below = evaluation(60_000L, 50_000L)
        assertTrue(below.overGoal)
        assertEquals(-10_000L, below.limitCents)
        assertNull(below.usageRatio)
        assertEquals(BudgetReminder.NONE, below.reminderLevel)
        // D6 边界：收入恰等于储蓄目标 → 仍判 overGoal
        val equal = evaluation(50_000L, 50_000L)
        assertTrue(equal.overGoal)
        assertEquals(0L, equal.limitCents)
        assertNull(equal.usageRatio)
        // 收入多出 1 分即恢复正常模式：无支出 → usage 0 → NONE
        val above = evaluation(50_000L, 50_001L, expense = 0L)
        assertFalse(above.overGoal)
        assertEquals(1L, above.limitCents)
        assertEquals(0f, above.usageRatio!!, 1e-5f)
        assertEquals(BudgetReminder.NONE, above.reminderLevel)
    }

    @Test fun categoryBudgetAggregatesOnlyBudgetedCategoriesAndSharesThresholds() {
        val budget = MonthlyBudget(
            "2026-08",
            50_000L,
            BudgetMode.CATEGORY,
            categoryBudgets = mapOf("food" to 30_000L, "transport" to 20_000L),
        )
        fun evaluation(food: Long, transport: Long, unbudgeted: Long) = FinanceRules.budgetEvaluation(
            budget,
            FinanceSummary(
                expenseCents = food + transport + unbudgeted,
                incomeCents = 100_000L,
                categoryTotals = listOf(
                    CategoryTotal("food", food, 0f),
                    CategoryTotal("transport", transport, 0f),
                    CategoryTotal("play", unbudgeted, 0f),
                ),
            ),
        )
        val normal = evaluation(24_000L, 10_000L, 5_000L)
        // rows 按额度降序（D8：未设额度分类不出行）
        assertEquals(listOf("food", "transport"), normal.categoryRows.map(CategoryBudgetRow::categoryId))
        assertEquals(30_000L, normal.categoryRows.first().limitCents)
        assertEquals(24_000L, normal.categoryRows.first().spentCents)
        assertEquals(0.8f, normal.categoryRows.first().ratio, 1e-5f)
        // 总进度只算已设额度分类：Σspent 34_000 / Σlimit 50_000 = 68%（未设的 play 5_000 不计入，D9）
        assertEquals(0.68f, normal.usageRatio!!, 1e-5f)
        assertEquals(BudgetReminder.NONE, normal.reminderLevel)
        assertNull(normal.limitCents)
        assertEquals(39_000L, normal.expenseCents) // 24_000 + 10_000 + 5_000
        assertFalse(normal.overGoal)
        // 阈值与 A/B 模式同源：80%/100% 按"已设额度分类"口径
        assertEquals(BudgetReminder.NOTICE, evaluation(30_000L, 10_000L, 9_999L).reminderLevel)
        assertEquals(BudgetReminder.EXCEEDED, evaluation(30_000L, 20_000L, 5_000L).reminderLevel)
    }

    @Test fun categoryBudgetGuardsAgainstEmptyMapAndNonPositiveLimits() {
        // 空 map：仓储层虽拒绝保存，计算层仍须防护（不崩溃、usage=null、reminder=NONE）
        val empty = FinanceRules.budgetEvaluation(
            MonthlyBudget("2026-08", 1L, BudgetMode.CATEGORY, categoryBudgets = emptyMap()),
            FinanceSummary(expenseCents = 10_000L),
        )
        assertTrue(empty.categoryRows.isEmpty())
        assertNull(empty.usageRatio)
        assertEquals(BudgetReminder.NONE, empty.reminderLevel)
        // limit ≤ 0 的行（仓储层已拒绝保存，此为纯防御路径）：ratio 记 0、不崩溃、
        // 总进度 = Σspent(全部行)/Σlimit(全部行) = 9_000/20_000 = 45%（实现口径，确定性输出）
        val withZero = FinanceRules.budgetEvaluation(
            MonthlyBudget(
                "2026-08",
                20_001L,
                BudgetMode.CATEGORY,
                categoryBudgets = mapOf("food" to 0L, "transport" to 20_000L),
            ),
            FinanceSummary(
                expenseCents = 9_000L,
                categoryTotals = listOf(CategoryTotal("food", 5_000L, 0f), CategoryTotal("transport", 4_000L, 0f)),
            ),
        )
        assertEquals(0f, withZero.categoryRows.first { it.categoryId == "food" }.ratio)
        assertEquals(20_000L, withZero.categoryLimitTotalCents)
        assertEquals(0.45f, withZero.usageRatio!!, 1e-5f)
        assertEquals(BudgetReminder.NONE, withZero.reminderLevel)
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
