package com.connectpro.data.profile

import com.connectpro.data.network.ApiClient

class UserProfileRepository(
    apiClient: ApiClient
) {
    private val api = apiClient.authenticatedRetrofitInstance
        .create(UserProfileApi::class.java)

    suspend fun getCurrentUser(): UserProfileResponse {
        return api.getCurrentUser()
    }
}
