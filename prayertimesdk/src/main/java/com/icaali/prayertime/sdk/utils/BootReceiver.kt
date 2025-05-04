package com.icaali.prayertime.sdk.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.icaali.prayertime.sdk.data.PrayerPrefManager
import com.icaali.prayertime.sdk.model.PrayerWithNotif
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefManager = PrayerPrefManager(context)
            val prayerList = prefManager.getPrayerList()

            for (prayer in prayerList) {
                if (prayer.isNotifEnabled) {
                    setPrayerAlarm(context, prayer)
                }
            }
        }
    }

    private fun setPrayerAlarm(context: Context, prayer: PrayerWithNotif) {
        val timeParts = prayer.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, PrayerReceiver::class.java).apply {
            putExtra("prayer_name", prayer.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayer.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}
