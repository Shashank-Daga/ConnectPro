
package com.connectpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.connectpro.data.emergency.EmergencyHistoryRepository
import com.connectpro.data.emergency.EmergencyIncidentResponse
import com.connectpro.data.network.ApiClient
import com.connectpro.data.profile.UserProfileRepository
import com.connectpro.data.profile.UserProfileResponse
import com.connectpro.ui.theme.ConnectProSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrustedContactsScreen() {
    TrustedContactsContent()
}

@Composable
fun ActivityScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val repository = remember(context) {
        EmergencyHistoryRepository(
            ApiClient(context.applicationContext)
        )
    }

    var incidents by remember {
        mutableStateOf<List<EmergencyIncidentResponse>?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    suspend fun loadHistory() {
        isLoading = true
        errorMessage = null

        try {
            incidents = repository.getEmergencyHistory()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            errorMessage = exception.message
                ?.takeIf { it.isNotBlank() }
                ?: "Unable to load emergency history. Please try again."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(repository) {
        loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = ConnectProSpacing.ScreenHorizontal,
                vertical = ConnectProSpacing.Large
            ),
        verticalArrangement = Arrangement.spacedBy(
            ConnectProSpacing.Large
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Small
            )
        ) {
            Text(
                text = "Your activity",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Review your emergency incident history.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            isLoading -> {
                androidx.compose.material3.CircularProgressIndicator()

                Text(
                    text = "Loading your activity…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            ConnectProSpacing.Large
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            ConnectProSpacing.Medium
                        )
                    ) {
                        Text(
                            text = "Activity unavailable",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    loadHistory()
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            incidents.isNullOrEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            ConnectProSpacing.Large
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            ConnectProSpacing.Small
                        )
                    ) {
                        Text(
                            text = "No emergency history yet",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Your emergency incidents will appear here when records are available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = "${incidents!!.size} incident(s)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                incidents!!.forEach { incident ->
                    EmergencyIncidentCard(incident)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loadHistory()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh activity")
                }
            }
        }
    }
}

@Composable
private fun EmergencyIncidentCard(
    incident: EmergencyIncidentResponse
) {
    val category = incident.category
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    val status = incident.status
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                ConnectProSpacing.Large
            ),
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Medium
            )
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.material3.Surface(
                color = when (incident.status.uppercase()) {
                    "ACTIVE" -> MaterialTheme.colorScheme.errorContainer
                    "RESOLVED" -> MaterialTheme.colorScheme.secondaryContainer
                    "CANCELLED" -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(
                        horizontal = ConnectProSpacing.Medium,
                        vertical = ConnectProSpacing.Small
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = when (incident.status.uppercase()) {
                        "ACTIVE" -> MaterialTheme.colorScheme.onErrorContainer
                        "RESOLVED" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "CANCELLED" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            EmergencyIncidentDetail(
                label = "Triggered",
                value = formatIncidentDate(incident.triggeredAt)
            )

            if (incident.resolvedAt != null) {
                EmergencyIncidentDetail(
                    label = "Resolved",
                    value = formatIncidentDate(incident.resolvedAt)
                )
            }
        }
    }
}

@Composable
private fun EmergencyIncidentDetail(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            ConnectProSpacing.Small
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatIncidentDate(value: String): String {
    return try {
        OffsetDateTime.parse(value).format(
            DateTimeFormatter.ofPattern(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )
        )
    } catch (exception: Exception) {
        value
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val repository = remember(context) {
        UserProfileRepository(
            ApiClient(context.applicationContext)
        )
    }

    var profile by remember {
        mutableStateOf<UserProfileResponse?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val coroutineScope = rememberCoroutineScope()

    suspend fun loadProfile() {
        isLoading = true
        errorMessage = null

        try {
            profile = repository.getCurrentUser()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            errorMessage = exception.message
                ?.takeIf { it.isNotBlank() }
                ?: "Unable to load your profile. Please try again."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(repository) {
        loadProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = ConnectProSpacing.ScreenHorizontal,
                vertical = ConnectProSpacing.Large
            ),
        verticalArrangement = Arrangement.spacedBy(
            ConnectProSpacing.Large
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Small
            )
        ) {
            Text(
                text = "Your profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "View your ConnectPro account information.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            isLoading -> {
                androidx.compose.material3.CircularProgressIndicator()
                Text("Loading your profile…")
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            ConnectProSpacing.Large
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            ConnectProSpacing.Medium
                        )
                    ) {
                        Text(
                            text = "Profile unavailable",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    loadProfile()
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            profile != null -> {
                val user = profile!!

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            ConnectProSpacing.Large
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            ConnectProSpacing.Medium
                        )
                    ) {
                        Text(
                            text = "Account details",
                            style = MaterialTheme.typography.titleLarge
                        )

                        ProfileDetail(
                            label = "Phone number",
                            value = user.phoneNumber
                        )

                        ProfileDetail(
                            label = "Email",
                            value = user.email
                        )

                        ProfileDetail(
                            label = "Account status",
                            value = user.status.replace('_', ' ')
                        )

                        ProfileDetail(
                            label = "Phone verification",
                            value = if (user.phoneVerifiedAt != null) {
                                "Verified"
                            } else {
                                "Not verified"
                            }
                        )

                        ProfileDetail(
                            label = "Email verification",
                            value = if (user.emailVerifiedAt != null) {
                                "Verified"
                            } else {
                                "Not verified"
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign out")
        }
    }
}

@Composable
private fun ProfileDetail(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            ConnectProSpacing.ExtraSmall
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
