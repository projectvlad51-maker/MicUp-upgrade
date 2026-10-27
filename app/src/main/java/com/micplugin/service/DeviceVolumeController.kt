package com.micplugin.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceVolumes(
    val media: Int, val mediaMax: Int,
    val call: Int, val callMin: Int, val callMax: Int,
    val fixed: Boolean,
)

@Singleton
class DeviceVolumeController @Inject constructor(@ApplicationContext context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private fun read() = DeviceVolumes(
        manager.getStreamVolume(AudioManager.STREAM_MUSIC), manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
        manager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
        if (Build.VERSION.SDK_INT >= 28) manager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL) else 0,
        manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), manager.isVolumeFixed,
    )
    private val _volumes = MutableStateFlow(read())
    val volumes: StateFlow<DeviceVolumes> = _volumes
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun refresh() { _volumes.value = read() }
    fun setMedia(value: Int) = set(AudioManager.STREAM_MUSIC, value, 0, manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    fun setCall(value: Int) = set(AudioManager.STREAM_VOICE_CALL, value,
        if (Build.VERSION.SDK_INT >= 28) manager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL) else 0,
        manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL))

    private fun set(stream: Int, value: Int, min: Int, max: Int) {
        try {
            if (manager.isVolumeFixed) {
                _error.value = "Громкость фиксирована устройством"
            } else {
                manager.setStreamVolume(stream, value.coerceIn(min, max), 0)
                _error.value = null
            }
        } catch (_: SecurityException) {
            _error.value = "Android ограничил изменение громкости"
        } finally { refresh() }
    }
}
