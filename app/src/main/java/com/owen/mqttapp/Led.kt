package com.owen.mqttapp

data class Led(
    val number: Int,
    val state: Boolean,
    val visible: Boolean,
    val colorOn: String,
    val colorOff: String
)
