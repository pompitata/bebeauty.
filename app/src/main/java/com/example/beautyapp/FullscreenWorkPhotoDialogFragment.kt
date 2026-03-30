package com.example.beautyapp

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.DialogFragment

class FullscreenWorkPhotoDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_work_photo)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )

        val photoUrl = arguments?.getString(ARG_PHOTO_URL).orEmpty()
        val photoView = dialog.findViewById<ImageView>(R.id.ivFullscreenWorkPhoto)
        val closeButton = dialog.findViewById<ImageButton>(R.id.btnCloseFullscreenPhoto)

        closeButton.setOnClickListener { dismiss() }
        loadPhoto(photoView, photoUrl)
        return dialog
    }

    private fun loadPhoto(imageView: ImageView, photoUrl: String) {
        RemoteImageLoader.loadInto(imageView, photoUrl, R.drawable.ic_avatar_placeholder)
    }

    companion object {
        private const val ARG_PHOTO_URL = "photoUrl"

        fun newInstance(photoUrl: String): FullscreenWorkPhotoDialogFragment {
            return FullscreenWorkPhotoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_URL, photoUrl)
                }
            }
        }
    }
}
