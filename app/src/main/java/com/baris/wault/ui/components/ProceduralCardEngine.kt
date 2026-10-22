package com.baris.wault.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Refined, fully vector EMV chip: muted champagne metal, thin etched contact lines.
 */
@Composable
fun PhotorealisticEmvChip(
    isGold: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 40.dp, height = 30.dp)) {
        val w = size.width
        val h = size.height
        val r = CornerRadius(6.dp.toPx())
        val metal = if (isGold) {
            listOf(Color(0xFFE8D9B0), Color(0xFFCDB682), Color(0xFFE2D2A6))
        } else {
            listOf(Color(0xFFE4E4E8), Color(0xFFBDBDC4), Color(0xFFDADADF))
        }
        drawRoundRect(
            brush = Brush.linearGradient(metal, start = Offset.Zero, end = Offset(w, h)),
            cornerRadius = r
        )
        val line = Color.Black.copy(alpha = 0.26f)
        val sw = 0.8.dp.toPx()
        // etched contact layout
        val cx = w * 0.5f
        val padL = w * 0.34f
        val padR = w * 0.66f
        val y1 = h * 0.36f
        val y2 = h * 0.64f
        drawLine(line, Offset(0f, y1), Offset(padL, y1), sw)
        drawLine(line, Offset(0f, y2), Offset(padL, y2), sw)
        drawLine(line, Offset(padR, y1), Offset(w, y1), sw)
        drawLine(line, Offset(padR, y2), Offset(w, y2), sw)
        drawLine(line, Offset(cx, 0f), Offset(cx, y1), sw)
        drawLine(line, Offset(cx, y2), Offset(cx, h), sw)
        drawRoundRect(
            color = line,
            topLeft = Offset(padL, y1),
            size = Size(padR - padL, y2 - y1),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(sw)
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.18f),
            cornerRadius = r,
            style = Stroke(sw)
        )
    }
}

/** Contactless indicator drawn as three clean arcs. */
@Composable
fun CardContactlessSymbol(
    tint: Color = Color.White.copy(alpha = 0.85f),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val sw = 1.7.dp.toPx()
        val cy = size.height / 2f
        val sweep = 72f
        for (i in 1..3) {
            val r = size.width * (0.22f + i * 0.24f)
            drawArc(
                color = tint.copy(alpha = tint.alpha * (1.0f - (i - 1) * 0.18f)),
                startAngle = -sweep / 2f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(-r + size.width * 0.12f, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Uniform network badge: fixed 48x24dp slot, single optical weight, no floating pills.
 */
@Composable
fun ProceduralNetworkLogo(
    network: BankPaymentNetwork,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .height(24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        when (network) {
            BankPaymentNetwork.MASTERCARD -> Canvas(Modifier.size(width = 36.dp, height = 22.dp)) {
                val r = size.height * 0.5f
                val c1 = Offset(size.width * 0.34f, size.height / 2f)
                val c2 = Offset(size.width * 0.66f, size.height / 2f)
                drawCircle(Color(0xFFEB001B), r, c1)
                drawCircle(Color(0xFFF79E1B), r, c2)
                drawCircle(Color(0xFFFF5F00), r * 0.62f, Offset(size.width / 2f, size.height / 2f))
            }

            BankPaymentNetwork.TROY -> Text(
                text = "troy",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    color = tint
                )
            )

            BankPaymentNetwork.AMEX -> Text(
                text = "AMEX",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = tint
                )
            )

            else -> Text(
                text = "VISA",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.4.sp,
                    color = tint
                )
            )
        }
    }
}
