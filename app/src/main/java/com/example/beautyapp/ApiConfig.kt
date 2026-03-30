package com.example.beautyapp

import android.os.Build

object ApiConfig {
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:8000"
    private const val DEVICE_BASE_URL = "http://94.125.52.153:1214"

    val BASE_URL: String
        get() = if (isEmulator()) EMULATOR_BASE_URL else DEVICE_BASE_URL

    fun resolveUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        return if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            pathOrUrl
        } else {
            "${BASE_URL}${if (pathOrUrl.startsWith("/")) "" else "/"}$pathOrUrl"
        }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk") ||
            Build.HARDWARE.contains("ranchu")
    }
}
