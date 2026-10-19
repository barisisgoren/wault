package com.baris.wault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated NFC radar indicator displayed below an enlarged card in Double-Tap Payment Mode.
 * Tapping the radar or completing an NFC POS transaction triggers the Payment Success animation.
 */
@Composable
fun PaymentReadyRadarIndicator(
    cardTitle: String,
    onTriggerPaymentSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NfcRadarPulse")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarWave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarWave2"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .clickable(onClick = onTriggerPaymentSuccess),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val maxRadius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = WaultPrimaryMint.copy(alpha = (1f - wave1) * 0.45f),
                    radius = maxRadius * (0.45f + wave1 * 0.55f),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = WaultPrimaryCyan.copy(alpha = (1f - wave2) * 0.38f),
                    radius = maxRadius * (0.45f + wave2 * 0.55f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                WaultPrimaryMint.copy(alpha = 0.25f),
                                WaultPrimaryCyan.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(1.5.dp, WaultPrimaryMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = "POS Cihazına Yaklaştırın",
                    tint = WaultPrimaryMint,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Text(
            text = "POS Cihazına Yaklaştırın",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )

        Surface(
            shape = CircleShape,
            color = WaultSurfaceContainerDark.copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = WaultPrimaryMint.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onTriggerPaymentSuccess)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = WaultPrimaryMint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "$cardTitle • Ödemeyi Tamamla",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * Celebratory Apple Pay / Google Wallet style Payment Success animation with
 * progressive checkmark drawing, shockwave ripples, and particle burst.
 */
@Composable
fun PaymentSuccessAnimationCard(
    cardTitle: String,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val badgeScale = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val rippleProgress = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(cardTitle) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(90)
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280)
            )
        }
        launch {
            delay(120)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(80)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
        }

        delay(2100)
        onAnimationFinished()
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0D1712).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(WaultPrimaryMint, WaultPrimaryCyan)
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onAnimationFinished)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding shockwave rings + radial particle burst
                Canvas(modifier = Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f
                    val rp = rippleProgress.value

                    if (rp > 0f) {
                        drawCircle(
                            color = WaultPrimaryMint.copy(alpha = (1f - rp) * 0.55f),
                            radius = maxR * (0.5f + rp * 0.55f),
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = WaultPrimaryCyan.copy(alpha = (1f - rp) * 0.4f),
                            radius = maxR * (0.38f + rp * 0.45f),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 12 radial celebration particles
                        val particleCount = 12
                        for (i in 0 until particleCount) {
                            val angleRad = Math.toRadians((i * (360.0 / particleCount)))
                            val dist = maxR * (0.48f + rp * 0.52f)
                            val px = center.x + (cos(angleRad) * dist).toFloat()
                            val py = center.y + (sin(angleRad) * dist).toFloat()
                            drawCircle(
                                color = if (i % 2 == 0) {
                                    WaultPrimaryMint.copy(alpha = (1f - rp) * 0.9f)
                                } else {
                                    WaultPrimaryCyan.copy(alpha = (1f - rp) * 0.9f)
                                },
                                radius = (4.dp.toPx() * (1f - rp * 0.6f)).coerceAtLeast(1f),
                                center = Offset(px, py)
                            )
                        }
                    }
                }

                // Glowing Emerald Circle + Animated Vector Checkmark
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(badgeScale.value)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(WaultPrimaryMint, Color(0xFF16A34A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(38.dp)) {
                        val fullCheckPath = Path().apply {
                            moveTo(size.width * 0.18f, size.height * 0.52f)
                            lineTo(size.width * 0.43f, size.height * 0.76f)
                            lineTo(size.width * 0.84f, size.height * 0.26f)
                        }
                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(fullCheckPath, false)
                        val partialPath = Path()
                        pathMeasure.getSegment(
                            0f,
                            pathMeasure.length * checkProgress.value,
                            partialPath,
                            true
                        )

                        drawPath(
                            path = partialPath,
                            color = Color(0xFF03140A),
                            style = Stroke(
                                width = 4.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ödeme Başarılı",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$cardTitle ile temassız işlem tamamlandı",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WaultPrimaryMint
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

