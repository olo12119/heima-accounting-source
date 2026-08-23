package com.heima.accounting.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

internal enum class InteractionSound { CONFIRM, IMPORTANT, UNDO }
internal enum class HapticCue { CONFIRM, IMPORTANT, SELECTION }

/** Sound, haptic and visual feedback remain independent user-facing systems. */
@Stable
class InteractionFeedback internal constructor(
    private val playSound: (InteractionSound) -> Unit,
    private val performHaptic: (HapticCue) -> Unit,
    private val soundEnabled: () -> Boolean,
    private val hapticEnabled: () -> Boolean,
    private val releaseOutput: () -> Unit = {},
) {
    fun confirm() = emit(InteractionSound.CONFIRM, HapticCue.CONFIRM)
    fun important() = emit(InteractionSound.IMPORTANT, HapticCue.IMPORTANT)
    fun undo() = emit(InteractionSound.UNDO, HapticCue.CONFIRM)

    fun selection() {
        if (hapticEnabled()) performHaptic(HapticCue.SELECTION)
    }

    private fun emit(sound: InteractionSound, haptic: HapticCue) {
        if (soundEnabled()) playSound(sound)
        if (hapticEnabled()) performHaptic(haptic)
    }

    internal fun release() = releaseOutput()
}

/**
 * Preloads three original, generated micro-sonifications into SoundPool. This
 * avoids creating MediaPlayer/ToneGenerator objects on every interaction and
 * keeps all samples tiny, local and license-free.
 */
private class InteractionSoundManager(context: Context) : AutoCloseable {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val sampleBySound = mutableMapOf<InteractionSound, Int>()
    private val loadedSamples = mutableSetOf<Int>()
    private var pendingSound: InteractionSound? = null

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSamples += sampleId
                val pending = pendingSound
                if (pending != null && sampleBySound[pending] == sampleId) {
                    pendingSound = null
                    play(pending)
                }
            }
        }
        val folder = File(context.cacheDir, "interaction-sounds").apply { mkdirs() }
        load(folder, InteractionSound.CONFIRM, 880.0, 54, .26)
        load(folder, InteractionSound.IMPORTANT, 510.0, 64, .22)
        load(folder, InteractionSound.UNDO, 690.0, 48, .24)
    }

    fun play(sound: InteractionSound) {
        val sampleId = sampleBySound[sound] ?: return
        if (sampleId !in loadedSamples) {
            pendingSound = sound
            return
        }
        val volume = when (sound) {
            InteractionSound.CONFIRM -> .34f
            InteractionSound.IMPORTANT -> .28f
            InteractionSound.UNDO -> .30f
        }
        soundPool.play(sampleId, volume, volume, 1, 0, 1f)
    }

    private fun load(
        folder: File,
        sound: InteractionSound,
        frequencyHz: Double,
        durationMs: Int,
        amplitude: Double,
    ) {
        val file = File(folder, "${sound.name.lowercase()}-v1.wav")
        if (!file.exists()) writeMicroSound(file, frequencyHz, durationMs, amplitude)
        sampleBySound[sound] = soundPool.load(file.absolutePath, 1)
    }

    override fun close() = soundPool.release()
}

@Composable
fun rememberInteractionFeedback(
    soundEnabled: () -> Boolean,
    hapticEnabled: () -> Boolean,
): InteractionFeedback {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val soundManager = remember(context) { InteractionSoundManager(context.applicationContext) }
    val feedback = remember(soundManager, haptic) {
        InteractionFeedback(
            playSound = soundManager::play,
            performHaptic = { cue ->
                haptic.performHapticFeedback(
                    when (cue) {
                        HapticCue.CONFIRM -> HapticFeedbackType.Confirm
                        HapticCue.IMPORTANT -> HapticFeedbackType.LongPress
                        HapticCue.SELECTION -> HapticFeedbackType.TextHandleMove
                    },
                )
            },
            soundEnabled = soundEnabled,
            hapticEnabled = hapticEnabled,
            releaseOutput = soundManager::close,
        )
    }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    return feedback
}

private fun writeMicroSound(
    file: File,
    frequencyHz: Double,
    durationMs: Int,
    amplitude: Double,
) {
    val sampleRate = 22_050
    val sampleCount = sampleRate * durationMs / 1_000
    val pcmBytes = sampleCount * 2
    DataOutputStream(FileOutputStream(file)).use { output ->
        output.writeBytes("RIFF")
        output.writeLittleEndianInt(36 + pcmBytes)
        output.writeBytes("WAVEfmt ")
        output.writeLittleEndianInt(16)
        output.writeLittleEndianShort(1)
        output.writeLittleEndianShort(1)
        output.writeLittleEndianInt(sampleRate)
        output.writeLittleEndianInt(sampleRate * 2)
        output.writeLittleEndianShort(2)
        output.writeLittleEndianShort(16)
        output.writeBytes("data")
        output.writeLittleEndianInt(pcmBytes)
        repeat(sampleCount) { index ->
            val phase = 2.0 * PI * frequencyHz * index / sampleRate
            val progress = index.toDouble() / sampleCount.coerceAtLeast(1)
            val envelope = sin(PI * progress).coerceAtLeast(0.0)
            val sample = (sin(phase) * envelope * amplitude * Short.MAX_VALUE).toInt()
            output.writeLittleEndianShort(sample)
        }
    }
}

private fun DataOutputStream.writeLittleEndianInt(value: Int) {
    writeByte(value and 0xFF)
    writeByte(value ushr 8 and 0xFF)
    writeByte(value ushr 16 and 0xFF)
    writeByte(value ushr 24 and 0xFF)
}

private fun DataOutputStream.writeLittleEndianShort(value: Int) {
    writeByte(value and 0xFF)
    writeByte(value ushr 8 and 0xFF)
}
