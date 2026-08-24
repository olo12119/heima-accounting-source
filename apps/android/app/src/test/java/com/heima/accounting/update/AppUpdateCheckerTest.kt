package com.heima.accounting.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {
    @Test
    fun comparesSemanticVersionsWithoutUsingTextOrder() {
        assertTrue(AppUpdateChecker.compareVersions("1.10.0", "1.9.9") > 0)
        assertTrue(AppUpdateChecker.compareVersions("1.0.9", "1.1.0") < 0)
        assertEquals(0, AppUpdateChecker.compareVersions("1.1", "1.1.0"))
    }

}
