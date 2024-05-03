package com.owen.mqttapp

import android.graphics.Color
import android.graphics.Color.parseColor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.owen.mqttapp.databinding.ActivitySecondBinding
import com.owen.mqttapp.preferences.Preference
import com.owen.mqttapp.utils.ButtonAdapter
import com.owen.mqttapp.utils.DataSet
import com.owen.mqttapp.utils.MQTTClient
import com.owen.mqttapp.utils.MessageCallback
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SecondActivity : AppCompatActivity(), MessageCallback,
    ButtonClickInterface {
    private lateinit var binding: ActivitySecondBinding
    private val handler = android.os.Handler()
    private var toggleCount = 0
    private var lightMode = true
    private lateinit var preferences: Preference
    private val mqttClient by lazy {
        MQTTClient(this, preferences)
    }
    private lateinit var ledList: MutableList<Led>
    private lateinit var buttonAdapter: ButtonAdapter
    private lateinit var ledAdapter: LedAdapter
//    private val buttonAdapter by lazy {
//        ButtonAdapter()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = Preference(this)

        mqttClient.setMessageCallback(this)
        buttonAdapter = ButtonAdapter(this,DataSet())
        ledList = mutableListOf()
        ledAdapter = LedAdapter(ledList)
        binding.recyclerViewButtons.apply {
            layoutManager = LinearLayoutManager(this@SecondActivity)
            adapter = buttonAdapter
        }
        binding.recyclerViewLed.apply {
            layoutManager = LinearLayoutManager(this@SecondActivity)
            adapter = ledAdapter
        }
        preferences.setMqttConnected(true)
        binding.btnSend.setOnClickListener {
            mqttClient.publish(
                preferences.getMqttTopic().toString(),
                binding.etSendText.text.toString()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mqttClient.connect()
    }

    /*
        private fun toggleColor(imageView: ImageView) {
            val drawable = imageView.background as GradientDrawable
            toggleCount = 0 // Reset toggle count
            lightMode = !lightMode // Toggle lightMode
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (toggleCount < 6) { // Total toggle count should be 6 for 3 times switching
                        if (lightMode) {
                            setLightColor(imageView, drawable)
                        } else {
                            setDarkColor(imageView, drawable)
                        }
                        lightMode = !lightMode // Toggle lightMode
                        toggleCount++
                        handler.postDelayed(this, 1000) // Change color every second
                    } else {
                        // Ensure it stops at dark mode after three switches
                        if (!lightMode) {
                            setDarkColor(imageView, drawable)
                        }
                    }
                }
            }, 0)
        }

        private fun setLightColor(imageView: ImageView, drawable: GradientDrawable) {
            when (imageView.id) {
                R.id.ivLogColor -> drawable.setColor(Color.parseColor("#90EE90")) // Light green
                R.id.ivStopColor -> drawable.setColor(Color.parseColor("#FFC0CB")) // Light pink
                R.id.ivUploadColor -> drawable.setColor(Color.parseColor("#FFD700")) // Light yellow
                R.id.ivCloseColor -> drawable.setColor(Color.parseColor("#FFA500")) // Light orange
            }
            imageView.background = drawable
        }

        private fun setDarkColor(imageView: ImageView, drawable: GradientDrawable) {
            when (imageView.id) {
                R.id.ivLogColor -> drawable.setColor(Color.parseColor("#008000")) // Dark green
                R.id.ivStopColor -> drawable.setColor(Color.parseColor("#8B0000")) // Dark red
                R.id.ivUploadColor -> drawable.setColor(Color.parseColor("#FFA500")) // Dark orange
                R.id.ivCloseColor -> drawable.setColor(Color.parseColor("#FF8C00")) // Dark orange
            }
            imageView.background = drawable
        }*/

    /* override fun onMessageReceived(message: String) {
         try {
             val jsonObject = JSONObject(message)
             val type = jsonObject.getString("_type")

             if (type == "banner_set") {
                 var textSize = jsonObject.getInt("size")
                 var textColor = Color.parseColor("#" + jsonObject.getString("color"))
                 var textGet = jsonObject.getString("text")
                 var backgroundColor = Color.parseColor("#" + jsonObject.getString("background"))

                 // Update TextView
                 binding.tvBannerMessage.apply {
                     text = textGet
                     textSize = textSize.toFloat().toInt()
                     setTextColor(textColor)
                     setBackgroundColor(backgroundColor)
                 }
             }
         } catch (e: JSONException) {
             e.printStackTrace()
             // Handle JSON parsing exception
         }
     }*/
    override fun onMessageReceived(message: String) {
        try {
            if (message.startsWith("[")) {
                // Handle JSON Array
                val jsonArray = JSONArray(message)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    handleJsonObject(jsonObject)
                }
            } else {
                // Handle JSON Object
                val jsonObject = JSONObject(message)
                handleJsonObject(jsonObject)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleJsonObject(jsonObject: JSONObject) {
        try {
            when (jsonObject.getString("_type")) {
                "banner_set" -> handleBannerSet(jsonObject)
                "button_set" -> handleButtonSet(jsonObject)
                "button_init" -> handleButtonInit(jsonObject)
                "button_get" -> handleButtonGet(jsonObject)
                "LED_init" -> handleLedInit(jsonObject)
                "LED_set" -> handleLedSet(jsonObject)
                "LED_get" -> handleLedGet(jsonObject)
                // Add more cases if needed
                else -> {
                    // Handle unknown type
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleBannerSet(jsonObject: JSONObject) {
        try {
            var textSize = jsonObject.getInt("size")
            val textColor = Color.parseColor("#" + jsonObject.getString("color"))
            val textGet = jsonObject.getString("text")
            val backgroundColor = Color.parseColor("#" + jsonObject.getString("background"))

            // Update TextView
            binding.tvBannerMessage.apply {
                text = textGet
                textSize = textSize.toFloat().toInt()
                setTextColor(textColor)
                setBackgroundColor(backgroundColor)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleButtonSet(jsonObject: JSONObject) {

        try {
            val buttonNumber = jsonObject.getInt("number")
            val buttonState = jsonObject.getBoolean("state")

            val datatobeset = DataSet((jsonObject.getString("_type")),buttonNumber,buttonState)

        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleButtonInit(jsonObject: JSONObject) {
        try {
            val buttonNumber = jsonObject.getInt("number")
            val type = (jsonObject.getString("_type"))
            val buttonState = jsonObject.getBoolean("state")
            val buttonVisible = jsonObject.getBoolean("visible")
            val buttonTextColorOn = jsonObject.getString("text_color_on")
            val buttonColorOn = parseColor(jsonObject.getString("color_on"))
            val buttonTextOn = jsonObject.getString("text_on")
            val buttonEmitOn = jsonObject.getString("emit_on")
            val buttonTextColorOff = jsonObject.getString("text_color_off")
            val buttonColorOff = parseColor(jsonObject.getString("color_off"))
            val buttonTextOff = jsonObject.getString("text_off")
            val buttonEmitOff = jsonObject.getString("emit_off")

            // Create a Button object with the retrieved data
            val button = Button(
                buttonNumber,
                type,
                buttonState,
                buttonVisible,
                buttonTextColorOn,
                buttonColorOn,
                buttonTextOn,
                buttonEmitOn,
                buttonTextColorOff,
                buttonColorOff,
                buttonTextOff,
                buttonEmitOff
            )

            // Pass the button object to the adapter to display it
            buttonAdapter.addButton(button)
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleButtonGet(jsonObject: JSONObject) {
        try {
            val buttonNumber = jsonObject.getInt("number")
            val buttonState = jsonObject.getBoolean("state")

            // Handle button state update
            // Example: updateButtonState(buttonNumber, buttonState)
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleLedInit(jsonObject: JSONObject) {
        try {
            val number = jsonObject.getInt("number")
            val state = jsonObject.optBoolean("state", false)
            val visible = jsonObject.optBoolean("visible", true)
            val colorOn = jsonObject.optString("color_on", "#FFFFFF")
            val colorOff = jsonObject.optString("color_off", "#000000")

            if (visible) {
                val led = Led(number, state, visible, colorOn, colorOff)
                ledAdapter.addLed(led)
            }
//            val led = Led(number, state, visible, colorOn, colorOff)
//            ledAdapter.addLed(led)
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }


    private fun handleLedSet(jsonObject: JSONObject) {
        try {
            val buttonNumber = jsonObject.getInt("number")
            val buttonState = jsonObject.getBoolean("state")

            // Handle button state update
            // Example: updateButtonState(buttonNumber, buttonState)
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleLedGet(jsonObject: JSONObject) {
        try {
            val buttonNumber = jsonObject.getInt("number")
            val buttonState = jsonObject.getBoolean("state")

            // Handle button state update
            // Example: updateButtonState(buttonNumber, buttonState)
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    override fun onButtonClick(position: Int, message: String) {
        mqttClient.publish(preferences.getMqttTopic().toString(), message)
    }


}