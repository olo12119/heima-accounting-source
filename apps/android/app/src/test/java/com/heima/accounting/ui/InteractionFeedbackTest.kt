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
}
