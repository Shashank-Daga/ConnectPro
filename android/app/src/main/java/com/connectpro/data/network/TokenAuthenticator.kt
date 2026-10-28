package com.connectpro.data.network

import com.connectpro.data.auth.RefreshTokenRequest
import com.connectpro.data.session.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        synchronized(sessionManager) {
            val failedToken = response.request
                .header("Authorization")
                ?.removePrefix("Bearer ")

            val currentToken = sessionManager.accessToken()

            // Another request may already have refreshed the session.
            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = sessionManager.refreshToken()
                ?: run {
                    sessionManager.clear()
                    return null
                }

            val refreshResponse = try {
                authApi.refresh(
                    RefreshTokenRequest(refreshToken)
                ).execute()
            } catch (_: IOException) {
                // Do not erase a potentially recoverable session just
                // because the network is temporarily unavailable.
                return null
            } catch (_: Exception) {
                return null
            }

            if (!refreshResponse.isSuccessful) {
                if (refreshResponse.code() == 401 ||
                    refreshResponse.code() == 403
                ) {
                    sessionManager.clear()
                }
                return null
            }

            val newTokens = refreshResponse.body() ?: return null

            try {
                sessionManager.saveTokens(newTokens)
            } catch (_: Exception) {
                sessionManager.clear()
                return null
            }

            return response.request.newBuilder()
                .header(
                    "Authorization",
                    "${newTokens.tokenType} ${newTokens.accessToken}"
                )
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse

        while (prior != null) {
            count++
            prior = prior.priorResponse
        }

        return count
    }
}
