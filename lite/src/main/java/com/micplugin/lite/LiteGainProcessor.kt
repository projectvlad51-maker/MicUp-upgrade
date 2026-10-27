package com.micplugin.lite

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

// Only changes amplitude: smooth boost plus a block peak limiter, no tone/EQ/pitch effects.
class LiteGainProcessor(private val sampleRate: Int = 48000) {
    private var gain = 1f
    private var limiter = 1f
    private val smoothing = (1.0 - exp(-1.0 / (sampleRate * 0.01))).toFloat()
    fun process(samples: FloatArray, count: Int, db: Float) {
        val cleanDb = if (db.isFinite()) db.coerceIn(0f, 36f) else 0f
        val target = 10.0.pow(cleanDb / 20.0).toFloat()
        var peak = 0f
        for (i in 0 until count) {
            gain += smoothing * (target - gain)
            samples[i] *= gain
            peak = max(peak, abs(samples[i]))
        }
        val limit = if (peak > 0.98f) 0.98f / peak else 1f
        if (limit < limiter) limiter = limit
        else limiter += (1.0 - exp(-count.toDouble() / (sampleRate * 0.08))).toFloat() * (limit - limiter)
        for (i in 0 until count) samples[i] *= limiter
    }
}
