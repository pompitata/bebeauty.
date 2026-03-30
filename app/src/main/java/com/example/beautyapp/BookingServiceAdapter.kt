package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class BookingServiceAdapter(
    private val onSelect: (MasterService) -> Unit,
) : RecyclerView.Adapter<BookingServiceAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterService>()
    private var selectedServiceId: Int? = null

    fun submitList(services: List<MasterService>) {
        items.clear()
        items.addAll(services)
        if (selectedServiceId != null && items.none { it.id == selectedServiceId }) {
            selectedServiceId = null
        }
        notifyDataSetChanged()
    }

    fun setSelectedService(serviceId: Int?) {
        selectedServiceId = serviceId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], selectedServiceId, onSelect)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.cardBookingService)
        private val nameView = itemView.findViewById<TextView>(R.id.tvBookingServiceName)
        private val detailsView = itemView.findViewById<TextView>(R.id.tvBookingServiceDetails)

        fun bind(
            item: MasterService,
            selectedServiceId: Int?,
            onSelect: (MasterService) -> Unit,
        ) {
            nameView.text = item.name
            detailsView.text =
                "${item.category.replaceFirstChar { it.titlecase() }} • ${item.durationMinutes} мин • ${item.price} ₽"

            val isSelected = item.id == selectedServiceId
            card.strokeColor = if (isSelected) 0xFFD27D56.toInt() else 0xFFE7D2C8.toInt()
            card.strokeWidth = if (isSelected) 3 else 1

            itemView.setOnClickListener { onSelect(item) }
        }
    }
}
