package com.icaali.prayertime.sdk.network

import com.icaali.prayertime.sdk.model.LocationResponse
import com.icaali.prayertime.sdk.model.PrayerTimeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PrayerTimeApi {
    @GET("v1/salat/time")
    suspend fun getPrayerTimes(
        @Query("date") date: String,
        @Query("period") period: Int,
        @Query("city") city: String,
        @Query("country") country: String,
        @Header("Authorization") token: String
    ): Response<PrayerTimeResponse>

    @GET("v1/salat/location")
    suspend fun getDefaultLocation(): Response<LocationResponse>
}
