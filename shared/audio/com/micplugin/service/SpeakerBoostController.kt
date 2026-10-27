package com.micplugin.service

import android.media.audiofx.LoudnessEnhancer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SpeakerBoostState(val gainDb: Float = 0f, val active: Boolean = false,
    val available: Boolean? = null, val error: String? = null)
internal interface SpeakerLoudnessEffect {
    fun configure(gainMillibels: Int, enabled: Boolean)
    fun release()
}

class SpeakerBoostController internal constructor(private val factory: (Int) -> SpeakerLoudnessEffect) {
    constructor() : this({ session ->
        val effect = LoudnessEnhancer(session)
        object : SpeakerLoudnessEffect {
            override fun configure(gainMillibels: Int, enabled: Boolean) {
                effect.setTargetGain(gainMillibels); effect.enabled = enabled
            }
            override fun release() = effect.release()
        }
    })
    private val _state = MutableStateFlow(SpeakerBoostState())
    val state: StateFlow<SpeakerBoostState> = _state
    private var effect: SpeakerLoudnessEffect? = null
    private var session: Int? = null

    @Synchronized fun setGain(db: Float) {
        val gain = if (db.isFinite()) db.coerceIn(0f, 24f) else 0f
        _state.value = _state.value.copy(gainDb = gain)
        if (gain == 0f) {
            try { effect?.configure(0, false) } catch (_: RuntimeException) {}
            releaseEffect()
            _state.value = _state.value.copy(active = false, available = null, error = null)
        } else if (session != null && _state.value.available != false) configure()
    }
    @Synchronized fun attach(sessionId: Int) {
        detach()
        session = sessionId
        // No insert effect at zero gain; never attach an enhancer to global session 0.
        if (_state.value.gainDb > 0f) configure()
    }
    private fun configure() {
        val id = session ?: return
        if (id <= 0) { unavailable(); return }
        try {
            if (effect == null) effect = factory(id)
            effect!!.configure((_state.value.gainDb * 100).toInt(), true)
            _state.value = _state.value.copy(active = true, available = true, error = null)
        } catch (_: RuntimeException) { unavailable() }
    }
    private fun releaseEffect() {
        try { effect?.release() } catch (_: RuntimeException) {}
        effect = null
    }
    private fun unavailable() {
        releaseEffect()
        _state.value = _state.value.copy(active = false, available = false,
            error = "Устройство не поддерживает усиление вывода MicUp")
    }
    @Synchronized fun detach() {
        releaseEffect(); session = null
        _state.value = _state.value.copy(active = false, available = null, error = null)
    }
}
