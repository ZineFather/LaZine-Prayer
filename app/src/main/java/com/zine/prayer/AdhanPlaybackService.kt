package com.zine.prayer

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

class AdhanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private val notificationId = 8100
    private val channelId = "prayer_adhan_v3"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }

        val prayer = intent?.getStringExtra("prayer") ?: "prayer"
        ensureChannel()
        startForeground(notificationId, buildNotification(prayer))

        stopPlayerOnly()
        try {
            val prefs = getSharedPreferences("lenzin_prayer_settings", MODE_PRIVATE)
            val customUri = prefs.getString("custom_adhan_uri", null)
            val selectedVoice = prefs.getString("adhan_voice", "adam") ?: "adam"
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(attributes)
            media.setVolume(1.0f, 1.0f)
            media.isLooping = false
            media.setOnCompletionListener { stopPlayback() }
            media.setOnErrorListener { _, _, _ -> stopPlayback(); true }
            if (customUri != null) {
                media.setDataSource(this, Uri.parse(customUri))
            } else {
                val audioRes = when (selectedVoice) { "sabah" -> R.raw.adhan_sabah; "aqib" -> R.raw.adhan_aqib; "andrewler" -> R.raw.adhan_andrewler; else -> R.raw.adhan }
                val afd = resources.openRawResourceFd(audioRes)
                media.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }
            media.setOnPreparedListener { mp -> if (player === mp) mp.start() }
            media.prepareAsync()
        } catch (_: Exception) {
            stopPlayback()
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Full Adhan",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Full offline Adhan playback"
                channel.setSound(null, null)
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(prayer: String): Notification {
        val prefs = getSharedPreferences("lenzin_prayer_settings", MODE_PRIVATE)
        val arabic = prefs.getString("lang", "ar") == "ar"
        val names = if (arabic) {
            mapOf("fajr" to "الفجر", "dhuhr" to "الظهر", "asr" to "العصر",
                "maghrib" to "المغرب", "isha" to "العشاء")
        } else {
            mapOf("fajr" to "Fajr", "dhuhr" to "Dhuhr", "asr" to "Asr",
                "maghrib" to "Maghrib", "isha" to "Isha")
        }
        val title = if (arabic) "حان موعد صلاة ${names[prayer] ?: prayer}"
            else "It is time for ${names[prayer] ?: prayer}"
        val stopIntent = Intent(this, PrayerAlarmReceiver::class.java)
            .setAction("com.zine.prayer.STOP_ADHAN")
            .putExtra("prayer", prayer)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 8101, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 8102, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopLabel = if (arabic) "إيقاف الأذان" else "Stop Adhan"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(if (arabic) "LaZine Prayer • الأذان يعمل الآن" else "LaZine Prayer • Adhan is playing")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, stopLabel, stopPendingIntent)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
    }

    private fun stopPlayerOnly() {
        try {
            player?.setOnCompletionListener(null)
            player?.setOnErrorListener(null)
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }

    private fun stopPlayback() {
        stopPlayerOnly()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        getSystemService(NotificationManager::class.java).cancel(notificationId)
        stopSelf()
    }

    override fun onDestroy() {
        stopPlayerOnly()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.zine.prayer.STOP_ADHAN_PLAYBACK"
    }
}
