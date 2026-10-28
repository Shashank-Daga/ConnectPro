package com.connectpro.data.network

import android.content.Context

import com.connectpro.BuildConfig
import com.connectpro.data.session.SessionManager

import okhttp3.OkHttpClient

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(
    context: Context,
//    baseUrl: String = "http://10.0.2.2:8080/"
    baseUrl: String = BuildConfig.API_BASE_URL
) {
    val sessionManager = SessionManager(context.applicationContext)

    private val unauthenticatedClient = OkHttpClient.Builder()
        .build()

    private val unauthenticatedRetrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(unauthenticatedClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Used for register, verification, login, refresh, and PIN reset.
    val authApi: AuthApi =
        unauthenticatedRetrofit.create(AuthApi::class.java)

    private val authenticatedClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val accessToken = sessionManager.accessToken()

            val request = if (accessToken.isNullOrBlank()) {
                originalRequest
            } else {
                originalRequest.newBuilder()
                    .header(
                        "Authorization",
                        "${sessionManager.tokenType()} $accessToken"
                    )
                    .build()
            }

            chain.proceed(request)
        }
        .authenticator(
            TokenAuthenticator(sessionManager, authApi)
        )
        .build()

    private val authenticatedRetrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(authenticatedClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Use this service for protected endpoints such as logout.
    val authenticatedAuthApi: AuthApi =
        authenticatedRetrofit.create(AuthApi::class.java)

    val authenticatedRetrofitInstance: Retrofit
        get() = authenticatedRetrofit
}
