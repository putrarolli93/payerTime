package com.icaali.prayertime.sdk.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.icaali.prayertime.sdk.R

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: return

        showNotification(context, prayerName)
    }

    private fun showNotification(context: Context, prayerName: String) {
        createNotificationChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = prayerName.hashCode() // Gunakan hashcode untuk ID unik

        val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.azantonenew}")

        val notification = NotificationCompat.Builder(context, "prayer_channel")
            .setContentTitle("Waktu Sholat: $prayerName")
            .setContentText("Ini adalah waktu sholat $prayerName!")
            .setSmallIcon(R.drawable.ic_notif_on) // Ganti dengan ikon notifikasi
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
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
                description = "Channel untuk notifikasi jadwal sholat"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
