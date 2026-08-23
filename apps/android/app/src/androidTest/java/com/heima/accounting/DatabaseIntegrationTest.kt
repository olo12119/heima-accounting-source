package com.heima.accounting

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heima.accounting.database.HeimaDatabase
import com.heima.accounting.data.AccountingRepository
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
import kotlinx.coroutines.runBlocking

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

    @Test fun versionOneToTwoMigrationPreservesOneHundredTransactionsAndCustomData() {
        database.close()
        context.deleteDatabase(HeimaDatabase.DATABASE_NAME)
        val legacy = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(HeimaDatabase.DATABASE_NAME),
            null,
        )
        createLegacyVersionOne(legacy)

        legacy.beginTransaction()
        try {
            legacy.insertOrThrow("categories", null, legacyCategory("expense_food", "餐饮", "food", 0xFFFFA45BL))
            legacy.insertOrThrow(
                "categories",
                null,
                legacyCategory("custom_commute", "通勤卡", "bus", 0xFF4FA3D1L, isCustom = true),
            )
            legacy.insertOrThrow(
                "categories",
                null,
                legacyCategory(
                    "custom_breakfast",
                    "我的早餐",
                    "coffee",
                    0xFFF2A65AL,
                    isCustom = true,
                    parentId = "expense_food",
                ),
            )
            repeat(100) { index ->
                legacy.insertOrThrow(
                    "transactions",
                    null,
                    ContentValues().apply {
                        put("type", EntryType.EXPENSE.name)
                        put("amount_cents", 100L + index)
                        put("category_id", if (index == 0) "custom_commute" else "expense_food")
                        if (index == 1) put("subcategory_id", "custom_breakfast") else putNull("subcategory_id")
                        put("note", "旧版账单$index")
                        put("occurred_at", 1_777_000_000_000L + index)
                        put("excluded_from_statistics", 0)
                        put("created_at", 1_777_000_000_000L + index)
                        put("updated_at", 1_777_000_000_000L + index)
                    },
                )
            }
            legacy.insertOrThrow(
                "monthly_budgets",
                null,
                ContentValues().apply {
                    put("month", "2026-08")
                    put("amount_cents", 450_000L)
                    put("updated_at", 1_777_000_000_000L)
                },
            )
            legacy.setTransactionSuccessful()
        } finally {
            legacy.endTransaction()
            legacy.version = 1
            legacy.close()
        }

        database = HeimaDatabase(context)

        assertTrue(database.integrityCheck())
        assertEquals(HeimaDatabase.DATABASE_VERSION, database.readableDatabase.version)
        assertEquals(100, database.readTransactions().size)
        assertEquals((100L..199L).sum(), database.readTransactions().sumOf(Transaction::amountCents))
        val custom = database.readCategories().single { it.id == "custom_commute" }
        assertEquals("通勤卡", custom.name)
        assertEquals("bus", custom.iconKey)
        assertEquals(0xFF4FA3D1L, custom.colorArgb)
        assertEquals("custom_breakfast", database.readTransactions().single { it.note == "旧版账单1" }.subcategoryId)
        assertEquals("我的早餐", database.readCategories().single { it.id == "custom_breakfast" }.name)
        assertEquals(450_000L, database.readBudgets().single().amountCents)
        assertTrue(database.readCategories().any { it.id == "expense_communication" })
        assertTrue(database.readCategories().any { it.id == "expense_subscription" })
    }

    @Test fun presetPresentationCanChangeButStableRelationshipsRemainProtected() = runBlocking {
        database.close()
        val repository = AccountingRepository(context)
        try {
            repository.initialize()
            val original = repository.state.value.snapshot.categories.single { it.id == "expense_food" }
            repository.saveCategory(
                existingId = original.id,
                type = original.type,
                name = "日常餐饮",
                parentId = original.parentId,
                iconKey = "coffee",
                colorArgb = 0xFFE97868L,
                isActive = true,
                sortOrder = 5,
            )
            val edited = repository.state.value.snapshot.categories.single { it.id == original.id }
            assertEquals(original.id, edited.id)
            assertEquals(EntryType.EXPENSE, edited.type)
            assertEquals(null, edited.parentId)
            assertEquals("日常餐饮", edited.name)
            assertEquals("coffee", edited.iconKey)
            assertEquals(0xFFE97868L, edited.colorArgb)

            val unsafeTypeChange = runCatching {
                repository.saveCategory(
                    existingId = original.id,
                    type = EntryType.INCOME,
                    name = edited.name,
                    parentId = null,
                    iconKey = edited.iconKey,
                    colorArgb = edited.colorArgb,
                )
            }
            assertTrue(unsafeTypeChange.isFailure)

            val desiredOrder = repository.state.value.snapshot.categories
                .filter { it.type == EntryType.EXPENSE && it.parentId == null }
                .sortedBy(Category::sortOrder)
                .map(Category::id)
                .reversed()
            repository.reorderCategories(desiredOrder)
            assertEquals(
                desiredOrder,
                repository.state.value.snapshot.categories
                    .filter { it.type == EntryType.EXPENSE && it.parentId == null }
                    .sortedBy(Category::sortOrder)
                    .map(Category::id),
            )

            repository.deleteCustomCategory(original.id)
            assertTrue(!repository.state.value.snapshot.categories.single { it.id == original.id }.isActive)
        } finally {
            repository.close()
            database = HeimaDatabase(context)
        }
    }

    private fun createLegacyVersionOne(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE schema_migrations (version INTEGER NOT NULL PRIMARY KEY, applied_at INTEGER NOT NULL)")
        db.execSQL(
            """CREATE TABLE categories (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL CHECK(type IN ('EXPENSE','INCOME')),
                name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 20),
                icon_key TEXT NOT NULL,
                color_argb INTEGER NOT NULL,
                parent_id TEXT REFERENCES categories(id) ON UPDATE CASCADE ON DELETE RESTRICT,
                is_custom INTEGER NOT NULL CHECK(is_custom IN (0,1)),
                is_active INTEGER NOT NULL CHECK(is_active IN (0,1)),
                sort_order INTEGER NOT NULL DEFAULT 0
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL CHECK(type IN ('EXPENSE','INCOME')),
                amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                category_id TEXT NOT NULL REFERENCES categories(id) ON UPDATE CASCADE ON DELETE RESTRICT,
                subcategory_id TEXT REFERENCES categories(id) ON UPDATE CASCADE ON DELETE RESTRICT,
                note TEXT NOT NULL DEFAULT '' CHECK(length(note) <= 200),
                occurred_at INTEGER NOT NULL,
                excluded_from_statistics INTEGER NOT NULL DEFAULT 0 CHECK(excluded_from_statistics IN (0,1)),
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL("CREATE TABLE monthly_budgets (month TEXT NOT NULL PRIMARY KEY CHECK(length(month) = 7), amount_cents INTEGER NOT NULL CHECK(amount_cents > 0), updated_at INTEGER NOT NULL)")
        db.execSQL("INSERT INTO schema_migrations(version, applied_at) VALUES(1, 1777000000000)")
    }

    private fun legacyCategory(
        id: String,
        name: String,
        iconKey: String,
        colorArgb: Long,
        isCustom: Boolean = false,
        parentId: String? = null,
    ) = ContentValues().apply {
        put("id", id)
        put("type", EntryType.EXPENSE.name)
        put("name", name)
        put("icon_key", iconKey)
        put("color_argb", colorArgb)
        if (parentId == null) putNull("parent_id") else put("parent_id", parentId)
        put("is_custom", if (isCustom) 1 else 0)
        put("is_active", 1)
        put("sort_order", 0)
    }
}
