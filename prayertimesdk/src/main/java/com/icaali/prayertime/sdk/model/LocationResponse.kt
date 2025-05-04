package com.icaali.prayertime.sdk.model

data class LocationResponse(
    val status: Status,
    val result: LocationResult
)

data class LocationResult(
    val city: String,
    val country: String
)