package com.example.beautyapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val backendClient = BackendClient()
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val masterCategories = listOf("ногти", "волосы", "брови", "ресницы")
    private var currentUser: UserProfile? = null
    private var pendingCameraUri: Uri? = null
    private val selectedMasterCategories = mutableListOf<String>()

    private val pickAvatarLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult

            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            updateAvatar(uri)
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (!isSuccess) return@registerForActivityResult
            pendingCameraUri?.let(::updateAvatar)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val avatarImage = view.findViewById<ImageView>(R.id.ivAvatar)
        val changePhotoButton = view.findViewById<Button>(R.id.btnChangePhoto)
        val displayName = view.findViewById<TextView>(R.id.tvProfileName)
        val displayPhone = view.findViewById<TextView>(R.id.tvProfilePhone)
        val profileBadge = view.findViewById<TextView>(R.id.tvProfileBadge)
        val profileMeta = view.findViewById<TextView>(R.id.tvProfileMeta)
        val usernameInput = view.findViewById<EditText>(R.id.etProfileName)
        val phoneInput = view.findViewById<EditText>(R.id.etProfilePhone)
        val masterSection = view.findViewById<View>(R.id.layoutMasterFields)
        val categoryInput = view.findViewById<AutoCompleteTextView>(R.id.actvProfileCategory)
        val addCategoryText = view.findViewById<TextView>(R.id.tvAddCategory)
        val selectedCategoriesLayout = view.findViewById<LinearLayout>(R.id.layoutSelectedCategories)
        val addressInput = view.findViewById<EditText>(R.id.etProfileAddress)
        val descriptionInput = view.findViewById<EditText>(R.id.etProfileDescription)
        val saveButton = view.findViewById<Button>(R.id.btnSaveProfile)
        val statusText = view.findViewById<TextView>(R.id.tvProfileStatus)
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        val progressBar = view.findViewById<LinearProgressIndicator>(R.id.profileProgress)
        val headerCard = view.findViewById<View>(R.id.profileHeaderCard)
        val detailsCard = view.findViewById<View>(R.id.profileDetailsCard)
        val token = sessionManager.getAccessToken()

        if (token == null) {
            displayName.text = "Профиль недоступен"
            displayPhone.text = "Войдите в приложение"
            profileBadge.text = "Гость"
            profileMeta.text = ""
            statusText.text = "Вы не авторизованы"
            changePhotoButton.visibility = View.GONE
            saveButton.visibility = View.GONE
            logoutButton.visibility = View.GONE
            return
        }

        animateEntrance(headerCard, 0L)
        animateEntrance(detailsCard, 110L)

        categoryInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, masterCategories),
        )
        configureCategoryDropdown(categoryInput, masterCategories)
        addCategoryText.setOnClickListener {
            val selectedCategory = categoryInput.text.toString().trim().lowercase()
            when {
                selectedCategory !in masterCategories -> statusText.text = "Выберите корректную категорию услуг"
                selectedCategory in selectedMasterCategories -> statusText.text = "Эта категория уже добавлена"
                else -> {
                    selectedMasterCategories.add(selectedCategory)
                    categoryInput.setText("", false)
                    renderSelectedCategories(selectedCategoriesLayout)
                    statusText.text = ""
                }
            }
        }

        changePhotoButton.setOnClickListener { showAvatarOptions() }
        avatarImage.setOnClickListener { showAvatarOptions() }

        logoutButton.setOnClickListener {
            sessionManager.clearSession()
            findNavController().navigate(R.id.authFragment)
        }

        saveButton.setOnClickListener {
            val user = currentUser ?: return@setOnClickListener
            val newUsername = usernameInput.text.toString().trim()
            val newPhone = phoneInput.text.toString().trim().ifBlank { null }
            val pendingCategory = categoryInput.text.toString().trim().lowercase().ifBlank { null }
            val newAddress = addressInput.text.toString().trim().ifBlank { null }
            val newDescription = descriptionInput.text.toString().trim().ifBlank { null }
            val categoriesToSave = selectedMasterCategories.toMutableList().apply {
                if (pendingCategory != null && pendingCategory in masterCategories && pendingCategory !in this) {
                    add(pendingCategory)
                }
            }

            val validationMessage = when {
                newUsername.length < 3 -> "Имя должно содержать минимум 3 символа"
                newPhone != null && newPhone.length > 20 -> "Номер телефона слишком длинный"
                user.isMaster && pendingCategory != null && pendingCategory !in masterCategories ->
                    "Выберите корректную категорию услуг"
                user.isMaster && categoriesToSave.isEmpty() ->
                    "Добавьте хотя бы одну категорию мастера"
                newAddress != null && newAddress.length > 255 -> "Адрес слишком длинный"
                newDescription != null && newDescription.length > 255 -> "Описание слишком длинное"
                else -> null
            }
            if (validationMessage != null) {
                statusText.text = validationMessage
                return@setOnClickListener
            }

            setLoading(progressBar, saveButton, true)
            statusText.text = ""

            Thread {
                runCatching {
                    backendClient.updateCurrentUser(
                        token = sessionManager.getAccessToken().orEmpty(),
                        username = newUsername,
                        phone = newPhone,
                        masterCategories = categoriesToSave,
                        masterCategory = categoriesToSave.firstOrNull(),
                        masterAddress = newAddress,
                        masterDescription = newDescription,
                    )
                }.onSuccess { result ->
                    sessionManager.saveAccessToken(result.accessToken)
                    currentUser = result.user
                    requireActivity().runOnUiThread {
                        setLoading(progressBar, saveButton, false)
                        renderProfile(
                            user = result.user,
                            avatarImage = avatarImage,
                            displayName = displayName,
                            displayPhone = displayPhone,
                            profileBadge = profileBadge,
                            profileMeta = profileMeta,
                            usernameInput = usernameInput,
                            phoneInput = phoneInput,
                            masterSection = masterSection,
                            categoryInput = categoryInput,
                            selectedCategoriesLayout = selectedCategoriesLayout,
                            addressInput = addressInput,
                            descriptionInput = descriptionInput,
                        )
                        statusText.text = "Профиль обновлен"
                    }
                }.onFailure { error ->
                    requireActivity().runOnUiThread {
                        setLoading(progressBar, saveButton, false)
                        statusText.text = error.message ?: "Не удалось обновить профиль"
                    }
                }
            }.start()
        }

        setLoading(progressBar, saveButton, true)
        statusText.text = ""

        Thread {
            runCatching {
                backendClient.getCurrentUser(token)
            }.onSuccess { user ->
                currentUser = user
                requireActivity().runOnUiThread {
                    setLoading(progressBar, saveButton, false)
                    renderProfile(
                        user = user,
                        avatarImage = avatarImage,
                        displayName = displayName,
                        displayPhone = displayPhone,
                        profileBadge = profileBadge,
                        profileMeta = profileMeta,
                        usernameInput = usernameInput,
                        phoneInput = phoneInput,
                        masterSection = masterSection,
                        categoryInput = categoryInput,
                        selectedCategoriesLayout = selectedCategoriesLayout,
                        addressInput = addressInput,
                        descriptionInput = descriptionInput,
                    )
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    setLoading(progressBar, saveButton, false)
                    displayName.text = "Не удалось загрузить профиль"
                    displayPhone.text = ""
                    statusText.text = error.message ?: "Не удалось загрузить профиль"
                }
            }
        }.start()
    }

    private fun renderProfile(
        user: UserProfile,
        avatarImage: ImageView,
        displayName: TextView,
        displayPhone: TextView,
        profileBadge: TextView,
        profileMeta: TextView,
        usernameInput: EditText,
        phoneInput: EditText,
        masterSection: View,
        categoryInput: AutoCompleteTextView,
        selectedCategoriesLayout: LinearLayout,
        addressInput: EditText,
        descriptionInput: EditText,
    ) {
        displayName.text = user.username
        displayPhone.text = user.phone ?: "Телефон не указан"
        profileBadge.text = if (user.isMaster) "Мастер" else "Личный кабинет"
        profileMeta.text = "ID ${user.id} • beauty profile"
        usernameInput.setText(user.username)
        phoneInput.setText(user.phone.orEmpty())
        masterSection.visibility = if (user.isMaster) View.VISIBLE else View.GONE
        selectedMasterCategories.clear()
        val profileCategories = user.masterProfile?.serviceCategories ?: emptyList()
        val fallbackCategory = user.masterProfile?.serviceCategory?.let(::listOf) ?: emptyList()
        selectedMasterCategories.addAll(profileCategories.ifEmpty { fallbackCategory })
        categoryInput.setText("", false)
        renderSelectedCategories(selectedCategoriesLayout)
        addressInput.setText(user.masterProfile?.address.orEmpty())
        descriptionInput.setText(user.masterProfile?.description.orEmpty())
        sessionManager.saveIsMaster(user.isMaster)
        renderAvatar(avatarImage, user.avatar)
    }

    private fun renderAvatar(avatarImage: ImageView, avatarPathOrUrl: String?) {
        val avatarUrl = ApiConfig.resolveUrl(avatarPathOrUrl)
        RemoteImageLoader.loadInto(avatarImage, avatarUrl, R.drawable.ic_avatar_placeholder)
    }

    private fun setLoading(
        progressBar: LinearProgressIndicator,
        saveButton: Button,
        isLoading: Boolean,
    ) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !isLoading
    }

    private fun configureCategoryDropdown(
        input: AutoCompleteTextView,
        categories: List<String>,
    ) {
        input.keyListener = null
        input.setOnClickListener { input.showDropDown() }
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) input.showDropDown()
        }
        input.setOnItemClickListener { _, _, position, _ ->
            input.setText(categories[position], false)
        }
    }

    private fun renderSelectedCategories(container: LinearLayout) {
        container.removeAllViews()
        selectedMasterCategories.forEach { category ->
            val categoryView = TextView(requireContext()).apply {
                text = "${category.replaceFirstChar { it.titlecase() }}  ×"
                setTextColor(0xFF764B39.toInt())
                textSize = 15f
                setPadding(24, 16, 24, 16)
                background = requireContext().getDrawable(R.drawable.bg_profile_badge)
                setOnClickListener {
                    selectedMasterCategories.remove(category)
                    renderSelectedCategories(container)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 12
            }
            categoryView.layoutParams = params
            container.addView(categoryView)
        }
    }

    private fun showAvatarOptions() {
        currentUser ?: return
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_avatar_options, null)

        sheetView.findViewById<LinearLayout>(R.id.optionGallery).setOnClickListener {
            dialog.dismiss()
            pickAvatarLauncher.launch(arrayOf("image/*"))
        }
        sheetView.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            launchCamera()
        }
        sheetView.findViewById<LinearLayout>(R.id.optionRemove).setOnClickListener {
            dialog.dismiss()
            deleteAvatar()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun launchCamera() {
        val imageFile = File(requireContext().filesDir, "avatar_capture_${System.currentTimeMillis()}.jpg")
        val imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile,
        )
        pendingCameraUri = imageUri
        takePhotoLauncher.launch(imageUri)
    }

    private fun updateAvatar(uri: Uri) {
        val token = sessionManager.getAccessToken() ?: return
        val avatarImage = view?.findViewById<ImageView>(R.id.ivAvatar) ?: return
        val statusText = view?.findViewById<TextView>(R.id.tvProfileStatus)
        statusText?.text = "Загрузка аватара..."
        Thread {
            runCatching {
                backendClient.uploadMyAvatar(requireContext(), token, uri)
            }.onSuccess { updatedUser ->
                currentUser = updatedUser
                if (!isAdded) return@onSuccess
                requireActivity().runOnUiThread {
                    renderAvatar(avatarImage, updatedUser.avatar)
                    statusText?.text = "Аватар обновлен"
                }
            }.onFailure { error ->
                if (!isAdded) return@onFailure
                requireActivity().runOnUiThread {
                    statusText?.text = error.message ?: "Не удалось обновить аватар"
                }
            }
        }.start()
    }

    private fun deleteAvatar() {
        val token = sessionManager.getAccessToken() ?: return
        val avatarImage = view?.findViewById<ImageView>(R.id.ivAvatar) ?: return
        val statusText = view?.findViewById<TextView>(R.id.tvProfileStatus)
        statusText?.text = "Удаление аватара..."
        Thread {
            runCatching {
                backendClient.deleteMyAvatar(token)
            }.onSuccess {
                currentUser = currentUser?.copy(avatar = null)
                if (!isAdded) return@onSuccess
                requireActivity().runOnUiThread {
                    avatarImage.setImageResource(R.drawable.ic_avatar_placeholder)
                    statusText?.text = "Аватар удален"
                }
            }.onFailure { error ->
                if (!isAdded) return@onFailure
                requireActivity().runOnUiThread {
                    statusText?.text = error.message ?: "Не удалось удалить аватар"
                }
            }
        }.start()
    }

    private fun animateEntrance(target: View, delay: Long) {
        target.alpha = 0f
        target.translationY = 40f
        target.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(320L)
            .start()
    }
}
