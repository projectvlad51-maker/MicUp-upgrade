package com.micplugin.service

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.micplugin.MainActivity

internal object AudioNotifications {
    // A new quiet channel also fixes existing installations with the former HIGH channel.
    const val CHANNEL_ID = "micplugin_audio_status"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Работа микрофона", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Тихий статус обработки звука MicUp"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(context: Context, text: String, latencyMs: Float): Notification {
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(context, 1,
            Intent(context, AudioProcessingService::class.java).setAction(AudioProcessingService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE)
        val latency = if (latencyMs > 0) " · ${latencyMs.toInt()} мс" else ""
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MicUp — обработка звука")
            .setContentText(text + latency)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Остановить", stop)
            .build()
    }
}
