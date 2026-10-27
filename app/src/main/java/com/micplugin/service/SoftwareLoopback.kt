package com.micplugin.service

import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue

data class OutputDevice(val id: Int, val name: String, val type: Int)

object SoftwareLoopback {

    private const val TAG         = "MicPlugin.Loopback"
    private const val SAMPLE_RATE = 48000
    private const val FORMAT      = AudioFormat.ENCODING_PCM_FLOAT
    private const val QUEUE_CAP   = 16  // max buffered frames before dropping

    private var monitorTrack:  AudioTrack?   = null
    private var workerThread:  Thread?       = null
    private var audioManager:  AudioManager? = null
    private var selectedDeviceId: Int        = -1
    @Volatile private var monitorEnabled:   Boolean    = true
    @Volatile private var running = false
    private var focusRequest: AudioFocusRequest? = null
    private var voipMode = false
    private var monitorVolume = 1f
    private var focusMultiplier = 1f
    private var previousAudioMode: Int? = null
    private val speakerBoost = SpeakerBoostController()
    val speakerBoostState = speakerBoost.state
    fun setSpeakerBoost(db: Float) = speakerBoost.setGain(db)

    fun setVoipMode(context: Context, enabled: Boolean) {
        if (voipMode == enabled) return
        val restart = running
        if (restart) stop()
        voipMode = enabled
        if (restart) start(context)
    }

    fun setMonitorVolume(volume: Float) {
        monitorVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 1f
        applyVolume()
    }

    private fun applyVolume() {
        monitorTrack?.setVolume(if (monitorEnabled) monitorVolume * focusMultiplier else 0f)
    }

    private fun playbackAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(if (voipMode) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun restoreAudioMode() {
        val previous = previousAudioMode ?: return
        try {
            if (audioManager?.mode == AudioManager.MODE_IN_COMMUNICATION) audioManager?.mode = previous
        } catch (e: Exception) { Log.e(TAG, "Cannot restore audio mode", e) }
        previousAudioMode = null
    }

    // Lock-free float queue — audio callback offers, worker thread drains
    private val queue = ArrayBlockingQueue<FloatArray>(QUEUE_CAP)

    // Buffer pool — reuse allocations to avoid GC pressure every frame
    private val pool  = ArrayBlockingQueue<FloatArray>(QUEUE_CAP * 2)

    val isRunning: Boolean get() = running

    // ── Start / Stop ──────────────────────────────────────────────────────────

    fun start(context: Context) {
        if (running) return
        audioManager = context.getSystemService(AudioManager::class.java)

        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO, FORMAT).coerceAtLeast(4096)

        monitorTrack = buildMonitorTrack(minBuf)
        if (monitorTrack == null) { Log.e(TAG, "AudioTrack init failed"); return }

        try {
            if (voipMode) {
                previousAudioMode = audioManager?.mode
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            }
            monitorTrack!!.play()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot start playback", e)
            monitorTrack?.release()
            monitorTrack = null
            restoreAudioMode()
            return
        }
        speakerBoost.attach(monitorTrack!!.audioSessionId)
        focusMultiplier = 1f
        applyVolume()

        running = true
        queue.clear()

        // Worker thread drains queue → writes to AudioTrack (blocking OK here)
        workerThread = Thread({
            Log.i(TAG, "Worker thread started")
            while (running) {
                try {
                    val buf = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                        ?: continue
                    if (monitorEnabled) {
                        monitorTrack?.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
                    }
                    // Recycle buffer back to pool — no GC
                    pool.offer(buf)
                } catch (_: InterruptedException) { break }
                  catch (e: Exception) { Log.e(TAG, "Worker write error: $e") }
            }
            Log.i(TAG, "Worker thread stopped")
        }, "MicUp-Monitor-Worker")
        workerThread!!.priority = Thread.MAX_PRIORITY
        workerThread!!.start()

        applyOutputDevice()
        requestAudioFocus()
        Log.i(TAG, "SoftwareLoopback started monitor=$monitorEnabled")
    }

    fun stop() {
        running = false
        queue.clear()
        pool.clear()
        workerThread?.interrupt()
        workerThread?.join(500)
        workerThread = null
        speakerBoost.detach()
        try { monitorTrack?.stop(); monitorTrack?.release() } catch (_: Exception) {}
        monitorTrack = null
        abandonAudioFocus()
        restoreAudioMode()
        Log.i(TAG, "SoftwareLoopback stopped")
    }

