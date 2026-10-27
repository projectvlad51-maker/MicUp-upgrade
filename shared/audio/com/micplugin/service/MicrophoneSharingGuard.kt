package com.micplugin.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

data class CaptureStatus(val session: Int, val silenced: Boolean)
internal interface RecordingMonitor {
    fun register(callback: (List<CaptureStatus>) -> Unit)
    fun unregister()
}

private class AndroidRecordingMonitor(context: Context) : RecordingMonitor {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var registered: AudioManager.AudioRecordingCallback? = null
    @Synchronized override fun register(callback: (List<CaptureStatus>) -> Unit) {
        if (Build.VERSION.SDK_INT < 29) return
        registered = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<android.media.AudioRecordingConfiguration>) {
                if (Build.VERSION.SDK_INT < 29) return
                callback(configs.map { CaptureStatus(it.clientAudioSessionId, it.isClientSilenced) })
            }
        }.also { manager.registerAudioRecordingCallback(it, Handler(Looper.getMainLooper())) }
    }
    @Synchronized override fun unregister() {
        registered?.let { manager.unregisterAudioRecordingCallback(it) }
        registered = null
    }
}

class MicrophoneSharingGuard internal constructor(private val monitor: RecordingMonitor,
    private val session: () -> Int, private val onSilenced: () -> Unit) {
    constructor(context: Context, session: () -> Int, onSilenced: () -> Unit) : this(AndroidRecordingMonitor(context), session, onSilenced)
    @Volatile private var active = false
    @Volatile private var fired = false
    fun start() {
        stop()
        active = true; fired = false
        monitor.register { configs ->
            val own = session()
            if (active && !fired && own > 0 && configs.any { it.session > 0 && it.silenced }) {
                // Yield even when the other app is silenced: its call must not lose the mic.
                fired = true
                onSilenced()
            }
        }
    }
    fun stop() { active = false; monitor.unregister() }
    companion object {
        fun communicationBusy(mode: Int) = mode != AudioManager.MODE_NORMAL
    }
}
