package com.micplugin.lite

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.micplugin.service.SpeakerBoostController
import com.micplugin.service.EchoCancellation
import com.micplugin.service.NoiseReduction
import com.micplugin.service.MicrophoneSharingGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class LiteRunState(val running: Boolean = false, val inputDb: Float = -100f,
    val outputDb: Float = -100f, val error: String? = null, val routeWarning: String? = null)

class LiteEngine(private val context: Context, private val settings: LiteSettingsStore,
    val devices: LiteDeviceCatalog) {
    private val _state = MutableStateFlow(LiteRunState())
    val state: StateFlow<LiteRunState> = _state
    val outputBoost = SpeakerBoostController()
    val echo = EchoCancellation()
    val noiseReduction = NoiseReduction()
    @Volatile private var running = false
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var thread: Thread? = null
    private val sharing = MicrophoneSharingGuard(context, { recorder?.audioSessionId ?: -1 }) {
        if (running) {
            _state.value = _state.value.copy(error = "Android передал микрофон другому приложению. MicUp Lite остановлен для звонка.")
            stop()
            onFailure?.invoke()
        }
    }
    @Volatile var onFailure: (() -> Unit)? = null

    fun permissionDenied() { _state.value = _state.value.copy(error = "Разрешите доступ к микрофону") }
    fun startDenied() { _state.value = _state.value.copy(error = "Android не разрешил запуск. Откройте MicUp Lite и повторите.") }

    fun start(): Boolean {
        if (running) return true
        if (thread?.isAlive == true) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = LiteRunState(error = "Разрешите доступ к микрофону")
            return false
        }
        val manager = devices.manager
        val previousMode = manager.mode
        if (MicrophoneSharingGuard.communicationBusy(previousMode)) {
            _state.value = LiteRunState(error = "Идёт звонок или связь другого приложения. Микрофон оставлен мессенджеру.")
            return false
        }
        var record: AudioRecord? = null
        var track: AudioTrack? = null
        try {
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            val formatIn = AudioFormat.Builder().setSampleRate(48000).setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()
            val formatOut = AudioFormat.Builder().setSampleRate(48000).setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            val inputMin = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
            val outputMin = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
            require(inputMin > 0 && outputMin > 0) { "Формат аудио недоступен" }
            // Communication capture provides the system echo-reference path for speaker playback.
            val recordBuilder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(formatIn).setBufferSizeInBytes(maxOf(inputMin * 2, 8192))
            if (Build.VERSION.SDK_INT >= 30) recordBuilder.setPrivacySensitive(false)
            record = recordBuilder.build()
            track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(formatOut).setBufferSizeInBytes(maxOf(outputMin * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            check(record.state == AudioRecord.STATE_INITIALIZED && track.state == AudioTrack.STATE_INITIALIZED)
            recorder = record
            echo.attach(record.audioSessionId)
            noiseReduction.attach(record.audioSessionId, settings.state.value.noiseReduction)
            applyRoute(record, track, settings.state.value, devices.state.value)
            outputBoost.setGain(settings.state.value.outputDb)
            outputBoost.attach(track.audioSessionId)
            track.setVolume(if (settings.state.value.monitoring) settings.state.value.volume else 0f)
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            track.play()
            running = true
            _state.value = _state.value.copy(running = true, error = null)
            sharing.start()
            val input = record
            val output = track
            thread = Thread({ stream(input, output, previousMode) }, "MicUp-Lite-Audio").also { it.start() }
            return true
        } catch (e: RuntimeException) {
            sharing.stop()
            outputBoost.detach()
            echo.detach()
            noiseReduction.detach()
            record?.release(); track?.release(); recorder = null
            restoreMode(previousMode)
            _state.value = LiteRunState(error = "Не удалось запустить звук: ${e.message ?: "ошибка устройства"}")
            return false
        }
    }

    fun stop() {
        sharing.stop()
        running = false
        try { recorder?.stop() } catch (_: RuntimeException) {}
        thread?.interrupt()
        thread?.join(1000)
    }

    private fun stream(input: AudioRecord, output: AudioTrack, previousMode: Int) {
        var failed = false
        try {
            try { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) } catch (_: RuntimeException) {}
            val samples = FloatArray(512)
            val processor = LiteGainProcessor()
            var last: LiteSettings? = null
            var routes: LiteDevices? = null
            var blocks = 0
            while (running) {
                val current = settings.state.value
                val catalog = devices.state.value
                if (current.inputKey != last?.inputKey || current.outputKey != last?.outputKey || catalog != routes) {
                    applyRoute(input, output, current, catalog)
                    routes = catalog
                }
                if (current.noiseReduction != last?.noiseReduction) noiseReduction.setEnabled(current.noiseReduction)
                if (current.outputDb != last?.outputDb) outputBoost.setGain(current.outputDb)
                if (current.volume != last?.volume || current.monitoring != last?.monitoring)
                    output.setVolume(if (current.monitoring) current.volume else 0f)
                last = current
                val count = input.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                if (!running) break
                check(count > 0) { "Микрофон недоступен ($count)" }
                val inputDb = level(samples, count)
                processor.process(samples, count, current.microphoneDb)
                if (++blocks % 10 == 0) _state.value = _state.value.copy(inputDb = inputDb, outputDb = level(samples, count))
                var offset = 0
                while (offset < count && running) {
                    val written = output.write(samples, offset, count - offset, AudioTrack.WRITE_BLOCKING)
                    check(written > 0) { "Выход недоступен ($written)" }
                    offset += written
                }
            }
        } catch (e: RuntimeException) {
            if (running) { failed = true; _state.value = _state.value.copy(error = "Звук остановлен: ${e.message}") }
        } finally {
            running = false
            sharing.stop()
            outputBoost.detach()
            echo.detach()
            noiseReduction.detach()
            try { input.stop() } catch (_: RuntimeException) {}
            try { output.stop() } catch (_: RuntimeException) {}
            input.release(); output.release(); recorder = null
            restoreMode(previousMode)
            thread = null
            _state.value = _state.value.copy(running = false, inputDb = -100f, outputDb = -100f)
            if (failed) onFailure?.invoke()
        }
    }

    private fun applyRoute(input: AudioRecord, output: AudioTrack, s: LiteSettings, catalog: LiteDevices) {
        val chosen = LiteDeviceCatalog.selected(s.outputKey, catalog.outputs) ?: LiteDeviceCatalog.automaticOutput(catalog.outputs)
        val mic = LiteDeviceCatalog.selected(s.inputKey, catalog.inputs)
        var applied = input.setPreferredDevice(mic?.info)
        chosen?.info?.let { device ->
            if (Build.VERSION.SDK_INT >= 31) applied = devices.manager.setCommunicationDevice(device) && applied
            else {
                @Suppress("DEPRECATION")
                devices.manager.isSpeakerphoneOn = device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            applied = output.setPreferredDevice(device) && applied
        }
        _state.value = _state.value.copy(routeWarning = if (applied) null else "Android не подтвердил выбранное устройство")
    }

    private fun restoreMode(previous: Int) {
        try {
            if (devices.manager.mode == AudioManager.MODE_IN_COMMUNICATION) {
                if (Build.VERSION.SDK_INT >= 31) devices.manager.clearCommunicationDevice()
                devices.manager.mode = previous
            }
        } catch (_: RuntimeException) {}
    }

    private fun level(samples: FloatArray, count: Int): Float {
        var sum = 0.0
        for (i in 0 until count) sum += samples[i].toDouble() * samples[i]
        return (20 * log10(sqrt(sum / count).coerceAtLeast(0.00001))).toFloat()
    }
}
