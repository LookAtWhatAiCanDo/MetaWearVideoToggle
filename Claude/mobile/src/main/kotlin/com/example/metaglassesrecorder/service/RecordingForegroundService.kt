package com.example.metaglassesrecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.metaglassesrecorder.ui.MainActivity

class RecordingForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "Starting foreground service")
                startForeground(NOTIFICATION_ID, buildNotification("Recording in progress…"))
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping foreground service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Meta glasses recording session"
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Meta Glasses")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(tap)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.metaglassesrecorder.START_RECORDING"
        const val ACTION_STOP  = "com.example.metaglassesrecorder.STOP_RECORDING"
        private const val CHANNEL_ID      = "recording_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG             = "RecordingService"
    }
}
