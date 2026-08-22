package com.heima.accounting.data

import com.heima.accounting.domain.Category
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.MonthlyBudget
import com.heima.accounting.domain.Transaction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    private const val FORMAT = "heima-accounting-android"
    private const val VERSION = 1

    fun encode(snapshot: LedgerSnapshot): String {
        val payload = JSONObject().apply {
            put("categories", JSONArray().apply { snapshot.categories.forEach { put(it.toJson()) } })
            put("transactions", JSONArray().apply { snapshot.transactions.forEach { put(it.toJson()) } })
            put("budgets", JSONArray().apply { snapshot.budgets.forEach { put(it.toJson()) } })
        }
        val canonicalPayload = payload.toString()
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", Instant.now().toString())
            put("payload", payload)
            put("sha256", sha256(canonicalPayload))
        }.toString(2)
    }

    fun decodeAndValidate(json: String): LedgerSnapshot {
        require(json.toByteArray(StandardCharsets.UTF_8).size <= 64 * 1024 * 1024) { "备份文件过大" }
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT) { "这不是黑马记账Android备份" }
        require(root.getInt("version") == VERSION) { "备份版本暂不支持" }
        val payload = root.getJSONObject("payload")
        require(sha256(payload.toString()).equals(root.getString("sha256"), ignoreCase = true)) { "备份校验值不一致" }

        val categories = payload.getJSONArray("categories").mapObjects(::categoryFromJson)
        val transactions = payload.getJSONArray("transactions").mapObjects(::transactionFromJson)
        val budgets = payload.getJSONArray("budgets").mapObjects(::budgetFromJson)
        validateRelations(categories, transactions, budgets)
        return LedgerSnapshot(categories, transactions, budgets)
    }

    private fun validateRelations(
        categories: List<Category>,
        transactions: List<Transaction>,
        budgets: List<MonthlyBudget>,
    ) {
        require(categories.map(Category::id).distinct().size == categories.size) { "分类编号重复" }
        val byId = categories.associateBy(Category::id)
        categories.forEach { category ->
            require(category.id.isNotBlank() && category.name.isNotBlank() && category.name.length <= 20) { "分类内容不正确" }
            val parent = category.parentId?.let(byId::get)
            require(parent == null || (parent.parentId == null && parent.type == category.type)) { "分类关系不正确" }
        }
        transactions.forEach { transaction ->
            require(transaction.amountCents > 0L && transaction.note.length <= 200) { "账单内容不正确" }
            val primary = byId[transaction.categoryId]
            require(primary != null && primary.parentId == null && primary.type == transaction.type) { "账单一级分类不正确" }
            val secondary = transaction.subcategoryId?.let(byId::get)
            require(secondary == null || (secondary.parentId == primary.id && secondary.type == transaction.type)) { "账单二级分类不正确" }
        }
        require(budgets.map(MonthlyBudget::month).distinct().size == budgets.size) { "预算月份重复" }
        budgets.forEach { budget ->
            require(budget.month.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) && budget.amountCents > 0L) { "预算内容不正确" }
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun Category.toJson() = JSONObject().apply {
    put("id", id); put("type", type.name); put("name", name); put("iconKey", iconKey)
    put("colorArgb", colorArgb); put("parentId", parentId); put("isCustom", isCustom)
    put("isActive", isActive); put("sortOrder", sortOrder)
}

private fun Transaction.toJson() = JSONObject().apply {
    put("id", id); put("type", type.name); put("amountCents", amountCents); put("categoryId", categoryId)
    put("subcategoryId", subcategoryId); put("note", note); put("occurredAt", occurredAtEpochMillis)
    put("excluded", excludedFromStatistics); put("createdAt", createdAtEpochMillis); put("updatedAt", updatedAtEpochMillis)
}

private fun MonthlyBudget.toJson() = JSONObject().apply {
    put("month", month); put("amountCents", amountCents); put("updatedAt", updatedAtEpochMillis)
}

private fun categoryFromJson(json: JSONObject) = Category(
    id = json.getString("id"), type = EntryType.valueOf(json.getString("type")), name = json.getString("name"),
    iconKey = json.getString("iconKey"), colorArgb = json.getLong("colorArgb"),
    parentId = json.optNullableString("parentId"), isCustom = json.getBoolean("isCustom"),
    isActive = json.getBoolean("isActive"), sortOrder = json.getInt("sortOrder"),
)

private fun transactionFromJson(json: JSONObject) = Transaction(
    id = json.getLong("id"), type = EntryType.valueOf(json.getString("type")), amountCents = json.getLong("amountCents"),
    categoryId = json.getString("categoryId"), subcategoryId = json.optNullableString("subcategoryId"),
    note = json.getString("note"), occurredAtEpochMillis = json.getLong("occurredAt"),
    excludedFromStatistics = json.getBoolean("excluded"), createdAtEpochMillis = json.getLong("createdAt"),
    updatedAtEpochMillis = json.getLong("updatedAt"),
)

private fun budgetFromJson(json: JSONObject) = MonthlyBudget(
    month = json.getString("month"), amountCents = json.getLong("amountCents"), updatedAtEpochMillis = json.getLong("updatedAt"),
)

private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else getString(key)

private inline fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> =
    List(length()) { index -> mapper(getJSONObject(index)) }
