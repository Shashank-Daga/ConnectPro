
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
import androidx.compose.runtime.LaunchedEffect
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
import com.connectpro.data.emergency.EmergencyHistoryRepository
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

    val historyRepository = remember(context) {
        EmergencyHistoryRepository(
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

    var showCancelConfirmation by rememberSaveable {
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

    // Restore the current incident from the backend whenever Home is opened.
    var isRestoring by remember {
        mutableStateOf(true)
    }

    var canStartEmergency by remember {
        mutableStateOf(false)
    }

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

    // Reset the local UI after the backend confirms cancellation.
    fun completeCancellation() {
        incidentId = null
        isActive = false
        canStartEmergency = true

        showCancelConfirmation = false

        errorMessage = null

        locationStatusMessage =
            "Your emergency request has been cancelled successfully."
    }

    // Restore the latest unresolved incident from the backend.
    fun refreshIncidentState() {
        if (isLoading) return

        scope.launch {
            isRestoring = true
            canStartEmergency = false
            errorMessage = null

            try {
                val currentIncident = historyRepository
                    .getEmergencyHistory()
                    .firstOrNull {
                        it.status == "ACTIVE" || it.status == "CREATED"
                    }

                if (currentIncident != null) {
                    incidentId = currentIncident.id
                    isActive = currentIncident.status == "ACTIVE"

                    locationStatusMessage = if (isActive) {
                        "Your active emergency request was restored from your account."
                    } else {
                        null
                    }

                    canStartEmergency = false
                } else {
                    incidentId = null
                    isActive = false
                    locationStatusMessage = null
                    canStartEmergency = true
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (_: Exception) {
                canStartEmergency = false

                errorMessage =
                    "Unable to check your existing emergency request. Please retry the status check before creating another request."

            } finally {
                isRestoring = false
            }
        }
    }

    LaunchedEffect(historyRepository) {
        refreshIncidentState()
    }

    // Create and activate an emergency incident.
    fun startEmergency(shouldShareLocation: Boolean) {
        if (
            isLoading ||
            isRestoring ||
            !canStartEmergency ||
            incidentId != null
        ) return

        scope.launch {
            isLoading = true
            canStartEmergency = false
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

    // Retry activation for an incident that is still in CREATED state.
    fun retryActivation() {
        val currentIncidentId = incidentId ?: return

        if (isLoading || isActive) return

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
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

    // Cancel an ACTIVE or CREATED emergency incident.
    fun cancelEmergency() {
        val currentIncidentId = incidentId ?: return

        if (isLoading || isRestoring) return

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = repository.cancelIncident(
                    currentIncidentId
                )

                when (response.status) {
                    "CANCELLED" -> {
                        completeCancellation()
                    }

                    "RESOLVED" -> {
                        completeCancellation()
                        locationStatusMessage =
                            "This emergency incident has already been resolved."
                    }

                    else -> {
                        // Check the server's actual status before deciding
                        // whether the incident is still active.
                        val verifiedStatus = repository.getIncident(
                            currentIncidentId
                        ).status

                        when (verifiedStatus) {
                            "CANCELLED" -> completeCancellation()

                            "RESOLVED" -> {
                                completeCancellation()
                                locationStatusMessage =
                                    "This emergency incident has already been resolved."
                            }

                            "ACTIVE", "CREATED" -> {
                                errorMessage =
                                    "Cancellation was not completed. The emergency request is still ${verifiedStatus.lowercase()}. Please try again."
                            }

                            else -> {
                                errorMessage =
                                    "The cancellation status could not be confirmed. Please check your incident status."
                            }
                        }
                    }
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (_: Exception) {
                // A cancellation request might have reached the backend
                // even if the response was lost. Verify before retrying.
                val verifiedStatus = try {
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

                when (verifiedStatus) {
                    "CANCELLED" -> {
                        completeCancellation()
                    }

                    "RESOLVED" -> {
                        completeCancellation()
                        locationStatusMessage =
                            "This emergency incident has already been resolved."
                    }

                    "ACTIVE", "CREATED" -> {
                        errorMessage =
                            "Cancellation could not be confirmed. Your emergency request is still ${verifiedStatus.lowercase()}. You can retry cancellation."
                    }

                    else -> {
                        errorMessage =
                            "Unable to confirm cancellation. Check your connection and retry. Your existing incident has been kept on screen."
                    }
                }

            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
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

            if (isRestoring || isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        ConnectProSpacing.Medium
                    )
                ) {
                    CircularProgressIndicator()

                    Text(
                        if (isRestoring) {
                            "Checking existing emergency request…"
                        } else {
                            "Processing emergency request…"
                        }
                    )
                }

            } else if (isActive) {
                Text(
                    text = "Incident status: ACTIVE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )

                TextButton(
                    onClick = {
                        showCancelConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel emergency request",
                        color = MaterialTheme.colorScheme.error
                    )
                }

            } else if (incidentId != null) {
                Button(
                    onClick = { retryActivation() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry activation")
                }

                TextButton(
                    onClick = {
                        showCancelConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel emergency request",
                        color = MaterialTheme.colorScheme.error
                    )
                }

            } else if (canStartEmergency) {
                Button(
                    onClick = {
                        errorMessage = null
                        showConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request emergency assistance")
                }

            } else {
                Button(
                    onClick = { refreshIncidentState() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry status check")
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

    // Existing emergency activation confirmation dialog.
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
                    enabled = !isLoading && !isRestoring && canStartEmergency,
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

    // New emergency cancellation confirmation dialog.
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showCancelConfirmation = false
                }
            },
            title = {
                Text("Cancel emergency request?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        ConnectProSpacing.Small
                    )
                ) {
                    Text(
                        "Are you sure you want to cancel this emergency request?"
                    )

                    Text(
                        text = "The incident will be marked as CANCELLED and will no longer appear as an active emergency in ConnectPro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Notifications already delivered to trusted contacts cannot be recalled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        showCancelConfirmation = false
                        cancelEmergency()
                    }
                ) {
                    Text(
                        text = "Confirm cancellation",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        showCancelConfirmation = false
                    }
                ) {
                    Text("Keep active")
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
