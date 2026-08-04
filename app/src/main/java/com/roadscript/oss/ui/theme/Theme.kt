package com.roadscript.oss.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme {
    INDIGO, BLUE, GREEN, RED, GREY
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnBackground,
    primaryContainer = DarkPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant
)

private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryContainer,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = DarkOnBackground,
    primaryContainer = DarkBluePrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = DarkOnBackground,
    primaryContainer = DarkGreenPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = RedPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant
)

private val RedDarkColorScheme = darkColorScheme(
    primary = DarkRedPrimary,
    onPrimary = DarkOnBackground,
    primaryContainer = DarkRedPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val GreyLightColorScheme = lightColorScheme(
    primary = GreyPrimary,
    onPrimary = Color.White,
    primaryContainer = GreyPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant
)

private val GreyDarkColorScheme = darkColorScheme(
    primary = DarkGreyPrimary,
    onPrimary = DarkOnBackground,
    primaryContainer = DarkGreyPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant
)

@Composable
fun SuiviVehiculeTheme(
    appTheme: AppTheme = AppTheme.INDIGO,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.INDIGO -> if (darkTheme) DarkColorScheme else LightColorScheme
        AppTheme.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        AppTheme.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        AppTheme.RED -> if (darkTheme) RedDarkColorScheme else RedLightColorScheme
        AppTheme.GREY -> if (darkTheme) GreyDarkColorScheme else GreyLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
