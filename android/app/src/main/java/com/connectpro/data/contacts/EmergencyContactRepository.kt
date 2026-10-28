package com.connectpro.data.contacts

import com.connectpro.data.network.ApiClient
import retrofit2.HttpException

class EmergencyContactRepository(
    apiClient: ApiClient
) {
    private val api: EmergencyContactApi =
        apiClient.authenticatedRetrofitInstance
            .create(EmergencyContactApi::class.java)

    suspend fun getAll(): List<EmergencyContactResponse> {
        return api.getAll()
    }

    suspend fun create(
        name: String,
        phoneNumber: String,
        relationship: String
    ): EmergencyContactResponse {
        return api.create(
            CreateEmergencyContactRequest(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                relationship = relationship.trim()
            )
        )
    }

    suspend fun update(
        contactId: String,
        name: String,
        phoneNumber: String,
        relationship: String
    ): EmergencyContactResponse {
        return api.update(
            contactId = contactId,
            request = UpdateEmergencyContactRequest(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                relationship = relationship.trim()
            )
        )
    }

    suspend fun delete(contactId: String) {
        val response = api.delete(contactId)

        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    suspend fun activate(
        contactId: String
    ): EmergencyContactResponse {
        return api.activate(contactId)
    }

    suspend fun deactivate(
        contactId: String
    ): EmergencyContactResponse {
        return api.deactivate(contactId)
    }
}
