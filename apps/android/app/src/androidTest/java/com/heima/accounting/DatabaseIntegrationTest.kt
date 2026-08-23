package com.heima.accounting

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heima.accounting.database.HeimaDatabase
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.Transaction
import java.time.LocalDate
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var database: HeimaDatabase

    @Before fun setup() {
        context.deleteDatabase(HeimaDatabase.DATABASE_NAME)
        database = HeimaDatabase(context)
    }

    @After fun cleanup() {
        database.close()
        context.deleteDatabase(HeimaDatabase.DATABASE_NAME)
    }

    @Test fun migrationSeedCrudAndReopenPersist() {
        assertTrue(database.integrityCheck())
        assertTrue(database.readCategories().size > 80)
        val id = database.insertTransaction(Transaction(type = EntryType.EXPENSE, amountCents = 1250, categoryId = "expense_food", occurredAtEpochMillis = System.currentTimeMillis()))
        assertTrue(id > 0)
        database.close()
        database = HeimaDatabase(context)
        assertEquals(1250L, database.readTransactions().single().amountCents)
        assertNotNull(database.deleteTransaction(id))
        assertTrue(database.readTransactions().isEmpty())
    }

    @Test fun failedReplacementRollsBackAllRows() {
        val before = database.readCategories().size
        runCatching {
            database.replaceAll(database.readCategories(), listOf(Transaction(type = EntryType.EXPENSE, amountCents = 100, categoryId = "missing", occurredAtEpochMillis = 1)), emptyList())
        }
        assertEquals(before, database.readCategories().size)
        assertTrue(database.readTransactions().isEmpty())
    }

    @Test fun secondaryCategoryUpdateBudgetAndUndoRemainConsistent() {
        database.upsertBudget(MonthlyBudget("2026-08", 300_000L))
        val originalId = database.insertTransaction(
            Transaction(
                type = EntryType.EXPENSE,
                amountCents = 1_250L,
                categoryId = "expense_food",
                subcategoryId = "expense_food_65e99910",
                note = "早餐",
                occurredAtEpochMillis = 1_777_000_000_000L,
            ),
        )
        val original = database.readTransactions().single()
        assertEquals(originalId, original.id)
        assertEquals("expense_food_65e99910", original.subcategoryId)

        assertTrue(database.updateTransaction(original.copy(amountCents = 1_880L, note = "早餐和咖啡")))
        assertEquals(1_880L, database.readTransactions().single().amountCents)
        val deleted = database.deleteTransaction(originalId)
        assertNotNull(deleted)
        assertTrue(database.readTransactions().isEmpty())
        database.restoreTransaction(requireNotNull(deleted))
        assertEquals(originalId, database.readTransactions().single().id)
        assertEquals(300_000L, database.readBudgets().single().amountCents)
    }

    @Test fun customCategoryCanBeEditedAndReferencedCategoryIsSafelyDeactivated() {
        val custom = Category(
            id = "custom_test",
            type = EntryType.EXPENSE,
            name = "通勤车票",
            iconKey = "transport",
            colorArgb = 0xFF55A6D9,
            parentId = "expense_transport",
            isCustom = true,
        )
        database.upsertCustomCategory(custom)
        database.upsertCustomCategory(custom.copy(name = "通勤"))
        assertEquals("通勤", database.readCategories().single { it.id == custom.id }.name)
        database.insertTransaction(
            Transaction(
                type = EntryType.EXPENSE,
                amountCents = 200L,
                categoryId = "expense_transport",
                subcategoryId = custom.id,
                occurredAtEpochMillis = 1_777_000_000_000L,
            ),
        )
        assertTrue(database.deactivateOrDeleteCustomCategory(custom.id))
        assertTrue(!database.readCategories().single { it.id == custom.id }.isActive)
        assertEquals(custom.id, database.readTransactions().single().subcategoryId)
    }

    @Test fun oneHundredOneThousandAndTenThousandRowsCanBeReplacedAndReadBack() {
        val categories = database.readCategories()
        listOf(100, 1_000, 10_000).forEach { rowCount ->
            val entries = List(rowCount) { index ->
                Transaction(
                    id = index + 1L,
                    type = EntryType.EXPENSE,
                    amountCents = 100L + index,
                    categoryId = "expense_food",
                    subcategoryId = "expense_food_65e99910",
                    occurredAtEpochMillis = 1_777_000_000_000L + index,
                )
            }
            database.replaceAll(categories, entries, emptyList())
            val restored = database.readTransactions()
            assertEquals(rowCount, restored.size)
            assertEquals(99L + rowCount, restored.first().amountCents)
        }
    }

    @Test fun dateRangeQueryFiltersInSqlAndIncludesTheLastLocalDay() {
        val zone = ZoneId.of("Asia/Shanghai")
        val august1 = LocalDate.of(2026, 8, 1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val august15 = LocalDate.of(2026, 8, 15).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        val august16 = LocalDate.of(2026, 8, 16).atStartOfDay(zone).toInstant().toEpochMilli()
        listOf(august1, august15, august16).forEachIndexed { index, epoch ->
            database.insertTransaction(
                Transaction(
                    type = EntryType.EXPENSE,
                    amountCents = 100L + index,
                    categoryId = "expense_food",
                    occurredAtEpochMillis = epoch,
                ),
            )
        }

        val rows = database.readTransactionsBetween(
            LocalDate.of(2026, 8, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
            LocalDate.of(2026, 8, 16).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        assertEquals(2, rows.size)
        assertEquals(setOf(100L, 101L), rows.map(Transaction::amountCents).toSet())
    }
}
