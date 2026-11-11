package app.wault.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wault'un kendi çizdiği ikonlar. Tek çizgi kalınlığı, yuvarlak uç. */
enum class Glyph { PLUS, CLOSE, HANDLE, CONTACTLESS, QR, BARCODE, CHIP, CHECK, CAMERA, IMAGE, NFC_PHONE, SLIDERS, CHEVRON, CARD, STAR, MORE, BELL, LOCK, TRASH, COPY }

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val s = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1 * w, y1 * w), Offset(x2 * w, y2 * w), strokeWidth = s.width, cap = StrokeCap.Round)
        when (glyph) {
            Glyph.PLUS -> { line(0.5f, 0.18f, 0.5f, 0.82f); line(0.18f, 0.5f, 0.82f, 0.5f) }
            Glyph.CLOSE -> { line(0.24f, 0.24f, 0.76f, 0.76f); line(0.76f, 0.24f, 0.24f, 0.76f) }
            Glyph.HANDLE -> { line(0.2f, 0.36f, 0.8f, 0.36f); line(0.2f, 0.5f, 0.8f, 0.5f); line(0.2f, 0.64f, 0.8f, 0.64f) }
            Glyph.CHECK -> { line(0.2f, 0.52f, 0.42f, 0.72f); line(0.42f, 0.72f, 0.8f, 0.3f) }
            Glyph.CONTACTLESS -> {
                for (i in 0 until 4) {
                    val r = w * (0.14f + i * 0.13f)
                    drawArc(tint, -38f, 76f, false,
                        topLeft = Offset(w * 0.18f - r, w * 0.5f - r), size = Size(r * 2, r * 2), style = s)
                }
            }
            Glyph.QR -> {
                val sq = Stroke(width = w * 0.075f)
                listOf(Offset(0.16f, 0.16f), Offset(0.58f, 0.16f), Offset(0.16f, 0.58f)).forEach {
                    drawRect(tint, Offset(it.x * w, it.y * w), Size(w * 0.26f, w * 0.26f), style = sq)
                }
                drawRect(tint, Offset(0.62f * w, 0.62f * w), Size(w * 0.1f, w * 0.1f))
                drawRect(tint, Offset(0.76f * w, 0.76f * w), Size(w * 0.08f, w * 0.08f))
            }
            Glyph.BARCODE -> {
                listOf(0.18f to 0.05f, 0.28f to 0.03f, 0.36f to 0.07f, 0.48f to 0.03f, 0.56f to 0.05f, 0.66f to 0.03f, 0.74f to 0.08f)
                    .forEach { (x, bw) -> drawRect(tint, Offset(x * w, 0.22f * w), Size(bw * w, 0.56f * w)) }
            }
            Glyph.CHIP -> {
                drawRoundRect(tint, Offset(0.12f * w, 0.22f * w), Size(0.76f * w, 0.56f * w),
                    androidx.compose.ui.geometry.CornerRadius(0.1f * w), style = Stroke(w * 0.06f))
                line(0.12f, 0.5f, 0.38f, 0.5f); line(0.62f, 0.5f, 0.88f, 0.5f); line(0.5f, 0.22f, 0.5f, 0.78f)
            }
            Glyph.CAMERA -> {
                drawRoundRect(tint, Offset(0.12f * w, 0.3f * w), Size(0.76f * w, 0.5f * w),
                    androidx.compose.ui.geometry.CornerRadius(0.08f * w), style = s)
                drawCircle(tint, 0.13f * w, Offset(0.5f * w, 0.55f * w), style = s)
                line(0.36f, 0.3f, 0.42f, 0.2f); line(0.42f, 0.2f, 0.58f, 0.2f); line(0.58f, 0.2f, 0.64f, 0.3f)
            }
            Glyph.IMAGE -> {
                drawRoundRect(tint, Offset(0.14f * w, 0.18f * w), Size(0.72f * w, 0.64f * w),
                    androidx.compose.ui.geometry.CornerRadius(0.08f * w), style = s)
                line(0.2f, 0.72f, 0.42f, 0.48f); line(0.42f, 0.48f, 0.56f, 0.62f); line(0.56f, 0.62f, 0.66f, 0.52f); line(0.66f, 0.52f, 0.8f, 0.68f)
                drawCircle(tint, 0.05f * w, Offset(0.66f * w, 0.34f * w))
            }
            Glyph.NFC_PHONE -> {
                drawRoundRect(tint, Offset(0.3f * w, 0.1f * w), Size(0.4f * w, 0.8f * w),
                    androidx.compose.ui.geometry.CornerRadius(0.08f * w), style = s)
                line(0.44f, 0.8f, 0.56f, 0.8f)
            }
            Glyph.CHEVRON -> { line(0.38f, 0.22f, 0.66f, 0.5f); line(0.66f, 0.5f, 0.38f, 0.78f) }
            Glyph.CARD -> {
                drawRoundRect(tint, Offset(0.1f * w, 0.24f * w), Size(0.8f * w, 0.52f * w),
                    androidx.compose.ui.geometry.CornerRadius(0.08f * w), style = s)
                line(0.1f, 0.4f, 0.9f, 0.4f)
                line(0.2f, 0.62f, 0.42f, 0.62f)
            }
            Glyph.STAR -> {
                val p = androidx.compose.ui.graphics.Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) 0.4f else 0.17f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val x = (0.5f + r * kotlin.math.cos(a).toFloat()) * w
                    val y = (0.53f + r * kotlin.math.sin(a).toFloat()) * w
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, tint, style = Stroke(w * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            Glyph.MORE -> listOf(0.24f, 0.5f, 0.76f).forEach { drawCircle(tint, 0.07f * w, Offset(it * w, 0.5f * w)) }
            Glyph.BELL -> {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0.22f * w, 0.7f * w)
                    lineTo(0.28f * w, 0.62f * w)
                    lineTo(0.28f * w, 0.44f * w)
                    cubicTo(0.28f * w, 0.18f * w, 0.72f * w, 0.18f * w, 0.72f * w, 0.44f * w)
                    lineTo(0.72f * w, 0.62f * w)
                    lineTo(0.78f * w, 0.7f * w)
                    close()
                }
                drawPath(p, tint, style = Stroke(w * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                line(0.44f, 0.82f, 0.56f, 0.82f)
            }
            Glyph.LOCK -> {
                drawRoundRect(tint, Offset(0.22f * w, 0.44f * w), Size(0.56f * w, 0.42f * w),
                    androidx.compose.ui.geometry.CornerRadius(0.08f * w), style = s)
                drawArc(tint, 180f, 180f, false, Offset(0.32f * w, 0.16f * w), Size(0.36f * w, 0.5f * w), style = s)
            }
            Glyph.TRASH -> {
                line(0.2f, 0.28f, 0.8f, 0.28f); line(0.4f, 0.18f, 0.6f, 0.18f)
                line(0.28f, 0.28f, 0.32f, 0.84f); line(0.32f, 0.84f, 0.68f, 0.84f); line(0.68f, 0.84f, 0.72f, 0.28f)
            }
            Glyph.COPY -> {
                // iki üst üste dikdörtgen: arkadaki yalnızca sol-üst köşesiyle görünür
                line(0.34f, 0.16f, 0.78f, 0.16f); line(0.78f, 0.16f, 0.78f, 0.62f)
                line(0.16f, 0.34f, 0.62f, 0.34f); line(0.62f, 0.34f, 0.62f, 0.84f)
                line(0.62f, 0.84f, 0.16f, 0.84f); line(0.16f, 0.84f, 0.16f, 0.34f)
                line(0.34f, 0.34f, 0.34f, 0.16f)
            }
            Glyph.SLIDERS -> {
                // iki ayar sürgüsü: çizgi + üzerinde dolu düğme
                line(0.16f, 0.34f, 0.84f, 0.34f)
                line(0.16f, 0.66f, 0.84f, 0.66f)
                drawCircle(tint, 0.1f * w, Offset(0.36f * w, 0.34f * w))
                drawCircle(tint, 0.1f * w, Offset(0.64f * w, 0.66f * w))
            }
        }
    }
}
