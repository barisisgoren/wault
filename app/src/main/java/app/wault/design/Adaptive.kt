package app.wault.design

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Yatay ekran ve geniş pencereler için ortak ölçüler. Uygulama dikey telefon için çizildi;
 * yatayda satırlar ekran boyu uzamasın, kartlar ekrandan taşmasın diye bunlar kullanılır.
 */

/** Liste ve form ekranlarında içeriğin en fazla genişliği (yatayda ortalanır). */
val ContentMaxWidth = 600.dp

/** Yatay telefon gibi alçak pencere: dikey alan kıt, öğeler küçülmeli ya da yan yana dizilmeli. */
@Composable
fun isShortWindow(): Boolean = LocalConfiguration.current.screenHeightDp < 480

/** Kart önizlemesinin en fazla genişliği: kartın yüksekliği pencerenin [fraction] kadarını geçmesin. */
@Composable
fun cardMaxWidth(fraction: Float = 0.55f): Dp = (LocalConfiguration.current.screenHeightDp * fraction * CARD_RATIO).dp

/** Yatayda kamera çentiği ve yandaki gezinme çubuğu: içerik bunların altına girmesin. */
@Composable
fun Modifier.horizontalSafePadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
