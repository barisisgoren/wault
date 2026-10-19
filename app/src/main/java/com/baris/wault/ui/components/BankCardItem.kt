package com.baris.wault.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.util.rememberStoredImageBitmap
import kotlin.math.abs

@Composable
fun BankCardItem(
    bank: BankPaymentApp,
    isExpanded: Boolean,
    isDefaultNfc: Boolean,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)
    val customCoverBitmap = rememberStoredImageBitmap(bank.customImagePath)

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused || isDragging -> 28.dp
            isExpanded -> 22.dp
            else -> 12.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BankCardElevation"
    )

    val gradientStart = bank.customColorHex?.let { Color(it) } ?: Color(bank.primaryColorArgb)
    val gradientEnd = bank.customColorHex?.let { base ->
        val c = Color(base)
        Color(
            red = (c.red * 0.36f).coerceIn(0f, 1f),
            green = (c.green * 0.36f).coerceIn(0f, 1f),
            blue = (c.blue * 0.36f).coerceIn(0f, 1f),
            alpha = 1f
        )
    } ?: Color(bank.secondaryColorArgb)

    val displaySuffix = bank.cardNumberSuffix
        ?: abs(bank.componentName.className.hashCode() % 9000 + 1000)
            .toString()
            .takeLast(4)

    val isMastercard = rememberNetworkIsMastercard(bank)
    val isSilverChip = abs(bank.flattenedComponent.hashCode()) % 3 == 0

    // Specular light-refraction top-edge gradient border
    val borderBrush = when {
        isFocused || isDragging -> Brush.verticalGradient(
            colors = listOf(
                WaultPrimaryCyan.copy(alpha = 0.9f),
                Color.White.copy(alpha = 0.22f)
            )
        )
        isDefaultNfc -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.50f),
                WaultPrimaryMint.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.08f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp)
            .shadow(
                elevation = elevation,
                shape = cardShape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 720f)
                )
            )
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = cardShape
            )
    ) {
        if (customCoverBitmap != null) {
            Image(
                bitmap = customCoverBitmap,
                contentDescription = bank.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.48f),
                                Color.Black.copy(alpha = 0.22f),
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
        } else {
            // Subtle glass sheen & depth highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.12f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.82f, size.height * 0.12f)
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.08f, 1.dp.toPx()),
                    end = Offset(size.width * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Left = Bank/Card Name (Bold), Right = Payment Network (VISA / Mastercard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (bank.iconBitmap != null) {
                        Image(
                            bitmap = bank.iconBitmap,
                            contentDescription = bank.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                    }

                    Text(
                        text = bank.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (bank.isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Ana Kart",
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                PaymentNetworkBadge(isMastercard = isMastercard)
            }

            // Middle-Left Area: Realistic Metallic EMV Chip + Contactless (NFC) Wave Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmvChipGraphic(isSilver = isSilverChip)

                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = "Temassız Ödeme",
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Bottom Row: Left = Masked Card Number (**** 4829), Right = Default Badge if active
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "**** $displaySuffix",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                        letterSpacing = 2.2.sp
                    )
                )

                if (isDefaultNfc) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.34f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            WaultPrimaryMint.copy(alpha = 0.65f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = WaultPrimaryMint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Varsayılan",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun rememberNetworkIsMastercard(bank: BankPaymentApp): Boolean {
    val lower = (bank.displayTitle + " " + bank.packageName).lowercase()
    return when {
        lower.contains("master") || lower.contains("bonus") || lower.contains("world") ||
            lower.contains("papara") || lower.contains("garanti") || lower.contains("yapikredi") -> true
        lower.contains("visa") || lower.contains("maximum") || lower.contains("ziraat") ||
            lower.contains("akbank") || lower.contains("qnb") -> false
        else -> abs(bank.flattenedComponent.hashCode()) % 2 == 1
    }
}

@Composable
private fun PaymentNetworkBadge(isMastercard: Boolean) {
    if (isMastercard) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 30.dp, height = 18.dp)) {
                val r = size.height * 0.48f
                drawCircle(
                    color = Color(0xFFEB001B).copy(alpha = 0.92f),
                    radius = r,
                    center = Offset(size.width * 0.36f, size.height * 0.5f)
                )
                drawCircle(
                    color = Color(0xFFF79E1B).copy(alpha = 0.90f),
                    radius = r,
                    center = Offset(size.width * 0.64f, size.height * 0.5f)
                )
            }
        }
    } else {
        Text(
            text = "VISA",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.8.sp,
                color = Color.White.copy(alpha = 0.95f)
            )
        )
    }
}

@Composable
private fun EmvChipGraphic(isSilver: Boolean = false) {
    Canvas(
        modifier = Modifier
            .width(44.dp)
            .height(32.dp)
    ) {
        val chipRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        val chipColors = if (isSilver) {
            listOf(
                Color(0xFFE5E7EB),
                Color(0xFF9CA3AF),
                Color(0xFFD1D5DB),
                Color(0xFF6B7280)
            )
        } else {
            listOf(
                Color(0xFFF3DC8A),
                Color(0xFFC59B38),
                Color(0xFFE8C86B),
                Color(0xFF9E7721)
            )
        }

        drawRoundRect(
            brush = Brush.linearGradient(colors = chipColors),
            cornerRadius = chipRadius
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            cornerRadius = chipRadius,
            style = Stroke(width = 1.dp.toPx())
        )

        val thirdH = size.height / 3f
        val thirdW = size.width / 3f
        val lineColor = Color.Black.copy(alpha = 0.22f)

        // Inner contact pad rounded rect
        drawRoundRect(
            color = lineColor,
            topLeft = Offset(thirdW * 0.85f, thirdH * 0.65f),
            size = Size(thirdW * 1.3f, thirdH * 1.7f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 0.9.dp.toPx())
        )

        drawLine(
            color = lineColor,
            start = Offset(0f, thirdH),
            end = Offset(thirdW * 0.85f, thirdH),
            strokeWidth = 0.9.dp.toPx()
        )
        drawLine(
            color = lineColor,
            start = Offset(0f, thirdH * 2f),
            end = Offset(thirdW * 0.85f, thirdH * 2f),
            strokeWidth = 0.9.dp.toPx()
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width - thirdW * 0.85f, thirdH),
            end = Offset(size.width, thirdH),
            strokeWidth = 0.9.dp.toPx()
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width - thirdW * 0.85f, thirdH * 2f),
            end = Offset(size.width, thirdH * 2f),
            strokeWidth = 0.9.dp.toPx()
        )
    }
}
