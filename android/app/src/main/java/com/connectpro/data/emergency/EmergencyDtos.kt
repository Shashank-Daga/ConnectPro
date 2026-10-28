package com.connectpro.data.emergency

data class CreateEmergencyIncidentRequest(
    val category: String
)

data class TrustedContactNotificationResponse(
    val id: String,
    val contactName: String,
    val maskedPhoneNumber: String,
    val status: String,
    val deliveryAttempts: Int,
    val sentAt: String?,
    val createdAt: String
)

data class SaveLocationSnapshotRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val capturedAt: String
)

data class LocationSnapshotResponse(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val capturedAt: String
)
