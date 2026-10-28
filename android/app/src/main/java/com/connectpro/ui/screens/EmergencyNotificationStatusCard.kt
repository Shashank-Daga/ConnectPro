package com.connectpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.connectpro.data.emergency.EmergencyIncidentRepository
import com.connectpro.data.emergency.TrustedContactNotificationResponse
import com.connectpro.data.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun EmergencyNotificationStatusCard(
    incidentId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember(context) {
        EmergencyIncidentRepository(
            ApiClient(context.applicationContext)
        )
    }

    var notifications by remember(incidentId) {
        mutableStateOf<List<TrustedContactNotificationResponse>>(emptyList())
    }

    var isLoading by remember(incidentId) {
        mutableStateOf(true)
    }

    var hasLoaded by remember(incidentId) {
        mutableStateOf(false)
    }

    var errorMessage by remember(incidentId) {
        mutableStateOf<String?>(null)
    }

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                notifications =
                    repository.getNotificationsForIncident(incidentId)
                hasLoaded = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HttpException) {
                errorMessage = when (exception.code()) {
                    401 ->
                        "Your session has expired. Please sign in again."

                    403 ->
                        "You are not allowed to view these notification statuses."

                    404 ->
                        "Notification status is not available for this incident."

                    else ->
                        "Unable to load notification statuses (HTTP ${exception.code()})."
                }
            } catch (exception: IOException) {
                errorMessage =
                    "Unable to reach the server. Check your connection and retry."
            } catch (exception: Exception) {
                errorMessage =
                    "Unable to load notification statuses. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(repository, incidentId) {
        loadNotifications()
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Trusted contact notifications",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Check the status of the alerts associated with this incident.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "A SENT status means the delivery provider accepted the send request; it does not confirm that the contact received or read the SMS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading && !hasLoaded) {
                CircularProgressIndicator()
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { loadNotifications() },
                    enabled = !isLoading
                ) {
                    Text("Retry")
                }
            }

            if (isLoading && hasLoaded) {
                CircularProgressIndicator()
            }

            if (hasLoaded && errorMessage == null && notifications.isEmpty()) {
                Text(
                    text = "No notification records were returned for this incident.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            notifications.forEach { notification ->
                NotificationStatusItem(notification)
            }

            OutlinedButton(
                onClick = { loadNotifications() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Refreshing…" else "Refresh statuses")
            }
        }
    }
}

@Composable
private fun NotificationStatusItem(
    notification: TrustedContactNotificationResponse
) {
    val status = notification.status.uppercase()

    val (statusLabel, statusColor) = when (status) {
        "PENDING" ->
            "Pending" to MaterialTheme.colorScheme.tertiary

        "SENT" ->
            "Sent to provider" to MaterialTheme.colorScheme.primary

        "FAILED" ->
            "Failed" to MaterialTheme.colorScheme.error

        else ->
            "Unknown status" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = notification.contactName,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = notification.maskedPhoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row {
                Text(
                    text = "Status: ",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            }

            Text(
                text = "Delivery attempts: ${notification.deliveryAttempts}",
                style = MaterialTheme.typography.bodySmall
            )

            if (notification.sentAt != null) {
                Text(
                    text = "Sent timestamp: ${notification.sentAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
