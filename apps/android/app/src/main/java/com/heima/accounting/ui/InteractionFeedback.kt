package com.heima.accounting.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class InteractionSound { CONFIRM, IMPORTANT, UNDO, KEY_TICK, SWITCH_WHOOSH, DING, OVER_BUDGET, FIRST_ARIA }
internal enum class HapticCue { CONFIRM, IMPORTANT, SELECTION, REJECT, MEDIUM_TICK }

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

    // 四期新增音效事件（E1~E5）
    fun keyTick() = emit(InteractionSound.KEY_TICK, HapticCue.SELECTION)
    fun switch() = emit(InteractionSound.SWITCH_WHOOSH, HapticCue.MEDIUM_TICK)
    fun amountSettled() {
        if (soundEnabled()) playSound(InteractionSound.DING)
    }
    fun budgetExceeded() = emit(InteractionSound.OVER_BUDGET, HapticCue.IMPORTANT)
    fun firstRecordSuccess() = emit(InteractionSound.FIRST_ARIA, HapticCue.CONFIRM)

    fun selection() {
        if (hapticEnabled()) performHaptic(HapticCue.SELECTION)
    }

    /** 校验失败不发声：Reject 双振独立承担错误信号。 */
    fun error() {
        if (hapticEnabled()) performHaptic(HapticCue.REJECT)
    }

    private fun emit(sound: InteractionSound, haptic: HapticCue) {
        if (soundEnabled()) playSound(sound)
        if (hapticEnabled()) performHaptic(haptic)
    }

    internal fun release() = releaseOutput()
}

/**
 * 一个合成音符：s(t) = env(t) · Σ ampᵢ · sin(2π · f · ratioᵢ · t)。
 * attackMs 线性起音；之后按 exp(−(t−attack)/τ) 指数衰减，τ = durationMs / decayDivisor；
 * 末尾 10ms raised-cosine 淡出到 0，杜绝爆音与拖尾杂音。
 */
private data class SynthNote(
    val frequencyHz: Double,
    val startMs: Int,
    val durationMs: Int,
    val partials: List<Pair<Double, Double>>, // (ratio, amp)
    val attackMs: Int = 6,
    val decayDivisor: Double = 4.2,
)

/** 明亮族：微失谐 ±2~3 音分带来"暖感"合唱，高次泛音提供空气感。 */
private val BrightPartials = listOf(
    1.0000 to 1.00,
    1.0016 to 0.55,
    0.9986 to 0.55,
    2.0000 to 0.32,
    3.0100 to 0.11,
    4.0300 to 0.05,
)

/** 柔和族：增强二次泛音、砍掉高频，听感圆润不发尖（IMPORTANT"闷响"用）。 */
private val SoftPartials = listOf(
    1.0000 to 1.00,
    1.0016 to 0.45,
    0.9986 to 0.45,
    2.0000 to 0.42,
    3.0100 to 0.06,
    4.0300 to 0.02,
)

private data class SoundRecipe(
    val notes: List<SynthNote>,
    val totalDurationMs: Int,
    val volume: Float,
)

private val SoundRecipes = mapOf(
    // 保存成功：E5 → A5 上行纯四度，30ms 交叠。
    InteractionSound.CONFIRM to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 659.26, startMs = 0, durationMs = 120, partials = BrightPartials),
            SynthNote(frequencyHz = 880.00, startMs = 90, durationMs = 160, partials = BrightPartials),
        ),
        totalDurationMs = 250,
        volume = .26f,
    ),
    // 删除/重要：C4 低中频柔和单音闷响。
    InteractionSound.IMPORTANT to SoundRecipe(
        notes = listOf(
            SynthNote(
                frequencyHz = 261.63,
                startMs = 0,
                durationMs = 190,
                partials = SoftPartials,
                attackMs = 8,
                decayDivisor = 3.6,
            ),
        ),
        totalDurationMs = 190,
        volume = .24f,
    ),
    // 撤销：A5 → E5 下行，与 CONFIRM 镜像呼应。
    InteractionSound.UNDO to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 880.00, startMs = 0, durationMs = 110, partials = BrightPartials),
            SynthNote(frequencyHz = 659.26, startMs = 85, durationMs = 140, partials = BrightPartials),
        ),
        totalDurationMs = 225,
        volume = .25f,
    ),
    // 键盘轻咔（E1）
    InteractionSound.KEY_TICK to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 1760.00, startMs = 0, durationMs = 40, partials = BrightPartials, attackMs = 2, decayDivisor = 2.0),
        ),
        totalDurationMs = 40,
        volume = .16f,
    ),
    // 图表切换嗡（E2）
    InteractionSound.SWITCH_WHOOSH to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 220.00, startMs = 0, durationMs = 40, partials = SoftPartials, attackMs = 3, decayDivisor = 2.0),
        ),
        totalDurationMs = 40,
        volume = .18f,
    ),
    // 数字滚动到位（E3）
    InteractionSound.DING to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 1318.51, startMs = 0, durationMs = 130, partials = BrightPartials, attackMs = 4, decayDivisor = 3.4),
        ),
        totalDurationMs = 130,
        volume = .24f,
    ),
    // 预算超额闷响（E4）
    InteractionSound.OVER_BUDGET to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 174.61, startMs = 0, durationMs = 200, partials = SoftPartials, attackMs = 8, decayDivisor = 3.2),
        ),
        totalDurationMs = 200,
        volume = .26f,
    ),
    // 首次记账琶音 C5 → E5 → G5（E5）
    InteractionSound.FIRST_ARIA to SoundRecipe(
        notes = listOf(
            SynthNote(frequencyHz = 523.25, startMs = 0, durationMs = 110, partials = BrightPartials),
            SynthNote(frequencyHz = 659.26, startMs = 70, durationMs = 120, partials = BrightPartials),
            SynthNote(frequencyHz = 783.99, startMs = 140, durationMs = 150, partials = BrightPartials),
        ),
        totalDurationMs = 290,
        volume = .28f,
    ),
)

