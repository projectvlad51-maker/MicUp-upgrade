package com.micplugin.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AudioNotificationsTest {
    @Test fun statusChannelIsQuietAndNotificationDoesNotAlertOnEveryUpdate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AudioNotifications.createChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(AudioNotifications.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
        val notification = AudioNotifications.build(context, "Работает", 10f)
        assertTrue(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(AudioNotifications.CHANNEL_ID, notification.channelId)
        assertEquals(0, notification.defaults and (Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE))
    }

    @Test fun stopActionTargetsAudioServiceWhileTappingStatusOpensMainScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notification = AudioNotifications.build(context, "Работает", 0f)
        val stop = shadowOf(notification.actions.single().actionIntent).savedIntent
        assertEquals(AudioProcessingService.ACTION_STOP, stop.action)
        assertEquals(AudioProcessingService::class.java.name, stop.component?.className)
        val open = shadowOf(notification.contentIntent).savedIntent
        assertEquals("com.micplugin.MainActivity", open.component?.className)
    }
}
