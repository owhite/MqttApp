package com.owen.mqttapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.owen.mqttapp.databinding.ActivityMainBinding
import com.owen.mqttapp.preferences.Preference
import com.owen.mqttapp.utils.MQTTClient
import com.owen.mqttapp.utils.MessageCallback
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

                                val intent = Intent(this@MainActivity, SecondActivity::class.java)
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
                    preferences.setMqttConnected(false)
                    binding.btnDisconnect.visibility = View.GONE
                    binding.btnSubscribe.visibility = View.VISIBLE
                }
            }

        }

        /*binding.btnSubscribe.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }*/
    }
    override fun onDestroy() {
        super.onDestroy()
        preferences.setMqttConnected(false)
        mqttClient.disconnectClient {}
    }

    override fun onResume() {
        super.onResume()
//        if (preferences.isDetailsSaved() == false) {
//            binding.etServer.text.clear()
//            binding.etPort.text.clear()
//            binding.etUsername.text.clear()
//            binding.etPassword.text.clear()
//            binding.etTopic.text.clear()
//        }
//
        if (preferences.isMqttConnected()) {
            binding.btnSubscribe.visibility = View.GONE
            binding.btnDisconnect.visibility = View.VISIBLE
        } else {
            binding.btnSubscribe.visibility = View.VISIBLE
            binding.btnDisconnect.visibility = View.GONE
        }
    }


    private fun changeView() {
        binding.btnSubscribe.visibility = View.GONE
        binding.btnDisconnect.visibility = View.VISIBLE
    }
}

//0a80df440665e9e1