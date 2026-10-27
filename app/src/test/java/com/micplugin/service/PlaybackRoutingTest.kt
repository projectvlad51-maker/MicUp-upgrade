package com.micplugin.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackRoutingTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = context.getSystemService(AudioManager::class.java)
    @Before fun setup() {
        SoftwareLoopback.stop()
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        manager.mode = AudioManager.MODE_NORMAL
    }
    @After fun cleanup() { SoftwareLoopback.stop() }
    private fun track(): AudioTrack = SoftwareLoopback::class.java.getDeclaredField("monitorTrack")
        .apply { isAccessible = true }.get(SoftwareLoopback) as AudioTrack

    @Test fun actualPlaybackHasOriginalMediaSpeechFloatMonoConfiguration() {
        SoftwareLoopback.start(context)
        val t = track()
        assertTrue(SoftwareLoopback.isRunning)
        assertEquals(AudioAttributes.USAGE_MEDIA, t.audioAttributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SPEECH, t.audioAttributes.contentType)
        assertEquals(AudioFormat.ENCODING_PCM_FLOAT, t.audioFormat)
        assertEquals(48000, t.sampleRate)
        assertEquals(1, t.channelCount)
        assertEquals(AudioManager.MODE_NORMAL, manager.mode)
    }
    @Test fun startingDuringCommunicationDoesNotRequestAnotherAudioMode() {
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        SoftwareLoopback.start(context)
        assertTrue(SoftwareLoopback.isRunning)
        assertEquals(AudioManager.MODE_IN_COMMUNICATION, manager.mode)
        assertEquals(AudioAttributes.USAGE_MEDIA, track().audioAttributes.usage)
    }
    @Test fun stoppingRestoresNormalModeAsInTheOriginal() {
        manager.mode = AudioManager.MODE_RINGTONE
        SoftwareLoopback.start(context)
        assertEquals(AudioManager.MODE_RINGTONE, manager.mode)
        SoftwareLoopback.stop()
        assertEquals(AudioManager.MODE_NORMAL, manager.mode)
    }
    @Test fun staleVoipPreferenceCannotChangeOriginalRouting() {
        context.getSharedPreferences("micup_prefs", Context.MODE_PRIVATE).edit().putString("playback_tier", "VOIP_STREAM").commit()
        VirtualMicService(context, ShizukuManager())
        SoftwareLoopback.start(context)
        assertEquals(AudioAttributes.USAGE_MEDIA, track().audioAttributes.usage)
        assertEquals(AudioManager.MODE_NORMAL, manager.mode)
    }
}
