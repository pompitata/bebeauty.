package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClientMastersAdapter : RecyclerView.Adapter<ClientMastersAdapter.ViewHolder>() {

    private val items = mutableListOf<UserProfile>()

    fun submitList(masters: List<UserProfile>) {
        items.clear()
        items.addAll(masters)
        notifyDataSetChanged()
    }

    var onMasterClick: ((UserProfile) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_master, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onMasterClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameView = itemView.findViewById<TextView>(R.id.tvClientMasterUsername)
        private val categoriesView = itemView.findViewById<TextView>(R.id.tvClientMasterCategories)
        private val phoneView = itemView.findViewById<TextView>(R.id.tvClientMasterPhone)

        fun bind(master: UserProfile) {
            val profile = master.masterProfile
            val categories = profile?.serviceCategories?.ifEmpty {
                profile.serviceCategory?.let(::listOf) ?: emptyList()
            } ?: emptyList()
            usernameView.text = master.username
            categoriesView.text = if (categories.isEmpty()) {
                "Категории не указаны"
            } else {
                categories.joinToString(", ") { it.replaceFirstChar { c -> c.titlecase() } }
            }
            phoneView.text = master.phone ?: "Телефон не указан"
        }
    }
}
