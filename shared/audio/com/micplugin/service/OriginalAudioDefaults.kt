package com.micplugin.service

import android.content.SharedPreferences

object OriginalAudioDefaults {
    private const val REVISION = 106
    fun full(prefs: SharedPreferences) {
        if (prefs.getInt("original_transport_revision", 0) == REVISION) return
        prefs.edit().putInt("original_transport_revision", REVISION)
            .putBoolean("monitoring_enabled", true).putFloat("monitor_volume", 1f)
            .putInt("output_device_id", -1).remove("playback_tier").apply()
    }
    fun lite(prefs: SharedPreferences) {
        if (prefs.getInt("original_transport_revision", 0) == REVISION) return
        prefs.edit().putInt("original_transport_revision", REVISION)
            .putBoolean("monitoring", true).putFloat("volume", 1f)
            .putString("input", "auto").putString("output", "auto").apply()
    }
}
