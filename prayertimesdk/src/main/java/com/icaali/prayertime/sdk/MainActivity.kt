package com.icaali.prayertime.sdk

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.icaali.prayertime.sdk.data.PrayerPrefManager
import com.icaali.prayertime.sdk.databinding.ActivityMainSdkBinding
import com.icaali.prayertime.sdk.model.DisplayPrayerTime
import com.icaali.prayertime.sdk.model.PrayerTime
import com.icaali.prayertime.sdk.model.PrayerWithNotif
import com.icaali.prayertime.sdk.ui.adapter.PrayerTimeAdapter
import com.icaali.prayertime.sdk.utils.PrayerReceiver
import com.icaali.prayertime.sdk.viewmodel.PrayerTimeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.icaali.prayertime.sdk.ui.activity.QiblaActivity
import com.icaali.prayertime.sdk.utils.capitalizeFirst
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainSdkBinding.inflate(layoutInflater)
    }
    private lateinit var viewModel: PrayerTimeViewModel
    private lateinit var adapter: PrayerTimeAdapter
    private lateinit var prayerPrefManager: PrayerPrefManager
    var pendingUpdatedItem: PrayerWithNotif? = null
    private var pendingPrayerItem: PrayerWithNotif? = null

    var savedCity = ""
    var savedCountry = ""
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
//        enableEdgeToEdge()

        prayerPrefManager = PrayerPrefManager(this)
        setUpView()
        setupRecyclerView()
        setupViewModel()
        setUpDefaultLocation()
