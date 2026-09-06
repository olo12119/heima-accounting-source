package com.heima.accounting.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InteractionFeedbackTest {
    @Test
    fun soundAndHapticGatesAreIndependentAndUsePositiveSemantics() {
        var soundEnabled = true
        var hapticEnabled = false
        val sounds = mutableListOf<InteractionSound>()
        val haptics = mutableListOf<HapticCue>()
        val feedback = InteractionFeedback(
            playSound = sounds::add,
            performHaptic = haptics::add,
            soundEnabled = { soundEnabled },
            hapticEnabled = { hapticEnabled },
        )

        feedback.confirm()
        assertEquals(listOf(InteractionSound.CONFIRM), sounds)
        assertEquals(emptyList<HapticCue>(), haptics)

        soundEnabled = false
        hapticEnabled = true
        feedback.selection()
        feedback.undo()
        assertEquals(listOf(InteractionSound.CONFIRM), sounds)
        assertEquals(listOf(HapticCue.SELECTION, HapticCue.CONFIRM), haptics)
    }

    @Test
    fun phase4CueMethodsMapToTheDocumentedSoundAndHaptic() {
        val sounds = mutableListOf<InteractionSound>()
        val haptics = mutableListOf<HapticCue>()
        val feedback = InteractionFeedback(
            playSound = sounds::add,
            performHaptic = haptics::add,
            soundEnabled = { true },
            hapticEnabled = { true },
        )

        feedback.keyTick()          // E1：键盘轻咔 + 轻 tick
        feedback.switch()           // E2：切换嗡 + 中档 tick
        feedback.amountSettled()    // E3：叮（仅声，无振）
        feedback.budgetExceeded()   // E4：超额闷响 + 长振
        feedback.firstRecordSuccess() // E5：琶音 + 确认振

        assertEquals(
            listOf(
                InteractionSound.KEY_TICK,
                InteractionSound.SWITCH_WHOOSH,
                InteractionSound.DING,
                InteractionSound.OVER_BUDGET,
                InteractionSound.FIRST_ARIA,
            ),
            sounds,
        )
        assertEquals(
            listOf(HapticCue.SELECTION, HapticCue.MEDIUM_TICK, HapticCue.IMPORTANT, HapticCue.CONFIRM),
            haptics,
        )
    }

    @Test
    fun amountSettledIsSoundOnlyAndRespectsSoundGate() {
        var soundEnabled = true
        val sounds = mutableListOf<InteractionSound>()
        val haptics = mutableListOf<HapticCue>()
        val feedback = InteractionFeedback(
            playSound = sounds::add,
            performHaptic = haptics::add,
            soundEnabled = { soundEnabled },
            hapticEnabled = { true },
        )

        feedback.amountSettled()
        assertEquals(listOf(InteractionSound.DING), sounds)
        assertEquals(emptyList<HapticCue>(), haptics)

        // 声音关闭后不再发声，也不产生任何触觉。
        soundEnabled = false
        feedback.amountSettled()
        assertEquals(listOf(InteractionSound.DING), sounds)
        assertEquals(emptyList<HapticCue>(), haptics)
    }
}
