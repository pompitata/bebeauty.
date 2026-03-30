package com.example.beautyapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {

    private val backendClient = BackendClient()
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private lateinit var adapter: ScheduleAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val addButton = view.findViewById<ImageButton>(R.id.btnAddSlot)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvSchedule)
        val emptyView = view.findViewById<TextView>(R.id.tvScheduleEmpty)
        val statusView = view.findViewById<TextView>(R.id.tvScheduleStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.scheduleProgress)
        val sessionManager = SessionManager(requireContext())

        adapter = ScheduleAdapter(
            onSlotClick = { slot ->
                showSlotDialog(slot) {
                    loadSchedule(progressBar, emptyView, statusView, sessionManager)
                }
            },
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            showSlotDialog(null) {
                loadSchedule(progressBar, emptyView, statusView, sessionManager)
            }
        }

        loadSchedule(progressBar, emptyView, statusView, sessionManager)
    }

    private fun loadSchedule(
        progressBar: ProgressBar,
        emptyView: TextView,
        statusView: TextView,
        sessionManager: SessionManager,
    ) {
        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) {
            statusView.text = "Сначала войдите в аккаунт"
            return
        }

        setLoading(progressBar, true)
        statusView.text = ""

        Thread {
            runCatching {
                backendClient.getMyAvailabilitySlots(token)
            }.onSuccess { slots ->
                requireActivity().runOnUiThread {
                    setLoading(progressBar, false)
                    adapter.submitList(slots)
                    emptyView.visibility = if (slots.isEmpty()) View.VISIBLE else View.GONE
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    setLoading(progressBar, false)
                    emptyView.visibility = View.GONE
                    statusView.text = error.message ?: "Не удалось загрузить график"
                }
            }
        }.start()
    }

    private fun showSlotDialog(
        slot: MasterAvailabilitySlot?,
        onScheduleChanged: () -> Unit,
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_schedule_slot, null)
        val dateTimeInput = dialogView.findViewById<EditText>(R.id.etSlotDateTime)
        var selectedDateTime = slot?.let { LocalDateTime.parse(it.availableAt, apiFormatter) }

        fun syncDateTimeInput() {
            dateTimeInput.setText(selectedDateTime?.format(displayFormatter).orEmpty())
            dateTimeInput.error = null
        }

        fun pickTime(date: LocalDate) {
            val now = LocalDateTime.now()
            val minTimeForToday = now.toLocalTime()
            val initialTime = when {
                selectedDateTime != null && selectedDateTime!!.toLocalDate() == date -> selectedDateTime!!.toLocalTime()
                date == now.toLocalDate() -> minTimeForToday
                else -> LocalTime.of(10, 0)
            }

            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val picked = LocalDateTime.of(date, LocalTime.of(hour, minute))
                    if (date == now.toLocalDate() && picked.isBefore(now)) {
                        dateTimeInput.error = "Нельзя выбрать прошедшее время"
                        return@TimePickerDialog
                    }
                    selectedDateTime = picked
                    syncDateTimeInput()
                },
                initialTime.hour,
                initialTime.minute,
                true,
            ).show()
        }

        fun pickDateTime() {
            val currentDate = selectedDateTime?.toLocalDate() ?: LocalDate.now()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    pickTime(LocalDate.of(year, month + 1, dayOfMonth))
                },
                currentDate.year,
                currentDate.monthValue - 1,
                currentDate.dayOfMonth,
            ).show()
        }

        syncDateTimeInput()
        dateTimeInput.setOnClickListener { pickDateTime() }
        dateTimeInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                pickDateTime()
            }
        }
        dateTimeInput.keyListener = null

        val title = if (slot == null) "Добавить окно" else "Редактировать окно"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .apply {
                if (slot != null) {
                    setNeutralButton("Удалить", null)
                }
            }
            .setPositiveButton("Сохранить", null)
            .show()
            .also { dialog ->
                if (slot != null) {
                    dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        val token = SessionManager(requireContext()).getAccessToken()
                        if (token.isNullOrBlank()) {
                            dialog.dismiss()
                            return@setOnClickListener
                        }

                        dialog.setCancelable(false)
                        toggleDialogButtons(dialog, false)

                        Thread {
                            runCatching {
                                backendClient.deleteMyAvailabilitySlot(token, slot.id)
                            }.onSuccess {
                                requireActivity().runOnUiThread {
                                    dialog.dismiss()
                                    onScheduleChanged()
                                }
                            }.onFailure { error ->
                                requireActivity().runOnUiThread {
                                    dialog.setCancelable(true)
                                    toggleDialogButtons(dialog, true)
                                    dateTimeInput.error = error.message ?: "Не удалось удалить окно"
                                }
                            }
                        }.start()
                    }
                }

                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val dateTime = selectedDateTime
                    if (dateTime == null) {
                        dateTimeInput.error = "Выберите дату и время"
                        return@setOnClickListener
                    }
                    if (dateTime.isBefore(LocalDateTime.now())) {
                        dateTimeInput.error = "Нельзя выбрать прошедшее время"
                        return@setOnClickListener
                    }

                    val token = SessionManager(requireContext()).getAccessToken()
                    if (token.isNullOrBlank()) {
                        dialog.dismiss()
                        return@setOnClickListener
                    }

                    dialog.setCancelable(false)
                    toggleDialogButtons(dialog, false)

                    Thread {
                        runCatching {
                            if (slot == null) {
                                backendClient.createMyAvailabilitySlot(token, dateTime.format(apiFormatter))
                            } else {
                                backendClient.updateMyAvailabilitySlot(
                                    token = token,
                                    slotId = slot.id,
                                    availableAt = dateTime.format(apiFormatter),
                                )
                            }
                        }.onSuccess {
                            requireActivity().runOnUiThread {
                                dialog.dismiss()
                                onScheduleChanged()
                            }
                        }.onFailure { error ->
                            requireActivity().runOnUiThread {
                                dialog.setCancelable(true)
                                toggleDialogButtons(dialog, true)
                                dateTimeInput.error = error.message ?: "Не удалось сохранить окно"
                            }
                        }
                    }.start()
                }
            }
    }

    private fun toggleDialogButtons(dialog: androidx.appcompat.app.AlertDialog, isEnabled: Boolean) {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.isEnabled = isEnabled
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.isEnabled = isEnabled
        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)?.isEnabled = isEnabled
    }

    private fun setLoading(progressBar: ProgressBar, isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
