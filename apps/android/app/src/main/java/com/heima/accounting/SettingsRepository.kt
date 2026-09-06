package com.heima.accounting

import android.content.Context
import androidx.core.content.edit
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaThemeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single source of truth for user-facing experience settings.
 *
 * A positive Boolean always means that the named feature is enabled. The only
 * intentionally negative product concept is [reduceMotionEnabled], where true
 * means the user explicitly requested reduced motion.
 *
 * 二期补丁：Liquid Glass、操作音效与触觉反馈恢复为用户开关。
 * SharedPreferences key 沿用一期原名——二期只是删了字段没删数据，
 * 恢复同名字段后老用户的旧值自动读回，不会出现"开关状态凭空重置"。
 * 视觉质量不回到用户可配置：省电/过热的静默降级唯一入口在 HeimaApp.effectiveQuality。
 */
data class HeimaSettings(
    val themeStyle: HeimaThemeStyle = HeimaThemeStyle.CLEAR_BLUE,
    val colorMode: HeimaColorMode = HeimaColorMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val liquidGlassEnabled: Boolean = true,
    val reduceMotionEnabled: Boolean = false,
    val amountsVisible: Boolean = true,
)

class SettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableState = MutableStateFlow(readSettings())
    val state: StateFlow<HeimaSettings> = mutableState.asStateFlow()

    fun setThemeStyle(value: HeimaThemeStyle) = update { copy(themeStyle = value) }
    fun setColorMode(value: HeimaColorMode) = update { copy(colorMode = value) }
    fun setSoundEnabled(value: Boolean) = update { copy(soundEnabled = value) }
    fun setHapticEnabled(value: Boolean) = update { copy(hapticEnabled = value) }
    fun setLiquidGlassEnabled(value: Boolean) = update { copy(liquidGlassEnabled = value) }
    fun setReduceMotionEnabled(value: Boolean) = update { copy(reduceMotionEnabled = value) }
    fun setAmountsVisible(value: Boolean) = update { copy(amountsVisible = value) }

    private fun update(transform: HeimaSettings.() -> HeimaSettings) {
        val updated = mutableState.value.transform()
        preferences.edit(commit = true) {
            putString(KEY_THEME_STYLE, updated.themeStyle.name)
            putString(KEY_COLOR_MODE, updated.colorMode.name)
            putBoolean(KEY_SOUND_ENABLED, updated.soundEnabled)
            putBoolean(KEY_HAPTIC_ENABLED, updated.hapticEnabled)
            putBoolean(KEY_LIQUID_GLASS_ENABLED, updated.liquidGlassEnabled)
            putBoolean(KEY_REDUCE_MOTION, updated.reduceMotionEnabled)
            putBoolean(KEY_AMOUNTS_VISIBLE, updated.amountsVisible)
        }
        mutableState.value = updated
    }

    private fun readSettings() = HeimaSettings(
        themeStyle = enumPreference(preferences.getString(KEY_THEME_STYLE, null), HeimaThemeStyle.CLEAR_BLUE),
        colorMode = enumPreference(preferences.getString(KEY_COLOR_MODE, null), HeimaColorMode.SYSTEM),
        soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
        hapticEnabled = preferences.getBoolean(KEY_HAPTIC_ENABLED, true),
        liquidGlassEnabled = preferences.getBoolean(KEY_LIQUID_GLASS_ENABLED, true),
        reduceMotionEnabled = preferences.getBoolean(KEY_REDUCE_MOTION, false),
        amountsVisible = preferences.getBoolean(KEY_AMOUNTS_VISIBLE, true),
    )

    private inline fun <reified T : Enum<T>> enumPreference(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    companion object {
        const val PREFERENCES_NAME = "heima_visual_preferences"
        private const val KEY_THEME_STYLE = "theme_style"
        private const val KEY_COLOR_MODE = "color_mode"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_LIQUID_GLASS_ENABLED = "liquid_glass_enabled"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_AMOUNTS_VISIBLE = "amounts_visible"
    }
}
