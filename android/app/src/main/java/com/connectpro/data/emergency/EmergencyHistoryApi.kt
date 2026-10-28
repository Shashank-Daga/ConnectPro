package com.connectpro.data.emergency

import retrofit2.http.GET

interface EmergencyHistoryApi {

    @GET("api/v1/emergencies")
    suspend fun getEmergencyHistory(): List<EmergencyIncidentResponse>
}