    // ── Called from JNI callback (audio thread) ───────────────────────────────
    // Fast path: grab pooled buffer, copy, offer. Never alloc if pool has one.
    fun writeProcessed(buf: FloatArray, size: Int) {
        if (!running || !monitorEnabled) return
        // Get buffer from pool or allocate (rare — only first QUEUE_CAP frames)
        val copy = pool.poll() ?: FloatArray(size)
        if (copy.size >= size) {
            System.arraycopy(buf, 0, copy, 0, size)
        } else {
            // Pool buffer too small (shouldn't happen) — just copy
            buf.copyInto(copy, 0, 0, minOf(size, copy.size))
        }
        // Drop-oldest: if queue full, evict oldest and recycle it
        if (!queue.offer(copy)) {
            val dropped = queue.poll()   // remove oldest
            dropped?.let { pool.offer(it) }  // recycle
            queue.offer(copy)            // now guaranteed space
        }
    }

    // ── Monitor toggle ────────────────────────────────────────────────────────

    fun setMonitorEnabled(enabled: Boolean) {
        monitorEnabled = enabled
        applyVolume()
        if (!enabled) queue.clear()
        Log.i(TAG, "Monitor ${if (enabled) "ON" else "OFF"}")
    }

    fun setMicrophoneSidetone(mute: Boolean) {}
    fun onVoipCallStateChanged(active: Boolean) {}

    // ── Output device ─────────────────────────────────────────────────────────

    fun getOutputDevices(context: Context): List<OutputDevice> {
        val am = context.getSystemService(AudioManager::class.java)
        val list = mutableListOf(OutputDevice(-1, "Auto (best available)", -1))
        if (Build.VERSION.SDK_INT >= 23) {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach { d ->
                val name = when (d.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE  -> "Earpiece"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER   -> "Speaker"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET     -> "Wired Headset"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES  -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO     -> "Bluetooth SCO"
                    AudioDeviceInfo.TYPE_BLE_HEADSET       -> "BLE Headset"
                    AudioDeviceInfo.TYPE_USB_HEADSET       -> "USB Headset"
                    else -> d.productName?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                }
                list.add(OutputDevice(d.id, name, d.type))
            }
        }
        return list
    }

    fun setOutputDevice(context: Context, deviceId: Int) {
        selectedDeviceId = deviceId
        audioManager = audioManager ?: context.getSystemService(AudioManager::class.java)
        applyOutputDevice()
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun applyOutputDevice() {
        if (Build.VERSION.SDK_INT < 23) return
        val am = audioManager ?: return
        try {
            val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val dev = if (selectedDeviceId == -1) {
                outputs.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET    ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET      ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                } ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            } else outputs.firstOrNull { it.id == selectedDeviceId }
            dev?.let { monitorTrack?.preferredDevice = it }
        } catch (e: Exception) { Log.e(TAG, "applyOutputDevice: $e") }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT          -> { focusMultiplier = 0f; applyVolume() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { focusMultiplier = 0.2f; applyVolume() }
                AudioManager.AUDIOFOCUS_GAIN                    -> { focusMultiplier = 1f; applyVolume() }
            }
        }
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes())
                .setOnAudioFocusChangeListener(listener)
                .setWillPauseWhenDucked(false).build()
            am.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(listener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= 26)
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            else @Suppress("DEPRECATION") audioManager?.abandonAudioFocus(null)
        } catch (_: Exception) {}
    }

    private fun buildMonitorTrack(bufSize: Int): AudioTrack? = try {
        if (Build.VERSION.SDK_INT >= 26) {
            AudioTrack.Builder()
                .setAudioAttributes(playbackAttributes())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(FORMAT)   // PCM_FLOAT — no conversion ever
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufSize * 4)   // float = 4 bytes per sample
                .setTransferMode(AudioTrack.MODE_STREAM).build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,   // pre-API26 fallback
                bufSize * 2, AudioTrack.MODE_STREAM)
        }
    } catch (e: Exception) { Log.e(TAG, "buildMonitorTrack: $e"); null }
}
