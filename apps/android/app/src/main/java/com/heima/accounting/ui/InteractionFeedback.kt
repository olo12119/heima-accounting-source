package com.heima.accounting.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

@Stable
class InteractionFeedback internal constructor(
    private val toneGenerator: ToneGenerator,
    private val haptic: HapticFeedback,
    private val soundEnabled: () -> Boolean,
    private val hapticEnabled: () -> Boolean,
) {
    fun confirm() {
        if (soundEnabled()) toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 55)
        if (hapticEnabled()) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun important() {
        if (soundEnabled()) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 65)
        if (hapticEnabled()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun selection() {
        if (hapticEnabled()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    internal fun release() = toneGenerator.release()
}

@Composable
fun rememberInteractionFeedback(
    soundEnabled: () -> Boolean,
    hapticEnabled: () -> Boolean,
): InteractionFeedback {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val feedback = remember(audioManager, haptic) {
        InteractionFeedback(
            ToneGenerator(AudioManager.STREAM_SYSTEM, 20),
            haptic,
            soundEnabled,
            hapticEnabled,
        )
    }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    return feedback
}
