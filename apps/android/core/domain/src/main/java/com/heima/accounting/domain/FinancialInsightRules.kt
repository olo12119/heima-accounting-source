package com.heima.accounting.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

enum class FinancialInsightLevel { INSUFFICIENT, STABLE, ATTENTION, HIGH_PRESSURE }

/** 财务体检四维度的分档（三期 3.2，C2：阈值写死在这里供 QA 逐条构造数据）。 */
enum class HealthGrade { GOOD, MEDIUM, POOR, N_A }

enum class HealthMetricKey { SAVING_RATE, CONCENTRATION, SAVINGS_PROGRESS, MONTH_OVER_MONTH }

/**
 * 单个体检指标。value 为原始比率；displayValue 为整数百分比文案；progress 与
 * displayValue 出自同一 ratio（C6：进度条与数值一致），供迷你进度条渲染。
 */
data class HealthMetric(
    val key: HealthMetricKey,
    val value: Double,
    val displayValue: String,
    val grade: HealthGrade,
    val text: String,
    val progress: Float,
)

/**
 * 财务体检报告：4 个维度各自可为 null（仅数据不足时整体为 null 行）。
 * 首页体检卡与统计页小结共用同一口径（同月同 snapshot 计算，C8）。
 */
data class FinanceHealthReport(
    val savingRate: HealthMetric?,
    val concentration: HealthMetric?,
    val savingsProgress: HealthMetric?,
    val monthOverMonth: HealthMetric?,
    val summaryText: String,
    val title: String,
    val level: FinancialInsightLevel,
)

/**
 * Transparent, deterministic rules derived only from the user's local ledger.
 * Thresholds deliberately live here rather than being scattered through UI text.
 */
object FinancialInsightRules {
    const val MINIMUM_CURRENT_TRANSACTIONS = 5

    /** 结余率分档：>30% 好 / 10%~30% 中 / <10%（含负）差 / 收入 0 → N_A。 */
    const val SAVING_RATE_GOOD = .30
    const val SAVING_RATE_MEDIUM = .10

    /** 支出集中度分档：<40% 好（分散）/ 40%~60% 中 / >60% 差（过于集中）/ 支出 0 → N_A。 */
    const val CONCENTRATION_GOOD = .40
    const val CONCENTRATION_MEDIUM = .60

    /** 储蓄进度分档：≥100% 好 / 50%~100% 中 / <50%（含负）差 / 无目标 → 退化行。 */
    const val SAVINGS_PROGRESS_MEDIUM = .50

    /** 环比分档（原 HISTORY_CHANGE_ATTENTION 口径并入本维度）：≤0% 好 / 0~20% 中 / >20% 差。 */
    const val MONTH_OVER_MONTH_MEDIUM = .20

    /** 小结第 3 句的输出门槛：|环比| ≥ 10% 才提。 */
    private const val SUMMARY_MONTH_OVER_MONTH_MIN = .10

