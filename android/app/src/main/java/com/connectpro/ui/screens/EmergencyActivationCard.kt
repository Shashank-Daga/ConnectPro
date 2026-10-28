
package com.connectpro.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.connectpro.data.emergency.EmergencyIncidentRepository
import com.connectpro.data.location.LocationProvider
import com.connectpro.data.location.LocationResult
import com.connectpro.data.network.ApiClient
import com.connectpro.ui.theme.ConnectProSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException

private val emergencyCategories = listOf(
    "MEDICAL" to "Medical",
    "ACCIDENT" to "Accident",
    "PERSONAL_THREAT" to "Personal threat",
    "FIRE" to "Fire",
    "CRIME" to "Crime",
    "MISSING_PERSON" to "Missing person",
    "OTHER" to "Other"
)

@Composable
fun EmergencyActivationCard() {
    val context = LocalContext.current

    val repository = remember(context) {
        EmergencyIncidentRepository(
            ApiClient(context.applicationContext)
        )
    }

    val locationProvider = remember(context) {
        LocationProvider(context.applicationContext)
    }

    val scope = rememberCoroutineScope()

    var showConfirmation by rememberSaveable {
        mutableStateOf(false)
    }
    var selectedCategory by rememberSaveable {
        mutableStateOf("PERSONAL_THREAT")
    }
    var shareLocation by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingShareLocation by rememberSaveable {
        mutableStateOf(false)
    }

    // Loading represents an in-flight request, so do not restore it.
    var isLoading by remember {
        mutableStateOf(false)
    }
    var incidentId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var isActive by rememberSaveable {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var locationStatusMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun startEmergency(shouldShareLocation: Boolean) {
        if (isLoading || incidentId != null) return

        scope.launch {
            isLoading = true
            errorMessage = null
            locationStatusMessage = null

            try {
                val created = repository.createIncident(
                    selectedCategory
                )
                incidentId = created.id

                if (shouldShareLocation) {
                    val locationResult = try {
                        locationProvider.getCurrentLocation()
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        LocationResult.Failed
                    }

                    when (locationResult) {
                        is LocationResult.Success -> {
                            try {
                                repository.saveLocationSnapshot(
                                    incidentId = created.id,
                                    location = locationResult.location
                                )
                                locationStatusMessage =
                                    "Initial emergency location shared successfully."
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                // The upload might have reached the server
                                // even when its response was lost.
                                val snapshotExists = try {
                                    repository.getLocationSnapshot(
                                        created.id
                                    )
                                    true
                                } catch (
                                    verificationCancellation: CancellationException
                                ) {
                                    throw verificationCancellation
                                } catch (_: Exception) {
                                    false
                                }

                                locationStatusMessage = when {
                                    snapshotExists ->
                                        "The initial location snapshot is saved, although the upload response was not confirmed."

                                    exception is HttpException &&
                                            exception.code() in 400..499 ->
                                        "The server rejected the location snapshot. Your incident can still be activated."

                                    else ->
                                        "Location sharing could not be confirmed. Your incident can still be activated."
                                }
                            }
                        }

                        LocationResult.PermissionDenied -> {
                            locationStatusMessage =
                                "Location was not shared because permission was not granted."
                        }

                        LocationResult.ServicesDisabled -> {
                            locationStatusMessage =
                                "Location was not shared because device location services are turned off."
                        }

                        LocationResult.ProviderUnavailable -> {
                            locationStatusMessage =
                                "Location was not shared because no location provider was available."
                        }

                        LocationResult.TimedOut -> {
                            locationStatusMessage =
                                "Location retrieval timed out. Your incident can still be activated without location."
                        }

                        LocationResult.Failed -> {
                            locationStatusMessage =
                                "Location could not be retrieved. Your incident can still be activated without location."
                        }
                    }
                } else {
                    locationStatusMessage =
                        "Location was not shared. You did not opt in to location sharing."
                }

                // Location sharing is optional and must not prevent activation.
                val activated = repository.activateIncident(created.id)

                if (activated.status == "ACTIVE") {
                    isActive = true
                    errorMessage = null
                } else {
                    errorMessage =
                        "Incident created, but activation is not confirmed. Use Retry activation."
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // If activation succeeded but its response was lost, reconcile
                // the existing incident rather than creating another one.
                val currentIncidentId = incidentId

                val serverStatus = if (currentIncidentId != null) {
                    try {
                        repository.getIncident(currentIncidentId).status
                    } catch (
                        verificationCancellation: CancellationException
                    ) {
                        throw verificationCancellation
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }

                when (serverStatus) {
                    "ACTIVE" -> {
                        isActive = true
                        errorMessage = null
                    }

                    "CREATED" -> {
                        errorMessage =
                            "Incident created, but activation could not be confirmed. Use Retry activation."
                    }

                    "RESOLVED", "CANCELLED" -> {
                        errorMessage =
                            "The incident is $serverStatus and cannot be activated."
                    }

                    else -> {
                        errorMessage = if (currentIncidentId != null) {
                            "Incident created, but activation could not be confirmed. Use Retry activation."
                        } else {
                            "The request could not be confirmed. Check Activity history before trying again."
                        }
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun retryActivation() {
        val currentIncidentId = incidentId ?: return
        if (isLoading || isActive) return

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Always retrieve the current server state first.
                val currentIncident = repository.getIncident(
                    currentIncidentId
                )

                when (currentIncident.status) {
                    "ACTIVE" -> {
                        isActive = true
                    }

                    "CREATED" -> {
                        try {
                            val response = repository.activateIncident(
                                currentIncidentId
                            )

                            if (response.status == "ACTIVE") {
                                isActive = true
                            } else {
                                errorMessage =
                                    "The server returned status ${response.status}. Activation is not confirmed."
                            }
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (_: Exception) {
                            // Activation may have succeeded even though the
                            // response was lost. Recheck the same incident.
                            val reconciledStatus = try {
                                repository.getIncident(
                                    currentIncidentId
                                ).status
                            } catch (
                                verificationCancellation: CancellationException
                            ) {
                                throw verificationCancellation
                            } catch (_: Exception) {
                                null
                            }

                            if (reconciledStatus == "ACTIVE") {
                                isActive = true
                            } else {
                                errorMessage =
                                    "Activation could not be confirmed. Retry using the same incident."
                            }
                        }
                    }

                    else -> {
                        errorMessage =
                            "The incident is ${currentIncident.status}. It cannot be activated from its current state."
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                errorMessage =
                    "The incident status could not be retrieved. Check your connection and retry."
            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Continue even if permission is denied; the location provider
        // checks the actual permission state.
        startEmergency(pendingShareLocation)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(ConnectProSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Medium
            )
        ) {
            Text(
                text = "Emergency assistance",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when {
                    isActive ->
                        "Your emergency incident is active. Incident reference: ${incidentId.orEmpty()}"

                    incidentId != null ->
                        "Your incident was created, but activation has not been confirmed. Retry activation to continue."

                    else ->
                        "Use this only when you need to raise an emergency incident."
                },
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
                    Text("Processing emergency request…")
                }
            } else if (isActive) {
                Text(
                    text = "Incident status: ACTIVE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (incidentId != null) {
                Button(
                    onClick = { retryActivation() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry activation")
                }
            } else {
                Button(
                    onClick = {
                        errorMessage = null
                        showConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request emergency assistance")
                }
            }

            locationStatusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isActive) {
                Text(
                    text = "This confirms the incident is active in ConnectPro. It does not confirm SMS delivery or contact with emergency services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            incidentId
                ?.takeIf { isActive }
                ?.let { activeIncidentId ->
                    EmergencyNotificationStatusCard(
                        incidentId = activeIncidentId
                    )
                }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) showConfirmation = false
            },
            title = {
                Text("Confirm emergency")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        ConnectProSpacing.Small
                    )
                ) {
                    Text(
                        "Choose the incident category. Continuing will create and activate an emergency incident."
                    )

                    emergencyCategories.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == code,
                                onClick = {
                                    selectedCategory = code
                                },
                                enabled = !isLoading
                            )

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = shareLocation,
                            onCheckedChange = {
                                shareLocation = it
                            },
                            enabled = !isLoading
                        )

                        Text(
                            text = "Share my current location with this emergency",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = "If selected, ConnectPro will request your location permission if needed and send one location snapshot for this incident. No background tracking is started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        showConfirmation = false

                        if (shareLocation && !hasLocationPermission(context)) {
                            pendingShareLocation = true
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        } else {
                            pendingShareLocation = shareLocation
                            startEmergency(shareLocation)
                        }
                    }
                ) {
                    Text("Confirm and activate")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        showConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun hasLocationPermission(
    context: Context
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
