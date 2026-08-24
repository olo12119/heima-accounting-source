package com.heima.accounting.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionMaterialTest {
    @Test
    fun disabledAlwaysUsesSolidMaterial() {
        assertEquals(
            GlassQuality.DISABLED,
            resolveGlassQuality(false, false, VisualQuality.REFINED),
        )
    }

    @Test
    fun reducedMotionAndPowerSaverAvoidRealtimeGlass() {
        assertEquals(
            GlassQuality.PERFORMANCE,
            resolveGlassQuality(true, true, VisualQuality.REFINED),
        )
        assertEquals(
            GlassQuality.PERFORMANCE,
            resolveGlassQuality(true, false, VisualQuality.POWER_SAVER),
        )
    }

    @Test
    fun refinedAndAutomaticResolveToDifferentCosts() {
        assertEquals(GlassQuality.HIGH, resolveGlassQuality(true, false, VisualQuality.REFINED))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(true, false, VisualQuality.AUTO))
    }
}
