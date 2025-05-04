package com.icaali.prayertime.sdk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icaali.prayertime.sdk.model.LocationResult
import com.icaali.prayertime.sdk.model.PrayerTime
import com.icaali.prayertime.sdk.network.RetrofitClient
import kotlinx.coroutines.launch

class PrayerTimeViewModel : ViewModel() {
    private val _prayerTimes = MutableLiveData<List<PrayerTime>>()
    val prayerTimes: LiveData<List<PrayerTime>> get() = _prayerTimes
    private val _location = MutableLiveData<LocationResult>()
    val location: LiveData<LocationResult> get() = _location

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchPrayerTimes(
        date: String,
        period: Int,
        city: String,
        country: String,
        token: String
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getPrayerTimes(date, period, city, country, token)
                if (response.isSuccessful && response.body() != null) {
                    _prayerTimes.value = response.body()!!.result.data
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            }
        }
    }

    fun getDefaultLocation() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getDefaultLocation()
                if (response.isSuccessful && response.body() != null) {
                    _location.value = response.body()!!.result
                } else {
                    _error.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            }
        }
    }
}