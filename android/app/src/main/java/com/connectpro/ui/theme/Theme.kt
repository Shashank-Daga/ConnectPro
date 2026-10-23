package com.connectpro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ConnectProNavy,
    onPrimary = OnConnectProNavy,

    primaryContainer = Color(0xFFD9E8F5),
    onPrimaryContainer = Color(0xFF10263A),

    secondary = ConnectProTeal,
    onSecondary = OnConnectProTeal,

    secondaryContainer = Color(0xFFC8EEF0),
    onSecondaryContainer = Color(0xFF00363B),

    tertiary = ConnectProSuccess,
    onTertiary = OnConnectProSuccess,

    tertiaryContainer = Color(0xFFD0F0DE),
    onTertiaryContainer = Color(0xFF003923),

    error = ConnectProEmergency,
    onError = OnConnectProEmergency,

    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = LightOutline,
    outlineVariant = Color(0xFFBCC8D2),

    surfaceTint = ConnectProNavy
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkConnectProNavy,
    onPrimary = OnDarkConnectProNavy,

    primaryContainer = Color(0xFF294966),
    onPrimaryContainer = Color(0xFFD9E8F5),

    secondary = DarkConnectProTeal,
    onSecondary = OnDarkConnectProTeal,

    secondaryContainer = Color(0xFF00545B),
    onSecondaryContainer = Color(0xFFC8EEF0),

    tertiary = DarkConnectProSuccess,
    onTertiary = OnDarkConnectProSuccess,

    tertiaryContainer = Color(0xFF145B3B),
    onTertiaryContainer = Color(0xFFD0F0DE),

    error = DarkConnectProEmergency,
    onError = OnDarkConnectProEmergency,

    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,
    outlineVariant = Color(0xFF465663),

    surfaceTint = DarkConnectProNavy
)

private val ConnectProShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ConnectProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            // Optional Android dynamic colors can be added back here
            // if explicitly requested in a future implementation.
            if (darkTheme) DarkColorScheme else LightColorScheme
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ConnectProShapes,
        content = content
    )
}
