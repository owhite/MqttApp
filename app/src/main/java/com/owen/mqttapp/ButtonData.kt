package com.owen.mqttapp

import com.google.gson.annotations.SerializedName


data class ButtonData (

  @SerializedName("_type"          ) var Type         : String?  = null,
  @SerializedName("number"         ) var number       : Int?     = null,
  @SerializedName("visible"        ) var visible      : Boolean? = null,
  @SerializedName("text_color_on"  ) var textColorOn  : String?  = null,
  @SerializedName("color_on"       ) var colorOn      : String?  = null,
  @SerializedName("text_on"        ) var textOn       : String?  = null,
  @SerializedName("emit_on"        ) var emitOn       : String?  = null,
  @SerializedName("text_color_off" ) var textColorOff : String?  = null,
  @SerializedName("color_off"      ) var colorOff     : String?  = null,
  @SerializedName("text_off"       ) var textOff      : String?  = null,
  @SerializedName("emit_off"       ) var emitOff      : String?  = null,
  @SerializedName("state"          ) var state        : Boolean? = null

)