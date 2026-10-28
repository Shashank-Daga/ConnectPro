package com.connectpro.data.profile

import retrofit2.http.GET

interface UserProfileApi {

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): UserProfileResponse
}
