package com.baris.wault.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WaultAmoledColorScheme = darkColorScheme(
    primary = WaultPrimaryMint,
    onPrimary = Color(0xFF03140A),
    primaryContainer = Color(0xFF0D2818),
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondary = WaultPrimaryCyan,
    onSecondary = Color(0xFF04131D),
    secondaryContainer = WaultSurfaceContainerDark,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = WaultAccentIndigo,
    background = WaultAmoledBlack,
    onBackground = Color(0xFFF8FAFC),
    surface = WaultSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = WaultSurfaceContainerDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = WaultGlassBorderDark
)

@Composable
fun WaultTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        WaultAmoledColorScheme
    }

    val colorScheme = baseScheme.copy(
        primary = WaultPrimaryMint,
        background = WaultAmoledBlack,
        surface = WaultSurfaceDark,
        surfaceVariant = WaultSurfaceContainerDark,
        surfaceContainer = WaultSurfaceContainerDark,
        outline = WaultGlassBorderDark
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}