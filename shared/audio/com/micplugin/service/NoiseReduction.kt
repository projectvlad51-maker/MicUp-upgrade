package com.micplugin.service

import android.media.audiofx.AudioEffect
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NoiseReductionState(val active: Boolean = false, val available: Boolean? = null)
internal interface NoiseEffect { fun enabled(value: Boolean): Boolean; fun release() }

class NoiseReduction internal constructor(private val supported: () -> Boolean, private val create: (Int) -> NoiseEffect?) {
    constructor() : this({ NoiseSuppressor.isAvailable() }, { session ->
        NoiseSuppressor.create(session)?.let { fx ->
            object : NoiseEffect {
                override fun enabled(value: Boolean) = fx.setEnabled(value) == AudioEffect.SUCCESS && fx.enabled == value
                override fun release() = fx.release()
            }
        }
    })
    private var effect: NoiseEffect? = null
    private var requested = true
    private val _state = MutableStateFlow(NoiseReductionState())
    val state: StateFlow<NoiseReductionState> = _state

    @Synchronized fun attach(session: Int, enabled: Boolean) {
        detach()
        requested = enabled
        try {
            if (session <= 0 || !supported()) { _state.value = NoiseReductionState(available = false); return }
            effect = create(session)
            configure()
        } catch (_: RuntimeException) { failed() }
    }
    @Synchronized fun setEnabled(enabled: Boolean) {
        requested = enabled
        if (effect != null) try { configure() } catch (_: RuntimeException) { failed() }
    }
    private fun configure() {
        val applied = effect?.enabled(requested) == true
        if (applied) _state.value = NoiseReductionState(active = requested, available = true)
        else failed()
    }
    private fun failed() {
        try { effect?.release() } catch (_: RuntimeException) {}
        effect = null
        _state.value = NoiseReductionState(available = false)
    }
    @Synchronized fun detach() {
        try { effect?.release() } catch (_: RuntimeException) {}
        effect = null
        _state.value = NoiseReductionState()
    }
}
