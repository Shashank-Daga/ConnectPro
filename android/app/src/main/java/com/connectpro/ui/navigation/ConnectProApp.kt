
package com.connectpro.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.connectpro.ui.screens.ActivityScreen
import com.connectpro.ui.screens.HomeScreen
import com.connectpro.ui.screens.ProfileScreen
import com.connectpro.ui.screens.TrustedContactsScreen

private enum class AppDestination(
    val title: String,
    val label: String
) {
    HOME(
        title = "Home",
        label = "Home"
    ),
    TRUSTED_CONTACTS(
        title = "Trusted Contacts",
        label = "Contacts"
    ),
    ACTIVITY(
        title = "Activity",
        label = "Activity"
    ),
    PROFILE(
        title = "Profile",
        label = "Profile"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectProApp() {
    var selectedDestination by rememberSaveable {
        mutableStateOf(AppDestination.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedDestination.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },

        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,

                        onClick = {
                            selectedDestination = destination
                        },

                        icon = {
                            DestinationIcon(
                                destination = destination,
                                selected = selectedDestination == destination
                            )
                        },

                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },

                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedDestination) {
                AppDestination.HOME -> {
                    HomeScreen(
                        onOpenTrustedContacts = {
                            selectedDestination =
                                AppDestination.TRUSTED_CONTACTS
                        }
                    )
                }

                AppDestination.TRUSTED_CONTACTS -> {
                    TrustedContactsScreen()
                }

                AppDestination.ACTIVITY -> {
                    ActivityScreen()
                }

                AppDestination.PROFILE -> {
                    ProfileScreen()
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestination,
    selected: Boolean
) {
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Canvas(
        modifier = Modifier.size(24.dp)
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val width = size.width
        val height = size.height
        val path = Path()

        when (destination) {
            AppDestination.HOME -> {
                // Roof
                path.moveTo(width * 0.12f, height * 0.46f)
                path.lineTo(width * 0.50f, height * 0.12f)
                path.lineTo(width * 0.88f, height * 0.46f)

                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )

                // House outline
                path.reset()
                path.moveTo(width * 0.22f, height * 0.40f)
                path.lineTo(width * 0.22f, height * 0.88f)
                path.lineTo(width * 0.78f, height * 0.88f)
                path.lineTo(width * 0.78f, height * 0.40f)

                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )

                // Door
                drawLine(
                    color = iconColor,
                    start = Offset(width * 0.44f, height * 0.88f),
                    end = Offset(width * 0.44f, height * 0.63f),
                    strokeWidth = strokeWidth
                )

                drawLine(
                    color = iconColor,
                    start = Offset(width * 0.56f, height * 0.88f),
                    end = Offset(width * 0.56f, height * 0.63f),
                    strokeWidth = strokeWidth
                )
            }

            AppDestination.TRUSTED_CONTACTS -> {
                // First person
                drawCircle(
                    color = iconColor,
                    radius = width * 0.13f,
                    center = Offset(width * 0.38f, height * 0.32f),
                    style = Stroke(width = strokeWidth)
                )

                // Second person
                drawCircle(
                    color = iconColor,
                    radius = width * 0.11f,
                    center = Offset(width * 0.69f, height * 0.38f),
                    style = Stroke(width = strokeWidth)
                )

                // First person's shoulders
                path.moveTo(width * 0.10f, height * 0.84f)
                path.cubicTo(
                    width * 0.10f, height * 0.57f,
                    width * 0.66f, height * 0.57f,
                    width * 0.66f, height * 0.84f
                )

                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )

                // Second person's shoulders
                path.reset()
                path.moveTo(width * 0.58f, height * 0.63f)
                path.cubicTo(
                    width * 0.88f, height * 0.58f,
                    width * 0.95f, height * 0.73f,
                    width * 0.92f, height * 0.84f
                )

                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )
            }

            AppDestination.ACTIVITY -> {
                // Activity waveform
                path.moveTo(width * 0.08f, height * 0.52f)
                path.lineTo(width * 0.28f, height * 0.52f)
                path.lineTo(width * 0.40f, height * 0.25f)
                path.lineTo(width * 0.55f, height * 0.77f)
                path.lineTo(width * 0.68f, height * 0.42f)
                path.lineTo(width * 0.77f, height * 0.52f)
                path.lineTo(width * 0.92f, height * 0.52f)

                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )
            }

            AppDestination.PROFILE -> {
                // Head
                drawCircle(
                    color = iconColor,
                    radius = width * 0.16f,
                    center = Offset(width * 0.50f, height * 0.30f),
                    style = Stroke(width = strokeWidth)
                )

                // Shoulders
                path.moveTo(width * 0.15f, height * 0.87f)
                path.cubicTo(
                    width * 0.15f, height * 0.55f,
                    width * 0.85f, height * 0.55f,
                    width * 0.85f, height * 0.87f
                )

                drawPath(
                    path = path,
                    color = iconColor,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
