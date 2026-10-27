package com.micplugin.ui

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micplugin.audio.AudioEngine
import com.micplugin.audio.AudioLevels
import com.micplugin.plugin.*
import com.micplugin.preset.EffectState
import com.micplugin.preset.Preset
import com.micplugin.preset.PresetManager
import com.micplugin.plugin.PluginPathPrefs
import com.micplugin.service.DeviceVolumeController
import com.micplugin.service.ShizukuManager
import com.micplugin.service.SoftwareLoopback
import com.micplugin.service.ShizukuState
import com.micplugin.service.VirtualMicService
import com.micplugin.service.VirtualMicTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class VoiceFilterState(
    val highPassEnabled: Boolean = false, val highPassHz: Float = 80f,
    val lowPassEnabled: Boolean = false, val lowPassHz: Float = 12000f,
)

data class GateState(
    val enabled: Boolean = true,
    val thresholdDb: Float = -50f,
    val attackMs: Float = 5f,
    val releaseMs: Float = 150f,
)

data class EqState(
    val enabled: Boolean = true,
    val bands: List<Float> = List(10) { 0f },
)

data class CompState(
    val enabled: Boolean = true,
    val thresholdDb: Float = -18f,
    val ratio: Float = 4f,
    val attackMs: Float = 10f,
    val releaseMs: Float = 100f,
    val makeupDb: Float = 0f,
)

data class ReverbState(
    val enabled: Boolean = false,
    val mix: Float = 0f,
    val roomSize: Float = 0.5f,
    val damping: Float = 0.5f,
)

data class PitchState(
    val enabled: Boolean = false,
    val semitones: Float = 0f,
)

