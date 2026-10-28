package com.connectpro.data.contacts

data class CreateEmergencyContactRequest(
    val name: String,
    val phoneNumber: String,
    val relationship: String
)

data class UpdateEmergencyContactRequest(
    val name: String,
    val phoneNumber: String,
    val relationship: String
)

data class EmergencyContactResponse(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
