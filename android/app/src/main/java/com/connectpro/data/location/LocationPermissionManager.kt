package com.connectpro.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class LocationPermissionManager(context: Context) {

    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun hasLocationPermission(): Boolean {
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return coarseGranted || fineGranted
    }

    fun hasCompletedInitialRequest(): Boolean {
        return preferences.getBoolean(
            KEY_INITIAL_REQUEST_COMPLETED,
            false
        )
    }

    fun markInitialRequestCompleted() {
        preferences.edit()
            .putBoolean(KEY_INITIAL_REQUEST_COMPLETED, true)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME =
            "connectpro_location_permissions"

        private const val KEY_INITIAL_REQUEST_COMPLETED =
            "initial_location_request_completed"
    }
}