@HiltViewModel
class AudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEngine: AudioEngine,
    private val pluginManager: PluginManager,
    private val presetManager: PresetManager,
    private val virtualMicService: VirtualMicService,
    private val shizukuManager: ShizukuManager,
    private val pluginPathPrefs: PluginPathPrefs,
    private val deviceVolumeController: DeviceVolumeController,
) : ViewModel() {

    val levels:      StateFlow<AudioLevels>    = audioEngine.levels
    val engineStatus = audioEngine.status
    val engineNotice = audioEngine.notice
    val pluginSlots: StateFlow<List<PluginSlot>> = audioEngine.pluginChain.slots
    val discovered:  StateFlow<List<PluginDescriptor>> = pluginManager.discovered
    val presets:     StateFlow<List<Preset>>   = presetManager.presets
    val virtualMicTier: StateFlow<VirtualMicTier> = virtualMicService.activeTier
    val shizukuState: StateFlow<ShizukuState> = shizukuManager.state
    val shizukuPermissionSettingsRequired = shizukuManager.permissionSettingsRequired

    val deviceVolumes = deviceVolumeController.volumes
    val volumeError = deviceVolumeController.error
    fun refreshDeviceVolumes() = deviceVolumeController.refresh()
    fun setDeviceMediaVolume(value: Int) = deviceVolumeController.setMedia(value)
    fun setDeviceCallVolume(value: Int) = deviceVolumeController.setCall(value)

    private val _monitoringEnabled = MutableStateFlow(false)
    val monitoringEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _monitoringEnabled
    val echoState = audioEngine.echo.state
    val noiseReductionState = audioEngine.noiseReduction.state
    private val _noiseReductionEnabled = MutableStateFlow(true)
    val noiseReductionEnabled: StateFlow<Boolean> = _noiseReductionEnabled
    fun setNoiseReduction(enabled: Boolean) {
        _noiseReductionEnabled.value = enabled
        audioEngine.setNoiseReduction(enabled)
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("noise_reduction_enabled", enabled).apply()
    }

    val speakerBoostState = SoftwareLoopback.speakerBoostState
    fun setSpeakerBoost(db: Float) {
        val boost = if (db.isFinite()) db.coerceIn(0f, 24f) else 0f
        SoftwareLoopback.setSpeakerBoost(boost)
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("speaker_boost_db", boost).apply()
    }

    private val _monitorVolume = MutableStateFlow(1f)
    val monitorVolume: StateFlow<Float> = _monitorVolume
    fun setMonitorVolume(value: Float) {
        val volume = if (value.isFinite()) value.coerceIn(0f, 1f) else 1f
        _monitorVolume.value = volume
        SoftwareLoopback.setMonitorVolume(volume)
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("monitor_volume", volume).apply()
    }

    private val _voiceFilters = MutableStateFlow(VoiceFilterState())
    val voiceFilters: StateFlow<VoiceFilterState> = _voiceFilters
    fun setVoiceFilters(state: VoiceFilterState) {
        val clean = state.copy(
            highPassHz = if (state.highPassHz.isFinite()) state.highPassHz.coerceIn(20f, 300f) else 80f,
            lowPassHz = if (state.lowPassHz.isFinite()) state.lowPassHz.coerceIn(3000f, 20000f) else 12000f)
        _voiceFilters.value = clean
        audioEngine.setVoiceFilters(clean.highPassEnabled, clean.highPassHz, clean.lowPassEnabled, clean.lowPassHz)
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("high_pass_enabled", clean.highPassEnabled).putFloat("high_pass_hz", clean.highPassHz)
            .putBoolean("low_pass_enabled", clean.lowPassEnabled).putFloat("low_pass_hz", clean.lowPassHz).apply()
    }

    private val _microphoneGainDb = MutableStateFlow(0f)
    val microphoneGainDb: StateFlow<Float> = _microphoneGainDb

    fun setMicrophoneGain(db: Float) {
        val boost = if (db.isFinite()) db.coerceIn(0f, 36f) else 0f
        _microphoneGainDb.value = boost
        audioEngine.setMicrophoneGain(boost)
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("microphone_gain_db", boost).apply()
    }

    private val _gateState   = MutableStateFlow(GateState())
    private val _eqState     = MutableStateFlow(EqState())
    private val _compState   = MutableStateFlow(CompState())
    private val _reverbState = MutableStateFlow(ReverbState())
    private val _pitchState  = MutableStateFlow(PitchState())
    private val _masterBypass = MutableStateFlow(false)

    val gateState:    StateFlow<GateState>    = _gateState
    val eqState:      StateFlow<EqState>      = _eqState
    val compState:    StateFlow<CompState>    = _compState
    val reverbState:  StateFlow<ReverbState>  = _reverbState
    val pitchState:   StateFlow<PitchState>   = _pitchState
    val masterBypass: StateFlow<Boolean>      = _masterBypass

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset

    init {
        val prefs = context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE)
        setMonitorVolume(prefs.getFloat("monitor_volume", 1f))
        setMonitoring(prefs.getBoolean("monitoring_enabled", false))
        setNoiseReduction(prefs.getBoolean("noise_reduction_enabled", true))
        setSpeakerBoost(prefs.getFloat("speaker_boost_db", 0f))
        setVoiceFilters(VoiceFilterState(prefs.getBoolean("high_pass_enabled", false),
            prefs.getFloat("high_pass_hz", 80f), prefs.getBoolean("low_pass_enabled", false),
            prefs.getFloat("low_pass_hz", 12000f)))
        setMicrophoneGain(context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE)
            .getFloat("microphone_gain_db", 0f))
        viewModelScope.launch { presetManager.loadAll() }
        viewModelScope.launch { pluginManager.scanAll() }
    }

    // ── Engine control ─────────────────────────────────────────────────────────
    fun toggleEngine(context: android.content.Context) {
        if (engineStatus.value.isRunning) {
            com.micplugin.service.AudioProcessingService.stop(context)
        } else {
            var owner: Context = context
            while (owner is android.content.ContextWrapper) {
                if (owner is com.micplugin.MainActivity) { owner.requestAudioStart(); return }
                val next = owner.baseContext
                if (next === owner) break
                owner = next
            }
            com.micplugin.service.AudioProcessingService.start(context)
        }
    }

    fun setMasterBypass(bypass: Boolean) {
        _masterBypass.value = bypass
        audioEngine.setMasterBypass(bypass)
    }

    // ── Gate ──────────────────────────────────────────────────────────────────
    fun setGateEnabled(on: Boolean) {
        _gateState.value = _gateState.value.copy(enabled = on)
        audioEngine.setGateEnabled(on)
    }
    fun setGateThreshold(db: Float) {
        _gateState.value = _gateState.value.copy(thresholdDb = db)
        audioEngine.setGateThreshold(db)
    }
    fun setGateAttack(ms: Float) {
        _gateState.value = _gateState.value.copy(attackMs = ms)
        audioEngine.setGateAttack(ms)
    }
    fun setGateRelease(ms: Float) {
        _gateState.value = _gateState.value.copy(releaseMs = ms)
        audioEngine.setGateRelease(ms)
    }

    // ── EQ ────────────────────────────────────────────────────────────────────
    fun setEqEnabled(on: Boolean) {
        _eqState.value = _eqState.value.copy(enabled = on)
        audioEngine.setEqEnabled(on)
    }
    fun setEqBand(band: Int, db: Float) {
        val bands = _eqState.value.bands.toMutableList()
        bands[band] = db
        _eqState.value = _eqState.value.copy(bands = bands)
        audioEngine.setEqBand(band, db)
    }

    // ── Compressor ────────────────────────────────────────────────────────────
    fun setCompEnabled(on: Boolean) {
        _compState.value = _compState.value.copy(enabled = on)
        audioEngine.setCompEnabled(on)
    }
    fun setCompThreshold(db: Float) {
        _compState.value = _compState.value.copy(thresholdDb = db)
        audioEngine.setCompThreshold(db)
    }
    fun setCompRatio(ratio: Float) {
        _compState.value = _compState.value.copy(ratio = ratio)
        audioEngine.setCompRatio(ratio)
    }
    fun setCompAttack(ms: Float) {
        _compState.value = _compState.value.copy(attackMs = ms)
        audioEngine.setCompAttack(ms)
    }
    fun setCompRelease(ms: Float) {
        _compState.value = _compState.value.copy(releaseMs = ms)
        audioEngine.setCompRelease(ms)
    }
    fun setCompMakeup(db: Float) {
        _compState.value = _compState.value.copy(makeupDb = db)
        audioEngine.setCompMakeup(db)
    }

    // ── Reverb ────────────────────────────────────────────────────────────────
    fun setReverbEnabled(on: Boolean) {
        _reverbState.value = _reverbState.value.copy(enabled = on)
        audioEngine.setReverbEnabled(on)
    }
    fun setReverbMix(mix: Float) {
        _reverbState.value = _reverbState.value.copy(mix = mix)
        audioEngine.setReverbMix(mix)
    }
    fun setReverbRoom(size: Float) {
        _reverbState.value = _reverbState.value.copy(roomSize = size)
        audioEngine.setReverbRoom(size)
    }
    fun setReverbDamp(d: Float) {
        _reverbState.value = _reverbState.value.copy(damping = d)
        audioEngine.setReverbDamp(d)
    }

    // ── Pitch ─────────────────────────────────────────────────────────────────
    fun setPitchEnabled(on: Boolean) {
        _pitchState.value = _pitchState.value.copy(enabled = on)
        audioEngine.setPitchEnabled(on)
    }
    fun setPitchSemitones(st: Float) {
        _pitchState.value = _pitchState.value.copy(semitones = st)
        audioEngine.setPitchSemitones(st)
    }

    // ── Plugin chain ──────────────────────────────────────────────────────────
    fun addPlugin(descriptor: PluginDescriptor) {
        viewModelScope.launch {
            val handle = if (descriptor.format != PluginFormat.APK)
                audioEngine.loadNativePlugin(descriptor.path, descriptor.format) else 0L
            val slot = PluginSlot(descriptor = descriptor, nativeHandle = handle)
            audioEngine.pluginChain.addPlugin(slot)
        }
    }

    fun removePlugin(id: UUID) {
        val slot = audioEngine.pluginChain.getSlot(id) ?: return
        if (slot.nativeHandle != 0L) audioEngine.unloadNativePlugin(slot.nativeHandle)
        audioEngine.pluginChain.removePlugin(id)
    }

    fun togglePlugin(id: UUID) = audioEngine.pluginChain.toggleEnabled(id)

    fun reorderPlugin(from: Int, to: Int) = audioEngine.pluginChain.reorder(from, to)

    fun setPluginParam(slotId: UUID, paramId: Int, value: Float) {
        val slot = audioEngine.pluginChain.getSlot(slotId) ?: return
        if (slot.nativeHandle != 0L)
            audioEngine.setNativePluginParam(slot.nativeHandle, paramId, value)
        audioEngine.pluginChain.updateParam(slotId, paramId, value)
    }

    // ── Presets ───────────────────────────────────────────────────────────────
    fun applyPreset(preset: Preset) {
        _selectedPreset.value = preset
        val e = preset.effects
        setMicrophoneGain(e.microphoneGainDb)
        setVoiceFilters(VoiceFilterState(e.highPassEnabled, e.highPassHz, e.lowPassEnabled, e.lowPassHz))
        setGateEnabled(e.gateEnabled);     setGateThreshold(e.gateThreshold)
        setGateAttack(e.gateAttack);       setGateRelease(e.gateRelease)
        setEqEnabled(e.eqEnabled);         e.eqBands.forEachIndexed { i, db -> setEqBand(i, db) }
        setCompEnabled(e.compEnabled);     setCompThreshold(e.compThreshold)
        setCompRatio(e.compRatio);         setCompAttack(e.compAttack)
        setCompRelease(e.compRelease);     setCompMakeup(e.compMakeup)
        setReverbEnabled(e.reverbEnabled); setReverbMix(e.reverbMix)
        setReverbRoom(e.reverbRoom);       setReverbDamp(e.reverbDamp)
        setPitchEnabled(e.pitchEnabled);   setPitchSemitones(e.pitchSemitones)
        _gateState.value   = GateState(e.gateEnabled, e.gateThreshold, e.gateAttack, e.gateRelease)
        _eqState.value     = EqState(e.eqEnabled, e.eqBands)
        _compState.value   = CompState(e.compEnabled, e.compThreshold, e.compRatio, e.compAttack, e.compRelease, e.compMakeup)
        _reverbState.value = ReverbState(e.reverbEnabled, e.reverbMix, e.reverbRoom, e.reverbDamp)
        _pitchState.value  = PitchState(e.pitchEnabled, e.pitchSemitones)
    }

    fun savePreset(name: String) {
        val preset = Preset(
            name    = name,
            effects = EffectState(
                microphoneGainDb = _microphoneGainDb.value,
                highPassEnabled = _voiceFilters.value.highPassEnabled,
                highPassHz = _voiceFilters.value.highPassHz,
                lowPassEnabled = _voiceFilters.value.lowPassEnabled,
                lowPassHz = _voiceFilters.value.lowPassHz,
                gateEnabled   = _gateState.value.enabled,
                gateThreshold = _gateState.value.thresholdDb,
                gateAttack    = _gateState.value.attackMs,
                gateRelease   = _gateState.value.releaseMs,
                eqEnabled     = _eqState.value.enabled,
                eqBands       = _eqState.value.bands,
                compEnabled   = _compState.value.enabled,
                compThreshold = _compState.value.thresholdDb,
                compRatio     = _compState.value.ratio,
                compAttack    = _compState.value.attackMs,
                compRelease   = _compState.value.releaseMs,
                compMakeup    = _compState.value.makeupDb,
                reverbEnabled = _reverbState.value.enabled,
                reverbMix     = _reverbState.value.mix,
                reverbRoom    = _reverbState.value.roomSize,
                reverbDamp    = _reverbState.value.damping,
                pitchEnabled  = _pitchState.value.enabled,
                pitchSemitones = _pitchState.value.semitones,
            ),
        )
        viewModelScope.launch { presetManager.save(preset) }
    }

    fun rescan() { viewModelScope.launch { pluginManager.scanAll() } }

    fun selectPlaybackTier(tier: VirtualMicTier) = virtualMicService.selectPlaybackTier(tier)

    fun connectShizuku(context: Context) {
        shizukuManager.init()
        when (shizukuManager.state.value) {
            ShizukuState.NEED_GRANT -> {
                if (shizukuManager.permissionSettingsRequired.value) openShizuku(context)
                else shizukuManager.requestPermission()
            }
            ShizukuState.READY -> shizukuManager.refreshState()
            ShizukuState.UNAVAILABLE -> openShizuku(context)
        }
    }

    private fun openShizuku(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent == null) {
            android.widget.Toast.makeText(context,
                "Установите Shizuku и запустите его сервис", android.widget.Toast.LENGTH_LONG).show()
        } else {
            try {
                context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(context,
                    "Откройте Shizuku вручную: разрешите доступ MicUp и запустите сервис",
                    android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openPluginStorageSettings(context: Context) {
        val action = if (android.os.Build.VERSION.SDK_INT >= 30)
            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            else android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        try {
            context.startActivity(android.content.Intent(action,
                android.net.Uri.parse("package:" + context.packageName))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(context, "Откройте разрешения MicUp в настройках Android",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun refreshShizuku() = shizukuManager.init()

    fun requestShizukuPermission() { shizukuManager.requestPermission() }

    val lv2Paths  = pluginPathPrefs.lv2Paths
    val clapPaths = pluginPathPrefs.clapPaths
    val vst3Paths = pluginPathPrefs.vst3Paths

    fun addPluginPath(context: android.content.Context, format: com.micplugin.plugin.PluginFormat, path: String) {
        pluginPathPrefs.addPath(context, format, path)
    }
    fun removePluginPath(context: android.content.Context, format: com.micplugin.plugin.PluginFormat, path: String) {
        pluginPathPrefs.removePath(context, format, path)
    }
    fun allPluginPaths(context: android.content.Context, format: com.micplugin.plugin.PluginFormat) =
        pluginPathPrefs.allPaths(context, format)



    fun setMonitoring(enabled: Boolean) {
        _monitoringEnabled.value = enabled
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("monitoring_enabled", enabled).apply()
        audioEngine.setMonitoring(enabled)
        SoftwareLoopback.setMonitorEnabled(enabled)
    }


    fun getPluginParamsJson(nativeHandle: Long): String =
        audioEngine.getPluginParamsJson(nativeHandle)


    private val _outputDevices = MutableStateFlow<List<com.micplugin.service.OutputDevice>>(emptyList())
    val outputDevices: kotlinx.coroutines.flow.StateFlow<List<com.micplugin.service.OutputDevice>> = _outputDevices

    private val _selectedOutputDeviceId = MutableStateFlow(-1)
    val selectedOutputDeviceId: kotlinx.coroutines.flow.StateFlow<Int> = _selectedOutputDeviceId

    fun loadOutputDevices(context: android.content.Context) {
        _outputDevices.value = SoftwareLoopback.getOutputDevices(context)
        // Restore saved selection
        val saved = context.getSharedPreferences("micup_prefs", android.content.Context.MODE_PRIVATE)
            .getInt("output_device_id", -1)
        _selectedOutputDeviceId.value = saved
        SoftwareLoopback.setOutputDevice(context, saved)
    }

    fun setOutputDevice(context: android.content.Context, deviceId: Int) {
        _selectedOutputDeviceId.value = deviceId
        SoftwareLoopback.setOutputDevice(context, deviceId)
        // Persist selection
        context.getSharedPreferences("micup_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putInt("output_device_id", deviceId).apply()
    }

}
