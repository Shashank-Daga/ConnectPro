package com.connectpro.data.profile

data class UserProfileResponse(
    val id: String,
    val phoneNumber: String,
    val email: String,
    val status: String,
    val phoneVerifiedAt: String?,
    val emailVerifiedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
