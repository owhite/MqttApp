package com.owen.mqttapp.utils

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.owen.mqttapp.ButtonClickInterface
import com.owen.mqttapp.ButtonData
import com.owen.mqttapp.R

class ButtonAdapter(
    private val mqttClient: ButtonClickInterface,
    var data: DataSet,
    var buttonsList: List<ButtonData>
) : RecyclerView.Adapter<ButtonAdapter.ButtonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.buttons, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val button = buttonsList[position]
        holder.bind(button, data)
    }

    override fun getItemCount(): Int = buttonsList.size
    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var button = itemView.findViewById<AppCompatButton>(R.id.btn)

        init {

            button.setOnClickListener {
                val buttonData = buttonsList[adapterPosition]
                val emitMessage = if (buttonData.state == true) {
                    """{"_type":"${buttonData.emitOn}","action":"reportLocation"}"""
                } else {
                    """{"_type":"${buttonData.emitOff}","action":"reportLocation"}"""
                }
                mqttClient.onButtonClick(adapterPosition, emitMessage)
            }
        }


        fun bind(button: ButtonData, data: DataSet) {
            // Bind button data to UI elements in the view holder
            val btn = itemView.findViewById<TextView>(R.id.btn)
            Log.i("buttonData", button.toString())
            // Set text based on button state
            if (data.type == "button_set") {
                if (data.number == button.number) {
                    button.state = data.state
                }
            }

            if (button.state == true) {
                btn.text = button.textOn
                btn.visibility = if (button.visible == true) View.VISIBLE else View.GONE
                btn.setTextColor(parseColor(button.textColorOn))
                btn.setBackgroundColor(parseColor(button.colorOn))

            } else {
                btn.text = button.textOff
                btn.visibility = if (button.visible == true) View.VISIBLE else View.GONE
                btn.setTextColor(parseColor(button.textColorOff))
                btn.setBackgroundColor(parseColor(button.colorOff))
            }
        }
    }

    private fun parseColor(color: String?): Int {
        var processedColor = color.orEmpty()
        if (!processedColor.startsWith("#")) {
            processedColor = "#$processedColor"
        }
        return Color.parseColor(processedColor)
    }

}
