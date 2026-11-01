package com.example.wavemusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder

class PlaybackService : Service() {
    private var player: MediaPlayer? = null
    private val channelId = "wave_playback"

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channelId, "Воспроизведение", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.data
                if (uri != null) play(uri)
            }
            ACTION_STOP -> {
                player?.pause()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun play(uri: Uri) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(this@PlaybackService, uri)
                setOnPreparedListener { it.start(); startForeground(1, notification()) }
                setOnCompletionListener { stopForeground(STOP_FOREGROUND_DETACH); stopSelf() }
                prepareAsync()
            }
        } catch (_: Exception) { stopSelf() }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, channelId)
            .setContentTitle("Wave Music")
            .setContentText("Воспроизведение аудио")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }

    companion object {
        const val ACTION_PLAY = "com.example.wavemusic.PLAY"
        const val ACTION_STOP = "com.example.wavemusic.STOP"
    }
}
