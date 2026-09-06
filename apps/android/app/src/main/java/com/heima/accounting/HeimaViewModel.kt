package com.heima.accounting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heima.accounting.data.AccountingRepository
import com.heima.accounting.data.LedgerState
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.Transaction
import com.heima.accounting.domain.DateRange
import com.heima.accounting.domain.StatisticsResult
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class TransactionSaved(val transaction: Transaction) : UiEvent
    data class TransactionDeleted(val transaction: Transaction) : UiEvent
    data class TransactionRestored(val transaction: Transaction) : UiEvent
    data class BackupRestored(val safetyFile: File) : UiEvent
}

class HeimaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AccountingRepository(application)
    private val settingsRepository = SettingsRepository(application)
    val ledgerState: StateFlow<LedgerState> = repository.state
    val settingsState: StateFlow<HeimaSettings> = settingsRepository.state
    private val mutableEvents = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<UiEvent> = mutableEvents.asSharedFlow()

    init { viewModelScope.launch { repository.initialize() } }

    fun saveTransaction(transaction: Transaction) = launchOperation {
        val id = repository.saveTransaction(transaction)
        val saved = if (transaction.id == 0L) transaction.copy(id = id) else transaction
        mutableEvents.emit(UiEvent.TransactionSaved(saved))
    }

    fun deleteTransaction(id: Long) = launchOperation {
        repository.deleteTransaction(id)?.let { mutableEvents.emit(UiEvent.TransactionDeleted(it)) }
    }

    fun undoDelete(transaction: Transaction) = launchOperation {
        repository.restoreDeletedTransaction(transaction)
        mutableEvents.emit(UiEvent.TransactionRestored(transaction))
    }

    /** 三模式预算保存：金额语义（主金额/储蓄目标/分类额度）由仓储层按 mode 校验。 */
    fun saveBudget(budget: MonthlyBudget) = launchOperation {
        repository.saveBudget(budget)
        mutableEvents.emit(UiEvent.Message("预算已保存"))
    }

    fun saveCategory(
        existingId: String? = null,
        type: EntryType,
        name: String,
        parentId: String? = null,
        iconKey: String = "other",
        colorArgb: Long = 0xFF7593B8,
        isActive: Boolean = true,
        sortOrder: Int? = null,
    ) = launchOperation {
        repository.saveCategory(existingId, type, name, parentId, iconKey, colorArgb, isActive, sortOrder)
        mutableEvents.emit(UiEvent.Message("分类已保存"))
    }

    fun reorderCategories(orderedIds: List<String>) = launchOperation {
        repository.reorderCategories(orderedIds)
        mutableEvents.emit(UiEvent.Message("分类顺序已保存"))
    }

    fun deleteCustomCategory(category: Category) = launchOperation {
        if (repository.deleteCustomCategory(category.id)) {
            mutableEvents.emit(UiEvent.Message(if (category.isCustom) "分类已停用或删除" else "预设分类已隐藏"))
        }
    }

    /**
     * 供记账页"＋添加细分"使用：直接返回新建分类，便于 UI 自动选中。
     * 失败返回 null（错误文案经 UiEvent.Message 下发）。
     * 图标与颜色由 UI 层从父分类透传，与分类管理页"添加二级分类"的继承规则一致。
     */
    suspend fun addSubcategory(
        parentId: String,
        type: EntryType,
        name: String,
        iconKey: String,
        colorArgb: Long,
    ): Category? = runCatching {
        repository.saveCategory(
            existingId = null,
            type = type,
            name = name,
            parentId = parentId,
            iconKey = iconKey,
            colorArgb = colorArgb,
        )
    }.onFailure { error ->
        mutableEvents.emit(UiEvent.Message(error.message ?: "操作没有完成，请再试一次"))
    }.getOrNull()

    suspend fun exportBackup(): String = repository.exportBackup()
    suspend fun exportCsv(): String = repository.exportCsv()
    suspend fun loadStatistics(range: DateRange): StatisticsResult = repository.statistics(range)

    /** 四期 B6：下拉刷新重读 repository 状态流（内存态重算，几乎瞬时）。 */
    fun refresh() = launchOperation { repository.refresh() }

    fun restoreBackup(json: String) = launchOperation {
        val safetyFile = repository.restoreBackup(json)
        mutableEvents.emit(UiEvent.BackupRestored(safetyFile))
    }

    fun setThemeStyle(value: com.heima.accounting.designsystem.HeimaThemeStyle) = settingsRepository.setThemeStyle(value)
    fun setColorMode(value: com.heima.accounting.designsystem.HeimaColorMode) = settingsRepository.setColorMode(value)
    fun setSoundEnabled(value: Boolean) = settingsRepository.setSoundEnabled(value)
    fun setHapticEnabled(value: Boolean) = settingsRepository.setHapticEnabled(value)
    fun setLiquidGlassEnabled(value: Boolean) = settingsRepository.setLiquidGlassEnabled(value)
    fun setReduceMotionEnabled(value: Boolean) = settingsRepository.setReduceMotionEnabled(value)
    fun setAmountsVisible(value: Boolean) = settingsRepository.setAmountsVisible(value)

    private fun launchOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { error ->
                mutableEvents.emit(UiEvent.Message(error.message ?: "操作没有完成，请再试一次"))
            }
        }
    }

    override fun onCleared() {
        repository.close()
    }
}
