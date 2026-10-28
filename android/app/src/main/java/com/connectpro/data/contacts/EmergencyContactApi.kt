package com.connectpro.data.contacts

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface EmergencyContactApi {

    @GET("api/v1/emergency-contacts")
    suspend fun getAll(): List<EmergencyContactResponse>

    @POST("api/v1/emergency-contacts")
    suspend fun create(
        @Body request: CreateEmergencyContactRequest
    ): EmergencyContactResponse

    @PATCH("api/v1/emergency-contacts/{contactId}")
    suspend fun update(
        @Path("contactId") contactId: String,
        @Body request: UpdateEmergencyContactRequest
    ): EmergencyContactResponse

    @DELETE("api/v1/emergency-contacts/{contactId}")
    suspend fun delete(
        @Path("contactId") contactId: String
    ): Response<Unit>

    @POST("api/v1/emergency-contacts/{contactId}/activate")
    suspend fun activate(
        @Path("contactId") contactId: String
    ): EmergencyContactResponse

    @POST("api/v1/emergency-contacts/{contactId}/deactivate")
    suspend fun deactivate(
        @Path("contactId") contactId: String
    ): EmergencyContactResponse
}
