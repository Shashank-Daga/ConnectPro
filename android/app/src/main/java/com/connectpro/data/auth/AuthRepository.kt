package com.connectpro.data.auth

import com.connectpro.data.network.ApiClient
import com.connectpro.data.network.ApiError
import com.connectpro.data.network.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiClient: ApiClient
) {
    private val api get() = apiClient.authApi
    private val sessionManager get() = apiClient.sessionManager

    suspend fun register(
        phoneNumber: String,
        email: String,
        pin: String
    ) {
        val response = api.register(
            RegisterRequest(
                phoneNumber = phoneNumber.trim(),
                email = email.trim().lowercase(),
                pin = pin
            )
        )
        ApiError.requireSuccess(response)
    }

    suspend fun verifyPhone(
        phoneNumber: String,
        verificationCode: String
    ) {
        val response = api.verifyPhone(
            VerifyPhoneRequest(
                phoneNumber = phoneNumber.trim(),
                verificationCode = verificationCode.trim()
            )
        )
        ApiError.requireSuccess(response)
    }

    suspend fun verifyEmail(
        email: String,
        verificationCode: String
    ) {
        val response = api.verifyEmail(
            VerifyEmailRequest(
                email = email.trim().lowercase(),
                verificationCode = verificationCode.trim()
            )
        )
        ApiError.requireSuccess(response)
    }

    suspend fun login(
        phoneNumber: String,
        pin: String
    ) {
        val response = api.login(
            LoginRequest(
                phoneNumber = phoneNumber.trim(),
                pin = pin
            )
        )

        ApiError.requireSuccess(response)

        val tokens = response.body()
            ?: throw ApiException("The server returned no session tokens.")

        sessionManager.saveTokens(tokens)
    }

    suspend fun refreshSession() {
        withContext(Dispatchers.IO) {
            val refreshToken = sessionManager.refreshToken()
                ?: throw ApiException("Please sign in again.")

            val response = api.refresh(
                RefreshTokenRequest(refreshToken)
            ).execute()

            ApiError.requireSuccess(response)

            val tokens = response.body()
                ?: throw ApiException("The server returned no session tokens.")

            sessionManager.saveTokens(tokens)
        }
    }

    suspend fun requestPinReset(identifier: String) {
        val response = api.requestPinReset(
            RequestPinResetRequest(identifier.trim())
        )
        ApiError.requireSuccess(response)
    }

    suspend fun resetPin(
        identifier: String,
        verificationCode: String,
        newPin: String
    ) {
        val response = api.resetPin(
            ResetPinRequest(
                identifier = identifier.trim(),
                verificationCode = verificationCode.trim(),
                newPin = newPin
            )
        )
        ApiError.requireSuccess(response)

        // The backend revokes existing sessions when the PIN changes.
        sessionManager.clear()
    }

    suspend fun logout() {
        try {
            if (sessionManager.accessToken() != null) {
                val response = apiClient.authenticatedAuthApi.logout()
                ApiError.requireSuccess(response)
            }
        } finally {
            sessionManager.clear()
        }
    }
}
