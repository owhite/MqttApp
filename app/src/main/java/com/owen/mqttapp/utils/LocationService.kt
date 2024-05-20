package com.owen.mqttapp.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.*
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.owen.mqttapp.SecondActivity
import com.owen.mqttapp.model.LocationData
import com.owen.mqttapp.model.LocationDataMqtt
import com.owen.mqttapp.notify.Notify
import com.owen.mqttapp.optimization.PermissionUtils
import com.owen.mqttapp.preferences.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit


class LocationService : Service() {


    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private val delayTime = 1 * 30 * 1000L // 30 sec
    private val minTimeForUpdate = 5 // In Mins

    private var recentLocation: Location? = null
    private var lastLocation: Location? = null

    private val NOTIFICATION_ID = 676
    private val PERMISSION_NOTIFICATION_ID = 677
    private var isGPSPermissionNotificationVisible = false

    private lateinit var locationManager: LocationManager
    private lateinit var powerManager: PowerManager

    private lateinit var telephonyManager: TelephonyManager
    private var UPDATE_INTERVAL_IN_MILLISECONDS = 15000L
    private var FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 15000L
    private var SMALLEST_DISPLACEMENT = 5F

    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var isLocationOn = false

    private val gson = Gson()
    private val mqttClient by lazy {
        MQTTClient(this, preferenceProvider)
    }


