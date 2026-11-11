package app.wault.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Renk hesapları saf Kotlin: telefonsuz, birim testte denenebilir. */
data class Hsv(val h: Float, val s: Float, val v: Float)

fun Color.toHsv(): Hsv {
    val r = red
    val g = green
    val b = blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Hsv(if (h < 0f) h + 360f else h, if (mx == 0f) 0f else d / mx, mx)
}

fun hsvColor(h: Float, s: Float, v: Float): Color =
    Color.hsv(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))

/** WCAG kontrast oranı (1..21). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/**
 * Bu zeminde beyaz yazı mı okunur? Beyazla kontrast en az 3:1 olduğu sürece beyaz kalır
 * (parlaklık 0,30'a kadar); sarı, nane, gümüş gibi açık renklerde koyu yazıya geçilir.
 */
fun prefersLightInk(base: Color): Boolean = base.luminance() <= 0.30f

fun Color.toHex(): String = "#%02X%02X%02X".format((red * 255f).roundToInt(), (green * 255f).roundToInt(), (blue * 255f).roundToInt())

/** "#RGB", "#RRGGBB" ya da başında # olmadan; geçersizse null. */
fun parseHexColor(text: String): Color? {
    val t = text.trim().removePrefix("#")
    val full = when (t.length) {
        3 -> t.map { "$it$it" }.joinToString("")
        6 -> t
        else -> return null
    }
    if (!full.all { it in "0123456789abcdefABCDEF" }) return null
    return Color(0xFF000000.toInt() or full.toInt(16))
}

fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

fun colorFromArgbLong(value: Long): Color = Color(value.toInt())
