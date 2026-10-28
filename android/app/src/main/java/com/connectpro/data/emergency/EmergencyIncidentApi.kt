package com.connectpro.data.emergency

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EmergencyIncidentApi {

    @POST("api/v1/emergencies")
    suspend fun createIncident(
        @Body request: CreateEmergencyIncidentRequest
    ): EmergencyIncidentResponse

    @POST("api/v1/emergencies/{incidentId}/activate")
    suspend fun activateIncident(
        @Path("incidentId") incidentId: String
    ): EmergencyIncidentResponse

    @POST("api/v1/emergencies/{incidentId}/location")
    suspend fun saveLocationSnapshot(
        @Path("incidentId") incidentId: String,
        @Body request: SaveLocationSnapshotRequest
    ): LocationSnapshotResponse

    @GET("api/v1/emergencies/{incidentId}/location")
    suspend fun getLocationSnapshot(
        @Path("incidentId") incidentId: String
    ): LocationSnapshotResponse

    @GET("api/v1/emergencies/{incidentId}/notifications")
    suspend fun getNotificationsForIncident(
        @Path("incidentId") incidentId: String
    ): List<TrustedContactNotificationResponse>

    @GET("api/v1/emergencies/{incidentId}")
    suspend fun getIncident(
        @Path("incidentId") incidentId: String
    ): EmergencyIncidentResponse
}
