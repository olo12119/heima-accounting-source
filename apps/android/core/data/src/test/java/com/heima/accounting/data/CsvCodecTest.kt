package com.heima.accounting.data

import com.heima.accounting.domain.DefaultCategories
import com.heima.accounting.domain.EntryType
import com.heima.accounting.domain.LedgerSnapshot
import com.heima.accounting.domain.Transaction
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {
    @Test fun escapesCommaQuoteAndNewLine() {
        assertEquals("\"早,\"\"餐\"\"\n备注\"", CsvCodec.escape("早,\"餐\"\n备注"))
    }

    @Test fun exportHasBomChineseHeaderAndYuanAmount() {
        val zone = ZoneId.of("Asia/Shanghai")
        val transaction = Transaction(
            type = EntryType.EXPENSE,
            amountCents = 1250,
            categoryId = "expense_food",
            note = "早餐",
            occurredAtEpochMillis = LocalDate.of(2026, 8, 22).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        val csv = CsvCodec.encode(LedgerSnapshot(DefaultCategories.all, listOf(transaction)), zone)
        assertTrue(csv.startsWith("\uFEFF类型,金额（元）"))
        assertTrue(csv.contains("支出,12.50,餐饮"))
    }
}
