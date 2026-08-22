package com.heima.accounting.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heima.accounting.HeimaViewModel
import com.heima.accounting.designsystem.HeimaAccountingTheme
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality

@Composable
fun HeimaApp() {
    val context = LocalContext.current
    val viewModel: HeimaViewModel = viewModel()
    val ledgerState by viewModel.ledgerState.collectAsStateWithLifecycle()
    val preferences = remember(context) {
        context.getSharedPreferences("heima_visual_preferences", Context.MODE_PRIVATE)
    }
    var themeStyle by remember {
        mutableStateOf(
            enumPreference(
                preferences.getString("theme_style", null),
                HeimaThemeStyle.CLEAR_BLUE,
            ),
        )
    }
    var colorMode by remember {
        mutableStateOf(
            enumPreference(
                preferences.getString("color_mode", null),
                HeimaColorMode.SYSTEM,
            ),
        )
    }
    var quality by remember {
        mutableStateOf(
            enumPreference(
                preferences.getString("visual_quality", null),
                VisualQuality.AUTO,
            ),
        )
    }
    var reduceMotion by remember {
        mutableStateOf(preferences.getBoolean("reduce_motion", false))
    }
    var amountsVisible by remember {
        mutableStateOf(preferences.getBoolean("amounts_visible", true))
    }
    var liquidGlassEnabled by remember {
        mutableStateOf(preferences.getBoolean("liquid_glass_enabled", true))
    }
    var soundEnabled by remember {
        mutableStateOf(preferences.getBoolean("sound_enabled", true))
    }
    var hapticEnabled by remember {
        mutableStateOf(preferences.getBoolean("haptic_enabled", true))
    }
    val powerSaveMode = rememberPowerSaveMode()
    val thermalStatus = rememberThermalStatus()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (colorMode) {
        HeimaColorMode.SYSTEM -> systemDark
        HeimaColorMode.LIGHT -> false
        HeimaColorMode.DARK -> true
    }
    val effectiveQuality = when {
        quality == VisualQuality.AUTO && (powerSaveMode || thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) ->
            VisualQuality.POWER_SAVER
        else -> quality
    }
    val feedback = rememberInteractionFeedback(
        soundEnabled = { soundEnabled },
        hapticEnabled = { hapticEnabled },
    )

    HeimaAccountingTheme(
        style = themeStyle,
        darkTheme = darkTheme,
        quality = effectiveQuality,
        reduceMotion = reduceMotion,
        liquidGlassEnabled = liquidGlassEnabled,
    ) {
        HeimaShell(
            viewModel = viewModel,
            ledgerState = ledgerState,
            feedback = feedback,
            themeStyle = themeStyle,
            colorMode = colorMode,
            visualQuality = quality,
            reduceMotion = reduceMotion,
            powerSaveMode = powerSaveMode,
            amountsVisible = amountsVisible,
            liquidGlassEnabled = liquidGlassEnabled,
            soundEnabled = soundEnabled,
            hapticEnabled = hapticEnabled,
            onThemeStyleChange = {
                themeStyle = it
                preferences.edit { putString("theme_style", it.name) }
            },
            onColorModeChange = {
                colorMode = it
                preferences.edit { putString("color_mode", it.name) }
            },
            onVisualQualityChange = {
                quality = it
                preferences.edit { putString("visual_quality", it.name) }
            },
            onReduceMotionChange = {
                reduceMotion = it
                preferences.edit { putBoolean("reduce_motion", it) }
            },
            onAmountsVisibleChange = {
                amountsVisible = it
                preferences.edit { putBoolean("amounts_visible", it) }
            },
            onLiquidGlassEnabledChange = {
                liquidGlassEnabled = it
                preferences.edit { putBoolean("liquid_glass_enabled", it) }
            },
            onSoundEnabledChange = {
                soundEnabled = it
                preferences.edit { putBoolean("sound_enabled", it) }
            },
            onHapticEnabledChange = {
                hapticEnabled = it
                preferences.edit { putBoolean("haptic_enabled", it) }
            },
        )
    }
}

@Composable
private fun rememberThermalStatus(): Int {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    var status by remember { mutableIntStateOf(powerManager.currentThermalStatus) }
    DisposableEffect(powerManager) {
        val listener = PowerManager.OnThermalStatusChangedListener { status = it }
        powerManager.addThermalStatusListener(listener)
        onDispose { powerManager.removeThermalStatusListener(listener) }
    }
    return status
}

private inline fun <reified T : Enum<T>> enumPreference(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

@Composable
private fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    var enabled by remember { mutableStateOf(powerManager.isPowerSaveMode) }

    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receivingContext: Context?, intent: Intent?) {
                enabled = powerManager.isPowerSaveMode
            }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return enabled
}
