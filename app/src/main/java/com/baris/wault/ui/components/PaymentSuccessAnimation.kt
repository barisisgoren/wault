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
 * Minimal, professional Apple Pay style Payment Success indicator.
 * Matches the exact geometry of the NFC prompt circle so the transition feels seamless:
 * a crisp circular badge with a smooth progressive checkmark stroke and clean typography.
 */
@Composable
fun PaymentSuccessAnimationCard(
    cardTitle: String,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    val circleScale = remember { Animatable(0.84f) }
    val circleAlpha = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(cardTitle) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            circleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
        }
        launch {
            circleScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            ringProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(110)
            checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(80)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        }

        delay(1450)
        onAnimationFinished()
    }

    val accentGreen = Color(0xFF30D158)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onAnimationFinished
            )
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .scale(circleScale.value),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(54.dp)) {
                val strokeWidthPx = 2.dp.toPx()
                val radius = (size.minDimension - strokeWidthPx) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Subtle dark elevated fill matching the NFC prompt circle
                drawCircle(
                    color = Color(0xFF1C1C1E).copy(alpha = circleAlpha.value),
                    radius = radius,
                    center = center
                )

                // Soft inner tint
                drawCircle(
                    color = accentGreen.copy(alpha = 0.14f * circleAlpha.value),
                    radius = radius,
                    center = center
                )

                // Smoothly drawn perimeter ring
                drawArc(
                    color = accentGreen.copy(alpha = circleAlpha.value),
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress.value,
                    useCenter = false,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Minimal progressive checkmark
                if (checkProgress.value > 0f) {
                    val checkPath = Path().apply {
                        moveTo(size.width * 0.30f, size.height * 0.52f)
                        lineTo(size.width * 0.45f, size.height * 0.66f)
                        lineTo(size.width * 0.72f, size.height * 0.37f)
                    }
                    val measure = PathMeasure()
                    measure.setPath(checkPath, false)
                    val segment = Path()
                    measure.getSegment(
                        0f,
                        measure.length * checkProgress.value,
                        segment,
                        true
                    )
                    drawPath(
                        path = segment,
                        color = accentGreen,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Tamamlandı",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = textAlpha.value),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = cardTitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.55f * textAlpha.value),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

