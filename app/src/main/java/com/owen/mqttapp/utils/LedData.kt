package com.owen.mqttapp.utils

import com.google.gson.annotations.SerializedName


data class LedData (

  @SerializedName("_type"     ) var Type     : String?  = null,
  @SerializedName("number"    ) var number   : Int?     = null,
  @SerializedName("visible"   ) var visible  : Boolean? = null,
  @SerializedName("color_on"  ) var colorOn  : String?  = null,
  @SerializedName("color_off" ) var colorOff : String?  = null,
  @SerializedName("state"     ) var state    : Boolean? = null

)