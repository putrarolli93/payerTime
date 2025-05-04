package com.icaali.prayertime.sdk.model

data class PrayerTimeResponse(
    val status: Status,
    val result: Result
)

data class Status(
    val status_code: Int,
    val message_client: String,
    val message_server: String,
    val error_detail: Map<String, Any> // bisa disesuaikan jika struktur detail-nya pasti
)

data class Result(
    val timezone: String,
    val data: List<PrayerTime>
)

data class PrayerTime(
    val asr: String,
    val dhuhr: String,
    val fajr: String,
    val first_third: String,
    val imsak: String,
    val isha: String,
    val last_third: String,
    val maghrib: String,
    val midnight: String,
    val sunrise: String,
    val sunset: String
)

data class PrayerWithNotif(
    val name: String,
    var time: String,
    val isNotifEnabled: Boolean
)

