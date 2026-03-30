package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ServicesFragment : Fragment(R.layout.fragment_services) {

    private val backendClient = BackendClient()
    private val maxServicePrice = 1_000_000
    private var availableCategories: List<String> = emptyList()
    private lateinit var adapter: MasterServicesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleView = view.findViewById<TextView>(R.id.tvServicesTitle)
        val addButton = view.findViewById<ImageButton>(R.id.btnAddService)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvServices)
        val emptyView = view.findViewById<TextView>(R.id.tvServicesEmpty)
        val statusView = view.findViewById<TextView>(R.id.tvServicesStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.servicesProgress)
        val sessionManager = SessionManager(requireContext())

        titleView.text = "Мои услуги"
        adapter = MasterServicesAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            showAddServiceDialog(
                onServiceCreated = { loadServices(progressBar, emptyView, statusView, sessionManager) },
            )
        }

        loadServices(progressBar, emptyView, statusView, sessionManager)
    }

    private fun loadServices(
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
                val user = backendClient.getCurrentUser(token)
                val profileCategories = user.masterProfile?.serviceCategories ?: emptyList()
                val fallbackCategory = user.masterProfile?.serviceCategory?.let(::listOf) ?: emptyList()
                val categories = profileCategories.ifEmpty { fallbackCategory }
                val services = backendClient.getMyServices(token)
                Pair(categories, services)
            }.onSuccess { (categories, services) ->
                requireActivity().runOnUiThread {
                    setLoading(progressBar, false)
                    availableCategories = categories
                    adapter.submitList(services)
                    emptyView.visibility = if (services.isEmpty()) View.VISIBLE else View.GONE
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    setLoading(progressBar, false)
                    emptyView.visibility = View.GONE
                    statusView.text = error.message ?: "Не удалось загрузить услуги"
                }
            }
        }.start()
    }

    private fun showAddServiceDialog(onServiceCreated: () -> Unit) {
        if (availableCategories.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("Сначала добавьте категории мастера в профиле")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_service, null)
        val categoryInput = dialogView.findViewById<AutoCompleteTextView>(R.id.actvServiceCategory)
        val nameInput = dialogView.findViewById<EditText>(R.id.etServiceName)
        val priceInput = dialogView.findViewById<EditText>(R.id.etServicePrice)
        val durationInput = dialogView.findViewById<EditText>(R.id.etServiceDuration)

        categoryInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableCategories),
        )
        configureCategoryDropdown(categoryInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить услугу")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .show()
            .also { dialog ->
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val category = categoryInput.text.toString().trim().lowercase()
                    val name = nameInput.text.toString().trim()
                    val price = priceInput.text.toString().trim().toIntOrNull()
                    val duration = durationInput.text.toString().trim().toIntOrNull()

                    val validationError = when {
                        category !in availableCategories -> "Выберите категорию из профиля мастера"
                        name.length < 2 -> "Название должно содержать минимум 2 символа"
                        price == null || price <= 0 -> "Укажите корректную стоимость"
                        price >= maxServicePrice -> "Стоимость должна быть меньше $maxServicePrice ₽"
                        duration == null || duration <= 0 -> "Укажите длительность в минутах"
                        else -> null
                    }

                    if (validationError != null) {
                        categoryInput.error = validationError.takeIf { category !in availableCategories }
                        nameInput.error = validationError.takeIf { name.length < 2 }
                        priceInput.error = validationError.takeIf {
                            price == null || price <= 0 || price >= maxServicePrice
                        }
                        durationInput.error = validationError.takeIf { duration == null || duration <= 0 }
                        return@setOnClickListener
                    }

                    val token = SessionManager(requireContext()).getAccessToken()
                    if (token.isNullOrBlank()) {
                        dialog.dismiss()
                        return@setOnClickListener
                    }

                    dialog.setCancelable(false)
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    val validPrice = price ?: return@setOnClickListener
                    val validDuration = duration ?: return@setOnClickListener

                    Thread {
                        runCatching {
                            backendClient.createMyService(
                                token = token,
                                category = category,
                                name = name,
                                price = validPrice,
                                durationMinutes = validDuration,
                            )
                        }.onSuccess {
                            requireActivity().runOnUiThread {
                                dialog.dismiss()
                                onServiceCreated()
                            }
                        }.onFailure { error ->
                            requireActivity().runOnUiThread {
                                dialog.setCancelable(true)
                                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                durationInput.error = error.message ?: "Не удалось сохранить услугу"
                            }
                        }
                    }.start()
                }
            }
    }

    private fun setLoading(progressBar: ProgressBar, isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun configureCategoryDropdown(input: AutoCompleteTextView) {
        input.keyListener = null
        input.setOnClickListener { input.showDropDown() }
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) input.showDropDown()
        }
        input.setOnItemClickListener { _, _, _, _ ->
            input.dismissDropDown()
        }
    }
}
