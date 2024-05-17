package com.owen.mqttapp.optimization

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.fragment.app.Fragment


object PermissionUtils {
    fun useRunTimePermissions(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
    }

    fun hasPermission(context: Context, permission: String?): Boolean {
        return if (useRunTimePermissions()) {
            context.checkSelfPermission(permission!!) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun hasLocationPermission(context: Context): Boolean {
        return if (useRunTimePermissions()) {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun hasWritePermission(context: Context): Boolean {
        if (useRunTimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    fun requestWritePermissions(activity: Activity, requestCode: Int) {
        if (useRunTimePermissions()) {
            val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            activity.requestPermissions(permission, requestCode)
        }
    }

    fun hasActivityRecognitionPermission(context: Context): Boolean {
        if (useRunTimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (useRunTimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    fun hasRequiredPermissionForLocation(context: Context): Boolean {
        return hasBackgroundLocationPermission(context) && hasLocationPermission(context)
    }

    fun hasCallPermission(context: Context): Boolean {
        return if (useRunTimePermissions()) {
            context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun requestCallPermissions(activity: Activity, requestCode: Int) {
        if (useRunTimePermissions()) {
            val permission = arrayOf(Manifest.permission.CALL_PHONE)
            activity.requestPermissions(permission, requestCode)
        }
    }

    fun requestPermissions(activity: Activity, permission: Array<String?>?, requestCode: Int) {
        if (useRunTimePermissions()) {
            activity.requestPermissions(permission!!, requestCode)
        }
    }

    fun requestPermissions(fragment: Fragment, permission: Array<String?>?, requestCode: Int) {
        if (useRunTimePermissions()) {
            fragment.requestPermissions(permission!!, requestCode)
        }
    }

    fun requestLocationPermissions(fragment: Fragment, requestCode: Int) {
        if (useRunTimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val permission = arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                fragment.requestPermissions(permission, requestCode)
            } else {
                val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                fragment.requestPermissions(permission, requestCode)
            }
        }
    }

    fun requestLocationAndActivityPermissions(activity: Activity, requestCode: Int) {
        if (useRunTimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val permission = arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
                activity.requestPermissions(permission, requestCode)
            } else {
                val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                activity.requestPermissions(permission, requestCode)
            }
        }
    }

    fun requestActivityRecognitionPermissions(activity: Activity, requestCode: Int) {
        if (useRunTimePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val permission = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
                activity.requestPermissions(permission, requestCode)
            }
        }
    }

    fun shouldShowRational(activity: Activity, permission: String?): Boolean {
        return if (useRunTimePermissions()) {
            activity.shouldShowRequestPermissionRationale(permission!!)
        } else false
    }

    fun shouldAskForPermission(activity: Activity, permission: String?): Boolean {
        return if (useRunTimePermissions()) {
            !hasPermission(activity, permission) &&
                    (!hasAskedForPermission(activity, permission) ||
                            shouldShowRational(activity, permission))
        } else false
    }

    fun goToAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    fun hasAskedForPermission(activity: Activity?, permission: String?): Boolean {
        return PreferenceManager
            .getDefaultSharedPreferences(activity)
            .getBoolean(permission, false)
    }

    fun markedPermissionAsAsked(activity: Activity?, permission: String?) {
        PreferenceManager
            .getDefaultSharedPreferences(activity)
            .edit()
            .putBoolean(permission, true)
            .apply()
    }
}

