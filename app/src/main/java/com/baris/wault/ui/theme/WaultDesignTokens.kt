package com.baris.wault.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wault design tokens — single source of truth for the presentation layer.
 * Dark-first, calm, one tonal system.
 */
object WaultTokens {

    // ---- Background layers (3 elevation levels) ----
    val Bg0 = Color(0xFF000000)          // canvas
    val Bg1 = Color(0xFF111113)          // raised surfaces (sheets, bars)
    val Bg2 = Color(0xFF1C1C1F)          // controls, chips, buttons
    val Stroke = Color.White.copy(alpha = 0.10f)
    val StrokeStrong = Color.White.copy(alpha = 0.18f)

    // ---- Text ----
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA1A1A8)
    val TextTertiary = Color(0xFF6E6E76)

    // ---- Accent ----
    val Accent = Color(0xFF0A84FF)
    val Favorite = Color(0xFFFFD60A)

    // ---- Shape ----
    val CardRadius = 24.dp
    val ControlRadius = 14.dp

    // ---- Spacing scale ----
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space24 = 24.dp

    // ---- Sizes ----
    val MinTouch = 44.dp
    val LogoTile = 40.dp
    val CardHeight = 216.dp
    val StackPeek = 64.dp

    // ---- Single type scale (one family, tabular numerals for digits) ----
    private val Family = FontFamily.SansSerif
    val TitleLarge = TextStyle(fontFamily = Family, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
    val CardTitle = TextStyle(fontFamily = Family, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val CardTitleExpanded = TextStyle(fontFamily = Family, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val Body = TextStyle(fontFamily = Family, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val Label = TextStyle(fontFamily = Family, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val Caption = TextStyle(fontFamily = Family, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val CardDigits = TextStyle(
        fontFamily = Family,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp,
        fontFeatureSettings = "tnum"
    )
}

/** True when the user disabled animations system-wide ("reduce motion"). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Light-sheen position in [-1, 1], driven by device tilt (accelerometer, low-pass filtered).
 * Cheap: one sensor listener, one float state read only inside draw blocks.
 */
val LocalCardSheen = compositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

@Composable
fun rememberTiltSheen(enabled: Boolean = true): State<Float> {
    val context = LocalContext.current
    val sheen = remember { mutableFloatStateOf(0f) }
    DisposableEffect(enabled) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sm == null || sensor == null) {
            sheen.floatValue = 0f
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val target = (event.values[0] / 6f).coerceIn(-1f, 1f)
                    sheen.floatValue += (target - sheen.floatValue) * 0.12f
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    return sheen
}
