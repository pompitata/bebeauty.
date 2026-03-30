package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class WorksAdapter(
    private val onWorkClick: (MasterWork) -> Unit,
) : RecyclerView.Adapter<WorksAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterWork>()

    fun submitList(works: List<MasterWork>) {
        items.clear()
        items.addAll(works)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_photo, parent, false)
        return ViewHolder(view, onWorkClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        itemView: View,
        private val onWorkClick: (MasterWork) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.ivWorkPhoto)

        fun bind(item: MasterWork) {
            RemoteImageLoader.loadInto(imageView, item.photoUrl, R.drawable.ic_avatar_placeholder)
            itemView.setOnClickListener { onWorkClick(item) }
        }
    }
}
