package com.heima.accounting.data

import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.formatYuan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvCodec {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun encode(snapshot: LedgerSnapshot, zoneId: ZoneId = ZoneId.systemDefault()): String = buildString {
        append('\uFEFF')
        appendLine("类型,金额（元）,一级分类,二级分类,日期,时间,备注,是否计入统计")
        snapshot.transactions.sortedByDescending { it.occurredAtEpochMillis }.forEach { transaction ->
            val dateTime = Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(zoneId)
            val values = listOf(
                if (transaction.type == EntryType.EXPENSE) "支出" else "收入",
                transaction.amountCents.formatYuan(showSymbol = false),
                snapshot.category(transaction.categoryId)?.name.orEmpty(),
                snapshot.category(transaction.subcategoryId)?.name.orEmpty(),
                dateTime.format(dateFormatter),
                dateTime.format(timeFormatter),
                transaction.note,
                if (transaction.excludedFromStatistics) "否" else "是",
            )
            appendLine(values.joinToString(",", transform = ::escape))
        }
    }

    internal fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
}
