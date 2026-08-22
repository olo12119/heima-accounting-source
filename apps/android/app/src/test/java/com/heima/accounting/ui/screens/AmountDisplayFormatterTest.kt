package com.heima.accounting.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountDisplayFormatterTest {
    @Test
    fun emptyInputShowsZeroYuan() {
        assertEquals("0.00", formatAmount(""))
    }

    @Test
    fun oneDigitMeansCents() {
        assertEquals("0.05", formatAmount("5"))
    }

    @Test
    fun twoDigitsMeansCents() {
        assertEquals("0.58", formatAmount("58"))
    }

    @Test
    fun threeDigitsShowsYuanAndCents() {
        assertEquals("12.50", formatAmount("1250"))
    }

    @Test
    fun leadingZerosDoNotChangeTheAmount() {
        assertEquals("12.50", formatAmount("0001250"))
    }
}
