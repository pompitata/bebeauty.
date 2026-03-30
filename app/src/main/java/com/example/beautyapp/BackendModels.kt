package com.example.beautyapp

data class AuthToken(
    val accessToken: String,
    val tokenType: String,
)

data class UserProfile(
    val id: Int,
    val username: String,
    val phone: String?,
    val avatar: String?,
    val isMaster: Boolean,
    val masterProfile: MasterProfile?,
)

data class ProfileUpdateResult(
    val user: UserProfile,
    val accessToken: String,
    val tokenType: String,
)

data class MasterProfile(
    val id: Int,
    val userId: Int?,
    val name: String?,
    val serviceCategory: String?,
    val serviceCategories: List<String>,
    val address: String?,
    val description: String?,
)

data class MasterService(
    val id: Int,
    val masterId: Int,
    val category: String,
    val name: String,
    val price: Int,
    val durationMinutes: Int,
)

data class MasterAvailabilitySlot(
    val id: Int,
    val masterId: Int,
    val availableAt: String,
)

data class MasterWork(
    val id: Int,
    val masterId: Int,
    val photoUrl: String,
)

data class Booking(
    val id: Int,
    val clientUserId: Int,
    val clientUsername: String,
    val clientPhone: String?,
    val masterId: Int,
    val masterUserId: Int,
    val serviceId: Int,
    val serviceName: String,
    val masterUsername: String,
    val scheduledAt: String,
    val status: String,
)

data class UserNotification(
    val id: Int,
    val userId: Int,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
)
