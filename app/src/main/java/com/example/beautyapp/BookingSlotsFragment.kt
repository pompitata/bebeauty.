package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BookingSlotsFragment : Fragment(R.layout.fragment_booking_slots) {

    private val backendClient = BackendClient()
    private var selectedSlot: MasterAvailabilitySlot? = null
    private var masterUsername: String = "мастер"
    private val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backButton = view.findViewById<ImageButton>(R.id.btnBookingSlotsBack)
        val titleView = view.findViewById<TextView>(R.id.tvBookingSlotsTitle)
        val statusView = view.findViewById<TextView>(R.id.tvBookingSlotsStatus)
        val emptyView = view.findViewById<TextView>(R.id.tvBookingSlotsEmpty)
        val slotsList = view.findViewById<RecyclerView>(R.id.rvBookingSlots)
        val submitButton = view.findViewById<Button>(R.id.btnSubmitBookingSlot)

        val masterUserId = arguments?.getInt("masterUserId", -1) ?: -1
        val serviceId = arguments?.getInt("serviceId", -1) ?: -1
        val serviceName = arguments?.getString("serviceName").orEmpty()
        val rescheduleBookingId = arguments?.getInt("bookingId", -1) ?: -1
        val token = SessionManager(requireContext()).getAccessToken()
        val isReschedule = rescheduleBookingId > 0

        titleView.text = if (isReschedule) {
            "Перенос: $serviceName"
        } else {
            "Выберите время: $serviceName"
        }

        var adapterRef: BookingSlotsAdapter? = null
        val adapter = BookingSlotsAdapter { slot ->
            selectedSlot = slot
            adapterRef?.setSelectedSlot(slot.id)
            submitButton.isEnabled = true
        }
        adapterRef = adapter

        slotsList.layoutManager = LinearLayoutManager(requireContext())
        slotsList.adapter = adapter

        backButton.setOnClickListener { findNavController().navigateUp() }
        submitButton.text = if (isReschedule) "Перенести" else "Записаться"
        submitButton.isEnabled = false
        submitButton.setOnClickListener {
            val slot = selectedSlot ?: return@setOnClickListener
            if (token.isNullOrBlank()) return@setOnClickListener

            submitButton.isEnabled = false
            statusView.text = if (isReschedule) "Перенос записи..." else "Создание записи..."

            Thread {
                runCatching {
                    if (isReschedule) {
                        backendClient.rescheduleBooking(token, rescheduleBookingId, slot.id)
                    } else {
                        backendClient.createBooking(token, serviceId, slot.id)
                    }
                }.onSuccess {
                    requireActivity().runOnUiThread {
                        statusView.text = ""
                        if (isReschedule) {
                            findNavController().navigate(R.id.bookingsFragment)
                        } else {
                            val dateTime = runCatching {
                                LocalDateTime.parse(slot.availableAt, apiFormatter).format(displayFormatter)
                            }.getOrElse { slot.availableAt }
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Запись создана")
                                .setMessage(
                                    "Вы успешно записаны $dateTime к мастеру $masterUsername на услугу $serviceName",
                                )
                                .setCancelable(false)
                                .setPositiveButton("OK") { _, _ ->
                                    findNavController().navigate(R.id.homeFragment)
                                }
                                .show()
                        }
                    }
                }.onFailure { error ->
                    requireActivity().runOnUiThread {
                        submitButton.isEnabled = true
                        statusView.text = error.message ?: "Не удалось сохранить запись"
                    }
                }
            }.start()
        }

        if (masterUserId <= 0 || token.isNullOrBlank() || (!isReschedule && serviceId <= 0)) {
            statusView.text = "Не удалось загрузить окна мастера"
            return
        }

        statusView.text = "Загрузка..."
        Thread {
            runCatching {
                val user = backendClient.getUserById(token, masterUserId)
                val slots = backendClient.getMasterAvailabilitySlotsByUser(token, masterUserId)
                Pair(user.username, slots)
            }.onSuccess { (username, slots) ->
                requireActivity().runOnUiThread {
                    masterUsername = username
                    statusView.text = ""
                    adapter.submitList(slots)
                    emptyView.visibility = if (slots.isEmpty()) View.VISIBLE else View.GONE
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    statusView.text = error.message ?: "Не удалось загрузить окна мастера"
                }
            }
        }.start()
    }
}
