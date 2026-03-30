package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BookingSlotsAdapter(
    private val onSelect: (MasterAvailabilitySlot) -> Unit,
) : RecyclerView.Adapter<BookingSlotsAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterAvailabilitySlot>()
    private var selectedSlotId: Int? = null
    private val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun submitList(slots: List<MasterAvailabilitySlot>) {
        items.clear()
        items.addAll(slots)
        if (selectedSlotId != null && items.none { it.id == selectedSlotId }) {
            selectedSlotId = null
        }
        notifyDataSetChanged()
    }

    fun setSelectedSlot(slotId: Int?) {
        selectedSlotId = slotId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            item = items[position],
            selectedSlotId = selectedSlotId,
            apiFormatter = apiFormatter,
            displayFormatter = displayFormatter,
            onSelect = onSelect,
        )
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.cardBookingSlot)
        private val dateTimeView = itemView.findViewById<TextView>(R.id.tvBookingSlotDateTime)

        fun bind(
            item: MasterAvailabilitySlot,
            selectedSlotId: Int?,
            apiFormatter: DateTimeFormatter,
            displayFormatter: DateTimeFormatter,
            onSelect: (MasterAvailabilitySlot) -> Unit,
        ) {
            val parsed = runCatching { LocalDateTime.parse(item.availableAt, apiFormatter) }.getOrNull()
            dateTimeView.text = parsed?.format(displayFormatter) ?: item.availableAt

            val isSelected = item.id == selectedSlotId
            card.strokeColor = if (isSelected) 0xFFD27D56.toInt() else 0xFFE7D2C8.toInt()
            card.strokeWidth = if (isSelected) 3 else 1

            itemView.setOnClickListener { onSelect(item) }
        }
    }
}
