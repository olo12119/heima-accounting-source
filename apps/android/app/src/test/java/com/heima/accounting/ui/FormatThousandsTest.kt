package com.heima.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatThousandsTest {
    @Test
    fun groupsIntegerPartByThousandsWithYuanSymbol() {
        assertEquals("¥0.00", 0L.formatThousands())
        assertEquals("¥1.00", 100L.formatThousands())
        assertEquals("¥12.34", 1_234L.formatThousands())
        assertEquals("¥1,234.56", 123_456L.formatThousands())
        assertEquals("¥12,345.67", 1_234_567L.formatThousands())
        assertEquals("¥123,456,789.01", 12_345_678_901L.formatThousands())
        assertEquals("¥999,999,999.99", 99_999_999_999L.formatThousands())
    }

    @Test
    fun handlesNegativeAmountsAndOmitsSymbolOnDemand() {
        assertEquals("¥-12,345.67", (-1_234_567L).formatThousands())
        assertEquals("-12,345.67", (-1_234_567L).formatThousands(showSymbol = false))
        assertEquals("1,234.56", 123_456L.formatThousands(showSymbol = false))
        assertEquals("0.00", 0L.formatThousands(showSymbol = false))
    }
}
