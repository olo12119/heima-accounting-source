package com.heima.accounting.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.heima.accounting.domain.Category
import com.heima.accounting.domain.DefaultCategories
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.Transaction

class HeimaDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    init { setWriteAheadLoggingEnabled(true) }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.rawQuery("PRAGMA busy_timeout=5000", null).use { it.moveToFirst() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createVersionOne(db)
        seedDefaultCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion
        while (version < newVersion) {
            when (version) {
                else -> error("缺少数据库迁移：$version → ${version + 1}")
            }
            version += 1
        }
    }

    fun integrityCheck(): Boolean = readableDatabase
        .rawQuery("PRAGMA quick_check(1)", null)
        .use { cursor -> cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true) }

    fun readCategories(): List<Category> = readableDatabase.query(
        "categories",
        CATEGORY_COLUMNS,
        null,
        null,
        null,
        null,
        "type ASC, parent_id IS NOT NULL ASC, sort_order ASC, name ASC",
    ).use { cursor -> cursor.mapRows(::categoryFromCursor) }

    fun readTransactions(): List<Transaction> = readableDatabase.query(
        "transactions",
        TRANSACTION_COLUMNS,
        null,
        null,
        null,
        null,
        "occurred_at DESC, id DESC",
    ).use { cursor -> cursor.mapRows(::transactionFromCursor) }

    fun readBudgets(): List<MonthlyBudget> = readableDatabase.query(
        "monthly_budgets",
        BUDGET_COLUMNS,
        null,
        null,
        null,
        null,
        "month DESC",
    ).use { cursor -> cursor.mapRows(::budgetFromCursor) }

    fun insertTransaction(transaction: Transaction): Long = writableDatabase.insertOrThrow(
        "transactions",
        null,
        transaction.toValues(includeId = false),
    )

    fun updateTransaction(transaction: Transaction): Boolean {
        require(transaction.id > 0L)
        return writableDatabase.update(
            "transactions",
            transaction.toValues(includeId = false),
            "id = ?",
            arrayOf(transaction.id.toString()),
        ) == 1
    }

    fun deleteTransaction(id: Long): Transaction? = writableDatabase.inTransaction {
        val existing = query(
            "transactions",
            TRANSACTION_COLUMNS,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
        ).use { cursor -> if (cursor.moveToFirst()) transactionFromCursor(cursor) else null }
        if (existing != null) delete("transactions", "id = ?", arrayOf(id.toString()))
        existing
    }

    fun restoreTransaction(transaction: Transaction): Long = writableDatabase.insertOrThrow(
        "transactions",
        null,
        transaction.toValues(includeId = true),
    )

    fun upsertBudget(budget: MonthlyBudget) {
        writableDatabase.insertWithOnConflict(
            "monthly_budgets",
            null,
            ContentValues().apply {
                put("month", budget.month)
                put("amount_cents", budget.amountCents)
                put("updated_at", budget.updatedAtEpochMillis)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun upsertCustomCategory(category: Category) {
        require(category.isCustom) { "只能新增或修改自定义分类" }
        require(category.name.isNotBlank() && category.name.length <= 20)
        val parent = category.parentId?.let(::findCategory)
        require(parent == null || (parent.parentId == null && parent.type == category.type)) {
            "二级分类必须属于同类型的一级分类"
        }
        writableDatabase.insertWithOnConflict(
            "categories",
            null,
            category.toValues(),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun deactivateOrDeleteCustomCategory(categoryId: String): Boolean = writableDatabase.inTransaction {
        val category = findCategory(categoryId) ?: return@inTransaction false
        require(category.isCustom) { "预设分类不能删除" }
        val count = rawQuery(
            "SELECT COUNT(*) FROM transactions WHERE category_id = ? OR subcategory_id = ?",
            arrayOf(categoryId, categoryId),
        ).use { cursor -> cursor.moveToFirst(); cursor.getLong(0) }
        if (count > 0L) {
            update("categories", ContentValues().apply { put("is_active", 0) }, "id = ?", arrayOf(categoryId)) == 1
        } else {
            delete("categories", "id = ?", arrayOf(categoryId)) == 1
        }
    }

    fun replaceAll(
        categories: List<Category>,
        transactions: List<Transaction>,
        budgets: List<MonthlyBudget>,
    ) {
        writableDatabase.inTransaction {
            delete("transactions", null, null)
            delete("monthly_budgets", null, null)
            // Categories reference their parent with ON DELETE RESTRICT. Deleting the
            // children first keeps foreign-key protection enabled during restoration.
            delete("categories", "parent_id IS NOT NULL", null)
            delete("categories", "parent_id IS NULL", null)
            categories.forEach { insertOrThrow("categories", null, it.toValues()) }
            transactions.forEach { insertOrThrow("transactions", null, it.toValues(includeId = true)) }
            budgets.forEach { budget ->
                insertOrThrow(
                    "monthly_budgets",
                    null,
                    ContentValues().apply {
                        put("month", budget.month)
                        put("amount_cents", budget.amountCents)
                        put("updated_at", budget.updatedAtEpochMillis)
                    },
                )
            }
        }
    }

    private fun findCategory(id: String): Category? = readableDatabase.query(
        "categories",
        CATEGORY_COLUMNS,
        "id = ?",
        arrayOf(id),
        null,
        null,
        null,
    ).use { cursor -> if (cursor.moveToFirst()) categoryFromCursor(cursor) else null }

    private fun createVersionOne(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE schema_migrations (
                version INTEGER NOT NULL PRIMARY KEY,
                applied_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE categories (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL CHECK(type IN ('EXPENSE','INCOME')),
                name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 20),
                icon_key TEXT NOT NULL,
                color_argb INTEGER NOT NULL,
                parent_id TEXT REFERENCES categories(id) ON UPDATE CASCADE ON DELETE RESTRICT,
                is_custom INTEGER NOT NULL CHECK(is_custom IN (0,1)),
                is_active INTEGER NOT NULL CHECK(is_active IN (0,1)),
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE transactions (
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
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE monthly_budgets (
                month TEXT NOT NULL PRIMARY KEY CHECK(length(month) = 7),
                amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_transactions_occurred_at ON transactions(occurred_at DESC)")
        db.execSQL("CREATE INDEX idx_transactions_category ON transactions(category_id, subcategory_id)")
        db.execSQL("CREATE INDEX idx_categories_parent ON categories(parent_id, sort_order)")
        db.execSQL(
            "INSERT INTO schema_migrations(version, applied_at) VALUES(?, ?)",
            arrayOf(1, System.currentTimeMillis()),
        )
    }

    private fun seedDefaultCategories(db: SQLiteDatabase) {
        DefaultCategories.all.forEach { db.insertOrThrow("categories", null, it.toValues()) }
    }

    companion object {
        const val DATABASE_NAME = "heima-accounting.sqlite3"
        const val DATABASE_VERSION = 1

        private val CATEGORY_COLUMNS = arrayOf(
            "id", "type", "name", "icon_key", "color_argb", "parent_id", "is_custom", "is_active", "sort_order",
        )
        private val TRANSACTION_COLUMNS = arrayOf(
            "id", "type", "amount_cents", "category_id", "subcategory_id", "note", "occurred_at",
            "excluded_from_statistics", "created_at", "updated_at",
        )
        private val BUDGET_COLUMNS = arrayOf("month", "amount_cents", "updated_at")
    }
}

private fun Category.toValues(): ContentValues = ContentValues().apply {
    put("id", id)
    put("type", type.name)
    put("name", name)
    put("icon_key", iconKey)
    put("color_argb", colorArgb)
    if (parentId == null) putNull("parent_id") else put("parent_id", parentId)
    put("is_custom", if (isCustom) 1 else 0)
    put("is_active", if (isActive) 1 else 0)
    put("sort_order", sortOrder)
}

private fun Transaction.toValues(includeId: Boolean): ContentValues = ContentValues().apply {
    if (includeId) put("id", id)
    put("type", type.name)
    put("amount_cents", amountCents)
    put("category_id", categoryId)
    if (subcategoryId == null) putNull("subcategory_id") else put("subcategory_id", subcategoryId)
    put("note", note.take(200))
    put("occurred_at", occurredAtEpochMillis)
    put("excluded_from_statistics", if (excludedFromStatistics) 1 else 0)
    put("created_at", createdAtEpochMillis)
    put("updated_at", updatedAtEpochMillis)
}

private fun categoryFromCursor(cursor: Cursor): Category = Category(
    id = cursor.getString(0),
    type = EntryType.valueOf(cursor.getString(1)),
    name = cursor.getString(2),
    iconKey = cursor.getString(3),
    colorArgb = cursor.getLong(4),
    parentId = if (cursor.isNull(5)) null else cursor.getString(5),
    isCustom = cursor.getInt(6) == 1,
    isActive = cursor.getInt(7) == 1,
    sortOrder = cursor.getInt(8),
)

private fun transactionFromCursor(cursor: Cursor): Transaction = Transaction(
    id = cursor.getLong(0),
    type = EntryType.valueOf(cursor.getString(1)),
    amountCents = cursor.getLong(2),
    categoryId = cursor.getString(3),
    subcategoryId = if (cursor.isNull(4)) null else cursor.getString(4),
    note = cursor.getString(5),
    occurredAtEpochMillis = cursor.getLong(6),
    excludedFromStatistics = cursor.getInt(7) == 1,
    createdAtEpochMillis = cursor.getLong(8),
    updatedAtEpochMillis = cursor.getLong(9),
)

private fun budgetFromCursor(cursor: Cursor): MonthlyBudget = MonthlyBudget(
    month = cursor.getString(0),
    amountCents = cursor.getLong(1),
    updatedAtEpochMillis = cursor.getLong(2),
)

private inline fun <T> Cursor.mapRows(mapper: (Cursor) -> T): List<T> = buildList {
    while (moveToNext()) add(mapper(this@mapRows))
}

private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
    beginTransaction()
    return try {
        val result = block()
        setTransactionSuccessful()
        result
    } finally {
        endTransaction()
    }
}
