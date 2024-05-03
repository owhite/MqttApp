package com.owen.mqttapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class LedAdapter(private val ledList: MutableList<Led>) :
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

        // Preprocess color strings to ensure they're in the correct format
        val colorOn = preprocessColor(led.colorOn)
        val colorOff = preprocessColor(led.colorOff)

        // Set LED color based on its state

        val color = if (led.state) colorOn else colorOff
//        val parsedColor = Color.parseColor(preprocessColor(color))
//        holder.ivLedColor.setColorFilter(parsedColor)
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
        }

        // Set the circular background drawable as the background of the CircleImageView
        holder.ivLedColor.background = backgroundDrawable
        holder.ivLedColor.visibility = if (led.visible) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount(): Int {
        return ledList.size
    }

    fun addLed(led: Led) {
        ledList.add(led)
        notifyDataSetChanged()
    }

 /*   private fun preprocessColor(color: String): String {
        return if (color.length == 6) "#$color" else "#$color"
    }*/
    private fun preprocessColor(color: String): String {
        return if (color.length == 6 && !color.startsWith("#")) "#$color" else color
    }

}
