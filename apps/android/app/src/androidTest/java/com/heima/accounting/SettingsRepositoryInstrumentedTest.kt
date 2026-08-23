package com.heima.accounting

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryInstrumentedTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clearSettings() {
        context.getSharedPreferences(SettingsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun positiveSwitchNamesMatchPersistedFeatureStateAfterRepositoryRestart() {
        val first = SettingsRepository(context)
        first.setLiquidGlassEnabled(false)
        first.setSoundEnabled(false)
        first.setHapticEnabled(false)
        first.setReduceMotionEnabled(true)
        first.setThemeStyle(HeimaThemeStyle.NATURE_HEALING)
        first.setColorMode(HeimaColorMode.DARK)
        first.setVisualQuality(VisualQuality.POWER_SAVER)

        val restored = SettingsRepository(context).state.value
        assertFalse(restored.liquidGlassEnabled)
        assertFalse(restored.soundEnabled)
        assertFalse(restored.hapticEnabled)
        assertTrue(restored.reduceMotionEnabled)
        assertEquals(HeimaThemeStyle.NATURE_HEALING, restored.themeStyle)
        assertEquals(HeimaColorMode.DARK, restored.colorMode)
        assertEquals(VisualQuality.POWER_SAVER, restored.visualQuality)
    }
}
