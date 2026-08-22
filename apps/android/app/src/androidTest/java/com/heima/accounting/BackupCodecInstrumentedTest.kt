package com.heima.accounting

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heima.accounting.data.BackupCodec
import com.heima.accounting.domain.DefaultCategories
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCodecInstrumentedTest {
    @Test fun roundTripKeepsIntegerAmountAndRelations() {
        val source = LedgerSnapshot(
            DefaultCategories.all,
            listOf(Transaction(id = 7, type = EntryType.EXPENSE, amountCents = 1250, categoryId = "expense_food", note = "早餐", occurredAtEpochMillis = 1234567)),
            listOf(MonthlyBudget("2026-08", 300_000)),
        )
        val restored = BackupCodec.decodeAndValidate(BackupCodec.encode(source))
        assertEquals(1250L, restored.transactions.single().amountCents)
        assertEquals("expense_food", restored.transactions.single().categoryId)
        assertEquals(300_000L, restored.budgets.single().amountCents)
    }

    @Test fun changedPayloadFailsChecksum() {
        val encoded = BackupCodec.encode(LedgerSnapshot(DefaultCategories.all))
        val corrupted = encoded.replace("餐饮", "餐费")
        assertTrue(runCatching { BackupCodec.decodeAndValidate(corrupted) }.isFailure)
    }

    @Test fun secondaryCategoryMustBelongToSelectedPrimaryCategory() {
        val invalid = LedgerSnapshot(
            DefaultCategories.all,
            listOf(
                Transaction(
                    id = 1,
                    type = EntryType.EXPENSE,
                    amountCents = 500,
                    categoryId = "expense_food",
                    subcategoryId = "expense_transport_516c4ea4",
                    occurredAtEpochMillis = 1_777_000_000_000L,
                ),
            ),
        )
        assertTrue(runCatching { BackupCodec.decodeAndValidate(BackupCodec.encode(invalid)) }.isFailure)
    }
}
