package com.connectpro.data.emergency

import com.connectpro.data.location.CurrentLocation
import com.connectpro.data.network.ApiClient
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class EmergencyIncidentRepository(apiClient: ApiClient) {
    private val api = apiClient.authenticatedRetrofitInstance
        .create(EmergencyIncidentApi::class.java)

    suspend fun createIncident(
        category: String
    ): EmergencyIncidentResponse {
        return api.createIncident(
            CreateEmergencyIncidentRequest(category)
        )
    }

    suspend fun activateIncident(
        incidentId: String
    ): EmergencyIncidentResponse {
        return api.activateIncident(incidentId)
    }

    suspend fun saveLocationSnapshot(
        incidentId: String,
        location: CurrentLocation
    ): LocationSnapshotResponse {
        val capturedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(location.capturedAtMillis)
                .atOffset(ZoneOffset.UTC)
        )

        return api.saveLocationSnapshot(
            incidentId = incidentId,
            request = SaveLocationSnapshotRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracyMeters?.toDouble(),
                capturedAt = capturedAt
            )
        )
    }

    suspend fun getLocationSnapshot(
        incidentId: String
    ): LocationSnapshotResponse {
        return api.getLocationSnapshot(incidentId)
    }

    suspend fun getNotificationsForIncident(
        incidentId: String
    ): List<TrustedContactNotificationResponse> {
        return api.getNotificationsForIncident(incidentId)
    }

    suspend fun getIncident(
        incidentId: String
    ): EmergencyIncidentResponse {
        return api.getIncident(incidentId)
    }
}
