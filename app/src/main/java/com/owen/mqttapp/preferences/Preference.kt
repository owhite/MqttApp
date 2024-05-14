package com.owen.mqttapp.preferences

import android.content.Context
import android.content.SharedPreferences


private const val userName = "userName"
private const val passWord = "passWord"
private const val brokerURL = "brokerURL"
private const val mqttTopic = "mqttTopic"
private const val isMqttConnected = "isMqttConnected"
private const val saved = "saved"
private const val mqttIp = "mqttIp"
private const val mqttPort = "mqttPort"
private const val mqttName = "mqttName"
private const val isConnected = "isConnected"
private const val mqttPassword = "mqttPassword"
private const val mqtTopic = "mqtTopic"


class Preference(private val context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(
            context.packageName + "_preferences_lang",
            Context.MODE_PRIVATE
        )

    fun setMqttDetails(
        ip: String,
        port: String,
        username: String,
        password: String,
        topic: String
    ) {
        preferences.edit()
            .putString("mqttIp", ip)
            .putString("mqttPort", port)
            .putString("mqttName", username)
            .putString("mqttPassword", password)
            .putString("mqtTopic", topic)
            .apply()
    }

    fun getMqttIp(): String? {
        return preferences.getString("mqttIp", "")
    }

    fun getMqttPort(): String? {
        return preferences.getString("mqttPort", "")
    }

    fun getMqttName(): String? {
        return preferences.getString("mqttName", "")
    }

    fun getPassword(): String? {
        return preferences.getString("mqttPassword", "")
    }

    fun getMqtTopic(): String? {
        return preferences.getString("mqttTopic", "")
    }


    fun setMqttConnected(connected: Boolean) {
        preferences.edit()
            .putBoolean(isConnected, connected)
            .apply()
    }

    fun isMqttConnected(): Boolean {
        return preferences.getBoolean(isConnected, false)
    }

    fun setMqttUsername(username: String) {
        preferences.edit()
            .putString(userName, username)
            .apply()
    }

    fun getMqttUsername(): String? {
        return preferences.getString(userName, "")
    }


    fun setDetailsSaved(isSaved: Boolean) {
        preferences.edit()
            .putBoolean(saved, isSaved)
            .apply()
    }

    fun isDetailsSaved(): Boolean? {
        return preferences.getBoolean(saved, false)
    }

    fun setMqttTopic(topic: String) {
        preferences.edit()
            .putString(mqttTopic, topic)
            .apply()
    }

    fun getMqttTopic(): String? {
        return preferences.getString(mqttTopic, "")
    }

    fun setMqttPassword(password: String) {
        preferences.edit()
            .putString(passWord, password)
            .apply()
    }

    fun getMqttPassword(): String? {
        return preferences.getString(passWord, "")
    }

    fun setMqttBrokerUrl(brokerUrl: String) {
        preferences.edit()
            .putString(brokerURL, brokerUrl)
            .apply()
    }

    fun getMqttBrokerUrl(): String? {
        return preferences.getString(brokerURL, "")
    }


}