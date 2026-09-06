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
 * Liquid Glass、操作音效、触觉反馈与视觉质量已在本期收敛为"默认最佳配置"，
 * 不再暴露为用户开关：默认值在 HeimaApp 层硬编码，省电/过热由系统自动降级。
 * SharedPreferences 中残留的旧 key 无人读取、自然失效，不影响老用户升级。
 */
data class HeimaSettings(
    val themeStyle: HeimaThemeStyle = HeimaThemeStyle.CLEAR_BLUE,
    val colorMode: HeimaColorMode = HeimaColorMode.SYSTEM,
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
    fun setReduceMotionEnabled(value: Boolean) = update { copy(reduceMotionEnabled = value) }
    fun setAmountsVisible(value: Boolean) = update { copy(amountsVisible = value) }

    private fun update(transform: HeimaSettings.() -> HeimaSettings) {
        val updated = mutableState.value.transform()
        preferences.edit(commit = true) {
            putString(KEY_THEME_STYLE, updated.themeStyle.name)
            putString(KEY_COLOR_MODE, updated.colorMode.name)
            putBoolean(KEY_REDUCE_MOTION, updated.reduceMotionEnabled)
            putBoolean(KEY_AMOUNTS_VISIBLE, updated.amountsVisible)
        }
        mutableState.value = updated
    }

    private fun readSettings() = HeimaSettings(
        themeStyle = enumPreference(preferences.getString(KEY_THEME_STYLE, null), HeimaThemeStyle.CLEAR_BLUE),
        colorMode = enumPreference(preferences.getString(KEY_COLOR_MODE, null), HeimaColorMode.SYSTEM),
        reduceMotionEnabled = preferences.getBoolean(KEY_REDUCE_MOTION, false),
        amountsVisible = preferences.getBoolean(KEY_AMOUNTS_VISIBLE, true),
    )

    private inline fun <reified T : Enum<T>> enumPreference(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    companion object {
        const val PREFERENCES_NAME = "heima_visual_preferences"
        private const val KEY_THEME_STYLE = "theme_style"
        private const val KEY_COLOR_MODE = "color_mode"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_AMOUNTS_VISIBLE = "amounts_visible"
    }
}
