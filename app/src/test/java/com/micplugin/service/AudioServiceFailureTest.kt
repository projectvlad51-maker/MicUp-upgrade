package com.micplugin.service

import android.app.Service
import com.micplugin.MicPluginApp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = MicPluginApp::class)
class AudioServiceFailureTest {
    @Test fun unavailableNativeEngineStopsServiceInsteadOfLeavingFakeActiveStatus() {
        // Android JNI audio is unavailable in this host test; exercise the real failure path.
        val controller = Robolectric.buildService(AudioProcessingService::class.java).create()
        val service = controller.get()
        try {
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(null, 0, 1))
            assertTrue(shadowOf(service).isStoppedBySelf)
            assertFalse(SoftwareLoopback.isRunning)
        } finally { controller.destroy() }
        assertTrue(shadowOf(service).isForegroundStopped)
    }
    @Test fun existingCommunicationModeDoesNotBlockTheNativeStartAttempt() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = context.getSystemService(android.media.AudioManager::class.java)
        manager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        val controller = Robolectric.buildService(AudioProcessingService::class.java).create()
        val service = controller.get()
        try {
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(null, 0, 1))
            assertEquals("Не удалось запустить микрофон", service.audioEngine.notice.value)
            assertEquals(android.media.AudioManager.MODE_IN_COMMUNICATION, manager.mode)
            assertFalse(SoftwareLoopback.isRunning)
        } finally { controller.destroy() }
    }

    @Test fun unavailableRootBackendNeverReportsSuccessfulCapture() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("micup_prefs", android.content.Context.MODE_PRIVATE)
        OriginalAudioDefaults.full(prefs)
        prefs.edit().putString("playback_tier", VirtualMicTier.ROOT_MAGISK.name).commit()
        val controller = Robolectric.buildService(AudioProcessingService::class.java).create()
        val service = controller.get()
        try {
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(null, 0, 1))
            assertTrue(service.audioEngine.notice.value.orEmpty().contains("Root"))
            assertFalse(service.audioEngine.status.value.isRunning)
            assertFalse(SoftwareLoopback.isRunning)
        } finally { controller.destroy() }
    }
}
