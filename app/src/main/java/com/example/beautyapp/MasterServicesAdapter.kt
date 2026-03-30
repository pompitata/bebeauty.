package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MasterServicesAdapter : RecyclerView.Adapter<MasterServicesAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterService>()

    fun submitList(services: List<MasterService>) {
        items.clear()
        items.addAll(services)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_master_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryText = itemView.findViewById<TextView>(R.id.tvServiceCategory)
        private val nameText = itemView.findViewById<TextView>(R.id.tvServiceName)
        private val durationText = itemView.findViewById<TextView>(R.id.tvServiceDuration)
        private val priceText = itemView.findViewById<TextView>(R.id.tvServicePrice)

        fun bind(item: MasterService) {
            categoryText.text = item.category.replaceFirstChar { it.titlecase() }
            nameText.text = item.name
            durationText.text = "${item.durationMinutes} мин"
            priceText.text = "${item.price} ₽"
        }
    }
}
