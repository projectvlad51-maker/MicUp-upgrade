package com.micplugin.service

import org.junit.Assert.*
import org.junit.Test

class NoiseReductionTest {
    private class Effect : NoiseEffect {
        var value = false
        var released = false
        override fun enabled(value: Boolean): Boolean { this.value = value; return true }
        override fun release() { released = true }
    }
    @Test fun canToggleSuppressionWithoutChangingCaptureSession() {
        val effect = Effect()
        var calls = 0
        val control = NoiseReduction({ true }, { calls++; effect })
        control.attach(20, true)
        assertTrue(control.state.value.active)
        control.setEnabled(false)
        assertFalse(effect.value)
        assertFalse(control.state.value.active)
        control.setEnabled(true)
        assertTrue(effect.value)
        assertEquals(1, calls)
    }
    @Test fun unsupportedHardwareIsReportedHonestly() {
        val control = NoiseReduction({ false }, { error("Unsupported") })
        control.attach(20, true)
        assertFalse(control.state.value.active)
        assertEquals(false, control.state.value.available)
    }
    @Test fun disabledPreferenceIsAppliedOnCaptureStart() {
        val effect = Effect()
        val control = NoiseReduction({ true }, { effect })
        control.attach(21, false)
        assertFalse(effect.value)
        assertFalse(control.state.value.active)
        assertEquals(true, control.state.value.available)
    }
    @Test fun detachReleasesEffectAndClearsActiveState() {
        val effect = Effect()
        val control = NoiseReduction({ true }, { effect })
        control.attach(21, true); control.detach()
        assertTrue(effect.released)
        assertFalse(control.state.value.active)
    }
    @Test fun invalidSessionDoesNotEnableSuppression() {
        val control = NoiseReduction({ true }, { error("Invalid") })
        control.attach(-1, true)
        assertFalse(control.state.value.active)
    }
}
