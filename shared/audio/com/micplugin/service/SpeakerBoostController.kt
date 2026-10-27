package com.micplugin.service

import android.media.audiofx.LoudnessEnhancer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SpeakerBoostState(
    val gainDb: Float = 0f,
    val active: Boolean = false,
    val available: Boolean? = null,
    val error: String? = null,
)

internal interface SpeakerLoudnessEffect {
    fun configure(gainMillibels: Int, enabled: Boolean)
    fun release()
}

class SpeakerBoostController internal constructor(private val factory: (Int) -> SpeakerLoudnessEffect) {
    constructor() : this({ session ->
        val effect = LoudnessEnhancer(session)
        object : SpeakerLoudnessEffect {
            override fun configure(gainMillibels: Int, enabled: Boolean) {
                effect.setTargetGain(gainMillibels)
                effect.enabled = enabled
            }
            override fun release() = effect.release()
        }
    })

    private val _state = MutableStateFlow(SpeakerBoostState())
    val state: StateFlow<SpeakerBoostState> = _state
    private var effect: SpeakerLoudnessEffect? = null

    fun setGain(db: Float) {
        val gain = if (db.isFinite()) db.coerceIn(0f, 24f) else 0f
        _state.value = _state.value.copy(gainDb = gain)
        configure()
    }

    fun attach(sessionId: Int) {
        detach()
        try {
            effect = factory(sessionId)
            _state.value = _state.value.copy(available = true, error = null)
            configure()
        } catch (_: RuntimeException) { unavailable() }
    }

    private fun configure() {
        val current = effect ?: return
        try {
            current.configure((_state.value.gainDb * 100).toInt(), _state.value.gainDb > 0f)
            _state.value = _state.value.copy(active = _state.value.gainDb > 0f, error = null)
        } catch (_: RuntimeException) { unavailable() }
    }

    private fun unavailable() {
        try { effect?.release() } catch (_: RuntimeException) {}
        effect = null
        _state.value = _state.value.copy(active = false, available = false,
            error = "Устройство не поддерживает усиление вывода MicUp")
    }

    fun detach() {
        try { effect?.release() } catch (_: RuntimeException) {}
        effect = null
        _state.value = _state.value.copy(active = false, available = null, error = null)
    }
}
