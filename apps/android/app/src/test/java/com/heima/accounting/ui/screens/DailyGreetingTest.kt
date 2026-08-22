package com.heima.accounting.ui.screens

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyGreetingTest {
    @Test
    fun greetingIsStableWithinTheSameDay() {
        val date = LocalDate.of(2026, 8, 22)
        assertEquals(dailyGreeting(date), dailyGreeting(date))
    }

    @Test
    fun consecutiveDaysRotateTheGreeting() {
        val date = LocalDate.of(2026, 8, 22)
        assertNotEquals(dailyGreeting(date), dailyGreeting(date.plusDays(1)))
    }
}
