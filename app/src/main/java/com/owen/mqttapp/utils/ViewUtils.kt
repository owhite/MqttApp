package com.owen.mqttapp.utils

import android.view.View
import java.text.DecimalFormat

fun View.setVisible() {
    this.visibility = View.VISIBLE
}
fun getCurrentMillis(): Long {
    return System.currentTimeMillis()
}
fun getSpeed(speed: Double): Double {
    return if (speed < 0) {
        0.0
    } else {
        speed
    }
}
// Create a DecimalFormat object with two decimal places
val decimalFormat = DecimalFormat("#.##")

