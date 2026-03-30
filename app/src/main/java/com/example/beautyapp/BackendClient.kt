package com.example.beautyapp

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class
BackendClient {

    fun register(username: String, phone: String?, password: String, isMaster: Boolean): UserProfile {
        val payload = JSONObject().apply {
            put("username", username)
            put("phone", if (phone.isNullOrBlank()) JSONObject.NULL else phone)
            put("password", password)
            put("is_master", isMaster)
        }

        val response = request(
            path = "/auth/register",
            method = "POST",
            contentType = "application/json",
            body = payload.toString(),
        )
        return response.toUserProfile()
    }

    fun login(username: String, password: String): AuthToken {
        val formBody = buildString {
            append("username=")
            append(URLEncoder.encode(username, Charsets.UTF_8.name()))
            append("&password=")
            append(URLEncoder.encode(password, Charsets.UTF_8.name()))
        }

        val response = request(
            path = "/auth/token",
            method = "POST",
            contentType = "application/x-www-form-urlencoded",
            body = formBody,
        )

        return AuthToken(
            accessToken = response.getString("access_token"),
            tokenType = response.getString("token_type"),
        )
    }

    fun checkPhoneForPasswordReset(phone: String) {
        val payload = JSONObject().apply {
            put("phone", phone)
        }
        request(
            path = "/auth/password/check-phone",
            method = "POST",
            contentType = "application/json",
            body = payload.toString(),
        )
    }

    fun resetPasswordByPhone(phone: String, newPassword: String) {
        val payload = JSONObject().apply {
            put("phone", phone)
            put("new_password", newPassword)
        }
        request(
            path = "/auth/password/reset-by-phone",
            method = "POST",
            contentType = "application/json",
            body = payload.toString(),
        )
    }

    fun getCurrentUser(token: String): UserProfile {
        val response = request(
            path = "/auth/me",
            method = "GET",
            bearerToken = token,
        )
        return response.toUserProfile()
    }

    fun getUsers(token: String): List<UserProfile> {
        val response = requestArray(
            path = "/users/",
            method = "GET",
            bearerToken = token,
        )
        return response.toUsers()
    }

    fun getUserById(token: String, userId: Int): UserProfile {
        val response = request(
            path = "/users/$userId",
            method = "GET",
            bearerToken = token,
        )
        return response.toUserProfile()
    }

    fun getMyServices(token: String): List<MasterService> {
        val response = requestArray(
            path = "/master-services/me",
            method = "GET",
            bearerToken = token,
        )
        return response.toMasterServices()
    }

    fun getMyAvailabilitySlots(token: String): List<MasterAvailabilitySlot> {
        val response = requestArray(
            path = "/master-availability/me",
            method = "GET",
            bearerToken = token,
        )
        return response.toMasterAvailabilitySlots()
    }

    fun getMasterAvailabilitySlotsByUser(token: String, userId: Int): List<MasterAvailabilitySlot> {
        val response = requestArray(
            path = "/master-availability/user/$userId",
            method = "GET",
            bearerToken = token,
        )
        return response.toMasterAvailabilitySlots()
    }

    fun getMyWorks(token: String): List<MasterWork> {
        val response = requestArray(
            path = "/master-works/me",
            method = "GET",
            bearerToken = token,
        )
        return response.toMasterWorks()
    }

    fun getMasterServicesByUser(token: String, userId: Int): List<MasterService> {
        val response = requestArray(
            path = "/master-services/user/$userId",
            method = "GET",
            bearerToken = token,
        )
        return response.toMasterServices()
    }

    fun getMasterWorksByUser(token: String, userId: Int): List<MasterWork> {
        val response = requestArray(
            path = "/master-works/user/$userId",
            method = "GET",
            bearerToken = token,
        )
        return response.toMasterWorks()
    }

    fun getMyBookings(token: String): List<Booking> {
        val response = requestArray(
            path = "/bookings/me",
            method = "GET",
            bearerToken = token,
        )
        return response.toBookings()
    }

    fun createBooking(token: String, serviceId: Int, slotId: Int): Booking {
        val payload = JSONObject().apply {
            put("service_id", serviceId)
            put("slot_id", slotId)
        }
        val response = request(
            path = "/bookings/",
            method = "POST",
            contentType = "application/json",
            body = payload.toString(),
            bearerToken = token,
        )
        return response.toBooking()
    }

    fun rescheduleBooking(token: String, bookingId: Int, slotId: Int): Booking {
        val payload = JSONObject().apply {
            put("slot_id", slotId)
        }
        val response = request(
            path = "/bookings/$bookingId/reschedule",
            method = "PUT",
            contentType = "application/json",
            body = payload.toString(),
            bearerToken = token,
        )
        return response.toBooking()
    }

    fun cancelBooking(token: String, bookingId: Int) {
        requestWithoutBody(
            path = "/bookings/$bookingId",
            method = "DELETE",
            bearerToken = token,
        )
    }

    fun getMyNotifications(token: String, unreadOnly: Boolean = true): List<UserNotification> {
        val response = requestArray(
            path = "/notifications/me?unread_only=$unreadOnly",
            method = "GET",
            bearerToken = token,
        )
        return response.toNotifications()
    }

    fun markNotificationRead(token: String, notificationId: Int) {
        requestWithoutBody(
            path = "/notifications/me/$notificationId/read",
            method = "POST",
            bearerToken = token,
        )
    }

    fun createMyService(
        token: String,
        category: String,
        name: String,
        price: Int,
        durationMinutes: Int,
    ): MasterService {
        val payload = JSONObject().apply {
            put("category", category)
            put("name", name)
            put("price", price)
            put("duration_minutes", durationMinutes)
        }

        val response = request(
            path = "/master-services/me",
            method = "POST",
            contentType = "application/json",
            body = payload.toString(),
            bearerToken = token,
        )
        return response.toMasterService()
    }

    fun createMyAvailabilitySlot(token: String, availableAt: String): MasterAvailabilitySlot {
        val payload = JSONObject().apply {
            put("available_at", availableAt)
        }

        val response = request(
            path = "/master-availability/me",
            method = "POST",
            contentType = "application/json",
            body = payload.toString(),
            bearerToken = token,
        )
        return response.toMasterAvailabilitySlot()
    }

    fun updateMyAvailabilitySlot(
        token: String,
        slotId: Int,
        availableAt: String,
    ): MasterAvailabilitySlot {
        val payload = JSONObject().apply {
            put("available_at", availableAt)
        }

        val response = request(
            path = "/master-availability/me/$slotId",
            method = "PUT",
            contentType = "application/json",
            body = payload.toString(),
            bearerToken = token,
        )
        return response.toMasterAvailabilitySlot()
    }

    fun deleteMyAvailabilitySlot(token: String, slotId: Int) {
        requestWithoutBody(
            path = "/master-availability/me/$slotId",
            method = "DELETE",
            bearerToken = token,
        )
    }

    fun uploadMyWork(context: Context, token: String, photoUri: Uri): MasterWork {
        val boundary = "Boundary-${UUID.randomUUID()}"
        val connection = (URL("${ApiConfig.BASE_URL}/master-works/me").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        return try {
            DataOutputStream(connection.outputStream).use { output ->
                val fileName = "work_${System.currentTimeMillis()}.jpg"
                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"photo\"; filename=\"$fileName\"\r\n",
                )
                output.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                context.contentResolver.openInputStream(photoUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw IllegalStateException("Не удалось прочитать фотографию")
                output.writeBytes("\r\n--$boundary--\r\n")
                output.flush()
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream?.readText().orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(parseErrorMessage(responseText, connection.responseMessage))
            }

            JSONObject(responseText).toMasterWork()
        } finally {
            connection.disconnect()
        }
    }

    fun uploadMyAvatar(context: Context, token: String, photoUri: Uri): UserProfile {
        val boundary = "Boundary-${UUID.randomUUID()}"
        val connection = (URL("${ApiConfig.BASE_URL}/auth/me/avatar").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        return try {
            DataOutputStream(connection.outputStream).use { output ->
                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"avatar\"; filename=\"$fileName\"\r\n",
                )
                output.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                context.contentResolver.openInputStream(photoUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw IllegalStateException("Не удалось прочитать фотографию")
                output.writeBytes("\r\n--$boundary--\r\n")
                output.flush()
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream?.readText().orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(parseErrorMessage(responseText, connection.responseMessage))
            }

            JSONObject(responseText).toUserProfile()
        } finally {
            connection.disconnect()
        }
    }

    fun deleteMyAvatar(token: String) {
        requestWithoutBody(
            path = "/auth/me/avatar",
            method = "DELETE",
            bearerToken = token,
        )
    }

    fun deleteMyWork(token: String, workId: Int) {
        requestWithoutBody(
            path = "/master-works/me/$workId",
            method = "DELETE",
            bearerToken = token,
        )
    }

    fun updateCurrentUser(
        token: String,
        username: String,
        phone: String?,
        masterCategories: List<String> = emptyList(),
        masterCategory: String? = null,
        masterAddress: String? = null,
        masterDescription: String? = null,
    ): ProfileUpdateResult {
        val payload = JSONObject().apply {
            put("username", username)
            put("phone", if (phone.isNullOrBlank()) JSONObject.NULL else phone)
            put("master_categories", JSONArray(masterCategories))
            put("master_category", if (masterCategory.isNullOrBlank()) JSONObject.NULL else masterCategory)
            put("master_address", if (masterAddress.isNullOrBlank()) JSONObject.NULL else masterAddress)
            put("master_description", if (masterDescription.isNullOrBlank()) JSONObject.NULL else masterDescription)
        }

        val response = request(
            path = "/auth/me",
            method = "PUT",
            contentType = "application/json",
            body = payload.toString(),
            bearerToken = token,
        )

        return ProfileUpdateResult(
            user = response.getJSONObject("user").toUserProfile(),
            accessToken = response.getString("access_token"),
            tokenType = response.getString("token_type"),
        )
    }

    private fun request(
        path: String,
        method: String,
        contentType: String? = null,
        body: String? = null,
        bearerToken: String? = null,
    ): JSONObject {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (contentType != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
            }
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        return try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.readText().orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(parseErrorMessage(responseText, connection.responseMessage))
            }

            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun requestArray(
        path: String,
        method: String,
        bearerToken: String? = null,
    ): JSONArray {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.readText().orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(parseErrorMessage(responseText, connection.responseMessage))
            }

            JSONArray(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun requestWithoutBody(
        path: String,
        method: String,
        bearerToken: String? = null,
    ) {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }

        try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream?.readText().orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(parseErrorMessage(responseText, connection.responseMessage))
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun InputStream.readText(): String =
        BufferedReader(InputStreamReader(this)).use { reader ->
            reader.readText()
        }

    private fun parseErrorMessage(responseText: String, fallback: String): String {
        return try {
            val json = JSONObject(responseText)
            json.optString("detail").takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            responseText.ifBlank { fallback }
        }
    }

    private fun JSONObject.toUserProfile(): UserProfile {
        return UserProfile(
            id = getInt("id"),
            username = getString("username"),
            phone = if (isNull("phone")) null else getString("phone"),
            avatar = if (isNull("avatar")) null else getString("avatar"),
            isMaster = getBoolean("is_master"),
            masterProfile = optJSONObject("master_profile")?.toMasterProfile(),
        )
    }

    private fun JSONObject.toMasterProfile(): MasterProfile {
        val categoryObjects = optJSONArray("categories")
        val categories = buildList {
            if (categoryObjects != null) {
                for (index in 0 until categoryObjects.length()) {
                    add(categoryObjects.getJSONObject(index).getString("category"))
                }
            }
        }
        return MasterProfile(
            id = getInt("id"),
            userId = if (isNull("user_id")) null else getInt("user_id"),
            name = if (isNull("name")) null else getString("name"),
            serviceCategory = if (isNull("service_category")) null else getString("service_category"),
            serviceCategories = categories,
            address = if (isNull("address")) null else getString("address"),
            description = if (isNull("description")) null else getString("description"),
        )
    }

    private fun JSONArray.toMasterServices(): List<MasterService> {
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toMasterService())
            }
        }
    }

    private fun JSONArray.toUsers(): List<UserProfile> {
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toUserProfile())
            }
        }
    }

    private fun JSONArray.toMasterAvailabilitySlots(): List<MasterAvailabilitySlot> {
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toMasterAvailabilitySlot())
            }
        }
    }

    private fun JSONArray.toMasterWorks(): List<MasterWork> {
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toMasterWork())
            }
        }
    }

    private fun JSONArray.toBookings(): List<Booking> {
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toBooking())
            }
        }
    }

    private fun JSONArray.toNotifications(): List<UserNotification> {
        return buildList(length()) {
            for (index in 0 until length()) {
                add(getJSONObject(index).toUserNotification())
            }
        }
    }

    private fun JSONObject.toMasterService(): MasterService {
        return MasterService(
            id = getInt("id"),
            masterId = getInt("master_id"),
            category = getString("category"),
            name = getString("name"),
            price = getInt("price"),
            durationMinutes = getInt("duration_minutes"),
        )
    }

    private fun JSONObject.toMasterAvailabilitySlot(): MasterAvailabilitySlot {
        return MasterAvailabilitySlot(
            id = getInt("id"),
            masterId = getInt("master_id"),
            availableAt = getString("available_at"),
        )
    }

    private fun JSONObject.toMasterWork(): MasterWork {
        return MasterWork(
            id = getInt("id"),
            masterId = getInt("master_id"),
            photoUrl = getString("photo_url"),
        )
    }

    private fun JSONObject.toBooking(): Booking {
        return Booking(
            id = getInt("id"),
            clientUserId = getInt("client_user_id"),
            clientUsername = getString("client_username"),
            clientPhone = if (isNull("client_phone")) null else getString("client_phone"),
            masterId = getInt("master_id"),
            masterUserId = getInt("master_user_id"),
            serviceId = getInt("service_id"),
            serviceName = getString("service_name"),
            masterUsername = getString("master_username"),
            scheduledAt = getString("scheduled_at"),
            status = getString("status"),
        )
    }

    private fun JSONObject.toUserNotification(): UserNotification {
        return UserNotification(
            id = getInt("id"),
            userId = getInt("user_id"),
            title = getString("title"),
            message = getString("message"),
            isRead = getBoolean("is_read"),
            createdAt = getString("created_at"),
        )
    }
}
