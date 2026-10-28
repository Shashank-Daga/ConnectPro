package com.connectpro.data.network

import org.json.JSONObject
import retrofit2.Response

class ApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)

object ApiError {

    fun <T> requireSuccess(response: Response<T>) {
        if (response.isSuccessful) return

        val rawBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        val message = extractMessage(rawBody)
            ?: when (response.code()) {
                400 -> "Please check the information you entered."
                401 -> "Your session is invalid or has expired."
                403 -> "You are not allowed to perform this action."
                404 -> "The requested resource was not found."
                409 -> "This request conflicts with existing account information."
                429 -> "Too many attempts. Please try again later."
                in 500..599 -> "The server is temporarily unavailable."
                else -> "The request failed. Please try again."
            }

        throw ApiException(message, response.code())
    }

    private fun extractMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null

        return try {
            val json = JSONObject(body)
            listOf("message", "detail", "error", "title")
                .firstNotNullOfOrNull { key ->
                    json.optString(key)
                        .takeIf { it.isNotBlank() && it != "null" }
                }
        } catch (_: Exception) {
            null
        }
    }
}
