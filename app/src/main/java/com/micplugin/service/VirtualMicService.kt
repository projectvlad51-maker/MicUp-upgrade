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

enum class VirtualMicTier {
    VOIP_STREAM, MEDIA_PROJECTION, SHIZUKU_ADB, ROOT_MAGISK;

    val displayName: String get() = when (this) {
        VOIP_STREAM      -> "VoIP Playback"
        MEDIA_PROJECTION -> "Local Monitor"
        SHIZUKU_ADB      -> "Shizuku — ADB Level"
        ROOT_MAGISK      -> "Root Mode — System-Wide"
    }
    val description: String get() = when (this) {
        VOIP_STREAM      -> "Plays processed audio using Android communication routing. Does not replace the microphone input of other apps."
        MEDIA_PROJECTION -> "Local monitoring through the media output. Does not send processed audio to other apps as a microphone."
        SHIZUKU_ADB      -> "Shizuku permission is managed separately. A system virtual microphone is not implemented in this build."
        ROOT_MAGISK      -> "System-wide microphone routing requires separate device-specific integration; unavailable in this build."
    }
    val badgeColorHex: Long get() = when (this) {
        VOIP_STREAM      -> 0xFF3DFCACL
        MEDIA_PROJECTION -> 0xFF7C5CFCL
        SHIZUKU_ADB      -> 0xFF00B4D8L
        ROOT_MAGISK      -> 0xFFFFD700L
    }
}

@Singleton
class VirtualMicService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
) {
    private val _activeTier = MutableStateFlow(VirtualMicTier.MEDIA_PROJECTION)
    val activeTier: StateFlow<VirtualMicTier> = _activeTier

    init {
        val saved = context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE)
            .getString("playback_tier", null)
        val preferred = VirtualMicTier.values().firstOrNull { it.name == saved }
        _activeTier.value = if (preferred == VirtualMicTier.VOIP_STREAM) preferred
            else VirtualMicTier.MEDIA_PROJECTION
        SoftwareLoopback.setVoipMode(context, _activeTier.value == VirtualMicTier.VOIP_STREAM)
    }

    fun selectPlaybackTier(tier: VirtualMicTier) {
        if (tier != VirtualMicTier.VOIP_STREAM && tier != VirtualMicTier.MEDIA_PROJECTION) return
        SoftwareLoopback.setVoipMode(context, tier == VirtualMicTier.VOIP_STREAM)
        _activeTier.value = tier
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putString("playback_tier", tier.name).apply()
    }

    fun getActiveTier(): VirtualMicTier = _activeTier.value
    fun getStatusDescription(): String = _activeTier.value.description
}
