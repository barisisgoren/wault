package com.baris.wault.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

/**
 * One tonal system for every card.
 * Each card keeps its brand HUE, but saturation and lightness are constrained to a single
 * range, with a subtle matte gradient (~10% lightness shift) and a fine grain overlay.
 */
data class WalletCardStyle(
    val top: Color,
    val bottom: Color,
    val onCard: Color,
    val onCardSecondary: Color,
    val isNeutralChip: Boolean = false
) {
    val mid: Color get() = Color(ColorUtils.blendARGB(top.toArgb(), bottom.toArgb(), 0.5f))
}

object WalletCardStyles {

    private const val SAT_MIN = 0.34f
    private const val SAT_MAX = 0.46f
    private const val L_TOP = 0.30f
    private const val L_BOTTOM = 0.20f     // 10% lightness shift

    /** Brand-derived, tonally constrained style (auto themes). */
    fun fromBrand(brand: Color): WalletCardStyle {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(brand.toArgb(), hsl)
        val neutral = hsl[1] < 0.12f
        val sat = if (neutral) 0.06f else hsl[1].coerceIn(SAT_MIN, SAT_MAX)
        val top = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], sat, L_TOP)))
        val bottom = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], sat * 0.92f, L_BOTTOM)))
        return withReadableText(top, bottom, neutral)
    }

    /** User-chosen colour: honoured, only softened into a matte gradient. */
    fun fromCustom(base: Color): WalletCardStyle {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        val bottomL = (hsl[2] - 0.10f).coerceIn(0.04f, 0.96f)
        val bottom = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], bottomL)))
        return withReadableText(base, bottom, false)
    }

    /** Picks white/black per card so text meets WCAG AA (>= 4.5:1) against the darkest/lightest stop. */
    private fun withReadableText(top: Color, bottom: Color, neutral: Boolean): WalletCardStyle {
        fun contrast(fg: Color, bg: Color) = ColorUtils.calculateContrast(fg.toArgb(), bg.toArgb())
        val whiteMin = minOf(contrast(Color.White, top), contrast(Color.White, bottom))
        val blackMin = minOf(contrast(Color.Black, top), contrast(Color.Black, bottom))
        val useWhite = whiteMin >= blackMin
        val fg = if (useWhite) Color(0xFFF5F5F7) else Color(0xFF111113)
        return WalletCardStyle(
            top = top,
            bottom = bottom,
            onCard = fg,
            onCardSecondary = fg.copy(alpha = if (useWhite) 0.74f else 0.72f),
            isNeutralChip = neutral
        )
    }

    fun brush(style: WalletCardStyle): Brush = Brush.linearGradient(
        colors = listOf(style.top, style.bottom),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(700f, 900f)
    )
}

/** Cached, tiled film-grain shader (built once, shared by every card). */
object CardGrain {
    val brush: ShaderBrush by lazy {
        val size = 96
        val rnd = Random(7)
        val px = IntArray(size * size) {
            val v = rnd.nextInt(256)
            val a = rnd.nextInt(0, 26)          // very faint
            (a shl 24) or (v shl 16) or (v shl 8) or v
        }
        val bmp = Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}
