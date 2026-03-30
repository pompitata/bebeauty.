package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class MasterCardWorksAdapter : RecyclerView.Adapter<MasterCardWorksAdapter.ViewHolder>() {

    private val items = mutableListOf<MasterWork>()
    var onWorkClick: ((MasterWork) -> Unit)? = null

    fun submitList(works: List<MasterWork>) {
        items.clear()
        items.addAll(works)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_master_card_work, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onWorkClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.ivMasterCardWork)

        fun bind(item: MasterWork, onClick: ((MasterWork) -> Unit)?) {
            itemView.setOnClickListener { onClick?.invoke(item) }
            RemoteImageLoader.loadInto(imageView, item.photoUrl, R.drawable.ic_avatar_placeholder)
        }
    }
}