    private lateinit var preferenceProvider: Preference

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            onLocationUpdated(locationResult.lastLocation)
        }

        override fun onLocationAvailability(p0: LocationAvailability?) {
            super.onLocationAvailability(p0)
            if (getGPSStatus()) {
                clearAskForLocationServicesNotification()
            } else {
                askForLocationServicesNotification()
            }
            if (preferenceProvider.isMqttEnabled()) {
                if (!mqttClient.isConnected()) {
                    mqttClient.connect()
                }
            }
            updateLocationToServer(recentLocation)
        }
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        preferenceProvider = Preference(this)
        if (preferenceProvider.isMqttEnabled()) {
            mqttClient.connect()
        }
        val notification = createNotification()
        startForeground(1, notification)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        if (intent != null) {
            when (intent.action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                Actions.RECHECK.name -> reCheckService()
                Actions.UPDATE.name -> updateService()
                else -> Log.i(
                    "LocationService",
                    "This should never happen. No action in the received intent"
                )
            }
        } else {
            Log.i(
                "LocationService",
                "with a null intent. It has been probably restarted by the system."
            )
        }


        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    private fun setServiceRunning(boolean: Boolean) {
        isServiceStarted = boolean
        preferenceProvider.setLocationServiceState(boolean)
    }

    private fun startService() {

        if (isServiceStarted) {
            return
        }
        isLocationOn = getGPSStatus()
        setServiceRunning(true)
        acquireWakeLock()
        startLocationUpdates()
        checkFunctioningUsingTimer()
        preferenceProvider.setServiceStatus(true)

    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            powerManager.run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService::lock").apply {
                    acquire()
                }
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    private fun checkFunctioningUsingTimer() {
        //checking the working of location updates with a loop
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    processLocations()
                }
                delay(delayTime)
            }
            Log.i("LocationService", "End of the loop for the service")
        }
    }

    private fun stopService() {
        releaseWakeLock()
        removeLocationUpdates()
        stopForeground(true)
        stopSelf()
        setServiceRunning(false)
        preferenceProvider.setServiceStatus(false)

    }

    private fun reCheckService() {
        startService()
        checkDoze()

    }

    private fun updateService() {
        startService()
        reInitLocationUpdates()
    }

    private fun startLocationUpdates() {

        if (!PermissionUtils.hasLocationPermission(this)) {
            askForLocationPermissionNotification()
        } else {
            clearLocationPermissionNotification()
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            createLocationRequest()
            requestLocationUpdates()
        }
    }

    private fun createLocationRequest() {

        UPDATE_INTERVAL_IN_MILLISECONDS = preferenceProvider.getUpdateInterval()
        SMALLEST_DISPLACEMENT = preferenceProvider.getSmallestDisplacement()
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.smallestDisplacement = SMALLEST_DISPLACEMENT
    }

    private fun requestLocationUpdates() {
        try {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
        }
    }

    private fun removeLocationUpdates() {
        if (this::mFusedLocationClient.isInitialized) {
            try {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            } catch (unlikely: SecurityException) {
                Log.i(
                    "LocationService",
                    "Service stopped without being started: ${unlikely.message}"
                )
            }
        }
    }

    private fun checkDoze() {
        if (preferenceProvider.getIsLoggedIn()) {
            if (powerManager.isDeviceIdleMode) {
                Log.d("Location Services", "Device is Idle")
//                    openNetworkAndUpdate(getLastLocation(this))
            }
            startAlarm(this)
            Log.d("Location Services", "Started Alarm")
        }
    }

    private fun reInitLocationUpdates() {
        removeLocationUpdates()
        Handler(Looper.getMainLooper()).postDelayed({
            startLocationUpdates()
        }, 3000)
    }


    private fun processLocations() {

        if (preferenceProvider.getIsLoggedIn()) {

            if (!PermissionUtils.hasLocationPermission(this)) {
                askForLocationPermissionNotification()
                return
            } else {
                clearLocationPermissionNotification()
            }

            if (!isLocationOn) {
                Toast.makeText(this, "Location is on", Toast.LENGTH_SHORT).show()
            } else {
                if (recentLocation != null && lastLocation != null) {
                    val currentTimeMillis = getCurrentMillis()
                    val recentTime = recentLocation?.time

                    val differenceTime = recentTime?.let { currentTimeMillis.minus(it) }
                    val mins = differenceTime?.let { TimeUnit.MILLISECONDS.toMinutes(it) }

                    if (mins != null) {
                        if (mins > minTimeForUpdate) {
                            reInitLocationUpdates()
                        }
                    }
                } else {
                    reInitLocationUpdates()
                }
            }
        } else {
            stopService()
        }
    }

    private fun clearAskForLocationServicesNotification() {
        isLocationOn = true
        Notify.cancel(this, NOTIFICATION_ID)
    }

    private fun clearLocationPermissionNotification() {
        Notify.cancel(this, PERMISSION_NOTIFICATION_ID)
        isGPSPermissionNotificationVisible = false
    }

    private fun askForLocationServicesNotification() {
        isLocationOn = false
        Notify.create(this)
            .setTitle(getString(com.owen.mqttapp.R.string.location_tracker))
            .setContent(getString(com.owen.mqttapp.R.string.please_enable_location_services))
            .setSmallIcon(com.owen.mqttapp.R.mipmap.ic_launcher_round)
            .setImportance(Notify.NotificationImportance.MAX)
            .setColor(com.owen.mqttapp.R.color.black)
            .setId(NOTIFICATION_ID)
            .show()
    }

    private fun askForLocationPermissionNotification() {
        if (!isGPSPermissionNotificationVisible) {
            Notify.create(this)
                .setTitle(getString(com.owen.mqttapp.R.string.location_tracker))
                .setContent(getString(com.owen.mqttapp.R.string.please_enable_location_permission))
                .setSmallIcon(com.owen.mqttapp.R.mipmap.ic_launcher_round)
                .setImportance(Notify.NotificationImportance.MAX)
                .setColor(com.owen.mqttapp.R.color.black)
                .setId(PERMISSION_NOTIFICATION_ID)
                .show()
            isGPSPermissionNotificationVisible = true
        }
    }

    private fun createNotification(): Notification {

        val notificationChannelId = "MqttServiceChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Location Service Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Location Service"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, SecondActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            ) else Notification.Builder(this)

        return builder
            .setContentTitle(getString(com.owen.mqttapp.R.string.app_name))
            .setContentText("Required to sync location while in background")
            .setContentIntent(pendingIntent)
            .setSmallIcon(com.owen.mqttapp.R.mipmap.ic_launcher_round)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOngoing(true)
            .build()
    }

    private fun onLocationUpdated(location: Location?) {
        if (preferenceProvider.getIsLoggedIn()) {
            if (preferenceProvider.isMqttEnabled()) {
                if (!mqttClient.isConnected()) {
                    mqttClient.connect()
                }
            }
            updateLocationToServer(location)
        } else stopService()
    }

    @SuppressLint("NewApi")
    private fun updateLocationToServer(location: Location?) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        if (location != null) {


            val locationData = LocationDataMqtt("location", LocationData())

            locationData.data.bearing = location.bearing.toDouble()
            locationData.data.latitude = location.latitude
            locationData.data.longitude = location.longitude
            val roundedSpeed = decimalFormat.format(getSpeed(location.speed.toDouble()))
            locationData.data.speed = roundedSpeed.toDouble()
            val currentTimeMillis = System.currentTimeMillis()
            val formattedTimestamp = dateFormat.format(Date(currentTimeMillis))
            locationData.data.timeStamp = formattedTimestamp
            locationData.data.deviceId =
                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)


            Log.d("LocationData", "LocationData before JSON conversion: $locationData")

            val jsonLocationData = gson.toJson(locationData)

            try {
                mqttClient.publish(
                    preferenceProvider.getMqttTopic().toString(),
                    jsonLocationData.toString()
                )
            } catch (e: NullPointerException) {

            }

            Log.d("JsonLocationData", "LocationData JSON: $jsonLocationData")


        }
    }

    private fun getGPSStatus(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        startAlarm(this@LocationService, 3)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
//        if (mqttClient.isConnected()) {
//            mqttClient.disconnectClient()
//        }
        isServiceStarted = false

        preferenceProvider.setLocationServiceState(false)
        super.onDestroy()
    }

    enum class Actions {
        START,
        STOP,
        RECHECK,
        UPDATE
    }

}


