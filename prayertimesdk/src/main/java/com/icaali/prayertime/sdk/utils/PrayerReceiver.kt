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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.icaali.prayertime.sdk.R

class PrayerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Memastikan receiver berjalan bahkan di background
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:PrayerWakeLock")
        wakeLock.acquire(10*60*1000L) // 10 menit

        val prayerName = intent.getStringExtra("prayer_name") ?: return

        // Tampilkan notifikasi
        showNotification(context, prayerName)

        wakeLock.release()
    }

    private fun showNotification(context: Context, prayerName: String) {
        createNotificationChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = prayerName.hashCode() // Gunakan hashcode untuk ID unik

        val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.azantonenew}")

        var title = "It's time for ${prayerName.capitalizeFirst()} prayer"
        var content = "the most beloved deed to Allah is performing the prayer on time. (HR. Bukhori, Muslim)"
        if (prayerName == "imsak") {
            title = "It's time for ${prayerName.capitalizeFirst()}"
            content = "Prepare yourself for the Fajr prayer."
        }

        val notification = NotificationCompat.Builder(context, "prayer_channel")
            .setContentTitle(title)
            .setContentText(content)
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
