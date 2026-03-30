package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private val backendClient = BackendClient()
    private val usernamePattern = Regex("^[A-Za-zА-Яа-яЁё0-9_.\\- ]{3,50}$")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val usernameInput = view.findViewById<EditText>(R.id.etUsername)
        val passwordInput = view.findViewById<EditText>(R.id.etPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val forgotPasswordLink = view.findViewById<TextView>(R.id.tvForgotPassword)
        val registerLink = view.findViewById<TextView>(R.id.tvGoToRegister)
        val statusText = view.findViewById<TextView>(R.id.tvAuthStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.authProgress)
        val sessionManager = SessionManager(requireContext())

        fun setLoading(isLoading: Boolean) {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginButton.isEnabled = !isLoading
            forgotPasswordLink.isEnabled = !isLoading
            registerLink.isEnabled = !isLoading
        }

        fun validate(username: String, password: String): Boolean {
            val message = when {
                username.length < 3 -> "Имя пользователя должно быть не короче 3 символов"
                !usernamePattern.matches(username) ->
                    "Логин может содержать русские и английские буквы, цифры, пробел, точку, дефис и нижнее подчеркивание"
                password.length < 6 -> "Пароль должен быть не короче 6 символов"
                else -> null
            }

            statusText.text = message.orEmpty()
            return message == null
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (!validate(username, password)) return@setOnClickListener

            setLoading(true)
            statusText.text = ""

            Thread {
                runCatching {
                    val token = backendClient.login(username, password)
                    val user = backendClient.getCurrentUser(token.accessToken)
                    sessionManager.saveIsMaster(user.isMaster)
                    token
                }.onSuccess { token ->
                    sessionManager.saveAccessToken(token.accessToken)
                    requireActivity().runOnUiThread {
                        setLoading(false)
                        findNavController().navigate(R.id.action_auth_to_home)
                    }
                }.onFailure { error ->
                    requireActivity().runOnUiThread {
                        setLoading(false)
                        statusText.text = error.message ?: "Не удалось выполнить вход"
                    }
                }
            }.start()
        }

        registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_auth_to_register)
        }

        forgotPasswordLink.setOnClickListener {
            showForgotPasswordDialog(statusText)
        }
    }

    private fun showForgotPasswordDialog(statusText: TextView) {
        val phoneInput = EditText(requireContext()).apply {
            hint = "Введите номер телефона"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Подтвердите номер телефона")
            .setView(phoneInput)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Подтвердить", null)
            .show()
            .also { dialog ->
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val phone = phoneInput.text.toString().trim()
                    if (phone.isBlank()) {
                        phoneInput.error = "Введите номер телефона"
                        return@setOnClickListener
                    }

                    dialog.setCancelable(false)
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    statusText.text = "Проверка номера..."

                    Thread {
                        runCatching {
                            backendClient.checkPhoneForPasswordReset(phone)
                        }.onSuccess {
                            requireActivity().runOnUiThread {
                                statusText.text = ""
                                dialog.dismiss()
                                findNavController().navigate(
                                    R.id.action_auth_to_resetPassword,
                                    bundleOf("phone" to phone),
                                )
                            }
                        }.onFailure { error ->
                            requireActivity().runOnUiThread {
                                dialog.setCancelable(true)
                                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                statusText.text = error.message ?: "Не удалось проверить номер телефона"
                            }
                        }
                    }.start()
                }
            }
    }

}
