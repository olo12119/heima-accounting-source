package com.heima.accounting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heima.accounting.data.AccountingRepository
import com.heima.accounting.data.LedgerState
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
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

    fun saveBudget(month: String, amountCents: Long) = launchOperation {
        repository.saveBudget(month, amountCents)
        mutableEvents.emit(UiEvent.Message("本月预算已保存"))
    }

    fun saveCustomCategory(
        existingId: String? = null,
        type: EntryType,
        name: String,
        parentId: String? = null,
    ) = launchOperation {
        repository.saveCustomCategory(existingId, type, name, parentId)
        mutableEvents.emit(UiEvent.Message("分类已保存"))
    }

    fun deleteCustomCategory(category: Category) = launchOperation {
        if (repository.deleteCustomCategory(category.id)) {
            mutableEvents.emit(UiEvent.Message(if (category.isCustom) "分类已停用或删除" else "分类未改变"))
        }
    }

    suspend fun exportBackup(): String = repository.exportBackup()
    suspend fun exportCsv(): String = repository.exportCsv()
    suspend fun loadStatistics(range: DateRange): StatisticsResult = repository.statistics(range)

    fun restoreBackup(json: String) = launchOperation {
        val safetyFile = repository.restoreBackup(json)
        mutableEvents.emit(UiEvent.BackupRestored(safetyFile))
    }

    fun setThemeStyle(value: com.heima.accounting.designsystem.HeimaThemeStyle) = settingsRepository.setThemeStyle(value)
    fun setColorMode(value: com.heima.accounting.designsystem.HeimaColorMode) = settingsRepository.setColorMode(value)
    fun setVisualQuality(value: com.heima.accounting.designsystem.VisualQuality) = settingsRepository.setVisualQuality(value)
    fun setLiquidGlassEnabled(value: Boolean) = settingsRepository.setLiquidGlassEnabled(value)
    fun setSoundEnabled(value: Boolean) = settingsRepository.setSoundEnabled(value)
    fun setHapticEnabled(value: Boolean) = settingsRepository.setHapticEnabled(value)
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
