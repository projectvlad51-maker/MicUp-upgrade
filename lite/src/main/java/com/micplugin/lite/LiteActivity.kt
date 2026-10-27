package com.micplugin.lite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat

class LiteActivity : ComponentActivity() {
    private val app get() = application as LiteApplication
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            startAudio()
        else app.engine.permissionDenied()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by app.settings.state.collectAsState()
            val audio by app.engine.state.collectAsState()
            val devices by app.devices.state.collectAsState()
            val boost by app.engine.outputBoostState.collectAsState()
            LiteScreen(settings, audio, devices, boost,
                onToggle = {
                    if (audio.running) stopService(Intent(this, LiteAudioService::class.java))
                    else requestAudio()
                },
                onMicrophone = app.settings::microphone, onOutput = app.settings::output,
                onVolume = app.settings::volume, onInputDevice = app.settings::input,
                onOutputDevice = app.settings::outputDevice, onPreset = app.settings::preset,
                onMonitoring = app.settings::monitoring, onMode = app.settings::mode,
                onNoiseReduction = app.settings::noiseReduction,
                onNoiseThreshold = app.settings::noiseThreshold)
        }
        if (savedInstanceState == null) requestAudio()
    }
    override fun onResume() { super.onResume(); app.devices.refresh() }
    private fun requestAudio() {
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(this@LiteActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@LiteActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (missing.isEmpty()) startAudio() else permissionRequest.launch(missing.toTypedArray())
    }
    private fun startAudio() {
        try { startForegroundService(Intent(this, LiteAudioService::class.java)) }
        catch (e: RuntimeException) { app.engine.startDenied() }
    }
}
