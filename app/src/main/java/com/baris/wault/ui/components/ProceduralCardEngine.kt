package com.baris.wault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentNetwork

/**
 * Photorealistic 3D Metallic EMV Smart Chip (ISO/IEC 7816 standard geometry).
 * Renders 6 distinct laser-cut metallic contact pads with subtle dark division grooves
 * and realistic gold/silver brushed metallic specular gradient.
 */
@Composable
fun PhotorealisticEmvChip(
    isGold: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(42.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())

        // 1. Base metallic substrate
        val metalBrush = if (isGold) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFECB3),
                    Color(0xFFFFD54F),
                    Color(0xFFFFB300),
                    Color(0xFFFFA000),
                    Color(0xFF8D6E63)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFE0E0E0),
                    Color(0xFFBDBDBD),
                    Color(0xFF757575)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        }

        drawRoundRect(
            brush = metalBrush,
            size = size,
            cornerRadius = corner
        )

        // 2. Chip outer bevel border
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 0.8.dp.toPx())
        )

        // 3. Etched Contact Pad Isolation Grooves (6 micro-contacts)
        val grooveColor = Color.Black.copy(alpha = 0.38f)
        val grooveHighlight = Color.White.copy(alpha = 0.28f)
        val strokeW = 0.75.dp.toPx()

        val y1 = h * 0.34f
        val y2 = h * 0.66f
        val xMid = w * 0.50f
        val centerBoxW = w * 0.38f
        val centerBoxH = h * 0.52f
        val centerBoxLeft = (w - centerBoxW) / 2f
        val centerBoxTop = (h - centerBoxH) / 2f

        // Center contact pad cutout
        drawRoundRect(
            color = grooveColor,
            topLeft = Offset(centerBoxLeft, centerBoxTop),
            size = Size(centerBoxW, centerBoxH),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = strokeW)
        )
        // Subtle highlight shadow under groove
        drawRoundRect(
            color = grooveHighlight,
            topLeft = Offset(centerBoxLeft + 0.5f, centerBoxTop + 0.5f),
            size = Size(centerBoxW, centerBoxH),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = strokeW * 0.5f)
        )

        // Horizontal division lines from outer edges to center pad
        // Top-left to center
        drawLine(grooveColor, Offset(0f, y1), Offset(centerBoxLeft, y1), strokeWidth = strokeW)
        drawLine(grooveHighlight, Offset(0f, y1 + 0.5f), Offset(centerBoxLeft, y1 + 0.5f), strokeWidth = strokeW * 0.5f)

        // Bottom-left to center
        drawLine(grooveColor, Offset(0f, y2), Offset(centerBoxLeft, y2), strokeWidth = strokeW)
        drawLine(grooveHighlight, Offset(0f, y2 + 0.5f), Offset(centerBoxLeft, y2 + 0.5f), strokeWidth = strokeW * 0.5f)

        // Top-right to center
        drawLine(grooveColor, Offset(centerBoxLeft + centerBoxW, y1), Offset(w, y1), strokeWidth = strokeW)
        drawLine(grooveHighlight, Offset(centerBoxLeft + centerBoxW, y1 + 0.5f), Offset(w, y1 + 0.5f), strokeWidth = strokeW * 0.5f)

        // Bottom-right to center
        drawLine(grooveColor, Offset(centerBoxLeft + centerBoxW, y2), Offset(w, y2), strokeWidth = strokeW)
        drawLine(grooveHighlight, Offset(centerBoxLeft + centerBoxW, y2 + 0.5f), Offset(w, y2 + 0.5f), strokeWidth = strokeW * 0.5f)

        // Vertical center division line inside center pad
        drawLine(
            grooveColor,
            Offset(xMid, centerBoxTop),
            Offset(xMid, centerBoxTop + centerBoxH),
            strokeWidth = strokeW
        )
    }
}

/**
 * Authentic Contactless Wave Symbol (4 concentric rounded arcs).
 */
@Composable
fun CardContactlessSymbol(
    tint: Color = Color.White.copy(alpha = 0.85f),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeW = 1.6.dp.toPx()
        val centerX = size.width * 0.15f
        val centerY = size.height * 0.5f

        val radii = listOf(size.width * 0.32f, size.width * 0.52f, size.width * 0.72f, size.width * 0.92f)
        val sweepAngle = 76f
        val startAngle = -sweepAngle / 2f

        for (r in radii) {
            drawArc(
                color = tint,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerX - r, centerY - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Precision Payment Scheme Logos (Mastercard, VISA, TROY, AMEX)
 * designed to look identical to official physical payment cards in Apple Wallet.
 */
@Composable
fun ProceduralNetworkLogo(
    network: BankPaymentNetwork,
    modifier: Modifier = Modifier
) {
    when (network) {
        BankPaymentNetwork.MASTERCARD -> {
            // Authentic Mastercard overlapping circles with center Venn lens
            Canvas(modifier = modifier.size(width = 34.dp, height = 22.dp)) {
                val r = size.height * 0.48f
                val c1 = Offset(size.width * 0.36f, size.height * 0.5f)
                val c2 = Offset(size.width * 0.64f, size.height * 0.5f)

                // Left red circle
                drawCircle(
                    color = Color(0xFFEB001B),
                    radius = r,
                    center = c1
                )
                // Right yellow/orange circle
                drawCircle(
                    color = Color(0xFFF79E1B),
                    radius = r,
                    center = c2
                )
                // Translucent overlapping center intersection (giving official Mastercard depth)
                drawCircle(
                    color = Color(0xFFFF5F00).copy(alpha = 0.95f),
                    radius = r * 0.92f,
                    center = Offset((c1.x + c2.x) / 2f, size.height * 0.5f)
                )
            }
        }

        BankPaymentNetwork.TROY -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "troy",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                )
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF00C0F3))
                )
            }
        }

        BankPaymentNetwork.AMEX -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF006FCF))
                    .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AMEX",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                )
            }
        }

        else -> {
            // VISA italic bold wordmark
            Text(
                text = "VISA",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.8.sp,
                    color = Color.White
                ),
                modifier = modifier
            )
        }
    }
}
