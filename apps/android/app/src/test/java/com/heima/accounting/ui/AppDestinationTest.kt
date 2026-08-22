package com.heima.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test
    fun bottomNavigationHasFiveStableEntries() {
        assertEquals(
            listOf("首页", "统计", "记账", "预算", "我的"),
            AppDestination.entries.map { it.label },
        )
    }

    @Test
    fun everyEntryHasAnAccessibilityDescription() {
        assertTrue(AppDestination.entries.all { it.accessibilityLabel.isNotBlank() })
    }
}