/** 同一事件两次播放间隔小于该值时直接丢弃，避免连点叠爆。 */
private const val PLAY_DEBOUNCE_MS = 60L

/**
 * Preloads the generated micro-sonifications into SoundPool. Samples are
 * synthesized once into cacheDir (v2 waveform: partial stacking + envelopes),
 * keeping them tiny, local and license-free with no bundled audio resources.
 */
private class InteractionSoundManager(context: Context) : AutoCloseable {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
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
    private val lastPlayAtMs = mutableMapOf<InteractionSound, Long>()

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
        SoundRecipes.forEach { (sound, recipe) -> load(folder, sound, recipe) }
    }

    fun play(sound: InteractionSound) {
        val now = SystemClock.elapsedRealtime()
        val last = lastPlayAtMs[sound]
        if (last != null && now - last < PLAY_DEBOUNCE_MS) return
        lastPlayAtMs[sound] = now
        val sampleId = sampleBySound[sound] ?: return
        if (sampleId !in loadedSamples) {
            pendingSound = sound
            return
        }
        val volume = SoundRecipes.getValue(sound).volume
        soundPool.play(sampleId, volume, volume, 1, 0, 1f)
    }

    private fun load(folder: File, sound: InteractionSound, recipe: SoundRecipe) {
        // v2 文件名强制老设备放弃 v1 正弦波缓存并重新合成。
        val file = File(folder, "${sound.name.lowercase()}-v2.wav")
        if (!file.exists()) writeWav(file, synthesizePcm(recipe.notes, recipe.totalDurationMs))
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
    val scope = rememberCoroutineScope()
    val soundManager = remember(context) { InteractionSoundManager(context.applicationContext) }
    val feedback = remember(soundManager, haptic) {
        InteractionFeedback(
            playSound = soundManager::play,
            performHaptic = { cue ->
                when (cue) {
                    // 错误反馈为双振：两次 Reject 间隔 100ms。
                    HapticCue.REJECT -> scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        delay(100)
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    }
                    HapticCue.CONFIRM -> haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    HapticCue.IMPORTANT -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    HapticCue.SELECTION -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    // 比 SELECTION 高一档、比 CONFIRM 低一档：连续两次轻 tick。
                    HapticCue.MEDIUM_TICK -> scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(40)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
            soundEnabled = soundEnabled,
            hapticEnabled = hapticEnabled,
            releaseOutput = soundManager::close,
        )
    }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    return feedback
}

/**
 * Mixes every note into one mono track and normalizes the final peak to
 * [masterAmplitude]. Each note is first normalized by its own partial-sum, so
 * 音色家族内不同事件的相对响度一致；最终峰值统一 0.45，配合 SoundPool
 * 0.22~0.26 的播放音量，有效峰值约 −18.6dB ~ −20dB 满幅。
 */
private fun synthesizePcm(
    notes: List<SynthNote>,
    totalDurationMs: Int,
    masterAmplitude: Double = 0.45,
    sampleRate: Int = 22_050,
): ShortArray {
    val sampleCount = sampleRate * totalDurationMs / 1_000
    val mix = DoubleArray(sampleCount)
    notes.forEach { note ->
        val startSample = sampleRate * note.startMs / 1_000
        val noteSamples = sampleRate * note.durationMs / 1_000
        val normalization = note.partials.sumOf { it.second }.coerceAtLeast(1e-9)
        val attackSamples = (sampleRate * note.attackMs / 1_000).coerceAtLeast(1)
        val tailSamples = (sampleRate * 10 / 1_000).coerceAtMost(noteSamples)
        val tauMs = note.durationMs / note.decayDivisor
        for (i in 0 until noteSamples) {
            val index = startSample + i
            if (index >= sampleCount) break
            val tMs = i * 1_000.0 / sampleRate
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decay = if (tMs <= note.attackMs) 1.0 else exp(-(tMs - note.attackMs) / tauMs)
            val tail = if (i >= noteSamples - tailSamples) {
                val remaining = (noteSamples - i).toDouble() / tailSamples
                0.5 * (1.0 + cos(PI * (1.0 - remaining)))
            } else {
                1.0
            }
            val tSeconds = i.toDouble() / sampleRate
            var sample = 0.0
            note.partials.forEach { (ratio, amplitude) ->
                sample += amplitude * sin(2.0 * PI * note.frequencyHz * ratio * tSeconds)
            }
            mix[index] += attack * decay * tail * sample / normalization
        }
    }
    val peak = mix.maxOf(::abs).coerceAtLeast(1e-9)
    val scale = masterAmplitude / peak * Short.MAX_VALUE
    return ShortArray(sampleCount) { index ->
        (mix[index] * scale)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }
}

private fun writeWav(file: File, pcm: ShortArray, sampleRate: Int = 22_050) {
    val pcmBytes = pcm.size * 2
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
        pcm.forEach { output.writeLittleEndianShort(it.toInt()) }
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
