package com.micplugin.lite

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LiteApplication::class)
class LiteServiceTest {
    @Test fun serviceHasQuietStopActionAndStopsIfMicrophonePermissionIsMissing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as Application).denyPermissions(Manifest.permission.RECORD_AUDIO)
        val controller = Robolectric.buildService(LiteAudioService::class.java).create()
        val service = controller.get()
        try {
            val notification = shadowOf(service).lastForegroundNotification
            assertTrue(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
            val action = shadowOf(notification.actions.single().actionIntent).savedIntent
            assertEquals(LiteAudioService.STOP, action.action)
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(LiteAudioService.CHANNEL)
            assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
            assertNull(channel.sound)
            assertEquals(Service.START_NOT_STICKY, service.onStartCommand(null, 0, 1))
            assertTrue(shadowOf(service).isStoppedBySelf)
            assertFalse((context as LiteApplication).engine.state.value.running)
        } finally { controller.destroy() }
        assertTrue(shadowOf(service).isForegroundStopped)
    }
    @Test fun captureCannotStartOverAnExistingCall() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as Application).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val manager = context.getSystemService(android.media.AudioManager::class.java)
        manager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        val app = context as LiteApplication
        assertFalse(app.engine.start())
        assertFalse(app.engine.state.value.running)
        assertTrue(app.engine.state.value.error.orEmpty().contains("звонок"))
        assertEquals(android.media.AudioManager.MODE_IN_COMMUNICATION, manager.mode)
    }

}
