package com.baris.wault.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val WaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val WaultAmoledColorScheme = darkColorScheme(
    primary = WaultPrimaryMint,
    onPrimary = Color(0xFF000000),
    primaryContainer = WaultShizukuActiveBg,
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondary = WaultPrimaryCyan,
    onSecondary = Color(0xFF04131D),
    secondaryContainer = WaultSurfaceContainerDark,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = WaultAccentIndigo,
    background = WaultAmoledBlack,
    onBackground = Color.White,
    surface = WaultSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = WaultSurfaceContainerDark,
    onSurfaceVariant = WaultMutedGray,
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
        shapes = WaultShapes,
        content = content
    )
}