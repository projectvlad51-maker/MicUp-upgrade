package com.micplugin.service

import org.junit.Assert.*
import org.junit.Test

class SpeakerBoostControllerTest {
    private class Effect : SpeakerLoudnessEffect {
        var target = -1
        var enabled = false
        var released = false
        override fun configure(gainMillibels: Int, enabled: Boolean) { target = gainMillibels; this.enabled = enabled }
        override fun release() { released = true }
    }

    @Test fun boostWaitsForPlaybackSessionAndSurvivesRestart() {
        val effects = mutableListOf<Effect>()
        val sessions = mutableListOf<Int>()
        val control = SpeakerBoostController { session -> sessions.add(session); Effect().also { effects.add(it) } }
        control.setGain(18f)
        assertFalse(control.state.value.active)
        assertNull(control.state.value.available)
        control.attach(123)
        assertEquals(1800, effects[0].target)
        assertTrue(effects[0].enabled)
        assertTrue(control.state.value.active)
        control.detach()
        assertTrue(effects[0].released)
        assertFalse(control.state.value.active)
        control.attach(456)
        assertEquals(listOf(123, 456), sessions)
        assertEquals(1800, effects[1].target)
    }

    @Test fun targetUpdatesWithoutRecreatingEffectAndZeroDisablesBoost() {
        val effect = Effect()
        var creations = 0
        val control = SpeakerBoostController { creations++; effect }
        control.attach(20)
        control.setGain(6f)
        assertEquals(600, effect.target)
        control.setGain(12f)
        assertEquals(1200, effect.target)
        assertEquals(1, creations)
        control.setGain(0f)
        assertFalse(effect.enabled)
        assertFalse(control.state.value.active)
    }

    @Test fun invalidAndExcessiveGainIsClamped() {
        val effect = Effect()
        val control = SpeakerBoostController { effect }
        control.attach(10)
        control.setGain(100f)
        assertEquals(2400, effect.target)
        control.setGain(-10f)
        assertEquals(0, effect.target)
        control.setGain(Float.NaN)
        assertEquals(0f, control.state.value.gainDb)
    }

    @Test fun unsupportedHardwareDoesNotPretendBoostIsActive() {
        val control = SpeakerBoostController { throw UnsupportedOperationException() }
        control.setGain(12f)
        control.attach(10)
        assertEquals(false, control.state.value.available)
        assertFalse(control.state.value.active)
        assertNotNull(control.state.value.error)
        assertEquals(12f, control.state.value.gainDb)
    }

    @Test fun failedEffectConfigurationReleasesResource() {
        var released = false
        val control = SpeakerBoostController {
            object : SpeakerLoudnessEffect {
                override fun configure(gainMillibels: Int, enabled: Boolean) { throw IllegalStateException() }
                override fun release() { released = true }
            }
        }
        control.attach(10)
        assertTrue(released)
        assertEquals(false, control.state.value.available)
        assertFalse(control.state.value.active)
    }
}
