
package com.connectpro.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.connectpro.data.location.LocationProvider
import com.connectpro.data.location.LocationResult
import com.connectpro.data.location.LocationPermissionManager
import com.connectpro.ui.theme.ConnectProSpacing
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun LocationPermissionCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationProvider = remember(context) {
        LocationProvider(context.applicationContext)
    }

    val permissionManager = remember(context) {
        LocationPermissionManager(context.applicationContext)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var locationResult by remember {
        mutableStateOf<LocationResult?>(null)
    }

    var permissionGranted by remember {
        mutableStateOf(permissionManager.hasLocationPermission())
    }

    // Refresh permission state when the user returns from Android Settings.
    val lifecycleOwner = context as? LifecycleOwner

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted =
                    permissionManager.hasLocationPermission()
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    fun retrieveLocation() {
        if (!permissionManager.hasLocationPermission()) {
            permissionGranted = false
            locationResult = LocationResult.PermissionDenied
            return
        }

        scope.launch {
            isLoading = true
            locationResult = null

            try {
                locationResult = locationProvider.getCurrentLocation()
                permissionGranted =
                    permissionManager.hasLocationPermission()
            } finally {
                isLoading = false
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ConnectProSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Medium
            )
        ) {
            Text(
                text = "Location access",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "ConnectPro uses your location only when required " +
                        "for safety features. Your location is not " +
                        "continuously tracked in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (permissionGranted) {
                    "Location permission is enabled."
                } else {
                    "Location permission is currently not enabled. " +
                            "You can grant it when sharing your location " +
                            "during an emergency."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (permissionGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        ConnectProSpacing.Medium
                    )
                ) {
                    CircularProgressIndicator()

                    Text("Getting your current location…")
                }
            }

            when (val result = locationResult) {
                is LocationResult.Success -> {
                    Text(
                        text = "Location retrieved successfully",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Latitude: ${
                            result.location.latitude
                        }\nLongitude: ${
                            result.location.longitude
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    result.location.accuracyMeters?.let { accuracy ->
                        Text(
                            text = "Reported accuracy: ${
                                accuracy.toInt()
                            } metres",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "Captured: ${
                            DateFormat.getDateTimeInstance().format(
                                Date(result.location.capturedAtMillis)
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LocationResult.PermissionDenied -> {
                    Text(
                        text = "Location permission was not granted.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.ServicesDisabled -> {
                    Text(
                        text = "Location services are turned off. " +
                                "Enable Location in your device settings.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.ProviderUnavailable -> {
                    Text(
                        text = "No location provider is currently available.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.TimedOut -> {
                    Text(
                        text = "Location retrieval timed out. " +
                                "Try again in an area with better reception.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.Failed -> {
                    Text(
                        text = "Could not retrieve your location.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                null -> Unit
            }

            if (permissionGranted) {
                Button(
                    onClick = {
                        retrieveLocation()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get my current location")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts(
                                "package",
                                context.packageName,
                                null
                            )
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open app settings")
                }
            }
        }
    }
}