//        checkExactAlarmPermission()
//        requestRelevantPermissions(this)
        getLocation(this@MainActivity)
        requestBatteryOptimization()
    }

    private fun requestRelevantPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()

        // Cek permission notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Cek dan request permission lokasi (jika dibutuhkan di sini juga)
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Jika semua permission sudah diberikan, cek izin alarm
            checkExactAlarmPermission(activity)
        }
    }

    private fun checkExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (granted && pendingUpdatedItem != null) {
                handlePrayerNotificationToggle(pendingUpdatedItem!!)
                pendingUpdatedItem = null
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation(this@MainActivity)
            } else {
                // Izin ditolak
                Log.d("Permission", "Lokasi ditolak")
            }
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error requesting battery optimization exception", e)
                }
            }
        }
    }

    private fun setUpDefaultLocation() {
        if (!prayerPrefManager.hasSavedLocation()) {
            viewModel.getDefaultLocation()
            viewModel.location.observe(this) { location ->
                if (location.city.isNotEmpty() && location.country.isNotEmpty()) {
                    prayerPrefManager.saveLocation(location.city, location.country)
                }
            }
        } else {
            savedCity = prayerPrefManager.getCity().toString()
            savedCountry = prayerPrefManager.getCountry().toString()
        }
    }

    private fun setUpView() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.ivLocation.setOnClickListener {
            val intent = Intent(this@MainActivity, QiblaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handlePrayerNotificationToggle(updatedItem: PrayerWithNotif) {

        // Update status di SharedPreferences
        prayerPrefManager.updateNotifStatus(updatedItem.name, updatedItem.isNotifEnabled)

        // Tampilkan snackbar sesuai status
        val text = when {
            updatedItem.name == "imsak" && updatedItem.isNotifEnabled ->
                "Active ${updatedItem.name.capitalizeFirst()} reminder"
            updatedItem.name == "imsak" && !updatedItem.isNotifEnabled ->
                "Inactive ${updatedItem.name.capitalizeFirst()} reminder"
            updatedItem.name == "fajr" && updatedItem.isNotifEnabled ->
                "Active Shubuh Prayer"
            updatedItem.name == "fajr" && !updatedItem.isNotifEnabled ->
                "Inactive Shubuh Prayer"
            updatedItem.isNotifEnabled ->
                "Active ${updatedItem.name.capitalizeFirst()} Prayer"
            else ->
                "Inactive ${updatedItem.name.capitalizeFirst()} Prayer"
        }

        val color = if (updatedItem.isNotifEnabled) R.color.colorYellow else R.color.colorRedNotif
        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, color))
            .show()

        // Refresh adapter
        val updatedList = prayerPrefManager.getPrayerList()
        adapter.setData(updatedList)

        if (hasExactAlarmPermission(this@MainActivity) && updatedItem.isNotifEnabled) {
            setPrayerAlarm(this, updatedItem)
        } else if (!hasExactAlarmPermission(this@MainActivity)) {
            pendingPrayerItem = updatedItem // Simpan niat
            requestExactAlarmPermission(this@MainActivity)
        }else {
            cancelPrayerAlarm(this, updatedItem) // Batalkan alarm
        }
    }

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // < Android 12 tidak butuh izin
        }
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        pendingPrayerItem?.let {
            if (hasExactAlarmPermission(this) && it.isNotifEnabled) {
                setPrayerAlarm(this, it)
                pendingPrayerItem = null
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PrayerTimeAdapter{ updatedItem ->
            if (!hasAllRelevantPermissions()) {
                pendingUpdatedItem = updatedItem
                requestRelevantPermissions(this@MainActivity)
            } else {
                handlePrayerNotificationToggle(updatedItem)
            }
        }
        binding.rvPrayerTime.layoutManager = LinearLayoutManager(this)
        binding.rvPrayerTime.adapter = adapter
    }

    private fun hasAllRelevantPermissions(): Boolean {
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true // Sebelum Android 13, gak perlu izin notifikasi

        val locationGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return notificationGranted && locationGranted
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[PrayerTimeViewModel::class.java]
        viewModel.prayerTimes.observe(this) { list ->
            val todayPrayer = list.firstOrNull()
            todayPrayer?.let {
                var prayerFormat = mapToPrayerWithNotifList(this, it)
                setAlarmEveryFetchLocation(this, prayerFormat)
                adapter.setData(prayerFormat)
                val nextPrayer = getNextPrayerTime(it)
                nextPrayer?.let { (name, time) ->
                    Log.d("NextPrayer", "Next prayer is $name at $time")

                    if (name == "imsak")
                        binding.tvNextPrayerTitle.text = "Time until Imsak"
                    else if (name == "fajr")
                        binding.tvNextPrayerTitle.text = "Next Shubuh" + " Prayer"
                    else
                        binding.tvNextPrayerTitle.text = "Next " + name.capitalizeFirst() + " Prayer"

                    startCountdownToPrayer(time)
                }
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCountdownToPrayer(time: String) {
        val timeParts = time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Buat calendar waktu target
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                // Jika waktu sudah lewat hari ini, tambah 1 hari
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val now = Calendar.getInstance().timeInMillis
        val diff = targetCal.timeInMillis - now

        object : CountDownTimer(diff, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60

                val countdownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                binding.tvNextPrayerTime.text = countdownText
            }

            override fun onFinish() {
                binding.tvNextPrayerTime.text = "00:00:00"
            }
        }.start()
    }

    private fun setAlarmEveryFetchLocation(context: Context, prayerFormat: List<PrayerWithNotif>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel semua alarm dulu
        prayerFormat.forEach { prayer ->
            val intent = Intent(context, PrayerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayer.name.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }

        // Nyalakan alarm baru hanya untuk yang notif-nya aktif
        prayerFormat.forEach { prayer ->
            if (prayer.isNotifEnabled) {
                setPrayerAlarm(this, prayer)
            }
        }
    }

    private fun fetchPrayerTimes(city: String, country: String) {
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        viewModel.fetchPrayerTimes(
            date = date,
            period = 2,
            city = city,
            country = country,
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjRhNzU5YjllLTVmN2YtNDViNC05NDVkLTA5MGE2NzVhYzMxMCIsImFwcCI6ImNvbS5pY2FhbGkudGFzYmVlaCIsImRjIjoxNzQ0OTY5MzgyfQ.sY35XuKbOuNMtUSePoXZoU0oSBQgmMfvOPsnXcHkKxc"
        )
    }

    private fun getLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Cek apakah izin lokasi sudah diberikan
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Jika izin belum diberikan, minta izin
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Jika izin sudah diberikan, dapatkan lokasi
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val cityAndCountry = getCityAndCountry(context, location)
                if (cityAndCountry != null) {
                    val (city, country) = cityAndCountry
                    fetchPrayerTimes(city, country)
                } else {
                    // Gagal dari Geocoder, fallback
                    if (savedCity != "" && savedCountry != "")
                        fetchPrayerTimes(savedCity, savedCountry)
                }
            } else {
                // Lokasi null, fallback
                if (savedCity != "" && savedCountry != "")
                    fetchPrayerTimes(savedCity, savedCountry)
            }
        }.addOnFailureListener {
            // Gagal mendapatkan lokasi, fallback
            if (savedCity != "" && savedCountry != "")
                fetchPrayerTimes(savedCity, savedCountry)
        }
    }

    private fun getCityAndCountry(context: Context, location: Location): Pair<String, String>? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses?.get(0)
                val city = address?.locality ?: "Unknown City"
                val country = address?.countryCode ?: "Unknown Country"

                binding.tvDesc.text = address?.subAdminArea + ", " + address?.countryName
                Pair(city, country)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getNextPrayerTime(prayerTime: PrayerTime): Pair<String, String>? {
        val currentTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Urutan sesuai waktu salat utama
        val orderedTimes = listOf(
            "imsak" to prayerTime.imsak,
            "fajr" to prayerTime.fajr,
            "dhuhr" to prayerTime.dhuhr,
            "asr" to prayerTime.asr,
            "maghrib" to prayerTime.maghrib,
            "isha" to prayerTime.isha
        )

        for ((name, timeStr) in orderedTimes) {
            val prayerDate = dateFormat.parse(timeStr)
            val prayerCalendar = Calendar.getInstance().apply {
                time = prayerDate!!
                set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
                set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))
            }

            if (prayerCalendar.after(currentTime)) {
                return name to timeStr
            }
        }

        // Kalau semua sudah lewat, kembali ke fajr esok hari
        return "fajr" to prayerTime.fajr
    }

    fun mapToPrayerWithNotifList(context: Context, prayerTime: PrayerTime): List<PrayerWithNotif> {
        val existingList = prayerPrefManager.getPrayerList()

        val timeMap = mapOf(
            "imsak" to prayerTime.imsak,
            "fajr" to prayerTime.fajr,
            "dhuhr" to prayerTime.dhuhr,
            "asr" to prayerTime.asr,
            "maghrib" to prayerTime.maghrib,
            "isha" to prayerTime.isha
        )

        val finalList: List<PrayerWithNotif> = if (existingList.isEmpty()) {
            // Belum pernah disimpan sebelumnya, buat list baru dengan default notif true
            val newList = timeMap.map { (name, time) ->
                PrayerWithNotif(
                    name = name,
                    time = time,
                    isNotifEnabled = false // default true untuk pertama kali
                )
            }
            prayerPrefManager.savePrayerList(newList)
            newList
        } else {
            // Sudah ada sebelumnya, update jam-nya saja, pertahankan status notifikasinya
            val updatedList = timeMap.map { (name, time) ->
                val existingItem = existingList.find { it.name == name }
                PrayerWithNotif(
                    name = name,
                    time = time,
                    isNotifEnabled = existingItem?.isNotifEnabled ?: true
                )
            }
            prayerPrefManager.savePrayerList(updatedList)
            updatedList
        }

        return finalList
    }

    fun setPrayerAlarm(context: Context, prayerWithNotif: PrayerWithNotif) {
        if (!prayerWithNotif.isNotifEnabled) {
            cancelPrayerAlarm(context, prayerWithNotif)
            return
        }

        val timeParts = prayerWithNotif.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // ⏰ Geser ke besok kalau waktu sudah lewat
            if (timeInMillis <= System.currentTimeMillis()) {
                Log.d("Alarmcuy", "Alarm udah lewat")
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, PrayerReceiver::class.java).apply {
            putExtra("prayer_name", prayerWithNotif.name)
            flags = Intent.FLAG_RECEIVER_FOREGROUND
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerWithNotif.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Gunakan setAlarmClock untuk Android 8+ (lebih reliable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Untuk Android 12+, periksa izin SCHEDULE_EXACT_ALARM
                if (alarmManager.canScheduleExactAlarms()) {
                    val triggerTime = System.currentTimeMillis() + 10 * 1000 // 10 detik dari sekarang
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Jika tidak memiliki izin, gunakan setAndAllowWhileIdle sebagai fallback
                    val dateFormat = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
                    val readableTime = dateFormat.format(Date(calendar.timeInMillis))
                    Log.d("Alarmcuy", "Alarm akan menyala pada: $readableTime")
                    val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                    alarmManager.setAlarmClock(info, pendingIntent)
                }
            } else {
                // Untuk Android 8-11
                val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } else {
            // Untuk Android 6-7
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    // Fungsi untuk membatalkan alarm
    fun cancelPrayerAlarm(context: Context, prayerWithNotif: PrayerWithNotif) {
        val intent = Intent(context, PrayerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerWithNotif.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun checkAndOpenNotificationSettings(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Mengecek apakah notifikasi untuk aplikasi ini diaktifkan
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()

        // Jika notifikasi belum diaktifkan, buka pengaturan aplikasi untuk notifikasi
        if (!areNotificationsEnabled) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }
}
