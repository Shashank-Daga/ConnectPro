package com.connectpro.data.emergency

data class EmergencyIncidentResponse(
    val id: String,
    val category: String,
    val status: String,
    val triggeredAt: String,
    val resolvedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
