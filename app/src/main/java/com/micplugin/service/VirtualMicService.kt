package com.micplugin.service

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirtualMicService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
) {
    private val prefs = context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE)
    init { OriginalAudioDefaults.full(prefs) }
    private val _activeTier = MutableStateFlow(VirtualMicTier.saved(prefs.getString("playback_tier", null)))
    val activeTier: StateFlow<VirtualMicTier> = _activeTier
    fun selectPlaybackTier(tier: VirtualMicTier) {
        _activeTier.value = tier
        prefs.edit().putString("playback_tier", tier.name).apply()
    }
    fun getActiveTier(): VirtualMicTier = _activeTier.value
    fun getStatusDescription(): String = _activeTier.value.description
}
