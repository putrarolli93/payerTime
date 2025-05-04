package com.icaali.prayertime.sdk.model

data class DisplayPrayerTime(
    val name: String,
    val time: String,
    val isNext: Boolean = false,
    val isAlarmOn: Boolean = false,
    val isSleepMode: Boolean = false
)
