package com.connectpro.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtMillis: Long
)

sealed interface LocationResult {
    data class Success(val location: CurrentLocation) : LocationResult
    data object PermissionDenied : LocationResult
    data object ServicesDisabled : LocationResult
    data object ProviderUnavailable : LocationResult
    data object TimedOut : LocationResult
    data object Failed : LocationResult
}

class LocationProvider(context: Context) {

    private val appContext = context.applicationContext

    suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.PermissionDenied
        }

        val manager = appContext.getSystemService(
            Context.LOCATION_SERVICE
        ) as? LocationManager ?: return LocationResult.Failed

        return try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                return LocationResult.ServicesDisabled
            }

            val provider = when {
                manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) -> LocationManager.NETWORK_PROVIDER

                manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) -> LocationManager.GPS_PROVIDER

                else -> return LocationResult.ProviderUnavailable
            }

            val location = withTimeout(15_000L) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cancellationSignal = CancellationSignal()

                    continuation.invokeOnCancellation {
                        cancellationSignal.cancel()
                    }

                    try {
                        LocationManagerCompat.getCurrentLocation(
                            manager,
                            provider,
                            cancellationSignal,
                            ContextCompat.getMainExecutor(appContext),
                            Consumer { result ->
                                if (continuation.isActive) {
                                    continuation.resume(result)
                                }
                            }
                        )
                    } catch (exception: SecurityException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    } catch (exception: RuntimeException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }

            if (location == null) {
                LocationResult.Failed
            } else {
                LocationResult.Success(
                    CurrentLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = if (location.hasAccuracy()) {
                            location.accuracy
                        } else {
                            null
                        },
                        capturedAtMillis = location.time
                    )
                )
            }
        } catch (exception: TimeoutCancellationException) {
            LocationResult.TimedOut
        } catch (exception: SecurityException) {
            LocationResult.PermissionDenied
        } catch (exception: RuntimeException) {
            LocationResult.Failed
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }
}
