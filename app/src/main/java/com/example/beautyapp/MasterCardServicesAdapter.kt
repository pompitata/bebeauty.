package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MasterCardServicesAdapter : RecyclerView.Adapter<MasterCardServicesAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterService>()

    fun submitList(services: List<MasterService>) {
        items.clear()
        items.addAll(services)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_master_card_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView = itemView.findViewById<TextView>(R.id.tvMasterCardServiceName)
        private val detailsView = itemView.findViewById<TextView>(R.id.tvMasterCardServiceDetails)

        fun bind(service: MasterService) {
            nameView.text = service.name
            detailsView.text = "${service.category.replaceFirstChar { it.titlecase() }} • ${service.durationMinutes} мин • ${service.price} ₽"
        }
    }
}
