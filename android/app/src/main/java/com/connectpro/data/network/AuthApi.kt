package com.connectpro.data.network

import com.connectpro.data.auth.AuthTokenResponse
import com.connectpro.data.auth.LoginRequest
import com.connectpro.data.auth.RefreshTokenRequest
import com.connectpro.data.auth.RegisterRequest
import com.connectpro.data.auth.RequestPinResetRequest
import com.connectpro.data.auth.ResetPinRequest
import com.connectpro.data.auth.VerifyEmailRequest
import com.connectpro.data.auth.VerifyPhoneRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Unit>

    @POST("api/v1/auth/verify/phone")
    suspend fun verifyPhone(
        @Body request: VerifyPhoneRequest
    ): Response<Unit>

    @POST("api/v1/auth/verify/email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): Response<Unit>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthTokenResponse>

    // A synchronous Call is required by OkHttp's Authenticator.
    // The caller must execute it off the main thread.
    @POST("api/v1/auth/refresh")
    fun refresh(
        @Body request: RefreshTokenRequest
    ): Call<AuthTokenResponse>

    @POST("api/v1/auth/pin-reset/request")
    suspend fun requestPinReset(
        @Body request: RequestPinResetRequest
    ): Response<Unit>

    @POST("api/v1/auth/pin-reset")
    suspend fun resetPin(
        @Body request: ResetPinRequest
    ): Response<Unit>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
