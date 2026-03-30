package com.example.beautyapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val backendClient = BackendClient()
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val categoryItems = listOf("ногти", "волосы", "брови", "ресницы")

    private lateinit var worksAdapter: WorksAdapter
    private lateinit var clientMastersAdapter: ClientMastersAdapter
    private lateinit var clientCategoryAdapter: ClientCategoryAdapter

    private var currentUser: UserProfile? = null
    private var pendingCameraUri: Uri? = null
    private var allMasters: List<UserProfile> = emptyList()
    private var selectedCategory: String? = null

    private val pickWorkLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult

            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            addWorkPhoto(uri)
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (!isSuccess) return@registerForActivityResult
            pendingCameraUri?.let(::addWorkPhoto)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutMasterHome = view.findViewById<View>(R.id.layoutMasterHome)
        val layoutClientHome = view.findViewById<View>(R.id.layoutClientHome)

        val tvHomeTitle = view.findViewById<TextView>(R.id.tvHomeTitle)
        val tvHomeSubtitle = view.findViewById<TextView>(R.id.tvHomeSubtitle)
        val btnAddWork = view.findViewById<Button>(R.id.btnAddWork)
        val tvWorksStatus = view.findViewById<TextView>(R.id.tvWorksStatus)
        val tvWorksEmpty = view.findViewById<TextView>(R.id.tvWorksEmpty)
        val rvWorks = view.findViewById<RecyclerView>(R.id.rvWorks)

        val tilClientSearch = view.findViewById<View>(R.id.tilClientSearch)
        val etClientSearch = view.findViewById<TextInputEditText>(R.id.etClientSearch)
        val btnClientCancel = view.findViewById<Button>(R.id.btnClientCancel)
        val btnClientBack = view.findViewById<Button>(R.id.btnClientBack)
        val tvClientStatus = view.findViewById<TextView>(R.id.tvClientStatus)
        val tvClientHint = view.findViewById<TextView>(R.id.tvClientHint)
        val tvClientEmpty = view.findViewById<TextView>(R.id.tvClientEmpty)
        val rvClientCategories = view.findViewById<RecyclerView>(R.id.rvClientCategories)
        val rvClientMasters = view.findViewById<RecyclerView>(R.id.rvClientMasters)

        worksAdapter = WorksAdapter { work ->
            confirmDeleteWork(work, tvWorksEmpty, tvWorksStatus)
        }
        rvWorks.layoutManager = GridLayoutManager(requireContext(), 2)
        rvWorks.adapter = worksAdapter

        clientMastersAdapter = ClientMastersAdapter()
        clientMastersAdapter.onMasterClick = { master ->
            findNavController().navigate(
                R.id.action_home_to_masterCard,
                bundleOf("masterUserId" to master.id),
            )
        }
        rvClientMasters.layoutManager = LinearLayoutManager(requireContext())
        rvClientMasters.adapter = clientMastersAdapter

        clientCategoryAdapter = ClientCategoryAdapter(categoryItems) { category ->
            selectedCategory = category
            etClientSearch.setText("")
            showMastersForCategory(
                category = category,
                tilClientSearch = tilClientSearch,
                btnClientCancel = btnClientCancel,
                btnClientBack = btnClientBack,
                rvClientCategories = rvClientCategories,
                rvClientMasters = rvClientMasters,
                tvClientHint = tvClientHint,
                tvClientEmpty = tvClientEmpty,
            )
        }
        rvClientCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        rvClientCategories.adapter = clientCategoryAdapter

        btnClientBack.setOnClickListener {
            selectedCategory = null
            etClientSearch.setText("")
            showClientCategories(
                tilClientSearch = tilClientSearch,
                btnClientCancel = btnClientCancel,
                btnClientBack = btnClientBack,
                rvClientCategories = rvClientCategories,
                rvClientMasters = rvClientMasters,
                tvClientHint = tvClientHint,
                tvClientEmpty = tvClientEmpty,
            )
        }
        btnClientCancel.setOnClickListener {
            selectedCategory = null
            etClientSearch.setText("")
            showClientCategories(
                tilClientSearch = tilClientSearch,
                btnClientCancel = btnClientCancel,
                btnClientBack = btnClientBack,
                rvClientCategories = rvClientCategories,
                rvClientMasters = rvClientMasters,
                tvClientHint = tvClientHint,
                tvClientEmpty = tvClientEmpty,
            )
        }
        etClientSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    applyClientSearch(
                        query = s?.toString().orEmpty(),
                        tilClientSearch = tilClientSearch,
                        btnClientCancel = btnClientCancel,
                        btnClientBack = btnClientBack,
                        rvClientCategories = rvClientCategories,
                        rvClientMasters = rvClientMasters,
                        tvClientHint = tvClientHint,
                        tvClientEmpty = tvClientEmpty,
                    )
                }
            },
        )
        etClientSearch.setOnEditorActionListener { _, _, event ->
            if (event == null || event.keyCode == KeyEvent.KEYCODE_ENTER) {
                applyClientSearch(
                    query = etClientSearch.text?.toString().orEmpty(),
                    tilClientSearch = tilClientSearch,
                    btnClientCancel = btnClientCancel,
                    btnClientBack = btnClientBack,
                    rvClientCategories = rvClientCategories,
                    rvClientMasters = rvClientMasters,
                    tvClientHint = tvClientHint,
                    tvClientEmpty = tvClientEmpty,
                )
                true
            } else {
                false
            }
        }

        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) {
            layoutMasterHome.visibility = View.GONE
            layoutClientHome.visibility = View.VISIBLE
            tvClientStatus.text = "Сначала войдите в аккаунт"
            showClientCategories(
                tilClientSearch = tilClientSearch,
                btnClientCancel = btnClientCancel,
                btnClientBack = btnClientBack,
                rvClientCategories = rvClientCategories,
                rvClientMasters = rvClientMasters,
                tvClientHint = tvClientHint,
                tvClientEmpty = tvClientEmpty,
            )
            return
        }

        btnAddWork.setOnClickListener { showWorkOptions() }

        Thread {
            runCatching { backendClient.getCurrentUser(token) }
                .onSuccess { user ->
                    currentUser = user
                    requireActivity().runOnUiThread {
                        if (user.isMaster) {
                            layoutMasterHome.visibility = View.VISIBLE
                            layoutClientHome.visibility = View.GONE
                            tvHomeTitle.text = "Мои работы"
                            tvHomeSubtitle.text =
                                "Добавляйте фотографии своих работ. Нажмите на фото, чтобы удалить его."
                            tvWorksStatus.text = ""
                            loadWorks(tvWorksEmpty, tvWorksStatus)
                        } else {
                            layoutMasterHome.visibility = View.GONE
                            layoutClientHome.visibility = View.VISIBLE
                            tvClientStatus.text = ""
                            loadClientCatalog(
                                token = token,
                                tilClientSearch = tilClientSearch,
                                btnClientCancel = btnClientCancel,
                                btnClientBack = btnClientBack,
                                rvClientCategories = rvClientCategories,
                                rvClientMasters = rvClientMasters,
                                tvClientStatus = tvClientStatus,
                                tvClientHint = tvClientHint,
                                tvClientEmpty = tvClientEmpty,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    requireActivity().runOnUiThread {
                        layoutMasterHome.visibility = View.GONE
                        layoutClientHome.visibility = View.VISIBLE
                        tvClientStatus.text = error.message ?: "Не удалось загрузить данные"
                    }
                }
        }.start()
    }

    private fun loadClientCatalog(
        token: String,
        tilClientSearch: View,
        btnClientCancel: Button,
        btnClientBack: Button,
        rvClientCategories: RecyclerView,
        rvClientMasters: RecyclerView,
        tvClientStatus: TextView,
        tvClientHint: TextView,
        tvClientEmpty: TextView,
    ) {
        tvClientStatus.text = "Загрузка мастеров..."
        Thread {
            runCatching {
                backendClient.getUsers(token).filter { it.isMaster }
            }.onSuccess { masters ->
                allMasters = masters
                requireActivity().runOnUiThread {
                    tvClientStatus.text = ""
                    showClientCategories(
                        tilClientSearch = tilClientSearch,
                        btnClientCancel = btnClientCancel,
                        btnClientBack = btnClientBack,
                        rvClientCategories = rvClientCategories,
                        rvClientMasters = rvClientMasters,
                        tvClientHint = tvClientHint,
                        tvClientEmpty = tvClientEmpty,
                    )
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    tvClientStatus.text = error.message ?: "Не удалось загрузить мастеров"
                }
            }
        }.start()
    }

    private fun applyClientSearch(
        query: String,
        tilClientSearch: View,
        btnClientCancel: Button,
        btnClientBack: Button,
        rvClientCategories: RecyclerView,
        rvClientMasters: RecyclerView,
        tvClientHint: TextView,
        tvClientEmpty: TextView,
    ) {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            val category = selectedCategory
            if (category == null) {
                showClientCategories(
                    tilClientSearch = tilClientSearch,
                    btnClientCancel = btnClientCancel,
                    btnClientBack = btnClientBack,
                    rvClientCategories = rvClientCategories,
                    rvClientMasters = rvClientMasters,
                    tvClientHint = tvClientHint,
                    tvClientEmpty = tvClientEmpty,
                )
            } else {
                showMastersForCategory(
                    category = category,
                    tilClientSearch = tilClientSearch,
                    btnClientCancel = btnClientCancel,
                    btnClientBack = btnClientBack,
                    rvClientCategories = rvClientCategories,
                    rvClientMasters = rvClientMasters,
                    tvClientHint = tvClientHint,
                    tvClientEmpty = tvClientEmpty,
                )
            }
            return
        }

        selectedCategory = null
        val exactMatches = allMasters.filter { it.username.equals(normalized, ignoreCase = true) }
        showClientMasters(
            masters = exactMatches,
            title = "Поиск по логину: $normalized",
            isSearchMode = true,
            tilClientSearch = tilClientSearch,
            btnClientCancel = btnClientCancel,
            btnClientBack = btnClientBack,
            rvClientCategories = rvClientCategories,
            rvClientMasters = rvClientMasters,
            tvClientHint = tvClientHint,
            tvClientEmpty = tvClientEmpty,
        )
    }

    private fun showMastersForCategory(
        category: String,
        tilClientSearch: View,
        btnClientCancel: Button,
        btnClientBack: Button,
        rvClientCategories: RecyclerView,
        rvClientMasters: RecyclerView,
        tvClientHint: TextView,
        tvClientEmpty: TextView,
    ) {
        val masters = allMasters.filter { master ->
            val profile = master.masterProfile
            val categories = profile?.serviceCategories?.ifEmpty {
                profile.serviceCategory?.let(::listOf) ?: emptyList()
            } ?: emptyList()
            categories.any { it.equals(category, ignoreCase = true) }
        }
        showClientMasters(
            masters = masters,
            title = "Категория: ${category.replaceFirstChar { it.titlecase() }}",
            isSearchMode = false,
            tilClientSearch = tilClientSearch,
            btnClientCancel = btnClientCancel,
            btnClientBack = btnClientBack,
            rvClientCategories = rvClientCategories,
            rvClientMasters = rvClientMasters,
            tvClientHint = tvClientHint,
            tvClientEmpty = tvClientEmpty,
        )
    }

    private fun showClientCategories(
        tilClientSearch: View,
        btnClientCancel: Button,
        btnClientBack: Button,
        rvClientCategories: RecyclerView,
        rvClientMasters: RecyclerView,
        tvClientHint: TextView,
        tvClientEmpty: TextView,
    ) {
        tilClientSearch.visibility = View.VISIBLE
        btnClientCancel.visibility = View.GONE
        btnClientBack.visibility = View.GONE
        rvClientCategories.visibility = View.VISIBLE
        rvClientMasters.visibility = View.GONE
        tvClientHint.text = "Выберите категорию или найдите мастера по логину."
        tvClientEmpty.visibility = View.GONE
    }

    private fun showClientMasters(
        masters: List<UserProfile>,
        title: String,
        isSearchMode: Boolean,
        tilClientSearch: View,
        btnClientCancel: Button,
        btnClientBack: Button,
        rvClientCategories: RecyclerView,
        rvClientMasters: RecyclerView,
        tvClientHint: TextView,
        tvClientEmpty: TextView,
    ) {
        tilClientSearch.visibility = if (isSearchMode) View.VISIBLE else View.GONE
        btnClientCancel.visibility = if (isSearchMode) View.VISIBLE else View.GONE
        btnClientBack.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        rvClientCategories.visibility = View.GONE
        rvClientMasters.visibility = View.VISIBLE
        tvClientHint.text = title
        clientMastersAdapter.submitList(masters)
        tvClientEmpty.visibility = if (masters.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadWorks(emptyView: TextView, statusView: TextView) {
        val token = sessionManager.getAccessToken() ?: return
        Thread {
            runCatching {
                backendClient.getMyWorks(token)
            }.onSuccess { works ->
                requireActivity().runOnUiThread {
                    worksAdapter.submitList(works)
                    emptyView.visibility = if (works.isEmpty()) View.VISIBLE else View.GONE
                    statusView.text = ""
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    statusView.text = error.message ?: "Не удалось загрузить работы"
                }
            }
        }.start()
    }

    private fun showWorkOptions() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_work_options, null)

        sheetView.findViewById<View>(R.id.optionGallery).setOnClickListener {
            dialog.dismiss()
            pickWorkLauncher.launch(arrayOf("image/*"))
        }
        sheetView.findViewById<View>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            launchCamera()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun launchCamera() {
        val imageFile = File(requireContext().filesDir, "work_${System.currentTimeMillis()}.jpg")
        val imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile,
        )
        pendingCameraUri = imageUri
        takePhotoLauncher.launch(imageUri)
    }

    private fun addWorkPhoto(uri: Uri) {
        val token = sessionManager.getAccessToken() ?: return
        val emptyView = view?.findViewById<TextView>(R.id.tvWorksEmpty) ?: return
        val statusView = view?.findViewById<TextView>(R.id.tvWorksStatus) ?: return
        statusView.text = "Загрузка фотографии..."
        Thread {
            runCatching {
                backendClient.uploadMyWork(requireContext(), token, uri)
            }.onSuccess {
                requireActivity().runOnUiThread {
                    loadWorks(emptyView, statusView)
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    statusView.text = error.message ?: "Не удалось загрузить фотографию"
                }
            }
        }.start()
    }

    private fun confirmDeleteWork(work: MasterWork, emptyView: TextView, statusView: TextView) {
        val token = sessionManager.getAccessToken() ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить работу")
            .setMessage("Эта фотография будет удалена из раздела работ.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                Thread {
                    runCatching {
                        backendClient.deleteMyWork(token, work.id)
                    }.onSuccess {
                        requireActivity().runOnUiThread {
                            loadWorks(emptyView, statusView)
                        }
                    }.onFailure { error ->
                        requireActivity().runOnUiThread {
                            statusView.text = error.message ?: "Не удалось удалить работу"
                        }
                    }
                }.start()
            }
            .show()
    }

}
