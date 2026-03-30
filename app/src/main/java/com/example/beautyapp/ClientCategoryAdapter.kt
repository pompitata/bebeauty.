package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClientCategoryAdapter(
    private val items: List<String>,
    private val onCategoryClick: (String) -> Unit,
) : RecyclerView.Adapter<ClientCategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_category, parent, false)
        return ViewHolder(view, onCategoryClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        itemView: View,
        private val onCategoryClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleView = itemView.findViewById<TextView>(R.id.tvClientCategoryTitle)
        private val bgView = itemView.findViewById<ImageView>(R.id.ivClientCategoryBg)

        fun bind(category: String) {
            val normalized = category.lowercase()
            titleView.text = normalized.replaceFirstChar { it.titlecase() }
            val imageRes = when (normalized) {
                "ногти" -> R.drawable.nails
                "брови" -> R.drawable.brows
                "ресницы" -> R.drawable.lashes
                "волосы" -> R.drawable.hair
                else -> R.drawable.nails
            }
            bgView.setImageResource(imageRes)
            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }
}
