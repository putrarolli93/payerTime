package com.icaali.prayertime.sdk.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.icaali.prayertime.sdk.R
import com.icaali.prayertime.sdk.databinding.ItemPrayerTimeBinding
import com.icaali.prayertime.sdk.model.PrayerWithNotif
import com.icaali.prayertime.sdk.utils.capitalizeFirst

class PrayerTimeAdapter(private val onAlarmToggle: (PrayerWithNotif) -> Unit) : RecyclerView.Adapter<PrayerTimeAdapter.PrayerTimeViewHolder>() {

    private val items = mutableListOf<PrayerWithNotif>()

    fun setData(prayerTime: List<PrayerWithNotif>) {
        items.clear()
        items.addAll(
            prayerTime
        )
        notifyDataSetChanged()
    }

    inner class PrayerTimeViewHolder(val binding: ItemPrayerTimeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerTimeViewHolder {
        val binding = ItemPrayerTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PrayerTimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrayerTimeViewHolder, position: Int) {
        val (name, time, enable) = items[position]
        if (name == "fajr")
            holder.binding.tvPrayerName.text = "shubuh".capitalizeFirst()
        else
            holder.binding.tvPrayerName.text = name.capitalizeFirst()

        holder.binding.tvPrayerTime.text = time

        holder.binding.imgAlarm.setOnClickListener {
            val updatedItem = items[position].copy(isNotifEnabled = !enable)
            onAlarmToggle.invoke(updatedItem)
        }

        holder.binding.imgAlarm.setImageDrawable(
            if (enable)  holder.binding.root.context.getDrawable(R.drawable.ic_notif_on)
            else holder.binding.root.context.getDrawable(R.drawable.ic_notif_sleep)
        )
    }

    override fun getItemCount(): Int = items.size
}

