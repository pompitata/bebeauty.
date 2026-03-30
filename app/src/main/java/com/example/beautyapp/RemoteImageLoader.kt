package com.example.beautyapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.net.URL

object RemoteImageLoader {

    fun loadInto(
        imageView: ImageView,
        imageUrl: String?,
        @DrawableRes placeholderRes: Int,
    ) {
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(placeholderRes)
            return
        }

        imageView.setImageDrawable(null)
        Thread {
            runCatching {
                loadBitmap(imageUrl)
            }.onSuccess { bitmap ->
                imageView.post {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(placeholderRes)
                    }
                }
            }.onFailure {
                imageView.post {
                    imageView.setImageResource(placeholderRes)
                }
            }
        }.start()
    }

    private fun loadBitmap(imageUrl: String): Bitmap? {
        val bytes = URL(imageUrl).openStream().use { it.readBytes() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        return applyExifOrientation(bitmap, exif)
    }

    private fun applyExifOrientation(bitmap: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
