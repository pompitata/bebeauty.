package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleAdapter(
    private val onSlotClick: (MasterAvailabilitySlot) -> Unit,
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterAvailabilitySlot>()
    private val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun submitList(slots: List<MasterAvailabilitySlot>) {
        items.clear()
        items.addAll(slots)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_slot, parent, false)
        return ViewHolder(view, onSlotClick, inputFormatter, dateFormatter, timeFormatter)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        itemView: View,
        private val onSlotClick: (MasterAvailabilitySlot) -> Unit,
        private val inputFormatter: DateTimeFormatter,
        private val dateFormatter: DateTimeFormatter,
        private val timeFormatter: DateTimeFormatter,
    ) : RecyclerView.ViewHolder(itemView) {
        private val dateText = itemView.findViewById<TextView>(R.id.tvSlotDate)
        private val timeText = itemView.findViewById<TextView>(R.id.tvSlotTime)

        fun bind(item: MasterAvailabilitySlot) {
            val dateTime = LocalDateTime.parse(item.availableAt, inputFormatter)
            dateText.text = dateTime.format(dateFormatter)
            timeText.text = dateTime.format(timeFormatter)
            itemView.setOnClickListener { onSlotClick(item) }
        }
    }
}
