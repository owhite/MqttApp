package com.owen.mqttapp.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MQTTClient(
    context: Context,
    private val preference: com.owen.mqttapp.preferences.Preference
) {
    private var messageCallback: MessageCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private val MAX_RECONNECT_ATTEMPTS = 5
    private var reconnectAttempts = 0

    companion object {
        private const val TAG = "MqttClientHelper"

        //        private const val QOS = 0 // Default QoS level
//
//        // MQTT Broker URL
        var MQTT_BROKER_URL = "tcp://62.10.56.137:1883"
//
//        var MQTT_BROKER_URL = ""

        //
//        // MQTT Client ID
        private val MQTT_CLIENT_ID = MqttAsyncClient.generateClientId()

        //
//        // MQTT Connection options
        var MQTT_USERNAME = "lambda" // If required by the broker
//        var MQTT_USERNAME = "" // If required by the broker

        var MQTT_PASSWORD = """gH6$#fG67aq!""" // If required by the broker
//        var MQTT_PASSWORD = """""" // If required by the broker
//        private const val MQTT_KEEP_ALIVE_INTERVAL = 60 // in seconds
//        private const val MQTT_CONNECTION_TIMEOUT = 60 // in seconds
//        private const val MQTT_CLEAN_SESSION = true
//        private const val MQTT_CONNECTION_RECONNECT = true
//        const val MQTT_USERNAME = "admin"
//        const val MQTT_PASSWORD = "admin"
//        const val MQTT_BROKER_URL = "tcp://mqtt-precisionprd.nerolac.com:"

        //const val ADAFRUIT_MQTT_HOST = "tcp://mqtt-precisionprd.nerolac.com:1883"
        private val RECONNECT_INTERVAL = 5000L // 5 seconds

        const val MQTT_CONNECTION_TIMEOUT = 3
        const val MQTT_KEEP_ALIVE_INTERVAL = 60
        const val MQTT_CLEAN_SESSION = true
        const val MQTT_CONNECTION_RECONNECT = false
        const val QOS = 0
    }

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private var isConnected: Boolean = false

    private var context: Context

    init {
        this.context = context
        setupMqttClient()
    }

    fun setupMqttClient() {
        mqttAndroidClient = MqttAndroidClient(
            context,
            preference.getMqttBrokerUrl().toString(),
            MQTT_CLIENT_ID
        )
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "MQTT connection lost")
                isConnected = false
                // Handle reconnection here if needed
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val msg = message?.toString()
                if (msg != null) {
                    Log.w(TAG, msg)
                }
                if (msg != null) {
                    Log.e("checkingmessage", msg)
                }

                // Pass the received message to the callback
                handler.post {
                    messageCallback?.onMessageReceived(msg ?: "")
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Message delivery complete")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                serverURI?.let {
                    Log.w(TAG, "Connected to $it")
                    isConnected = true

                    if (reconnect) {
                        resubscribeToTopics()
                    }
                }
            }
        })
        connect()
    }

    private fun resubscribeToTopics() {
        // Resubscribe to the topic(s) here
        // For example:
        val topic = preference.getMqttTopic().toString()
        subscribe(topic) { success ->
            if (success) {
                Log.d(TAG, "Successfully re-subscribed to topic: $topic")
            } else {
                Log.e(TAG, "Failed to re-subscribe to topic: $topic")
            }
        }
    }

    private fun scheduleReconnect() {
        // Schedule a reconnect attempt after a delay
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            // Schedule a reconnect attempt after a delay
            handler.postDelayed({
                if (!mqttAndroidClient.isConnected) {
                    Log.d(TAG, "Reconnect")
                    connect() // Attempt reconnection
                }
            }, RECONNECT_INTERVAL)
        }
    }

    fun connect() {
        val mqttConnectOptions = MqttConnectOptions().apply {
            isAutomaticReconnect = MQTT_CONNECTION_RECONNECT
            isCleanSession = MQTT_CLEAN_SESSION
            userName = preference.getMqttUsername()
            password = preference.getMqttPassword()?.toCharArray()
            connectionTimeout = MQTT_CONNECTION_TIMEOUT
            keepAliveInterval = MQTT_KEEP_ALIVE_INTERVAL
        }

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connected to the broker")
                    preference.setMqttConnected(true)

//                    Log.d(TAG, MQTT_CLIENT_ID.toString())

                    // Set up any additional configurations after connection
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.w(TAG, "Failed to connect: ${exception?.message}")
                }
            })
        } catch (ex: MqttException) {
            Log.e(TAG, "Error connecting: ${ex.message}")
        }
    }

    fun publish(topic: String, msg: String) {
        try {
            val message = MqttMessage(msg.toByteArray())

            mqttAndroidClient.publish(
                topic,
                message,
                null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Message published successfully to topic: $topic")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(
                            TAG,
                            "Failed to publish message to topic: $topic, error: ${exception?.message}"
                        )
                    }
                })
        } catch (e: MqttException) {
            Log.e(TAG, "Error publishing to $topic: ${e.message}")
        }
    }

    fun subscribe(topic: String, callback: (Boolean) -> Unit) {
        try {
            mqttAndroidClient.subscribe(topic, QOS, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                    callback(true)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to subscribe to topic: $topic, error: ${exception?.message}")
                    callback(false)
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error subscribing to $topic: ${e.message}")
            callback(false)
        }
    }

    fun setMessageCallback(callback: MessageCallback) {
        this.messageCallback = callback
    }

    fun unsubscribe(topic: String) {
        try {
            mqttAndroidClient.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(
                        TAG,
                        "Failed to unsubscribe from topic: $topic, error: ${exception?.message}"
                    )
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error unsubscribing from $topic: ${e.message}")
        }
    }


    fun disconnectClient(callback: (Boolean) -> Unit) {
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.disconnect()
            callback(true)
        }
    }

    fun isConnected(): Boolean {
//        return mqttAndroidClient.isConnected
        return isConnected
    }

    fun destroy() {
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.unregisterResources()
            mqttAndroidClient.disconnect()
        }
    }
}

interface MessageCallback {
    fun onMessageReceived(message: String)
}
