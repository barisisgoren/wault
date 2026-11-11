package app.wault.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.wault.R

/**
 * Apple Wallet hissi: açık modda yumuşak gri zemin + beyaz gruplar, koyu modda saf siyah.
 * Renk kartların kendisinde; arayüz sessiz ve nötr durur.
 */
@Immutable
data class WaultColors(
    val bg: Color,        // ekran zemini
    val surface: Color,   // gruplu kart/satır yüzeyi
    val surfaceAlt: Color,// basılı/seçili yüzey
    val label: Color,     // birincil metin
    val secondary: Color, // ikincil metin
    val tertiary: Color,  // placeholder, en silik
    val separator: Color, // ince ayraç çizgisi
    val accent: Color,    // tek vurgu (eylem, seçili)
    val danger: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val LightColors = WaultColors(
    bg = Color(0xFFF3F4F6),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEDEDF2),
    label = Color(0xFF17191C),
    secondary = Color(0xFF686B73),
    tertiary = Color(0xFFBFBFC6),
    separator = Color(0xFFE4E4EA),
    accent = Color(0xFF007AFF),
    danger = Color(0xFFE5484D),
    onAccent = Color.White,
    isDark = false,
)

val DarkColors = WaultColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceAlt = Color(0xFF2C2C2E),
    label = Color(0xFFF5F5F7),
    secondary = Color(0xFF98989F),
    tertiary = Color(0xFF5A5A60),
    separator = Color(0xFF2A2A2C),
    accent = Color(0xFF409CFF),
    danger = Color(0xFFFF453A),
    onAccent = Color.White,
    isDark = true,
)

private fun inst(weight: Int, res: Int) = FontFamily(Font(res, FontWeight(weight)))

object WaultFonts {
    val regular = inst(400, R.font.instrument_sans_400)
    val medium = inst(500, R.font.instrument_sans_500)
    val semibold = inst(600, R.font.instrument_sans_600)
    val bold = inst(700, R.font.instrument_sans_700)
}

/** iOS türü ölçek: large title, title, headline, body, subhead, footnote, caption. */
object WaultType {
    val largeTitle = TextStyle(fontFamily = WaultFonts.bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp)
    val title = TextStyle(fontFamily = WaultFonts.semibold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp)
    val headline = TextStyle(fontFamily = WaultFonts.semibold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val body = TextStyle(fontFamily = WaultFonts.regular, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val bodyMedium = TextStyle(fontFamily = WaultFonts.medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val callout = TextStyle(fontFamily = WaultFonts.regular, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.2).sp)
    val subhead = TextStyle(fontFamily = WaultFonts.regular, fontSize = 15.sp, lineHeight = 20.sp)
    val subheadMedium = TextStyle(fontFamily = WaultFonts.medium, fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = TextStyle(fontFamily = WaultFonts.regular, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = WaultFonts.medium, fontSize = 12.sp, lineHeight = 16.sp)

    // kart yüzü
    val cardIssuer = TextStyle(fontFamily = WaultFonts.semibold, fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = (-0.2).sp)
    val cardType = TextStyle(fontFamily = WaultFonts.medium, fontSize = 13.sp, lineHeight = 16.sp)
    val cardNumber = TextStyle(fontFamily = WaultFonts.medium, fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = 1.sp)
    val cardScheme = TextStyle(fontFamily = WaultFonts.bold, fontSize = 16.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp)
}

val LocalWaultColors = staticCompositionLocalOf { LightColors }
val LocalReducedMotion = staticCompositionLocalOf { false }
