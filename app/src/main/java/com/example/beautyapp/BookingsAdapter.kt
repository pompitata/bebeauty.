package com.example.beautyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BookingsAdapter(
    private val isMasterView: Boolean,
    private val onPrimaryAction: (Booking) -> Unit,
    private val onSecondaryAction: (Booking) -> Unit,
) : RecyclerView.Adapter<BookingsAdapter.ViewHolder>() {

    private val items = mutableListOf<Booking>()
    private var expandedBookingId: Int? = null
    private var showActions: Boolean = true
    private val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun submitList(bookings: List<Booking>) {
        items.clear()
        items.addAll(bookings)
        if (expandedBookingId != null && items.none { it.id == expandedBookingId }) {
            expandedBookingId = null
        }
        notifyDataSetChanged()
    }

    fun setShowActions(value: Boolean) {
        showActions = value
        if (!showActions) {
            expandedBookingId = null
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            item = items[position],
            isExpanded = showActions && items[position].id == expandedBookingId,
            onExpandToggle = {
                expandedBookingId = if (expandedBookingId == items[position].id) null else items[position].id
                notifyDataSetChanged()
            },
            isMasterView = isMasterView,
            showActions = showActions,
            onPrimaryAction = onPrimaryAction,
            onSecondaryAction = onSecondaryAction,
            apiFormatter = apiFormatter,
            displayFormatter = displayFormatter,
        )
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.cardBooking)
        private val dateTimeView = itemView.findViewById<TextView>(R.id.tvBookingDateTime)
        private val serviceView = itemView.findViewById<TextView>(R.id.tvBookingService)
        private val counterpartyView = itemView.findViewById<TextView>(R.id.tvBookingMaster)
        private val actions = itemView.findViewById<View>(R.id.layoutBookingActions)
        private val primaryButton = itemView.findViewById<Button>(R.id.btnBookingReschedule)
        private val secondaryButton = itemView.findViewById<Button>(R.id.btnBookingCancel)

        fun bind(
            item: Booking,
            isExpanded: Boolean,
            onExpandToggle: () -> Unit,
            isMasterView: Boolean,
            showActions: Boolean,
            onPrimaryAction: (Booking) -> Unit,
            onSecondaryAction: (Booking) -> Unit,
            apiFormatter: DateTimeFormatter,
            displayFormatter: DateTimeFormatter,
        ) {
            val parsed = runCatching { LocalDateTime.parse(item.scheduledAt, apiFormatter) }.getOrNull()
            dateTimeView.text = parsed?.format(displayFormatter) ?: item.scheduledAt
            serviceView.text = item.serviceName
            counterpartyView.text = if (isMasterView) {
                "Клиент: ${item.clientUsername}"
            } else {
                "Мастер: ${item.masterUsername}"
            }

            actions.visibility = if (showActions && isExpanded) View.VISIBLE else View.GONE
            card.strokeWidth = if (isExpanded) 2 else 1
            primaryButton.text = if (isMasterView) "Показать номер" else "Перенести"
            secondaryButton.visibility = if (isMasterView) View.GONE else View.VISIBLE
            secondaryButton.text = "Отменить"
            secondaryButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                0xFFD27D56.toInt(),
            )

            itemView.setOnClickListener {
                if (showActions) onExpandToggle()
            }
            primaryButton.setOnClickListener { onPrimaryAction(item) }
            secondaryButton.setOnClickListener {
                if (!isMasterView) onSecondaryAction(item)
            }
        }
    }
}
