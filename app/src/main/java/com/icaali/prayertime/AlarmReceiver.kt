package com.icaali.prayertime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.icaali.prayertime.sdk.R
import com.icaali.prayertime.sdk.utils.capitalizeFirst

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("Alarmcuy", "onReceive")
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "app:alarmWakeLock")
        wakeLock.acquire(3000)
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        Log.d("Alarmcuy", "showNotification")

        createNotificationChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = "testAlarm".hashCode()
        val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.azantonenew}")

        val notification = NotificationCompat.Builder(context, "prayer_channel")
            .setContentTitle("Test Title")
            .setContentText("test desc")
            .setSmallIcon(R.drawable.ic_prayer_time) // Ganti dengan ikon notifikasi
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Menandai sebagai notifikasi alarm
            .setAutoCancel(true)
            .setSound(soundUri)
            .setDefaults(Notification.DEFAULT_SOUND)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "prayer_channel"
            val channelName = "Prayer Alarm Channel"

            val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.azantonenew}")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes) // <- pasang suara DI SINI
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Terlihat di layar kunci
                description = "Channel untuk notifikasi jadwal sholat"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
