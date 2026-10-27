package com.micplugin.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.micplugin.MainActivity
import com.micplugin.R
import com.micplugin.audio.AudioEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AudioProcessingService : Service() {

    companion object {
        const val CHANNEL_ID   = AudioNotifications.CHANNEL_ID
        const val NOTIF_ID     = 1001
        const val ACTION_STOP  = "com.micplugin.STOP"

        fun start(context: Context) {
            val intent = Intent(context, AudioProcessingService::class.java)
            context.startForegroundService(intent)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, AudioProcessingService::class.java))
        }
    }

    @Inject lateinit var audioEngine: AudioEngine

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var statusJob: Job? = null
    private var lastNotificationKey: String? = null

    private fun startStatusUpdates() {
        statusJob?.cancel()
        statusJob = scope.launch {
            var failCount = 0
            while (isActive) {
                try {
                    val status = audioEngine.status.value
                    val pluginCount = audioEngine.pluginChain.slots.value.size
                    updateNotification(
                        "Active · $pluginCount plugins",
                        pluginCount,
                        status.latencyMs,
                    )
                    failCount = 0
                } catch (e: Exception) {
                    failCount++
                    android.util.Log.e("MicUp.Service", "Status update failed #$failCount: $e")
                    // Re-pin notification so OEM can't kill it
                    try {
                        startForeground(NOTIF_ID, buildNotification("Active", 0, 0f))
                    } catch (_: Exception) {}
                }
                delay(5000)
            }
        }
    }

    private fun updateNotification(text: String, plugins: Int, latencyMs: Float) {
        val key = "$text:${latencyMs.toInt()}"
        if (lastNotificationKey == key) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text, plugins, latencyMs))
        lastNotificationKey = key
    }

    private fun buildNotification(text: String, plugins: Int, latencyMs: Float): Notification =
        AudioNotifications.build(this, text, latencyMs)

    private fun createNotificationChannel() = AudioNotifications.createChannel(this)

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "MicPlugin::AudioWakeLock"
        ).apply { acquire() } // No timeout — released explicitly in onDestroy
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification("Starting…", 0, 0f),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, buildNotification("Starting…", 0, 0f))
        }
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Wire processed audio from C++ → SoftwareLoopback monitor
        audioEngine.setMonitorCallback { buf: FloatArray, size: Int -> SoftwareLoopback.writeProcessed(buf, size) }
        val started = audioEngine.status.value.isRunning || audioEngine.start()
        if (!started) {
            // Do not leave a fake active notification or spawn update loops after a failed start.
            stopSelf()
            return START_NOT_STICKY
        }
        startStatusUpdates()
        if (!SoftwareLoopback.isRunning) {
            SoftwareLoopback.start(this)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        audioEngine.stop()
        SoftwareLoopback.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        wakeLock?.release()
        super.onDestroy()
    }


}
