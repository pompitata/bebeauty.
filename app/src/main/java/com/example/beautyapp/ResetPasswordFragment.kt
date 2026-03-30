package com.example.beautyapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ResetPasswordFragment : Fragment(R.layout.fragment_reset_password) {

    private val backendClient = BackendClient()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val phone = arguments?.getString("phone").orEmpty()
        val phoneLabel = view.findViewById<TextView>(R.id.tvResetPhone)
        val passwordInput = view.findViewById<EditText>(R.id.etResetPassword)
        val confirmInput = view.findViewById<EditText>(R.id.etResetPasswordConfirm)
        val submitButton = view.findViewById<Button>(R.id.btnResetPassword)
        val statusText = view.findViewById<TextView>(R.id.tvResetPasswordStatus)
        val progressBar = view.findViewById<ProgressBar>(R.id.resetPasswordProgress)

        phoneLabel.text = "Номер: $phone"

        fun setLoading(isLoading: Boolean) {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            submitButton.isEnabled = !isLoading
        }

        submitButton.setOnClickListener {
            val password = passwordInput.text.toString().trim()
            val confirm = confirmInput.text.toString().trim()

            val error = when {
                phone.isBlank() -> "Номер телефона не передан"
                password.length < 6 -> "Пароль должен быть не короче 6 символов"
                password != confirm -> "Пароли не совпадают"
                else -> null
            }
            if (error != null) {
                statusText.text = error
                return@setOnClickListener
            }

            setLoading(true)
            statusText.text = ""
            Thread {
                runCatching {
                    backendClient.resetPasswordByPhone(phone, password)
                }.onSuccess {
                    requireActivity().runOnUiThread {
                        setLoading(false)
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage("Пароль успешно изменен")
                            .setCancelable(false)
                            .setPositiveButton("OK") { _, _ ->
                                findNavController().navigate(R.id.authFragment)
                            }
                            .show()
                    }
                }.onFailure { error ->
                    requireActivity().runOnUiThread {
                        setLoading(false)
                        statusText.text = error.message ?: "Не удалось изменить пароль"
                    }
                }
            }.start()
        }
    }
}
