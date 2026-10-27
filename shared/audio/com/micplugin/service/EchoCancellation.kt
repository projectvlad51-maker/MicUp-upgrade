package com.micplugin.service

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EchoCancellationState(val active: Boolean = false, val available: Boolean? = null)
internal interface EchoEffect { fun enable(): Boolean; fun release() }

class EchoCancellation internal constructor(
    private val supported: () -> Boolean,
    private val create: (Int) -> EchoEffect?,
) {
    constructor() : this({ AcousticEchoCanceler.isAvailable() }, { session ->
        AcousticEchoCanceler.create(session)?.let { effect ->
            object : EchoEffect {
                override fun enable() = effect.setEnabled(true) == AudioEffect.SUCCESS && effect.enabled
                override fun release() = effect.release()
            }
        }
    })
    private var effect: EchoEffect? = null
    private val _state = MutableStateFlow(EchoCancellationState())
    val state: StateFlow<EchoCancellationState> = _state

    @Synchronized fun attach(session: Int) {
        detach()
        try {
            if (session <= 0 || !supported()) { _state.value = EchoCancellationState(available = false); return }
            effect = create(session)
            val active = effect?.enable() == true
            if (!active) { effect?.release(); effect = null }
            _state.value = EchoCancellationState(active = active, available = active)
        } catch (_: RuntimeException) {
            try { effect?.release() } catch (_: RuntimeException) {}
            effect = null
            _state.value = EchoCancellationState(available = false)
        }
    }
    @Synchronized fun detach() {
        try { effect?.release() } catch (_: RuntimeException) {}
        effect = null
        _state.value = EchoCancellationState()
    }
}
