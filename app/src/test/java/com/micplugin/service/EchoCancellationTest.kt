package com.micplugin.service

import org.junit.Assert.*
import org.junit.Test

class EchoCancellationTest {
    private class Effect : EchoEffect {
        var released = false
        override fun enable() = true
        override fun release() { released = true }
    }
    @Test fun attachesEchoCancellationToCaptureSession() {
        var session = 0
        val effect = Effect()
        val controller = EchoCancellation({ true }, { session = it; effect })
        controller.attach(17)
        assertEquals(17, session)
        assertTrue(controller.state.value.active)
        controller.detach()
        assertTrue(effect.released)
        assertFalse(controller.state.value.active)
    }
    @Test fun unsupportedDeviceDoesNotPretendEchoIsCancelled() {
        val controller = EchoCancellation({ false }, { error("Must not create unsupported effect") })
        controller.attach(17)
        assertEquals(false, controller.state.value.available)
        assertFalse(controller.state.value.active)
    }
    @Test fun invalidCaptureSessionNeverCreatesEffect() {
        val controller = EchoCancellation({ true }, { error("Invalid session") })
        controller.attach(-1)
        assertFalse(controller.state.value.active)
        assertEquals(false, controller.state.value.available)
    }
    @Test fun nullEffectIsReportedAsUnavailable() {
        val controller = EchoCancellation({ true }, { null })
        controller.attach(10)
        assertEquals(false, controller.state.value.available)
    }
    @Test fun enableFailureReleasesEffectAndReportsUnavailable() {
        var released = false
        val controller = EchoCancellation({ true }, {
            object : EchoEffect {
                override fun enable(): Boolean { throw IllegalStateException() }
                override fun release() { released = true }
            }
        })
        controller.attach(12)
        assertTrue(released)
        assertFalse(controller.state.value.active)
    }
    @Test fun restartedCaptureGetsNewEffectAndReleasesOldSession() {
        val effects = mutableListOf<Effect>()
        val controller = EchoCancellation({ true }, { Effect().also { effects.add(it) } })
        controller.attach(10); controller.attach(11)
        assertTrue(effects.first().released)
        assertFalse(effects.last().released)
        controller.detach()
        assertTrue(effects.last().released)
    }
}
