package com.micplugin.service

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaybackRoutingTest {
    private lateinit var context: Context
    private lateinit var audioManager: AudioManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SoftwareLoopback.stop()
        SoftwareLoopback.setVoipMode(context, false)
        audioManager = context.getSystemService(AudioManager::class.java)
        audioManager.mode = AudioManager.MODE_NORMAL
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        SoftwareLoopback.stop()
        SoftwareLoopback.setVoipMode(context, false)
    }

    @Test fun selectingVoipWhileStoppedWaitsForPlaybackAndRestoresModeOnStop() {
        SoftwareLoopback.setVoipMode(context, true)
        assertFalse(SoftwareLoopback.isRunning)
        assertEquals(AudioManager.MODE_NORMAL, audioManager.mode)
        SoftwareLoopback.start(context)
        assertTrue(SoftwareLoopback.isRunning)
        assertEquals(AudioManager.MODE_IN_COMMUNICATION, audioManager.mode)
        SoftwareLoopback.stop()
        assertEquals(AudioManager.MODE_NORMAL, audioManager.mode)
    }

    @Test fun switchingPlaybackRoutesWhileRunningUpdatesAudioMode() {
        SoftwareLoopback.start(context)
        assertEquals(AudioManager.MODE_NORMAL, audioManager.mode)
        SoftwareLoopback.setVoipMode(context, true)
        assertTrue(SoftwareLoopback.isRunning)
        assertEquals(AudioManager.MODE_IN_COMMUNICATION, audioManager.mode)
        SoftwareLoopback.setVoipMode(context, false)
        assertTrue(SoftwareLoopback.isRunning)
        assertEquals(AudioManager.MODE_NORMAL, audioManager.mode)
    }

    @Test fun stoppingMediaPlaybackDoesNotResetAnotherAudioMode() {
        audioManager.mode = AudioManager.MODE_RINGTONE
        SoftwareLoopback.start(context)
        SoftwareLoopback.stop()
        assertEquals(AudioManager.MODE_RINGTONE, audioManager.mode)
        SoftwareLoopback.setVoipMode(context, true)
        SoftwareLoopback.start(context)
        SoftwareLoopback.stop()
        assertEquals(AudioManager.MODE_RINGTONE, audioManager.mode)
    }

    @Test fun selectedPlaybackModeSurvivesServiceRecreation() {
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit()
            .putString("playback_tier", VirtualMicTier.MEDIA_PROJECTION.name).commit()
        val service = VirtualMicService(context, ShizukuManager())
        service.selectPlaybackTier(VirtualMicTier.MEDIA_PROJECTION)
        service.selectPlaybackTier(VirtualMicTier.VOIP_STREAM)
        assertEquals(VirtualMicTier.VOIP_STREAM, service.getActiveTier())
        assertEquals(VirtualMicTier.VOIP_STREAM,
            VirtualMicService(context, ShizukuManager()).getActiveTier())
        service.selectPlaybackTier(VirtualMicTier.MEDIA_PROJECTION)
        assertEquals(VirtualMicTier.MEDIA_PROJECTION,
            VirtualMicService(context, ShizukuManager()).getActiveTier())
    }
}
