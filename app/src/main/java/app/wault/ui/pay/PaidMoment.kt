package app.wault.ui.pay

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wault.data.WaultCard
import app.wault.design.CardArtSwatch
import app.wault.design.PrimaryButton
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.design.rememberCardArt
import app.wault.notify.PaidEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val PaidGreen = Color(0xFF34C759)

/** Onay halkası ve içine çekilen işaret. ring ve check 0..1 arası ilerlemedir. */
@Composable
fun PaidBadge(ring: Float, check: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sw = size.minDimension * 0.054f
        drawArc(
            PaidGreen, -90f, 360f * ring, false, style = Stroke(sw, cap = StrokeCap.Round),
            topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
        )
        val w = size.width
        val path = Path().apply {
            moveTo(w * 0.3f, w * 0.52f); lineTo(w * 0.45f, w * 0.66f); lineTo(w * 0.72f, w * 0.38f)
        }
        val pm = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        pm.getSegment(0f, pm.length * check, part, true)
        drawPath(part, PaidGreen, style = Stroke(sw * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** "Ödendi", tutar ve kart satırı. Hem ödeme anında hem karşılama ekranındaki önizlemede kullanılır. */
@Composable
fun PaidSummary(
    ring: Float,
    check: Float,
    textAlpha: Float,
    amount: String,
    card: WaultCard?,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 112.dp,
    amountStyle: TextStyle = WaultType.largeTitle.copy(fontSize = 46.sp, lineHeight = 52.sp),
) {
    val c = Wault.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PaidBadge(ring, check, Modifier.size(badgeSize))
        Spacer(Modifier.height(badgeSize * 0.25f))
        Column(
            Modifier
                .graphicsLayer { alpha = textAlpha; translationY = (1f - textAlpha) * 24f }
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Ödendi", style = WaultType.headline, color = c.secondary)
            Spacer(Modifier.height(4.dp))
            Text(amount, style = amountStyle, color = c.label, textAlign = TextAlign.Center)
            if (card != null) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardArtSwatch(rememberCardArt(card), Modifier.width(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        listOfNotNull(card.title, card.last4.takeIf { it.isNotBlank() }?.let { "•••• $it" }).joinToString("  "),
                        style = WaultType.subheadMedium, color = c.label,
                    )
                }
            }
        }
    }
}

/**
 * Ödeme onayı: halka çizilir, içine onay işareti çekilir, tutar belirir, telefon bir kez titrer.
 * Wault'un tek orkestre anı.
 */
@Composable
fun PaidMoment(event: PaidEvent, card: WaultCard?, onDone: () -> Unit) {
    val c = Wault.colors
    val reduced = Wault.reducedMotion
    val view = LocalView.current
    val glowOn = Wault.appearance.lightEffects
    val fade = remember { Animatable(if (reduced) 1f else 0f) }
    val ring = remember { Animatable(if (reduced) 1f else 0f) }
    val check = remember { Animatable(if (reduced) 1f else 0f) }
    val text = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(event.at) {
        if (!reduced) {
            launch { fade.animateTo(1f, tween(180)) }
            ring.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
            )
            launch { check.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
            text.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 400f))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        delay(5000)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(c.bg)
            .drawBehind {
                // Işık efektleri açıksa onay halkasının yeşili zemine yumuşak bir ışık olarak yayılır
                if (glowOn) drawRect(
                    Brush.radialGradient(
                        listOf(PaidGreen.copy(alpha = (if (c.isDark) 0.26f else 0.16f) * ring.value), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.width * 1.1f,
                    )
                )
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDone)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        PaidSummary(ring.value, check.value, text.value, event.amount, card, Modifier.align(Alignment.Center))
        PrimaryButton("Tamam", onDone, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp))
    }
}
