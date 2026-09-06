package com.heima.accounting

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaThemeStyle
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
    fun remainingSettingsPersistAfterRepositoryRestart() {
        val first = SettingsRepository(context)
        first.setReduceMotionEnabled(true)
        first.setThemeStyle(HeimaThemeStyle.NATURE_HEALING)
        first.setColorMode(HeimaColorMode.DARK)
        first.setAmountsVisible(false)

        val restored = SettingsRepository(context).state.value
        assertTrue(restored.reduceMotionEnabled)
        assertEquals(HeimaThemeStyle.NATURE_HEALING, restored.themeStyle)
        assertEquals(HeimaColorMode.DARK, restored.colorMode)
        assertFalse(restored.amountsVisible)
    }

    @Test
    fun legacyRemovedKeysBecomeDeadAndNeverAffectDefaults() {
        context.getSharedPreferences(SettingsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putBoolean("liquid_glass_enabled", false)
            putBoolean("sound_enabled", false)
            putBoolean("haptic_enabled", false)
            putString("visual_quality", "POWER_SAVER")
        }

        val restored = SettingsRepository(context).state.value
        assertFalse(restored.reduceMotionEnabled)
        assertTrue(restored.amountsVisible)
        assertEquals(HeimaThemeStyle.CLEAR_BLUE, restored.themeStyle)
        assertEquals(HeimaColorMode.SYSTEM, restored.colorMode)
    }
}
