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

    @Test
    fun visualRecordSlotMapsContinuouslyBetweenStatisticsAndBudget() {
        assertEquals(0f, pagerPositionToVisualSlot(0f), 0.001f)
        assertEquals(1f, pagerPositionToVisualSlot(1f), 0.001f)
        assertEquals(2f, pagerPositionToVisualSlot(1.5f), 0.001f)
        assertEquals(3f, pagerPositionToVisualSlot(2f), 0.001f)
        assertEquals(4f, pagerPositionToVisualSlot(3f), 0.001f)

        assertEquals(1f, visualSlotToPagerPosition(1f), 0.001f)
        assertEquals(1.5f, visualSlotToPagerPosition(2f), 0.001f)
        assertEquals(2f, visualSlotToPagerPosition(3f), 0.001f)
    }

    @Test
    fun dragSettlementUsesPositionAndVelocityWithoutCreatingARecordPage() {
        assertEquals(0, navigationDragTargetPage(pagePosition = .20f, fingerVelocityX = 0f))
        assertEquals(1, navigationDragTargetPage(pagePosition = .60f, fingerVelocityX = 0f))
        assertEquals(2, navigationDragTargetPage(pagePosition = 1.20f, fingerVelocityX = 1_000f))
        assertEquals(1, navigationDragTargetPage(pagePosition = 1.80f, fingerVelocityX = -1_000f))
    }
}
