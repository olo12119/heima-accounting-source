package com.heima.accounting.data

import android.content.Context
import com.heima.accounting.database.HeimaDatabase
import com.heima.accounting.domain.BudgetMode
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.Transaction
import com.heima.accounting.domain.DateRange
import com.heima.accounting.domain.FinanceRules
import com.heima.accounting.domain.StatisticsResult
import java.io.File
import java.util.UUID
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class LedgerState(
    val snapshot: LedgerSnapshot = LedgerSnapshot(),
    val loading: Boolean = true,
    val integrityOkay: Boolean = true,
    val errorMessage: String? = null,
)

class AccountingRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val database = HeimaDatabase(appContext)
    private val writeMutex = Mutex()
    private val mutableState = MutableStateFlow(LedgerState())
    val state: StateFlow<LedgerState> = mutableState.asStateFlow()

    suspend fun initialize() = withContext(ioDispatcher) {
        runCatching {
            if (!database.integrityCheck()) {
                mutableState.value = LedgerState(
                    loading = false,
                    integrityOkay = false,
                    errorMessage = "本地账本完整性检查未通过，原文件已保留。",
                )
            } else {
                refreshInternal()
            }
        }.onFailure { error ->
            mutableState.value = LedgerState(
                loading = false,
                integrityOkay = false,
                errorMessage = error.message ?: "本地账本暂时无法打开，原文件已保留。",
            )
        }
    }

    suspend fun saveTransaction(transaction: Transaction): Long = mutate {
        validateTransaction(transaction, database.readCategories())
        if (transaction.id == 0L) database.insertTransaction(transaction) else {
            check(database.updateTransaction(transaction)) { "没有找到要修改的账单" }
            transaction.id
        }
    }

    suspend fun deleteTransaction(id: Long): Transaction? = mutate {
        database.deleteTransaction(id)
    }

    suspend fun restoreDeletedTransaction(transaction: Transaction): Long = mutate {
        database.restoreTransaction(transaction)
    }

    /**
     * 三模式预算保存（三期 3.3）。按 mode 校验对应金额字段；主金额 amountCents 恒 >0
     * 由 DB CHECK 兜底。分类额度仅限支出一级分类（拍板 5），UI 与仓储双层校验。
     */
    suspend fun saveBudget(budget: MonthlyBudget) = mutate {
        require(budget.month.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) { "月份格式不正确" }
        when (budget.mode) {
            BudgetMode.MONTHLY_CAP -> require(budget.amountCents > 0L) { "预算必须大于0" }
            BudgetMode.SAVINGS_GOAL -> require(budget.savingsGoalCents > 0L) { "储蓄目标必须大于0" }
            BudgetMode.CATEGORY -> {
                require(budget.categoryBudgets.isNotEmpty()) { "至少给一个分类设置额度" }
                require(budget.categoryBudgets.values.all { it > 0L }) { "分类额度必须大于0" }
                require(budget.categoryBudgets.keys.all { id ->
                    database.readCategories().any { it.id == id && it.type == EntryType.EXPENSE && it.parentId == null }
                }) { "只能给支出一级分类设置额度" }
            }
        }
        database.upsertBudget(budget)
    }

    suspend fun saveCategory(
        existingId: String? = null,
        type: EntryType,
        name: String,
        parentId: String? = null,
        iconKey: String = "other",
        colorArgb: Long = 0xFF7593B8,
        isActive: Boolean = true,
        sortOrder: Int? = null,
    ): Category = mutate {
        val categories = database.readCategories()
        val normalized = name.trim()
        require(normalized.isNotBlank() && normalized.length <= 20) { "分类名称需要1到20个字" }
        require(
            categories.none {
                it.id != existingId && it.type == type && it.parentId == parentId && it.name == normalized && it.isActive
            },
        ) { "同一层级已经有这个分类" }
        val previous = existingId?.let { id -> categories.firstOrNull { it.id == id } }
        if (previous != null) {
            require(previous.type == type) { "已有分类的收支类型不能改变" }
            require(previous.parentId == parentId) { "已有分类的父子关系不能改变" }
        }
        val category = Category(
            id = previous?.id ?: "custom_${UUID.randomUUID()}",
            type = type,
            name = normalized,
            iconKey = iconKey,
            colorArgb = colorArgb,
            parentId = parentId,
            isCustom = previous?.isCustom ?: true,
            isActive = isActive,
            sortOrder = sortOrder ?: previous?.sortOrder ?: (
                categories.filter { it.type == type && it.parentId == parentId }.maxOfOrNull(Category::sortOrder) ?: -1
            ) + 1,
        )
        database.upsertCategory(category)
        category
    }

    suspend fun saveCustomCategory(
        existingId: String? = null,
        type: EntryType,
        name: String,
        parentId: String? = null,
        iconKey: String = "other",
        colorArgb: Long = 0xFF7593B8,
    ): Category = saveCategory(existingId, type, name, parentId, iconKey, colorArgb)

    suspend fun reorderCategories(orderedIds: List<String>) = mutate {
        database.reorderCategories(orderedIds)
    }

    suspend fun deleteCustomCategory(id: String): Boolean = mutate {
        database.deactivateOrDeleteCustomCategory(id)
    }

    suspend fun exportBackup(): String = withContext(ioDispatcher) {
        BackupCodec.encode(readSnapshot())
    }

    suspend fun exportCsv(): String = withContext(ioDispatcher) {
        CsvCodec.encode(readSnapshot())
    }

    suspend fun restoreBackup(json: String): File = writeMutex.withLock {
        withContext(ioDispatcher) {
            val incoming = BackupCodec.decodeAndValidate(json)
            val safetyFolder = File(appContext.filesDir, "safety-backups").apply { mkdirs() }
            val safetyFile = File(safetyFolder, "before-restore-${System.currentTimeMillis()}.heima-backup.json")
            safetyFile.writeText(BackupCodec.encode(readSnapshot()), Charsets.UTF_8)
            database.replaceAll(incoming.categories, incoming.transactions, incoming.budgets)
            refreshInternal()
            safetyFile
        }
    }

    suspend fun refresh() = withContext(ioDispatcher) { refreshInternal() }

    suspend fun statistics(
        range: DateRange,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatisticsResult = withContext(ioDispatcher) {
        val start = range.startInclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = range.endInclusive.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val transactions = database.readTransactionsBetween(start, endExclusive)
        StatisticsResult(
            summary = FinanceRules.summarize(transactions, range, zoneId),
            transactions = transactions.filterNot(Transaction::excludedFromStatistics),
        )
    }

    override fun close() = database.close()

    private suspend fun <T> mutate(block: () -> T): T = writeMutex.withLock {
        withContext(ioDispatcher) {
            val result = block()
            refreshInternal()
            result
        }
    }

    private fun readSnapshot(): LedgerSnapshot = LedgerSnapshot(
        categories = database.readCategories(),
        transactions = database.readTransactions(),
        budgets = database.readBudgets(),
    )

    private fun refreshInternal() {
        mutableState.value = LedgerState(
            snapshot = readSnapshot(),
            loading = false,
            integrityOkay = true,
        )
    }

    private fun validateTransaction(transaction: Transaction, categories: List<Category>) {
        require(transaction.amountCents > 0L) { "金额必须大于0" }
        require(transaction.note.length <= 200) { "备注最多200字" }
        val primary = categories.firstOrNull { it.id == transaction.categoryId && it.isActive }
            ?: error("一级分类不存在或已停用")
        require(primary.parentId == null && primary.type == transaction.type) { "一级分类与收支类型不匹配" }
        if (transaction.subcategoryId != null) {
            val secondary = categories.firstOrNull { it.id == transaction.subcategoryId && it.isActive }
                ?: error("二级分类不存在或已停用")
            require(secondary.parentId == primary.id && secondary.type == transaction.type) { "二级分类不属于所选一级分类" }
        }
    }
}
