package com.heima.accounting.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.heima.accounting.designsystem.HeimaAccountingTheme
import com.heima.accounting.designsystem.HeimaThemeStyle
import com.heima.accounting.designsystem.VisualQuality

@Composable
fun HeimaApp() {
    var themeStyle by rememberSaveable { mutableStateOf(HeimaThemeStyle.LIQUID_GLASS) }
    var quality by rememberSaveable { mutableStateOf(VisualQuality.AUTO) }
    var reduceMotion by rememberSaveable { mutableStateOf(false) }
    val powerSaveMode = rememberPowerSaveMode()
    val effectiveQuality = when {
        quality == VisualQuality.AUTO && powerSaveMode -> VisualQuality.POWER_SAVER
        else -> quality
    }

    HeimaAccountingTheme(
        style = themeStyle,
        quality = effectiveQuality,
        reduceMotion = reduceMotion,
    ) {
        HeimaShell(
            themeStyle = themeStyle,
            visualQuality = quality,
            reduceMotion = reduceMotion,
            powerSaveMode = powerSaveMode,
            onThemeStyleChange = { themeStyle = it },
            onVisualQualityChange = { quality = it },
            onReduceMotionChange = { reduceMotion = it },
        )
    }
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

