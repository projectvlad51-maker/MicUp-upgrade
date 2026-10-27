package com.micplugin.lite

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat

class LiteAudioService : Service() {
    private val app get() = application as LiteApplication
    private var wakeLock: PowerManager.WakeLock? = null
    companion object {
        const val STOP = "com.micplugin.lite.STOP"
        const val CHANNEL = "lite_audio_status"
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Звук MicUp Lite", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null); enableVibration(false); setShowBadge(false)
            })
        val open = PendingIntent.getActivity(this, 0, Intent(this, LiteActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, LiteAudioService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL).setContentTitle("MicUp Lite")
            .setContentText("VoIP · обработка звука включена").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open).setOngoing(true).setSilent(true).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).addAction(0, "Остановить", stop).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, notification)
        app.engine.onFailure = { Handler(Looper.getMainLooper()).post { stopSelf() } }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (!app.engine.start()) { stopSelf(); return START_NOT_STICKY }
        if (wakeLock == null) wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MicUpLite::Audio").apply { acquire() }
        return START_STICKY // preserve an active session across service recreation, as in the original
    }
    override fun onDestroy() {
        app.engine.onFailure = null
        app.engine.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
