package com.owen.mqttapp.model

import java.io.Serializable

data class LocationDataMqtt(
    var type: String = "location",
    var data: LocationData
) : Serializable {

}

data class LocationData(

    var deviceId: String = "",
    var bearing: Double? = 0.0,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,
    var speed: Double? = 0.0,
    var timeStamp: String? = ""
) : Serializable {
}