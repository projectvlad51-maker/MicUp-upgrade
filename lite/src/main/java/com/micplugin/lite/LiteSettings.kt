package com.micplugin.lite

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LiteSettings(
    val microphoneDb: Float = 0f,
    val outputDb: Float = 0f,
    val volume: Float = 1f,
    val inputKey: String = "auto",
    val outputKey: String = "auto",
    val presetId: String? = "natural",
    val monitoring: Boolean = true,
    val noiseReduction: Boolean = true,
    val noiseThresholdDb: Float = -50f,
    val mode: com.micplugin.service.VirtualMicTier = com.micplugin.service.VirtualMicTier.saved(null),
)

data class LitePreset(val id: String, val name: String, val description: String,
    val microphoneDb: Float, val outputDb: Float, val volume: Float = 1f)

object LitePresets {
    // Level settings only: no EQ, pitch shifting, reverb or spectral processing.
    val all = listOf(
        LitePreset("natural", "01 · Естественный", "Исходный уровень голоса", 0f, 0f),
        LitePreset("gentle", "02 · Чуть громче", "Мягкое усиление микрофона", 3f, 0f),
        LitePreset("speech", "03 · Разборчивая речь", "Усиление обычной речи", 6f, 0f),
        LitePreset("quiet", "04 · Тихий голос", "Для негромкого микрофона", 12f, 0f),
        LitePreset("distant", "05 · Далёкий микрофон", "Больше уровня тихого источника", 18f, 0f),
        LitePreset("speaker", "06 · Динамики комфорт", "Умеренное усиление входа и выхода", 6f, 3f),
        LitePreset("speaker_loud", "07 · Динамики громко", "Громкое воспроизведение речи", 9f, 6f),
        LitePreset("headphones", "08 · Наушники комфорт", "Усиление с меньшей громкостью выхода", 3f, 3f, 0.8f),
        LitePreset("headphones_loud", "09 · Наушники громко", "Усиленный выход для наушников", 6f, 6f),
        LitePreset("very_quiet", "10 · Очень тихий источник", "Сильное усиление негромкого сигнала", 24f, 9f),
    )
}

class LiteSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("lite_audio_settings", Context.MODE_PRIVATE)
    init { com.micplugin.service.OriginalAudioDefaults.lite(prefs) }
    private fun finite(value: Float, min: Float, max: Float, fallback: Float) =
        if (value.isFinite()) value.coerceIn(min, max) else fallback
    private val _state = MutableStateFlow(LiteSettings(
        finite(prefs.getFloat("microphone_db", 0f), 0f, 36f, 0f),
        finite(prefs.getFloat("output_db", 0f), 0f, 24f, 0f),
        finite(prefs.getFloat("volume", 1f), 0f, 1f, 1f),
        prefs.getString("input", "auto") ?: "auto",
        prefs.getString("output", "auto") ?: "auto",
        prefs.getString("preset", "natural")?.takeIf { id -> LitePresets.all.any { it.id == id } },
        mode = com.micplugin.service.VirtualMicTier.saved(prefs.getString("audio_mode", null)),
        monitoring = prefs.getBoolean("monitoring", true),
        noiseReduction = prefs.getBoolean("noise_reduction", true),
        noiseThresholdDb = finite(prefs.getFloat("noise_threshold_db", -50f), -70f, -20f, -50f),
    ))
    val state: StateFlow<LiteSettings> = _state

    private fun save(value: LiteSettings) {
        _state.value = value
        prefs.edit().putFloat("microphone_db", value.microphoneDb).putFloat("output_db", value.outputDb)
            .putFloat("volume", value.volume).putString("input", value.inputKey)
            .putString("output", value.outputKey).putString("preset", value.presetId ?: "")
            .putString("audio_mode", value.mode.name).putBoolean("monitoring", value.monitoring).putBoolean("noise_reduction", value.noiseReduction).putFloat("noise_threshold_db", value.noiseThresholdDb).apply()
    }
    fun mode(value: com.micplugin.service.VirtualMicTier) = save(state.value.copy(mode = value))
    fun noiseReduction(enabled: Boolean) = save(state.value.copy(noiseReduction = enabled))
    fun noiseThreshold(db: Float) = save(state.value.copy(noiseThresholdDb = finite(db, -70f, -20f, -50f)))
    fun monitoring(enabled: Boolean) = save(state.value.copy(monitoring = enabled))
    fun microphone(db: Float) = save(state.value.copy(microphoneDb = finite(db, 0f, 36f, 0f), presetId = null))
    fun output(db: Float) = save(state.value.copy(outputDb = finite(db, 0f, 24f, 0f), presetId = null))
    fun volume(value: Float) = save(state.value.copy(volume = finite(value, 0f, 1f, 1f), presetId = null))
    fun input(key: String) = save(state.value.copy(inputKey = key))
    fun outputDevice(key: String) = save(state.value.copy(outputKey = key))
    fun preset(id: String) {
        val p = LitePresets.all.firstOrNull { it.id == id } ?: return
        save(state.value.copy(microphoneDb = p.microphoneDb, outputDb = p.outputDb,
            volume = p.volume, presetId = p.id))
    }
}
