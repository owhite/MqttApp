package com.owen.mqttapp.utils

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.owen.mqttapp.Button
import com.owen.mqttapp.ButtonClickInterface
import com.owen.mqttapp.R
import com.owen.mqttapp.preferences.Preference

class ButtonAdapter(
    private val mqttClient: ButtonClickInterface, var data: DataSet
) : RecyclerView.Adapter<ButtonAdapter.ButtonViewHolder>() {
    private val buttons: MutableList<Button> = mutableListOf()

    fun addButton(button: Button) {
        buttons.add(button)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.buttons, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val button = buttons[position]
        holder.bind(button, data)
    }

    override fun getItemCount(): Int = buttons.size
    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var button = itemView.findViewById<AppCompatButton>(R.id.btn)

        init {
            // Set click listener for the button
            button.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
                val button = buttons[adapterPosition]
                // Publish message to MQTT
                mqttClient.onButtonClick(adapterPosition, buttonMessage)
//                }
            }
        }

        private var buttonMessage: String = ""

        fun bind(button: Button, data: DataSet) {
            // Bind button data to UI elements in the view holder
            val btn = itemView.findViewById<TextView>(R.id.btn)

            // Set text based on button state
            if (button.type == "button_set") {
                Log.i("number", data.toString())

            }
            if (button.state) {
                buttonMessage = """{"_type":"cmd","action":"reportLocation"}"""
                btn.text = button.textOn
                btn.visibility = if (button.visible) View.VISIBLE else View.INVISIBLE
                btn.setTextColor(parseColor(button.textColorOn))
                btn.setBackgroundColor(parseColor(button.colorOn.toString()))
            } else {
                buttonMessage = """{"_type":"button_state","number":1, "state":0}"""
                btn.text = button.textOff
                btn.setTextColor(parseColor(button.textColorOff))
                btn.setBackgroundColor(parseColor(button.colorOff.toString()))
            }
        }

        private fun parseColor(colorString: String): Int {
            return try {
                if (colorString.startsWith("#")) {
                    Color.parseColor(colorString)
                } else {
                    colorString.toIntOrNull() ?: Color.BLACK
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Color.BLACK // Return default color in case of parsing error
            }
        }
    }


}
