package app.wault.ui.common

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.wault.design.Glyph
import app.wault.design.GlyphIcon
import app.wault.design.IconTile
import app.wault.design.Wault

/** Kurulu bir uygulamanın gerçek ikonu; bulunamazsa nötr kart ikonu. */
@Composable
fun AppIcon(packageName: String?, size: Dp = 30.dp) {
    val context = LocalContext.current
    val bmp = remember(packageName) {
        packageName?.let {
            runCatching { context.packageManager.getApplicationIcon(it).toBitmap(144, 144).asImageBitmap() }.getOrNull()
        }
    }
    if (bmp != null) {
        Image(bmp, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)))
    } else {
        IconTile(Glyph.CARD, Wault.colors.secondary, Modifier.size(size))
    }
}

/**
 * Birkaç tur oynayıp duran nabız. Sonsuz animasyon ekranı sürekli yeniden çizdirip pili yer;
 * hazır olma bilgisi birkaç turdan sonra durağan halkalarla da anlaşılır.
 * Sistem animasyonları kapalıysa hiç oynamaz.
 */
@Stable
class Pulse internal constructor(private val anim: Animatable<Float, AnimationVector1D>) {
    internal var running by mutableStateOf(false)

    /** Çizim aşamasında okunur: oynarken 0..1, durunca [rest]. */
    fun value(rest: Float): Float = if (running) anim.value else rest
}

@Composable
fun rememberPulse(cycles: Int, durationMs: Int): Pulse {
    val reduced = Wault.reducedMotion
    val anim = remember { Animatable(0f) }
    val pulse = remember { Pulse(anim) }
    LaunchedEffect(reduced) {
        if (reduced) { pulse.running = false; return@LaunchedEffect }
        pulse.running = true
        repeat(cycles) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(durationMs, easing = LinearEasing))
        }
        pulse.running = false
    }
    return pulse
}

/**
 * "Ödemeye hazır" göstergesi: temassız işaretinden dışarı yayılan üç halka, kartın renginde.
 * Sadece kart gerçekten okuyucuya hazırken görünür; birkaç tur oynayıp durağan kalır.
 */
@Composable
fun ReadyPulse(modifier: Modifier = Modifier, size: Dp = 88.dp, tint: Color = Wault.colors.accent) {
    val c = Wault.colors
    val pulse = rememberPulse(cycles = 5, durationMs = 2600)
    Box(
        modifier
            .size(size)
            .drawBehind {
                val maxR = this.size.minDimension / 2
                val t = pulse.value(rest = 0.3f)
                for (k in 0..2) {
                    val phase = (t + k / 3f) % 1f
                    val r = maxR * (0.56f + 0.44f * phase)
                    drawCircle(tint.copy(alpha = (1f - phase) * (if (c.isDark) 0.5f else 0.38f)), radius = r, style = Stroke(1.6.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size * 0.56f).clip(CircleShape).background(tint.copy(alpha = if (c.isDark) 0.22f else 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(Glyph.CONTACTLESS, tint, size = size * 0.28f)
        }
    }
}
