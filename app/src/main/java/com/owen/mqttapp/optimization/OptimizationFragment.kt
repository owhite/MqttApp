package com.owen.mqttapp.optimization

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.tasks.Task
import com.owen.mqttapp.R
import com.owen.mqttapp.databinding.FragmentOptimizationBinding
import com.owen.mqttapp.utils.setVisible
import com.permissionx.guolindev.PermissionX

class OptimizationFragment : Fragment() {

    private lateinit var pm: PowerManager
    private lateinit var manager: LocationManager

    private val INTERVAL = (100 * 1000).toLong()
    private val SMALLEST_DISPLACEMENT = 100.toFloat()
    private val FASTEST_INTERVAL = (100 * 1000).toLong()
    private val REQUEST_CHECK_SETTINGS = 21
    private val READ_SMS_PERMISSION_CODE = 101
    private val REQUEST_ACCESSIBILITY_PERMISSION = 102
    private val SCHEDULE_EXACT_ALARM_PERMISSION_CODE = 104

    private val READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 1001
    private val REQUEST_READ_CALL_LOG = 1
    private val REQUEST_READ_EXTERNAL_STORAGE = 123
    private var prominentDialog: android.app.AlertDialog? = null
    private var isProminentOpened: Boolean = false

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? =
        LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    private var UPDATE_INTERVAL_IN_MILLISECONDS = 2000000L
    private var FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500000L
    private var hasLocationUpdatedResquested = false
    private var isDoNextDone = false
    private lateinit var binding: FragmentOptimizationBinding

