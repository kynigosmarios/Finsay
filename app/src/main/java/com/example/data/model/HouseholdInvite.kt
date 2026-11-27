package com.example.data.model

data class HouseholdInvite(
    val code: String = "",
    val url: String = "",
    val householdId: String = "",
    val householdName: String = "",
    val ownerId: String = "",
    val role: String = HouseholdRole.REGISTAR.key,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
)
