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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heima.accounting.HeimaViewModel
import com.heima.accounting.designsystem.HeimaAccountingTheme
import com.heima.accounting.designsystem.HeimaColorMode
import com.heima.accounting.designsystem.VisualQuality

@Composable
fun HeimaApp() {
    val viewModel: HeimaViewModel = viewModel()
    val ledgerState by viewModel.ledgerState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val powerSaveMode = rememberPowerSaveMode()
    val thermalStatus = rememberThermalStatus()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.colorMode) {
        HeimaColorMode.SYSTEM -> systemDark
        HeimaColorMode.LIGHT -> false
        HeimaColorMode.DARK -> true
    }
    // 默认"最佳配置"硬编码：玻璃开、音效开、触觉开、画质精美；
    // 仅在系统省电模式或设备过热时静默降级，不打扰用户。
    val effectiveQuality = if (powerSaveMode || thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
        VisualQuality.POWER_SAVER
    } else {
        VisualQuality.REFINED
    }
    val feedback = rememberInteractionFeedback(
        soundEnabled = { true },
        hapticEnabled = { true },
    )

    HeimaAccountingTheme(
        style = settings.themeStyle,
        darkTheme = darkTheme,
        quality = effectiveQuality,
        reduceMotion = settings.reduceMotionEnabled,
        liquidGlassEnabled = true,
    ) {
        HeimaShell(
            viewModel = viewModel,
            ledgerState = ledgerState,
            feedback = feedback,
            themeStyle = settings.themeStyle,
            colorMode = settings.colorMode,
            reduceMotion = settings.reduceMotionEnabled,
            amountsVisible = settings.amountsVisible,
            onThemeStyleChange = viewModel::setThemeStyle,
            onColorModeChange = viewModel::setColorMode,
            onReduceMotionChange = viewModel::setReduceMotionEnabled,
            onAmountsVisibleChange = viewModel::setAmountsVisible,
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
