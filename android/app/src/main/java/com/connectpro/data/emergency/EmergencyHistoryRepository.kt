package com.connectpro.data.emergency

import com.connectpro.data.network.ApiClient

class EmergencyHistoryRepository(
    apiClient: ApiClient
) {
    private val api = apiClient.authenticatedRetrofitInstance
        .create(EmergencyHistoryApi::class.java)

    suspend fun getEmergencyHistory(): List<EmergencyIncidentResponse> {
        return api.getEmergencyHistory()
    }
}
