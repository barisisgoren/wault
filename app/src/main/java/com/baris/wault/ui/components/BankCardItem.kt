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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.BankPaymentNetwork
import com.baris.wault.ui.theme.AppleWalletBankGradients
import com.baris.wault.util.rememberStoredImageBitmap
import kotlin.math.abs

@Composable
fun BankCardItem(
    bank: BankPaymentApp,
    isExpanded: Boolean = true,
    isDefaultNfc: Boolean = false,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val customCoverBitmap = rememberStoredImageBitmap(bank.customImagePath)

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused || isDragging -> 30.dp
            isExpanded -> 22.dp
            else -> 12.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "AppleCardElevation"
    )

    val palette = if (bank.customColorHex != null) {
        val base = Color(bank.customColorHex!!)
        val dark = Color(
            red = (base.red * 0.34f).coerceIn(0f, 1f),
            green = (base.green * 0.34f).coerceIn(0f, 1f),
            blue = (base.blue * 0.34f).coerceIn(0f, 1f),
            alpha = 1f
        )
        base to dark
    } else {
        val resolved = AppleWalletBankGradients.resolveForBank(
            titleOrPkg = "${bank.displayTitle} ${bank.packageName}",
            fallbackStartArgb = bank.primaryColorArgb,
            fallbackEndArgb = bank.secondaryColorArgb
        )
        resolved.start to resolved.end
    }

    val displaySuffix = bank.cardNumberSuffix
        ?: when {
            bank.packageName.contains("akbank") -> "0454"
            bank.packageName.contains("garanti") -> "8219"
            bank.packageName.contains("iscep") -> "3940"
            bank.packageName.contains("ykb") -> "6108"
            bank.packageName.contains("ziraat") -> "1923"
            else -> abs(bank.componentName.className.hashCode() % 9000 + 1000)
                .toString()
                .takeLast(4)
        }

    val effectiveNetwork = resolveEffectivePaymentNetwork(bank)

    // Subtle specular glass border on top edge like a physical metallic Apple Pay card
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.34f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.04f)
        )
    )

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
                    colors = listOf(palette.first, palette.second),
                    start = Offset(0f, 0f),
                    end = Offset(980f, 680f)
                )
            )
            .border(
                width = 0.9.dp,
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
                                Color.Black.copy(alpha = 0.44f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.68f)
                            )
                        )
                    )
            )
        } else {
            // Subtle anodized metallic sheen
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.84f, size.height * 0.10f),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.84f, size.height * 0.10f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row (Visible in 32.dp peek strip):
            // Left = Clean Bank Title, Right = Payment Network Logo (VISA / Mastercard / TROY / Amex)
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
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = cleanBankTitle(bank.displayTitle),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                AppleCardNetworkLogo(network = effectiveNetwork)
            }

            // Middle-Left: Realistic Gold EMV Chip + Contactless Waves Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldEmvChip()

                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(23.dp)
                )
            }

            // Bottom-Left: ONLY Masked Card Number (•••• 0454) — zero technical clutter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "•••• $displaySuffix",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.90f),
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }
}

private fun cleanBankTitle(rawTitle: String): String {
    val trimmed = rawTitle.trim()
    if (trimmed.contains('.') && !trimmed.contains(' ')) {
        val lastSegment = trimmed.substringAfterLast('.')
        return lastSegment.replaceFirstChar { it.uppercase() }
    }
    return trimmed
}

private fun resolveEffectivePaymentNetwork(bank: BankPaymentApp): BankPaymentNetwork {
    if (bank.paymentNetwork != BankPaymentNetwork.AUTO) {
        return bank.paymentNetwork
    }
    val lower = (bank.displayTitle + " " + bank.packageName).lowercase()
    return when {
        lower.contains("troy") || lower.contains("ziraat") || lower.contains("vakıf") ||
            lower.contains("vakif") || lower.contains("halk") -> BankPaymentNetwork.TROY
        lower.contains("amex") || lower.contains("american") || lower.contains("wings") -> BankPaymentNetwork.AMEX
        lower.contains("master") || lower.contains("garanti") || lower.contains("bonus") ||
            lower.contains("ykb") || lower.contains("yapı") || lower.contains("world") ||
            lower.contains("kentkart") -> BankPaymentNetwork.MASTERCARD
        lower.contains("visa") || lower.contains("akbank") || lower.contains("juzdan") ||
            lower.contains("iscep") || lower.contains("iş") || lower.contains("maximum") ||
            lower.contains("paycell") -> BankPaymentNetwork.VISA
        else -> if (abs(bank.flattenedComponent.hashCode()) % 2 == 1) {
            BankPaymentNetwork.MASTERCARD
        } else {
            BankPaymentNetwork.VISA
        }
    }
}

@Composable
private fun AppleCardNetworkLogo(network: BankPaymentNetwork) {
    when (network) {
        BankPaymentNetwork.MASTERCARD -> {
            Canvas(modifier = Modifier.size(width = 32.dp, height = 20.dp)) {
                val r = size.height * 0.48f
                drawCircle(
                    color = Color(0xFFEB001B).copy(alpha = 0.94f),
                    radius = r,
                    center = Offset(size.width * 0.36f, size.height * 0.5f)
                )
                drawCircle(
                    color = Color(0xFFF79E1B).copy(alpha = 0.92f),
                    radius = r,
                    center = Offset(size.width * 0.64f, size.height * 0.5f)
                )
            }
        }

        BankPaymentNetwork.TROY -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "troy",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = Color.White
                    )
                )
            }
        }

        BankPaymentNetwork.AMEX -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0A84FF).copy(alpha = 0.32f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AMEX",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                )
            }
        }

        else -> {
            Text(
                text = "VISA",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.8.sp,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
private fun GoldEmvChip() {
    Canvas(
        modifier = Modifier
            .width(44.dp)
            .height(32.dp)
    ) {
        val chipRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF4DF90),
                    Color(0xFFC89E3A),
                    Color(0xFFE9CB70),
                    Color(0xFFA27B24)
                )
            ),
            cornerRadius = chipRadius
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.26f),
            cornerRadius = chipRadius,
            style = Stroke(width = 0.9.dp.toPx())
        )

        val thirdH = size.height / 3f
        val thirdW = size.width / 3f
        val lineColor = Color.Black.copy(alpha = 0.22f)

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
