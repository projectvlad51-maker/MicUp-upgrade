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
}
