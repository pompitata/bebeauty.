package com.example.beautyapp

import android.content.Context
import org.json.JSONArray

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("beauty_app_session", Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveIsMaster(isMaster: Boolean) {
        prefs.edit().putBoolean(KEY_IS_MASTER, isMaster).apply()
    }

    fun isMaster(): Boolean = prefs.getBoolean(KEY_IS_MASTER, false)

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_IS_MASTER)
            .apply()
    }

    fun saveAvatarUri(username: String, uri: String?) {
        prefs.edit().putString(getAvatarKey(username), uri).apply()
    }

    fun getAvatarUri(username: String): String? = prefs.getString(getAvatarKey(username), null)

    fun moveAvatarUri(oldUsername: String, newUsername: String) {
        if (oldUsername == newUsername) return
        val uri = getAvatarUri(oldUsername)
        prefs.edit().remove(getAvatarKey(oldUsername)).apply()
        if (uri != null) {
            saveAvatarUri(newUsername, uri)
        }
        moveWorkUris(oldUsername, newUsername)
    }

    fun getWorkUris(username: String): List<String> {
        val raw = prefs.getString(getWorksKey(username), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addWorkUri(username: String, uri: String) {
        val updated = getWorkUris(username).toMutableList().apply {
            if (uri !in this) add(0, uri)
        }
        saveWorkUris(username, updated)
    }

    fun removeWorkUri(username: String, uri: String) {
        val updated = getWorkUris(username).toMutableList().apply {
            remove(uri)
        }
        saveWorkUris(username, updated)
    }

    private fun saveWorkUris(username: String, uris: List<String>) {
        val json = JSONArray()
        uris.forEach { json.put(it) }
        prefs.edit().putString(getWorksKey(username), json.toString()).apply()
    }

    private fun moveWorkUris(oldUsername: String, newUsername: String) {
        if (oldUsername == newUsername) return
        val uris = getWorkUris(oldUsername)
        prefs.edit().remove(getWorksKey(oldUsername)).apply()
        if (uris.isNotEmpty()) {
            saveWorkUris(newUsername, uris)
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_IS_MASTER = "is_master"

        private fun getAvatarKey(username: String): String = "avatar_uri_$username"
        private fun getWorksKey(username: String): String = "works_uri_$username"
    }
}