    /**
     * 三期 3.2 新入口，取代原 evaluate()（旧"过去 3 个月平均"口径并入环比维度，
     * 改为与"上月同期"单月对比；FinancialInsightLevel 复用，标题语义保持）。
     *
     * @param month  分析的目标自然月（统计页切到上月时传上月）
     * @param today  用于"本月同期"截断未来日期与数据不足判断
     */
    fun evaluateHealth(
        snapshot: LedgerSnapshot,
        month: YearMonth,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): FinanceHealthReport {
        val monthTx = snapshot.transactions.filter {
            !it.excludedFromStatistics &&
                YearMonth.from(it.localDate(zoneId)) == month &&
                !it.localDate(zoneId).isAfter(today)
        }
        if (monthTx.size < MINIMUM_CURRENT_TRANSACTIONS) {
            return FinanceHealthReport(
                savingRate = null,
                concentration = null,
                savingsProgress = null,
                monthOverMonth = null,
                summaryText = "本月刚开始，记几笔账后这里会给出小结。",
                title = "数据还在积累",
                level = FinancialInsightLevel.INSUFFICIENT,
            )
        }

        val incomeCents = monthTx.filter { it.type == EntryType.INCOME }.sumOf(Transaction::amountCents)
        val expenseCents = monthTx.filter { it.type == EntryType.EXPENSE }.sumOf(Transaction::amountCents)
        val balanceCents = incomeCents - expenseCents

        // 维度 1 · 结余率
        val savingRate = if (incomeCents <= 0L) {
            notAvailable(
                HealthMetricKey.SAVING_RATE,
                "本月还没有收入，暂时算不出结余率",
            )
        } else {
            val rate = balanceCents.toDouble() / incomeCents
            val grade = when {
                rate > SAVING_RATE_GOOD -> HealthGrade.GOOD
                rate >= SAVING_RATE_MEDIUM -> HealthGrade.MEDIUM
                else -> HealthGrade.POOR
            }
            val text = when {
                rate < 0 -> "本月花超了收入的${percent(abs(rate))}"
                grade == HealthGrade.POOR -> "基本花光了收入，要注意了"
                else -> "存下了收入的${percent(rate)}"
            }
            HealthMetric(
                key = HealthMetricKey.SAVING_RATE,
                value = rate,
                displayValue = percent(rate),
                grade = grade,
                text = text,
                progress = rate.coerceIn(0.0, 1.0).toFloat(),
            )
        }

        // 维度 2 · 支出集中度
        val concentration = if (expenseCents <= 0L) {
            notAvailable(
                HealthMetricKey.CONCENTRATION,
                "本月还没有支出，暂时算不出集中度",
            )
        } else {
            val expenseItems = monthTx.filter { it.type == EntryType.EXPENSE }
            val top = expenseItems
                .groupBy(Transaction::categoryId)
                .map { (categoryId, items) -> categoryId to items.sumOf(Transaction::amountCents) }
                .maxByOrNull { it.second }
            if (top == null) {
                notAvailable(HealthMetricKey.CONCENTRATION, "本月还没有支出，暂时算不出集中度")
            } else {
                val ratio = top.second.toDouble() / expenseCents
                val grade = when {
                    ratio < CONCENTRATION_GOOD -> HealthGrade.GOOD
                    ratio <= CONCENTRATION_MEDIUM -> HealthGrade.MEDIUM
                    else -> HealthGrade.POOR
                }
                val categoryName = snapshot.category(top.first)?.name ?: "某一分类"
                val text = when (grade) {
                    HealthGrade.GOOD -> "各类支出分布均衡"
                    HealthGrade.MEDIUM -> "$categoryName 占总支出${percent(ratio)}，稍显集中"
                    else -> "$categoryName 占总支出${percent(ratio)}，开销很集中"
                }
                HealthMetric(
                    key = HealthMetricKey.CONCENTRATION,
                    value = ratio,
                    displayValue = percent(ratio),
                    grade = grade,
                    text = text,
                    progress = ratio.coerceIn(0.0, 1.0).toFloat(),
                )
            }
        }

        // 维度 3 · 储蓄进度（拍板 3：无储蓄目标 → 退化行，复用结余率口径，不出空态）
        val budget = snapshot.budgets.firstOrNull { it.month == FinanceRules.monthKey(month.atDay(1)) }
        val savingsProgress = if (budget == null || budget.mode != BudgetMode.SAVINGS_GOAL || budget.savingsGoalCents <= 0L) {
            HealthMetric(
                key = HealthMetricKey.SAVINGS_PROGRESS,
                value = savingRate.value,
                displayValue = "结余率",
                grade = savingRate.grade,
                text = when {
                    savingRate.grade == HealthGrade.N_A -> "未设储蓄目标，且本月暂无收入可参考"
                    savingRate.value < 0 -> "未设储蓄目标，按结余率参考：本月花超了收入的${percent(abs(savingRate.value))}"
                    else -> "未设储蓄目标，按结余率参考：存下了收入的${percent(savingRate.value)}"
                },
                progress = savingRate.progress,
            )
        } else {
            val completion = balanceCents.toDouble() / budget.savingsGoalCents
            val grade = when {
                completion >= 1.0 -> HealthGrade.GOOD
                completion >= SAVINGS_PROGRESS_MEDIUM -> HealthGrade.MEDIUM
                else -> HealthGrade.POOR
            }
            val text = when {
                completion < 0 -> "本月还没存下钱，储蓄目标尚未开始"
                grade == HealthGrade.POOR -> "储蓄目标只完成${percent(completion)}，要注意了"
                else -> "储蓄目标已完成${percent(completion)}"
            }
            HealthMetric(
                key = HealthMetricKey.SAVINGS_PROGRESS,
                value = completion,
                displayValue = percent(completion),
                grade = grade,
                text = text,
                progress = completion.coerceIn(0.0, 1.0).toFloat(),
            )
        }

        // 维度 4 · 与上月同期对比（"同期"= 上月取到与目标月相同的"已过天数"为止）
        val monthOverMonth = compareWithPreviousMonth(snapshot, month, today, zoneId, expenseCents)

        val grades = listOfNotNull(savingRate, concentration, savingsProgress, monthOverMonth)
        val poorCount = grades.count { it.grade == HealthGrade.POOR }
        val mediumCount = grades.count { it.grade == HealthGrade.MEDIUM }
        val level = when {
            poorCount >= 2 -> FinancialInsightLevel.HIGH_PRESSURE
            poorCount >= 1 || mediumCount >= 2 -> FinancialInsightLevel.ATTENTION
            else -> FinancialInsightLevel.STABLE
        }
        val title = when (level) {
            FinancialInsightLevel.STABLE -> "本月收支平稳"
            FinancialInsightLevel.ATTENTION -> "有一项变化值得关注"
            FinancialInsightLevel.HIGH_PRESSURE -> "本月资金压力较高"
            FinancialInsightLevel.INSUFFICIENT -> "数据还在积累"
        }
        val summaryText = buildSummaryText(
            snapshot = snapshot,
            month = month,
            today = today,
            zoneId = zoneId,
            savingRate = savingRate,
            concentration = concentration,
            monthOverMonth = monthOverMonth,
        )
        return FinanceHealthReport(
            savingRate = savingRate,
            concentration = concentration,
            savingsProgress = savingsProgress,
            monthOverMonth = monthOverMonth,
            summaryText = summaryText,
            title = title,
            level = level,
        )
    }

