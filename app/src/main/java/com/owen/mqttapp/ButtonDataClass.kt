package com.owen.mqttapp

data class Button(
    val number: Int,
    val type: String,
    val state: Boolean,
    val visible: Boolean,
    val textColorOn: String,
    val colorOn: Int,
    val textOn: String,
    val emitOn: String,
    val textColorOff: String,
    val colorOff: Int,
    val textOff: String,
    val emitOff: String
)
