package com.owen.mqttapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import com.owen.mqttapp.databinding.ActivityMainBinding
import com.owen.mqttapp.preferences.Preference
import com.owen.mqttapp.utils.MQTTClient
import com.owen.mqttapp.utils.MessageCallback
import com.owen.mqttapp.utils.startLocationService
import com.owen.mqttapp.utils.stopLocationService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: Preference
    private val mqttClient by lazy {
        MQTTClient(this, preferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = Preference(this)


        initializeSpinners()



        if (preferences.isDetailsSaved() == true) {
            binding.etServer.setText(preferences.getMqttIp().toString())
            binding.etPort.setText(preferences.getMqttPort().toString())
            binding.etUsername.setText(preferences.getMqttName().toString())
            binding.etPassword.setText(preferences.getPassword().toString())
            binding.etTopic.setText(preferences.getMqtTopic().toString())
        }

        binding.btnSubscribe.setOnClickListener {
            val server = binding.etServer.text.toString()
            val port = binding.etPort.text.toString()
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()

            val topic = binding.etTopic.text.toString()
            if (server.isEmpty() || port.isEmpty() || username.isEmpty() || password.isEmpty() || topic.isEmpty()) {
                // Show alert that the fields are empty
                Toast.makeText(this@MainActivity, "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener // Exit the click listener
            }
            preferences.setMqttTopic(topic)

            val brokerUrl = "tcp://$server:$port"


            preferences.setMqttUsername(username)
            preferences.setMqttPassword(password)
            preferences.setMqttBrokerUrl(brokerUrl)
            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Connecting to MQTT...")
            progressDialog.setCancelable(false)
            progressDialog.show()
            GlobalScope.launch {
                mqttClient.setupMqttClient()
                delay(5000)
                progressDialog.dismiss()
                runOnUiThread {
                    if (mqttClient.isConnected()) {
                        changeView()
                        mqttClient.subscribe(topic) { success ->
                            if (success) {
                                preferences.setMqttConnected(true)
                                preferences.setIsLoggedIn(true)
                                startLocationService(this@MainActivity)
                                val intent = Intent(this@MainActivity, SecondActivity::class.java)
                                finishAffinity()
                                startActivity(intent)
                            } else {

                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to Connect", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
        binding.btnSave.setOnClickListener {
            if (binding.etServer.text.toString()
                    .isEmpty() || binding.etPort.text.toString()
                    .isEmpty() || binding.etUsername.text.toString()
                    .isEmpty() || binding.etPassword.text.toString()
                    .isEmpty() || binding.etTopic.text.toString().isEmpty()
            ) {
                Toast.makeText(this@MainActivity, "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener // Exit the click listener
            }
            preferences.setMqttDetails(
                binding.etServer.text.toString(),
                binding.etPort.text.toString(),
                binding.etUsername.text.toString(),
                binding.etPassword.text.toString(),
                binding.etTopic.text.toString()
            )
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            preferences.setDetailsSaved(true)

        }


        binding.btnDisconnect.setOnClickListener {
            mqttClient.disconnectClient() { disconnected ->
                if (disconnected) {
                    stopLocationService(this)
                    preferences.setIsLoggedIn(false)
                    preferences.setMqttConnected(false)
                    binding.btnDisconnect.visibility = View.GONE
                    binding.btnSubscribe.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (preferences.isMqttConnected()) {
            binding.btnSubscribe.visibility = View.GONE
            binding.btnDisconnect.visibility = View.VISIBLE
        } else {
            binding.btnSubscribe.visibility = View.VISIBLE
            binding.btnDisconnect.visibility = View.GONE
        }
    }


    private fun initializeSpinners() {
        // Spinner for update interval
        val updateIntervalSpinner: Spinner = findViewById(R.id.updateIntervalSpinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.update_intervals,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            updateIntervalSpinner.adapter = adapter
        }
        updateIntervalSpinner.setSelection(getUpdateIntervalIndex(preferences.getUpdateInterval()))
        updateIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val intervals = resources.getStringArray(R.array.update_intervals)
                val intervalValue = when (position) {
                    0 -> 15000L
                    1 -> 30000L
                    2 -> 60000L
                    else -> 15000L
                }
                preferences.setUpdateInterval(intervalValue)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Spinner for smallest displacement
        val smallestDisplacementSpinner: Spinner = findViewById(R.id.smallestDisplacementSpinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.smallest_displacements,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            smallestDisplacementSpinner.adapter = adapter
        }
        smallestDisplacementSpinner.setSelection(getSmallestDisplacementIndex(preferences.getSmallestDisplacement()))
        smallestDisplacementSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val displacements = resources.getStringArray(R.array.smallest_displacements)
                val displacementValue = when (position) {
                    0 -> 2F
                    1 -> 5F
                    2 -> 10F
                    3 -> 25F
                    4 -> 50F
                    else -> 5F
                }
                preferences.setSmallestDisplacement(displacementValue)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getUpdateIntervalIndex(interval: Long): Int {
        return when (interval) {
            15000L -> 0
            30000L -> 1
            60000L -> 2
            else -> 0
        }
    }

    private fun getSmallestDisplacementIndex(displacement: Float): Int {
        return when (displacement) {
            2F -> 0
            5F -> 1
            10F -> 2
            25F -> 3
            50F -> 4
            else -> 1
        }
    }
    private fun changeView() {
        binding.btnSubscribe.visibility = View.GONE
        binding.btnDisconnect.visibility = View.VISIBLE
    }
}

//0a80df440665e9e1