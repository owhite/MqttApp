package com.owen.mqttapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.owen.mqttapp.databinding.ActivitySecondBinding
import com.owen.mqttapp.preferences.Preference
import com.owen.mqttapp.utils.ButtonAdapter
import com.owen.mqttapp.utils.DataSet
import com.owen.mqttapp.utils.LedData
import com.owen.mqttapp.utils.MQTTClient
import com.owen.mqttapp.utils.MessageCallback
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SecondActivity : AppCompatActivity(), MessageCallback,
    ButtonClickInterface {
    private lateinit var binding: ActivitySecondBinding
    private lateinit var preferences: Preference
    private val mqttClient by lazy {
        MQTTClient(this, preferences)
    }
    private lateinit var buttonAdapter: ButtonAdapter
    private lateinit var ledAdapter: LedAdapter
    private val gson = Gson()
    private lateinit var buttons: List<ButtonData>
    private lateinit var leds: List<LedData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = Preference(this)

        mqttClient.setMessageCallback(this)

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

    override fun onMessageReceived(message: String) {
        try {
            if (message.startsWith("[")) {
                // Handle JSON Array
                val jsonArray = JSONArray(message)
//                val expectedButtonCount = jsonArray.length()
//                for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(0)
                handleJsonObject(message, jsonObject)
//                }
            } else {
                // Handle JSON Object
                val jsonObject = JSONObject(message)
                handleJsonObject(message, jsonObject)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleJsonObject(message: String, jsonObject: JSONObject) {
        try {
            when (jsonObject.getString("_type")) {
                "banner_set" -> handleBannerSet(jsonObject)
                "button_set" -> handleButtonSet(jsonObject)
                "button_init" -> handleButtonInit(message, jsonObject)
                "button_get" -> handleButtonGet(jsonObject)
                "LED_init" -> handleLedInit(message, jsonObject)
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
            val textColor = parseColor(jsonObject.getString("color"))
            val textGet = jsonObject.getString("text")
            val bannerVisible = jsonObject.getBoolean("visible")
            val backgroundColor = parseColor(jsonObject.getString("background"))


            // Update TextView
            binding.tvBannerMessage.apply {
                if (bannerVisible) {
                    text = textGet
                } else {
                    text = ""
                }
//                text = textGet
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
                setTextColor(textColor)
                setBackgroundColor(backgroundColor)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun parseColor(color: String?): Int {
        var processedColor = color.orEmpty()
        if (!processedColor.startsWith("#")) {
            processedColor = "#$processedColor"
        }
        return Color.parseColor(processedColor)
    }

    private fun handleButtonInit(message: String, jsonObject: JSONObject) {

        try {

            buttons =
                Gson().fromJson(message, object : TypeToken<List<ButtonData>>() {}.type)

            buttonAdapter = ButtonAdapter(this, DataSet(), buttons)
            binding.recyclerViewButtons.apply {
                layoutManager = LinearLayoutManager(this@SecondActivity)
                adapter = buttonAdapter
            }

            // Pass the button object to the adapter to display it
//            buttonAdapter.addButton(button)
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }

    private fun handleButtonSet(jsonObject: JSONObject) {
        try {
            val buttonNumber = jsonObject.getInt("number")
            val buttonState = jsonObject.getBoolean("state")

            val dataToBeSet = DataSet((jsonObject.getString("_type")), buttonNumber, buttonState)
            buttonAdapter = ButtonAdapter(this, dataToBeSet, buttons)
            binding.recyclerViewButtons.apply {
                layoutManager = LinearLayoutManager(this@SecondActivity)
                adapter = buttonAdapter
            }
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

    private fun handleLedInit(message: String, jsonObject: JSONObject) {
        try {
            leds =
                Gson().fromJson(message, object : TypeToken<List<LedData>>() {}.type)

            ledAdapter = LedAdapter(leds, DataSet())
            binding.recyclerViewLed.apply {
                layoutManager = LinearLayoutManager(this@SecondActivity)
                adapter = ledAdapter
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            // Handle JSON parsing exception
        }
    }


    private fun handleLedSet(jsonObject: JSONObject) {
        try {
            val buttonNumber = jsonObject.getInt("number")
            val buttonState = jsonObject.getBoolean("state")

            val dataToBeSet = DataSet((jsonObject.getString("_type")), buttonNumber, buttonState)
            ledAdapter = LedAdapter(leds, dataToBeSet)
            binding.recyclerViewLed.apply {
                layoutManager = LinearLayoutManager(this@SecondActivity)
                adapter = ledAdapter
            }
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