    private fun compareWithPreviousMonth(
        snapshot: LedgerSnapshot,
        month: YearMonth,
        today: LocalDate,
        zoneId: ZoneId,
        currentExpenseCents: Long,
    ): HealthMetric {
        val previousMonth = month.minusMonths(1)
        val isCurrentMonth = month == YearMonth.from(today)
        val endDay = if (isCurrentMonth) {
            minOf(today.dayOfMonth, previousMonth.lengthOfMonth())
        } else {
            // 分析历史月份时"上月同期"取整个上月（该月已完结，不存在截断未来的问题）。
            previousMonth.lengthOfMonth()
        }
        val previousExpense = snapshot.transactions
            .filter {
                !it.excludedFromStatistics &&
                    it.type == EntryType.EXPENSE &&
                    YearMonth.from(it.localDate(zoneId)) == previousMonth &&
                    it.localDate(zoneId).dayOfMonth <= endDay
            }
            .sumOf(Transaction::amountCents)
        if (previousExpense <= 0L) {
            return notAvailable(
                HealthMetricKey.MONTH_OVER_MONTH,
                "上月同期没有支出记录，暂时无法对比",
            )
        }
        val change = (currentExpenseCents - previousExpense).toDouble() / previousExpense
        val grade = when {
            change <= 0.0 -> HealthGrade.GOOD
            change <= MONTH_OVER_MONTH_MEDIUM -> HealthGrade.MEDIUM
            else -> HealthGrade.POOR
        }
        val text = when {
            change == 0.0 -> "与上月同期支出持平"
            change < 0 -> "比上月同期少花${percent(abs(change))}"
            grade == HealthGrade.POOR -> "比上月同期多花${percent(change)}，要留意"
            else -> "比上月同期多花${percent(change)}"
        }
        val display = if (change > 0) "+${percent(change)}" else if (change < 0) "-${percent(abs(change))}" else "0%"
        return HealthMetric(
            key = HealthMetricKey.MONTH_OVER_MONTH,
            value = change,
            displayValue = display,
            grade = grade,
            text = text,
            progress = abs(change).coerceIn(0.0, 1.0).toFloat(),
        )
    }

