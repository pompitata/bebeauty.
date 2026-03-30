package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val backendClient = BackendClient()
    private val usernamePattern = Regex("^[A-Za-zА-Яа-яЁё0-9_.\\- ]{3,50}$")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val usernameInput = view.findViewById<EditText>(R.id.etUsername)
        val phoneInput = view.findViewById<EditText>(R.id.etPhone)
        val passwordInput = view.findViewById<EditText>(R.id.etPassword)
        val masterCheckbox = view.findViewById<CheckBox>(R.id.cbRegisterAsMaster)
        val registerButton = view.findViewById<Button>(R.id.btnRegister)
        val loginLink = view.findViewById<TextView>(R.id.tvGoToLogin)
        val statusText = view.findViewById<TextView>(R.id.tvRegisterStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.registerProgress)
        val sessionManager = SessionManager(requireContext())

        fun setLoading(isLoading: Boolean) {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            registerButton.isEnabled = !isLoading
            loginLink.isEnabled = !isLoading
        }

        fun validate(username: String, phone: String, password: String): Boolean {
            val message = when {
                username.length < 3 -> "Имя пользователя должно быть не короче 3 символов"
                !usernamePattern.matches(username) ->
                    "Логин может содержать русские и английские буквы, цифры, пробел, точку, дефис и нижнее подчеркивание"
                phone.isBlank() -> "Номер телефона обязателен"
                password.length < 6 -> "Пароль должен быть не короче 6 символов"
                else -> null
            }

            statusText.text = message.orEmpty()
            return message == null
        }

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val isMaster = masterCheckbox.isChecked
            if (!validate(username, phone, password)) return@setOnClickListener

            setLoading(true)
            statusText.text = ""

            Thread {
                runCatching {
                    backendClient.register(
                        username = username,
                        phone = phone,
                        password = password,
                        isMaster = isMaster,
                    )
                    val token = backendClient.login(username, password)
                    val user = backendClient.getCurrentUser(token.accessToken)
                    sessionManager.saveIsMaster(user.isMaster)
                    token
                }.onSuccess { token ->
                    sessionManager.saveAccessToken(token.accessToken)
                    requireActivity().runOnUiThread {
                        setLoading(false)
                        findNavController().navigate(R.id.action_register_to_home)
                    }
                }.onFailure { error ->
                    requireActivity().runOnUiThread {
                        setLoading(false)
                        statusText.text = error.message ?: "Не удалось создать пользователя"
                    }
                }
            }.start()
        }

        loginLink.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
