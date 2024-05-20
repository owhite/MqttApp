package com.owen.mqttapp.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat


fun startLocationService(context: Context) {
    Intent(context, LocationService::class.java).also {
        it.action = LocationService.Actions.START.name
        ContextCompat.startForegroundService(context, it)
    }
}

fun stopLocationService(context: Context) {
    Intent(context, LocationService::class.java).also {
        it.action = LocationService.Actions.STOP.name
        ContextCompat.startForegroundService(context, it)
    }
}

fun startAlarm(context: Context) {
    startAlarm(context, 10 * 60)
}

@SuppressLint("MissingPermission", "ScheduleExactAlarm")
fun startAlarm(context: Context, timeGap: Long) {
    val intent = Intent(context, LocationService::class.java)
    intent.action = LocationService.Actions.UPDATE.name
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PendingIntent.getForegroundService(
            context,
            MyKeys.ALARM_REQUEST_CODE_IDLE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    } else {
        PendingIntent.getService(
            context,
            MyKeys.ALARM_REQUEST_CODE_IDLE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (hasScheduleExactAlarmPermission(context)) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeGap * 1000, pi
            )
        } else {
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeGap * 1000, pi
            )
        }
    } else {
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeGap * 1000, pi
        )
    }
}

fun hasScheduleExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        if (alarmManager?.canScheduleExactAlarms() == false) {
            return false
        }
    }
    return true
}
