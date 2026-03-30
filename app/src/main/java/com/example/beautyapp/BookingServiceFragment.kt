package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BookingServiceFragment : Fragment(R.layout.fragment_booking_service) {

    private val backendClient = BackendClient()
    private var selectedService: MasterService? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backButton = view.findViewById<ImageButton>(R.id.btnBookingServiceBack)
        val titleView = view.findViewById<TextView>(R.id.tvBookingServiceTitle)
        val statusView = view.findViewById<TextView>(R.id.tvBookingServiceStatus)
        val emptyView = view.findViewById<TextView>(R.id.tvBookingServiceEmpty)
        val servicesList = view.findViewById<RecyclerView>(R.id.rvBookingServices)
        val continueButton = view.findViewById<Button>(R.id.btnContinueBookingService)

        val masterUserId = arguments?.getInt("masterUserId", -1) ?: -1
        val token = SessionManager(requireContext()).getAccessToken()

        var adapterRef: BookingServiceAdapter? = null
        val adapter = BookingServiceAdapter { service ->
            selectedService = service
            adapterRef?.setSelectedService(service.id)
            continueButton.isEnabled = true
        }
        adapterRef = adapter
        servicesList.layoutManager = LinearLayoutManager(requireContext())
        servicesList.adapter = adapter

        backButton.setOnClickListener { findNavController().navigateUp() }
        continueButton.isEnabled = false
        continueButton.setOnClickListener {
            val service = selectedService ?: return@setOnClickListener
            findNavController().navigate(
                R.id.action_bookingService_to_bookingSlots,
                bundleOf(
                    "masterUserId" to masterUserId,
                    "serviceId" to service.id,
                    "serviceName" to service.name,
                ),
            )
        }

        if (masterUserId <= 0 || token.isNullOrBlank()) {
            statusView.text = "Не удалось загрузить услуги мастера"
            return
        }

        statusView.text = "Загрузка..."
        Thread {
            runCatching {
                val user = backendClient.getUserById(token, masterUserId)
                val services = backendClient.getMasterServicesByUser(token, masterUserId)
                Pair(user.username, services)
            }.onSuccess { (username, services) ->
                requireActivity().runOnUiThread {
                    titleView.text = "Выберите услугу: $username"
                    statusView.text = ""
                    adapter.submitList(services)
                    emptyView.visibility = if (services.isEmpty()) View.VISIBLE else View.GONE
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    statusView.text = error.message ?: "Не удалось загрузить услуги мастера"
                }
            }
        }.start()
    }
}
