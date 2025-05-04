package com.icaali.prayertime.sdk.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.icaali.prayertime.sdk.model.PrayerWithNotif

class PrayerPrefManager(context: Context) {

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = Gson()
    private val key = "prayer_time_with_notif"
    private val cityKey = "user_city"
    private val countryKey = "user_country"

    fun savePrayerList(list: List<PrayerWithNotif>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    fun getPrayerList(): List<PrayerWithNotif> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<PrayerWithNotif>>() {}.type
        return gson.fromJson(json, type)
    }

    fun updateAllPrayerTimes(timeMap: Map<String, String>) {
        val currentList = getPrayerList()
        val updatedList = timeMap.map { (name, newTime) ->
            val isEnabled = currentList.find { it.name == name }?.isNotifEnabled ?: true
            PrayerWithNotif(name, newTime, isEnabled)
        }
        savePrayerList(updatedList)
    }

    fun updateNotifStatus(name: String, isEnabled: Boolean) {
        val currentList = getPrayerList().toMutableList()
        val updatedList = currentList.map {
            if (it.name == name) it.copy(isNotifEnabled = isEnabled) else it
        }
        savePrayerList(updatedList)
    }

    fun updatePrayerTime(name: String, newTime: String) {
        val currentList = getPrayerList().toMutableList()
        val updatedList = currentList.map {
            if (it.name == name) it.copy(time = newTime) else it
        }
        savePrayerList(updatedList)
    }

    fun hasSavedLocation(): Boolean {
        return prefs.contains(cityKey) && prefs.contains(countryKey)
    }

    fun saveLocation(city: String, country: String) {
        prefs.edit()
            .putString(cityKey, city)
            .putString(countryKey, country)
            .apply()
    }

    fun getCity(): String? = prefs.getString(cityKey, null)
    fun getCountry(): String? = prefs.getString(countryKey, null)
}