    /** 统计页小结（拍板 4：1~3 句大白话，纯展示）。 */
    private fun buildSummaryText(
        snapshot: LedgerSnapshot,
        month: YearMonth,
        today: LocalDate,
        zoneId: ZoneId,
        savingRate: HealthMetric?,
        concentration: HealthMetric?,
        monthOverMonth: HealthMetric?,
    ): String {
        val sentences = mutableListOf<String>()

        // 第 1 句（结余），若上月同期有数据追加"多存/少存 X 个百分点"。
        if (savingRate != null && savingRate.grade != HealthGrade.N_A) {
            val first = StringBuilder("本月存下了收入的${percent(savingRate.value)}")
            val previousRate = previousMonthSavingRate(snapshot, month, today, zoneId)
            if (previousRate != null) {
                val deltaPoints = ((savingRate.value - previousRate) * 100).roundToInt()
                if (deltaPoints > 0) first.append("，比上月多存 $deltaPoints 个百分点")
                if (deltaPoints < 0) first.append("，比上月少存 ${-deltaPoints} 个百分点")
            }
            sentences += first.toString()
        }

        // 第 2 句（集中度）：top1 占比 ≥ 40% 时点名，否则"分布均衡"。
        if (concentration != null && concentration.grade != HealthGrade.N_A) {
            sentences += if (concentration.value >= CONCENTRATION_GOOD) {
                val topName = concentration.text.substringBefore(" 占总支出")
                "「$topName」占总支出${concentration.displayValue}，是最集中的开销"
            } else {
                "各类支出分布均衡"
            }
        }

        // 第 3 句（环比）：仅 |环比| ≥ 10% 时输出。
        if (monthOverMonth != null && monthOverMonth.grade != HealthGrade.N_A) {
            val change = monthOverMonth.value
            if (abs(change) >= SUMMARY_MONTH_OVER_MONTH_MIN) {
                sentences += if (change > 0) {
                    "本月支出比上月同期多${percent(change)}"
                } else {
                    "本月支出比上月同期少${percent(abs(change))}"
                }
            }
        }

        if (sentences.isEmpty()) sentences += "本月收支已汇总完成，继续记录会有更完整的分析。"
        return sentences.take(3).joinToString("；") + "。"
    }

    /** 上月同期结余率（仅当上月同期有收入时返回，用于小结第 1 句的多存/少存对比）。 */
    private fun previousMonthSavingRate(
        snapshot: LedgerSnapshot,
        month: YearMonth,
        today: LocalDate,
        zoneId: ZoneId,
    ): Double? {
        val previousMonth = month.minusMonths(1)
        val isCurrentMonth = month == YearMonth.from(today)
        val endDay = if (isCurrentMonth) minOf(today.dayOfMonth, previousMonth.lengthOfMonth()) else previousMonth.lengthOfMonth()
        val previous = snapshot.transactions.filter {
            !it.excludedFromStatistics &&
                YearMonth.from(it.localDate(zoneId)) == previousMonth &&
                it.localDate(zoneId).dayOfMonth <= endDay
        }
        val income = previous.filter { it.type == EntryType.INCOME }.sumOf(Transaction::amountCents)
        if (income <= 0L) return null
        val expense = previous.filter { it.type == EntryType.EXPENSE }.sumOf(Transaction::amountCents)
        return (income - expense).toDouble() / income
    }

    private fun notAvailable(key: HealthMetricKey, text: String): HealthMetric = HealthMetric(
        key = key,
        value = 0.0,
        displayValue = "—",
        grade = HealthGrade.N_A,
        text = text,
        progress = 0f,
    )

    private fun percent(value: Double): String = "${(value * 100).roundToInt()}%"
}
