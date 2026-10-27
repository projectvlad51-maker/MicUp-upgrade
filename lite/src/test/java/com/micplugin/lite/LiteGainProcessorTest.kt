package com.micplugin.lite

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class LiteGainProcessorTest {
    @Test fun zeroBoostPreservesNormalSamplesExactly() {
        val x = floatArrayOf(0f, 0.1f, -0.5f, 0.75f)
        val original = x.copyOf()
        LiteGainProcessor().process(x, x.size, 0f)
        assertArrayEquals(original, x, 0f)
    }
    @Test fun quietSignalGetsSixDbGainWithSmoothTransition() {
        val x = FloatArray(48000) { 0.01f }
        LiteGainProcessor().process(x, x.size, 6f)
        assertTrue(x.first() > 0.01f && x.first() < 0.0101f)
        assertEquals(0.0199526f, x.last(), 0.00001f)
    }
    @Test fun maximumGainHasNoOverflowOrHardClippedSamples() {
        val processor = LiteGainProcessor()
        repeat(100) {
            val x = FloatArray(512) { if (it % 2 == 0) 0.4f else -0.4f }
            processor.process(x, x.size, 36f)
            assertTrue(x.all { it.isFinite() && abs(it) <= 0.98001f })
            assertTrue(x[0] > 0 && x[1] < 0)
        }
    }
    @Test fun boostDoesNotChangeRelativeFrequencyResponseForQuietSpeech() {
        fun ratio(hz: Float): Double {
            val processor = LiteGainProcessor()
            var inputEnergy = 0.0
            var outputEnergy = 0.0
            repeat(100) { block ->
                val x = FloatArray(512) { i -> 0.005f * sin(2 * PI * hz * (block * 512 + i) / 48000).toFloat() }
                if (block > 50) inputEnergy += x.sumOf { it.toDouble() * it }
                processor.process(x, x.size, 12f)
                if (block > 50) outputEnergy += x.sumOf { it.toDouble() * it }
            }
            return sqrt(outputEnergy / inputEnergy)
        }
        assertEquals(10.0.pow(12.0 / 20), ratio(400f), 0.002)
        assertEquals(ratio(400f), ratio(3200f), 0.002)
    }
}
