package com.heima.accounting.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountDisplayFormatterTest {
    @Test
    fun emptyInputShowsZeroYuan() {
        assertEquals("0", formatAmount(""))
    }

    @Test
    fun digitsAreEnteredAsYuanInsteadOfCents() {
        var input = ""
        input = appendAmountInput(input, "1")
        assertEquals("1", formatAmount(input))
        input = appendAmountInput(input, "2")
        assertEquals("12", formatAmount(input))
    }

    @Test
    fun decimalInputKeepsExactlyWhatTheUserTyped() {
        var input = "12"
        input = appendAmountInput(input, ".")
        assertEquals("12.", formatAmount(input))
        input = appendAmountInput(input, "5")
        assertEquals("12.5", formatAmount(input))
        input = appendAmountInput(input, "0")
        assertEquals("12.50", formatAmount(input))
    }

    @Test
    fun decimalPointAtTheStartProducesZeroPoint() {
        assertEquals("0.", appendAmountInput("", "."))
    }

    @Test
    fun duplicateDecimalAndThirdDecimalPlaceAreIgnored() {
        assertEquals("12.3", appendAmountInput("12.3", "."))
        assertEquals("12.34", appendAmountInput("12.34", "5"))
    }

    @Test
    fun leadingZeroIsKeptOnlyWhenUseful() {
        assertEquals("0", appendAmountInput("0", "0"))
        assertEquals("8", appendAmountInput("0", "8"))
        assertEquals("0.5", appendAmountInput("0.", "5"))
    }

    @Test
    fun amountIsConvertedToIntegerCentsOnlyWhenSaving() {
        assertEquals(1_250L, amountInputToCents("12.50"))
        assertEquals(1_205L, amountInputToCents("12.05"))
        assertEquals(1_200L, amountInputToCents("12"))
        assertEquals(1_200L, amountInputToCents("12."))
        assertNull(amountInputToCents(""))
    }
}
