
package com.connectpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.connectpro.ui.theme.ConnectProSpacing

@Composable
fun TrustedContactsScreen() {
    FeatureScreen(
        marker = "TC",
        eyebrow = "YOUR SAFETY NETWORK",
        heading = "People you trust",
        description = "Your trusted network will help you organize "
                + "the people you want to include in your personal safety plan.",
        statusTitle = "Your network starts here",
        statusDescription = "Adding, editing, removing, and syncing "
                + "trusted contacts are not available yet.",
        footer = "No contacts have been added or loaded."
    )
}

@Composable
fun ActivityScreen() {
    FeatureScreen(
        marker = "AH",
        eyebrow = "YOUR ACTIVITY",
        heading = "Your activity history",
        description = "This area is designed to present relevant "
                + "ConnectPro activity when its supporting workflows are available.",
        statusTitle = "Activity history is coming later",
        statusDescription = "Activity tracking and history are not "
                + "connected in this version.",
        footer = "No activity records are being displayed."
    )
}

@Composable
fun ProfileScreen() {
    FeatureScreen(
        marker = "ME",
        eyebrow = "YOUR ACCOUNT",
        heading = "Your profile",
        description = "This area will provide a home for your account "
                + "information and profile preferences.",
        statusTitle = "Profile settings are not available yet",
        statusDescription = "Account details and profile management "
                + "will be introduced in a later phase.",
        footer = "No account information is being loaded."
    )
}

@Composable
private fun FeatureScreen(
    marker: String,
    eyebrow: String,
    heading: String,
    description: String,
    statusTitle: String,
    statusDescription: String,
    footer: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = ConnectProSpacing.ScreenHorizontal,
                vertical = ConnectProSpacing.Medium
            ),
        verticalArrangement = Arrangement.spacedBy(
            ConnectProSpacing.Large
        )
    ) {
        FeatureIntroduction(
            marker = marker,
            eyebrow = eyebrow,
            heading = heading,
            description = description
        )

        FeatureAvailabilityCard(
            title = statusTitle,
            description = statusDescription
        )

        Text(
            text = footer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ConnectProSpacing.ExtraSmall
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(ConnectProSpacing.Small))
    }
}

@Composable
private fun FeatureIntroduction(
    marker: String,
    eyebrow: String,
    heading: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            ConnectProSpacing.Medium
        )
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = marker,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Small
            )
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = heading,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureAvailabilityCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ConnectProSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(
                ConnectProSpacing.Medium
            )
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "PLANNED FEATURE",
                    modifier = Modifier.padding(
                        horizontal = ConnectProSpacing.SmallMedium,
                        vertical = ConnectProSpacing.ExtraSmall
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
