
package com.connectpro.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.connectpro.data.location.LocationProvider
import com.connectpro.data.location.LocationResult
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

    var isLoading by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var locationResult by remember {
        mutableStateOf<LocationResult?>(null)
    }
    var permissionGranted by remember {
        mutableStateOf(hasLocationPermission(context))
    }

    fun retrieveLocation() {
        scope.launch {
            isLoading = true
            locationResult = null

            try {
                locationResult = locationProvider.getCurrentLocation()
                permissionGranted = hasLocationPermission(context)
            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissionGranted = hasLocationPermission(context)
            permissionDenied = !permissionGranted

            if (permissionGranted) {
                retrieveLocation()
            } else {
                locationResult = LocationResult.PermissionDenied
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
                text = "ConnectPro can use your current location to " +
                        "support safety features. Location is retrieved " +
                        "only when you request it here. This E1 feature " +
                        "does not upload or save your coordinates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = "Location permission was not granted. " +
                                "You can allow approximate or precise " +
                                "location to use this feature.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.ServicesDisabled -> {
                    Text(
                        text = "Location services are turned off. " +
                                "Enable Location in your device settings " +
                                "and try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.ProviderUnavailable -> {
                    Text(
                        text = "No location provider is currently " +
                                "available. Check your device settings " +
                                "and try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.TimedOut -> {
                    Text(
                        text = "Location retrieval timed out. Move " +
                                "to an area with better reception and try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LocationResult.Failed -> {
                    Text(
                        text = "Could not retrieve your location. " +
                                "Check location settings and try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                null -> Unit
            }

            Button(
                onClick = {
                    permissionGranted = hasLocationPermission(context)

                    if (permissionGranted) {
                        permissionDenied = false
                        retrieveLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (permissionGranted) {
                        "Get my current location"
                    } else {
                        "Allow location and continue"
                    }
                )
            }

            if (permissionDenied) {
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

private fun hasLocationPermission(
    context: android.content.Context
): Boolean {
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return coarseGranted || fineGranted
}
