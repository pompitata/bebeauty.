package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BookingsFragment : Fragment(R.layout.fragment_bookings) {

    private val backendClient = BackendClient()
    private val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private lateinit var adapter: BookingsAdapter
    private var allBookings: List<Booking> = emptyList()
    private var isArchiveTab: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val statusView = view.findViewById<TextView>(R.id.tvBookingsStatus)
        val emptyView = view.findViewById<TextView>(R.id.tvBookingsEmpty)
        val listView = view.findViewById<RecyclerView>(R.id.rvBookings)
        val upcomingTab = view.findViewById<Button>(R.id.btnBookingsUpcoming)
        val archiveTab = view.findViewById<Button>(R.id.btnBookingsArchive)
        val sessionManager = SessionManager(requireContext())
        val isMasterView = sessionManager.isMaster()

        adapter = BookingsAdapter(
            isMasterView = isMasterView,
            onPrimaryAction = { booking ->
                if (isMasterView) {
                    showClientPhoneDialog(booking)
                } else {
                    findNavController().navigate(
                        R.id.bookingSlotsFragment,
                        bundleOf(
                            "bookingId" to booking.id,
                            "masterUserId" to booking.masterUserId,
                            "serviceName" to booking.serviceName,
                        ),
                    )
                }
            },
            onSecondaryAction = { booking ->
                showCancelDialog(booking) {
                    loadBookings(statusView, emptyView, sessionManager)
                }
            },
        )

        listView.layoutManager = LinearLayoutManager(requireContext())
        listView.adapter = adapter

        upcomingTab.setOnClickListener {
            isArchiveTab = false
            updateTabUi(upcomingTab, archiveTab)
            renderBookings(emptyView)
        }
        archiveTab.setOnClickListener {
            isArchiveTab = true
            updateTabUi(upcomingTab, archiveTab)
            renderBookings(emptyView)
        }
        updateTabUi(upcomingTab, archiveTab)

        loadBookings(statusView, emptyView, sessionManager)
    }

    override fun onResume() {
        super.onResume()
        view?.let { root ->
            val statusView = root.findViewById<TextView>(R.id.tvBookingsStatus)
            val emptyView = root.findViewById<TextView>(R.id.tvBookingsEmpty)
            loadBookings(statusView, emptyView, SessionManager(requireContext()))
        }
    }

    private fun loadBookings(
        statusView: TextView,
        emptyView: TextView,
        sessionManager: SessionManager,
    ) {
        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) {
            statusView.text = "Сначала войдите в аккаунт"
            return
        }

        statusView.text = "Загрузка..."
        Thread {
            runCatching {
                backendClient.getMyBookings(token)
            }.onSuccess { bookings ->
                requireActivity().runOnUiThread {
                    statusView.text = ""
                    allBookings = bookings
                    renderBookings(emptyView)
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    statusView.text = error.message ?: "Не удалось загрузить записи"
                }
            }
        }.start()
    }

    private fun renderBookings(emptyView: TextView) {
        val now = LocalDateTime.now()
        val filtered = allBookings.filter { booking ->
            val dateTime = runCatching {
                LocalDateTime.parse(booking.scheduledAt, apiFormatter)
            }.getOrNull()
            if (dateTime == null) {
                !isArchiveTab
            } else if (isArchiveTab) {
                dateTime.isBefore(now)
            } else {
                !dateTime.isBefore(now)
            }
        }

        adapter.setShowActions(!isArchiveTab)
        adapter.submitList(filtered)
        emptyView.text = if (isArchiveTab) {
            "Пока нет записей в архиве"
        } else {
            "Пока нет предстоящих записей"
        }
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateTabUi(upcomingTab: Button, archiveTab: Button) {
        val activeColor = 0xFFD27D56.toInt()
        val inactiveColor = 0xFFE7D2C8.toInt()
        val activeText = 0xFFFFFFFF.toInt()
        val inactiveText = 0xFF6A4435.toInt()

        upcomingTab.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (!isArchiveTab) activeColor else inactiveColor,
        )
        archiveTab.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isArchiveTab) activeColor else inactiveColor,
        )
        upcomingTab.setTextColor(if (!isArchiveTab) activeText else inactiveText)
        archiveTab.setTextColor(if (isArchiveTab) activeText else inactiveText)
    }

    private fun showCancelDialog(booking: Booking, onCanceled: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Отменить запись?")
            .setMessage("Запись на ${booking.serviceName} будет отменена.")
            .setNegativeButton("Нет", null)
            .setPositiveButton("Да") { _, _ ->
                val token = SessionManager(requireContext()).getAccessToken() ?: return@setPositiveButton
                val statusView = view?.findViewById<TextView>(R.id.tvBookingsStatus) ?: return@setPositiveButton
                statusView.text = "Отмена записи..."
                Thread {
                    runCatching {
                        backendClient.cancelBooking(token, booking.id)
                    }.onSuccess {
                        requireActivity().runOnUiThread { onCanceled() }
                    }.onFailure { error ->
                        requireActivity().runOnUiThread {
                            statusView.text = error.message ?: "Не удалось отменить запись"
                        }
                    }
                }.start()
            }
            .show()
    }

    private fun showClientPhoneDialog(booking: Booking) {
        val phone = booking.clientPhone?.takeIf { it.isNotBlank() }
        val message = if (phone == null) {
            "клиент не добавил номер телефона"
        } else {
            phone
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Номер для связи")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

}
