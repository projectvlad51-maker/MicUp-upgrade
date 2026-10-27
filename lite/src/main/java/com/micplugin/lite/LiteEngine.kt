package com.micplugin.lite

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.micplugin.audio.NativeAudioDriver
import com.micplugin.audio.OboeEngine
import com.micplugin.service.SoftwareLoopback
import com.micplugin.service.SpeakerBoostState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A small UI/service around the original MicUp engine and queued playback. */
data class LiteRunState(val running: Boolean = false, val inputDb: Float = -100f,
    val outputDb: Float = -100f, val error: String? = null, val routeWarning: String? = null)

internal interface LitePlayback {
    val boost: StateFlow<SpeakerBoostState>
    val running: Boolean
    fun configure(settings: LiteSettings, deviceId: Int)
    fun start(monitoring: Boolean)
    fun write(samples: FloatArray, count: Int)
    fun stop()
}
private class OriginalPlayback(private val context: Context) : LitePlayback {
    override val boost get() = SoftwareLoopback.speakerBoostState
    override val running get() = SoftwareLoopback.isRunning
    override fun configure(settings: LiteSettings, deviceId: Int) {
        SoftwareLoopback.setMonitorVolume(settings.volume)
        SoftwareLoopback.setMonitorEnabled(settings.monitoring)
        SoftwareLoopback.setSpeakerBoost(settings.outputDb)
        SoftwareLoopback.setOutputDevice(context, deviceId)
    }
    override fun start(monitoring: Boolean) = SoftwareLoopback.start(context, monitoring)
    override fun write(samples: FloatArray, count: Int) = SoftwareLoopback.writeProcessed(samples, count)
    override fun stop() = SoftwareLoopback.stop()
}

class LiteEngine internal constructor(private val context: Context, private val settings: LiteSettingsStore,
    val devices: LiteDeviceCatalog, private val native: NativeAudioDriver, private val playback: LitePlayback) {
    constructor(context: Context, settings: LiteSettingsStore, devices: LiteDeviceCatalog) :
        this(context, settings, devices, OboeEngine(), OriginalPlayback(context))

    private val _state = MutableStateFlow(LiteRunState())
    val state: StateFlow<LiteRunState> = _state
    val outputBoostState get() = playback.boost
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var updates: Job? = null
    private var appliedInputId = 0
    @Volatile var onFailure: (() -> Unit)? = null

    fun permissionDenied() { _state.value = _state.value.copy(error = "Разрешите доступ к микрофону") }
    fun startDenied() { _state.value = _state.value.copy(error = "Android не разрешил запуск. Откройте MicUp Lite и повторите.") }

    fun start(): Boolean {
        if (state.value.running) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionDenied(); return false
        }
        // Match the original: do not reject MODE_IN_COMMUNICATION or stop on recording callbacks.
        // Android decides whether concurrent recording is available on the actual device.
        return try {
            val s = settings.state.value
            s.mode.unavailableReason?.let { error(it) }
            configure(s)
            native.setMonitorCallback(playback::write)
            check(native.start()) { "Не удалось запустить исходный аудиодвижок MicUp" }
            playback.start(s.monitoring)
            check(playback.running) { "Не удалось запустить вывод звука" }
            _state.value = _state.value.copy(running = true, error = null)
            observeSettings()
            true
        } catch (e: RuntimeException) {
            stop()
            _state.value = _state.value.copy(error = e.message ?: "Не удалось запустить звук")
            false
        }
    }

    fun stop() {
        updates?.cancel(); updates = null
        // Stop capture before deleting its callback or releasing playback/effects.
        native.stop()
        native.setMonitorCallback(null)
        playback.stop()
        _state.value = _state.value.copy(running = false, inputDb = -100f, outputDb = -100f)
    }

    private fun configure(s: LiteSettings) {
        s.mode.unavailableReason?.let { error(it) }
        val catalog = devices.state.value
        val input = LiteDeviceCatalog.selected(s.inputKey, catalog.inputs)?.info?.id ?: 0
        if (input != appliedInputId) {
            if (state.value.running) native.stop()
            native.setInputDevice(input); appliedInputId = input
            if (state.value.running) check(native.start()) { "Не удалось переключить микрофон" }
        }
        // Same gate and gain as full MicUp; tone-changing processors stay disabled in Lite.
        native.setParam(1, 10, 0f)
        native.setParam(2, 5, 0f)
        native.setParam(3, 3, 0f)
        native.setParam(4, 1, 0f)
        native.setParam(6, 0, 0f); native.setParam(6, 2, 0f)
        native.setParam(0, 3, if (s.noiseReduction) 1f else 0f)
        native.setParam(0, 0, s.noiseThresholdDb)
        native.setParam(5, 0, s.microphoneDb)
        val output = LiteDeviceCatalog.selected(s.outputKey, catalog.outputs)
        playback.configure(s, output?.info?.id ?: -1)
    }

    private fun observeSettings() {
        updates?.cancel()
        updates = scope.launch {
            try {
                var lastSettings: LiteSettings? = null
                var lastDevices: LiteDevices? = null
                while (isActive && state.value.running) {
                    val s = settings.state.value
                    val catalog = devices.state.value
                    if (s != lastSettings || catalog != lastDevices) {
                        configure(s); lastSettings = s; lastDevices = catalog
                    }
                    check(native.isRunning()) { "Аудиоустройство отключилось. Повторно включите звук." }
                            val levels = native.getLevels()
                    _state.value = _state.value.copy(inputDb = levels.getOrElse(0) { -100f },
                        outputDb = levels.getOrElse(1) { -100f })
                    delay(50)
                }
            } catch (e: CancellationException) { throw e }
              catch (e: RuntimeException) {
                stop()
                _state.value = _state.value.copy(error = e.message ?: "Звук остановлен")
                onFailure?.invoke()
            }
        }
    }
}
