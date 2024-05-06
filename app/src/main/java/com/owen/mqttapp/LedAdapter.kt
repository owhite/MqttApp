package com.owen.mqttapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.owen.mqttapp.utils.DataSet
import com.owen.mqttapp.utils.LedData
import de.hdodenhof.circleimageview.CircleImageView

class LedAdapter(
    private val ledList: List<LedData>,
    var dataSet: DataSet
) :
    RecyclerView.Adapter<LedAdapter.LedViewHolder>() {

    inner class LedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivLedColor: CircleImageView = itemView.findViewById(R.id.ivLedColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.led, parent, false)
        return LedViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LedViewHolder, position: Int) {
        val led = ledList[position]
        val data = dataSet

        if (data.type == "LED_set") {
            if (data.number == led.number) {
                led.state = data.state
            }
        }

        // Preprocess color strings to ensure they're in the correct format
        val colorOn = led.colorOn?.let { preprocessColor(it) }
        val colorOff = led.colorOff?.let { preprocessColor(it) }

        // Set LED color based on its state
        val color = if (led.state == true) {
            colorOn
        } else {
            colorOff
        }
//        val parsedColor = Color.parseColor(preprocessColor(color))
//        holder.ivLedColor.setColorFilter(parsedColor)
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
        }

        // Set the circular background drawable as the background of the CircleImageView
        holder.ivLedColor.background = backgroundDrawable
        holder.ivLedColor.visibility = if (led.visible == true) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return ledList.size
    }

    private fun preprocessColor(color: String): String {
        return if (color.length == 6 && !color.startsWith("#")) {
            "#$color"
        } else {
            color
        }
    }


}
