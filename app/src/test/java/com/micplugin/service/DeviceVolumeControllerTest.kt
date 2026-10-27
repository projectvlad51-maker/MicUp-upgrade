package com.micplugin.service

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeviceVolumeControllerTest {
    @Test fun mediaVolumeChangesRealStreamAndClampsToDeviceLimit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val am = context.getSystemService(AudioManager::class.java)
        val controller = DeviceVolumeController(context)
        val call = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        controller.setMedia(1000)
        assertEquals(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), am.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals(call, am.getStreamVolume(AudioManager.STREAM_VOICE_CALL))
        controller.setMedia(-10)
        assertEquals(0, am.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals(0, controller.volumes.value.media)
    }

    @Test fun refreshTracksExternalChangesAndCallVolumeHasSeparateControl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val am = context.getSystemService(AudioManager::class.java)
        val controller = DeviceVolumeController(context)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0)
        controller.refresh()
        assertEquals(3, controller.volumes.value.media)
        controller.setCall(1000)
        assertEquals(am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), controller.volumes.value.call)
        assertEquals(3, am.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
}
