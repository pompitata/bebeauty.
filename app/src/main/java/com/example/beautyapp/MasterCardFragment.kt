package com.example.beautyapp

import android.content.res.ColorStateList
import android.graphics.Color
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView

class MasterCardFragment : Fragment(R.layout.fragment_master_card) {

    private val backendClient = BackendClient()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val backButton = view.findViewById<ImageButton>(R.id.btnMasterCardBack)
        val avatarView = view.findViewById<ShapeableImageView>(R.id.ivMasterCardAvatar)
        val usernameView = view.findViewById<TextView>(R.id.tvMasterCardUsername)
        val bookButton = view.findViewById<Button>(R.id.btnMasterCardBook)
        val categoriesGroup = view.findViewById<ChipGroup>(R.id.cgMasterCardCategories)
        val servicesStatus = view.findViewById<TextView>(R.id.tvMasterCardServicesStatus)
        val servicesList = view.findViewById<RecyclerView>(R.id.rvMasterCardServices)
        val worksList = view.findViewById<RecyclerView>(R.id.rvMasterCardWorks)
        val worksEmpty = view.findViewById<TextView>(R.id.tvMasterCardWorksEmpty)

        val servicesAdapter = MasterCardServicesAdapter()
        servicesList.layoutManager = LinearLayoutManager(requireContext())
        servicesList.adapter = servicesAdapter

        val worksAdapter = MasterCardWorksAdapter()
        worksList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        worksList.adapter = worksAdapter
        worksAdapter.onWorkClick = { work ->
            FullscreenWorkPhotoDialogFragment.newInstance(work.photoUrl)
                .show(parentFragmentManager, "fullscreenWorkPhoto")
        }

        val masterUserId = arguments?.getInt("masterUserId", -1) ?: -1
        val token = SessionManager(requireContext()).getAccessToken()

        avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
        backButton.setOnClickListener { findNavController().navigateUp() }
        bookButton.setOnClickListener {
            if (masterUserId <= 0) return@setOnClickListener
            findNavController().navigate(
                R.id.action_masterCard_to_bookingService,
                bundleOf("masterUserId" to masterUserId),
            )
        }
        if (masterUserId <= 0 || token.isNullOrBlank()) {
            servicesStatus.text = "Не удалось открыть карточку мастера"
            return
        }

        servicesStatus.text = "Загрузка..."
        Thread {
            runCatching {
                val user = backendClient.getUserById(token, masterUserId)
                val services = backendClient.getMasterServicesByUser(token, masterUserId)
                val works = backendClient.getMasterWorksByUser(token, masterUserId)
                Triple(user, services, works)
            }.onSuccess { (user, services, works) ->
                requireActivity().runOnUiThread {
                    usernameView.text = user.username
                    renderAvatar(avatarView, user.avatar)
                    servicesAdapter.submitList(services)
                    worksAdapter.submitList(works)
                    worksEmpty.visibility = if (works.isEmpty()) View.VISIBLE else View.GONE
                    servicesStatus.text = if (services.isEmpty()) "Услуги пока не добавлены" else ""
                    renderCategories(categoriesGroup, user)
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    servicesStatus.text = error.message ?: "Не удалось загрузить карточку мастера"
                }
            }
        }.start()
    }

    private fun renderCategories(group: ChipGroup, user: UserProfile) {
        group.removeAllViews()
        val profile = user.masterProfile
        val categories = profile?.serviceCategories?.ifEmpty {
            profile.serviceCategory?.let(::listOf) ?: emptyList()
        } ?: emptyList()

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.replaceFirstChar { it.titlecase() }
                isClickable = false
                isCheckable = false
                chipCornerRadius = 18f
                chipMinHeight = 36f
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#F3DED2"))
                setTextColor(Color.parseColor("#6A4435"))
                setEnsureMinTouchTargetSize(false)
            }
            group.addView(chip)
        }
    }

    private fun renderAvatar(avatarView: ShapeableImageView, avatarPathOrUrl: String?) {
        val avatarUrl = ApiConfig.resolveUrl(avatarPathOrUrl)
        RemoteImageLoader.loadInto(avatarView, avatarUrl, R.drawable.ic_avatar_placeholder)
    }
}
