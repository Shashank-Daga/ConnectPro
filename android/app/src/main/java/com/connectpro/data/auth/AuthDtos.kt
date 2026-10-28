package com.connectpro.data.auth

data class RegisterRequest(
    val phoneNumber: String,
    val email: String,
    val pin: String
)

data class LoginRequest(
    val phoneNumber: String,
    val pin: String
)

data class VerifyPhoneRequest(
    val phoneNumber: String,
    val verificationCode: String
)

data class VerifyEmailRequest(
    val email: String,
    val verificationCode: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RequestPinResetRequest(
    val identifier: String
)

data class ResetPinRequest(
    val identifier: String,
    val verificationCode: String,
    val newPin: String
)

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)