    private var mLocationCallback: LocationCallback? = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
        }

        override fun onLocationAvailability(p0: LocationAvailability?) {
            super.onLocationAvailability(p0)
            if (getGPSStatus()) {
                binding.gpsPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
                binding.gpsPermissionDescription.visibility = View.GONE
            } else {
                binding.gpsPermissionCheckmark.setImageResource(R.drawable.error_checkmark)
                binding.gpsPermissionDescription.visibility = View.VISIBLE
            }
        }
    }

    private fun getGPSStatus(): Boolean {
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOptimizationBinding.inflate(inflater, container, false)
        return (binding.root)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    companion object {
        @JvmStatic
        fun newInstance() = OptimizationFragment()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        if (!isDoNextDone)
            checkStatus()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkStatus() {
        prominentDialog?.dismiss()

        var shouldProceed = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (activity?.let { PermissionUtils.hasBackgroundLocationPermission(it) } == true) {
                binding.locationPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
                binding.locationPermissionDescription.visibility = View.GONE
                binding.tvLocationPermission.text = "Location permission granted"
                startLocationUpdates()
            } else {
                binding.locationPermissionCheckmark.setImageResource(R.drawable.error_checkmark)
                binding.locationPermissionDescription.visibility = View.VISIBLE
                shouldProceed = false
            }
        } else {
            if (activity?.let { PermissionUtils.hasLocationPermission(it) } == true) {
                binding.locationPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
                binding.locationPermissionDescription.visibility = View.GONE
                binding.tvLocationPermission.text = "Location permission granted"
                startLocationUpdates()
            } else {
                binding.locationPermissionCheckmark.setImageResource(R.drawable.error_checkmark)
                binding.locationPermissionDescription.visibility = View.VISIBLE
                shouldProceed = false
            }
        }

        if (pm.isIgnoringBatteryOptimizations(activity?.packageName)) {
            binding.batteryOptimizationCheckmark.setImageResource(R.drawable.done_checkmark)
            binding.batteryOptimizationPermissionDescription.visibility = View.GONE
            binding.tvBatteryOptimizationPermission.text = "Battery optimization disabled"
        } else {
            binding.batteryOptimizationCheckmark.setImageResource(R.drawable.error_checkmark)
            binding.batteryOptimizationPermissionDescription.visibility = View.VISIBLE
            shouldProceed = false
        }

        if (getGPSStatus()) {
            binding.gpsPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
            binding.gpsPermissionDescription.visibility = View.GONE
            binding.tvGpsPermission.text = "Location/GPS enabled"
        } else {
            binding.gpsPermissionCheckmark.setImageResource(R.drawable.error_checkmark)
            binding.gpsPermissionDescription.visibility = View.VISIBLE
            shouldProceed = false
        }
        if (binding.llNotificationPermission.isVisible) {
            when {
                context?.let {
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } == PackageManager.PERMISSION_GRANTED -> {
                    binding.notificationPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
                    binding.notificationPermissionDescription.visibility = View.GONE
                    binding.tvNotificationPermission.text = "Notification permission granted"
                }

                else -> {

                }
            }
        }

        if (shouldProceed) {
            isDoNextDone = true
            (activity as OptimizationActivity).doNext()
        }
    }

    private fun startLocationUpdates() {
        if (hasLocationUpdatedResquested) return
        hasLocationUpdatedResquested = true
        try {
            createLocationRequest()
            requestLocationUpdates()
            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                null
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest?.smallestDisplacement = SMALLEST_DISPLACEMENT
    }

    private fun requestLocationUpdates() {
        try {
            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showProminentDialog() {
        if (!isProminentOpened) {
            val alertDialog = android.app.AlertDialog.Builder(activity)
            val message =
                "<b> This App </b> collects location data to identify your current location even when the app is closed or not in use.<br><br>This data is used to show your current location."
            alertDialog.setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            alertDialog.setPositiveButton(R.string.ok) { dialogInterface, i ->
                dialogInterface.dismiss()
                isProminentOpened = true
                requestPermission()
            }
            prominentDialog = alertDialog.create()
            prominentDialog?.show()
        } else {
            requestPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermission() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            /* onExplainRequestReason -  */
            .onExplainRequestReason { scope, deniedList, beforeRequest ->
                val message =
                    if (deniedList.contains("android.permission.ACCESS_BACKGROUND_LOCATION")) {
                        getString(R.string.request_always_allow)
                    } else {
                        "This app only works correctly if it can access your location"
                    }
                scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
            }
            /* onForwardToSettings - Deny permission with checked Never ask again */
            .onForwardToSettings { scope, deniedList ->
                if (deniedList.isNotEmpty()) {
                    val message =
                        if (deniedList.contains("android.permission.ACCESS_BACKGROUND_LOCATION")) {
                            "This app only works correctly if it can always access your location." +
                                    "\n\n1. In app info, tap \"Permissions\"" +
                                    "\n\n2. Tap into \"Location permission\"" +
                                    "\n\n3. Select \"allow all the time\""
                        } else {
                            "This app only works correctly if it can always access your location." +
                                    "\n\n1. In the app info, tap \"Permissions\"" +
                                    "\n\n2. Turn on \"Location\""
                        }
                    scope.showForwardToSettingsDialog(deniedList, message, "Go to Settings", "Deny")
                }
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    checkStatus()
                }
            }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun init() {

        showNotificationPermissionDialog()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        pm = activity?.getSystemService(Context.POWER_SERVICE) as PowerManager
        manager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Log.i("Debug", Build.VERSION.SDK_INT.toString())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.llNotificationPermission.setVisible()
            (binding.llLocationPermission.layoutParams as LinearLayout.LayoutParams).apply {
                topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    30F,
                    resources.displayMetrics
                ).toInt()
            }
        }

        binding.notificationPermissionDescription.setOnClickListener {
            showNotificationPermissionDialog()
        }

        binding.locationPermissionDescription.setOnClickListener {
            showProminentDialog()
        }
        binding.gpsPermissionDescription.setOnClickListener {
            enableGPS()
        }
        binding.batteryOptimizationPermissionDescription.setOnClickListener {
            ignoreBatteryOptimization()
        }

    }


    private fun showNotificationPermissionDialog() {
        PermissionX.init(this)
            .permissions(Manifest.permission.POST_NOTIFICATIONS)
            .onForwardToSettings { scope, deniedList ->
                if (deniedList.isNotEmpty()) {
                    val message =
                        "Enable notifications to get notified." +
                                "\n\n1. In This App info, tap \"Notifications\"" +
                                "\n\n2. Enable \"notifications\""

                    scope.showForwardToSettingsDialog(deniedList, message, "Go to Settings", "Deny")
                }
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    binding.notificationPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
                    binding.notificationPermissionDescription.visibility = View.GONE
                    binding.tvNotificationPermission.text = "Notification permission granted"
                } else {
                    binding.notificationPermissionCheckmark.setImageResource(R.drawable.error_checkmark)
                    binding.notificationPermissionDescription.visibility = View.VISIBLE
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun ignoreBatteryOptimization() {
        val intent = Intent()
        val packageName = activity?.packageName
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            checkStatus()
        }
    }


    private fun enableGPS() {

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            binding.gpsPermissionCheckmark.setImageResource(R.drawable.done_checkmark)
            binding.gpsPermissionDescription.visibility = View.GONE
            binding.tvGpsPermission.text = "GPS enabled"
            return
        }

        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = INTERVAL
        mLocationRequest.fastestInterval = FASTEST_INTERVAL
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.smallestDisplacement = SMALLEST_DISPLACEMENT


        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val result: Task<LocationSettingsResponse>? =
            settingsClient.checkLocationSettings(locationSettingsRequest)

        result?.addOnCompleteListener { task: Task<LocationSettingsResponse> ->
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                requireActivity(),
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    //LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
                    // Location settings are not satisfied. However, we have no way to fix the
                    // settings so we won't show the dialog.
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mFusedLocationClient != null) {
            mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        }
        mFusedLocationClient = null
        mLocationRequest = null
        mLocationCallback = null
    }

}