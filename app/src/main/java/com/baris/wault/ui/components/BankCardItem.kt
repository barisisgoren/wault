package com.baris.wault.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.BankPaymentNetwork
import com.baris.wault.ui.theme.CardPatternType
import com.baris.wault.ui.theme.ProceduralCardThemes
import com.baris.wault.util.rememberStoredImageBitmap
import com.baris.wault.util.rememberStoredImageLuminance
import kotlin.math.abs

/**
 * Apple Wallet-grade Procedural Card Component (WalletCardComponent).
 * Built with procedural multi-stop gradients, brushed metal / carbon / transit patterns,
 * 3D photorealistic EMV chip, contactless waves, and authentic scheme badges.
 */
@Composable
fun BankCardItem(
    bank: BankPaymentApp,
    isExpanded: Boolean = true,
    isDefaultNfc: Boolean = false,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val customCoverBitmap = rememberStoredImageBitmap(bank.customImagePath)
    val customCoverLuminance = rememberStoredImageLuminance(bank.customImagePath)

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused || isDragging -> 28.dp
            isExpanded -> 18.dp
            else -> 10.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "AppleCardElevation"
    )

    // Resolve procedural theme or custom color
    val cardStyle = if (bank.customColorHex != null) {
        val base = Color(bank.customColorHex!!)
        val dark = Color(
            (base.red * 0.28f).coerceIn(0f, 1f),
            (base.green * 0.28f).coerceIn(0f, 1f),
            (base.blue * 0.28f).coerceIn(0f, 1f)
        )
        val deep = Color(
            (base.red * 0.12f).coerceIn(0f, 1f),
            (base.green * 0.12f).coerceIn(0f, 1f),
            (base.blue * 0.12f).coerceIn(0f, 1f)
        )
        com.baris.wault.ui.theme.ProceduralCardStyle(
            backgroundBrush = Brush.linearGradient(
                colors = listOf(base, dark, deep),
                start = Offset(0f, 0f),
                end = Offset(900f, 650f)
            ),
            isChipGold = true,
            patternType = CardPatternType.BRUSHED_METAL
        )
    } else {
        ProceduralCardThemes.resolve(
            titleOrPkg = "${bank.displayTitle} ${bank.packageName}",
            fallbackStart = bank.primaryColorArgb,
            fallbackEnd = bank.secondaryColorArgb
        )
    }

    val displaySuffix = bank.cardNumberSuffix
        ?: when {
            bank.packageName.contains("kentkart") || bank.displayTitle.contains("kentkart", ignoreCase = true) -> "2639"
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

    // Apple 1px Inner Specular Highlight along top and left edges
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.44f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 650f)
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
            .background(brush = cardStyle.backgroundBrush)
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
            // Smart Palette Luminance Scrim
            val topAlpha = if (customCoverLuminance > 0.45f) 0.60f else 0.42f
            val midAlpha = if (customCoverLuminance > 0.55f) 0.22f else 0.12f
            val botAlpha = if (customCoverLuminance > 0.45f) 0.76f else 0.64f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = topAlpha),
                                Color.Black.copy(alpha = midAlpha),
                                Color.Black.copy(alpha = botAlpha)
                            )
                        )
                    )
            )
        } else {
            // Procedural Micro-Texture and Watermark Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Radial ambient light at top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.82f, h * 0.12f),
                        radius = w * 0.58f
                    ),
                    radius = w * 0.58f,
                    center = Offset(w * 0.82f, h * 0.12f)
                )

                when (cardStyle.patternType) {
                    CardPatternType.TRANSIT_WAVES -> {
                        // Smooth flowing transit line arcs
                        val waveColor = Color.White.copy(alpha = 0.06f)
                        val waveStroke = 2.dp.toPx()
                        for (i in 0..4) {
                            val r = w * 0.45f + i * 28.dp.toPx()
                            drawArc(
                                color = waveColor,
                                startAngle = 180f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(w - r, h - r),
                                size = Size(r * 2f, r * 2f),
                                style = Stroke(width = waveStroke, cap = StrokeCap.Round)
                            )
                        }
                    }

                    CardPatternType.CARBON_WEAVE -> {
                        // Fine carbon weave pattern
                        val weaveColor = Color.White.copy(alpha = 0.022f)
                        val step = 14.dp.toPx()
                        var x = -h
                        while (x < w + h) {
                            drawLine(
                                color = weaveColor,
                                start = Offset(x, 0f),
                                end = Offset(x + h, h),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = weaveColor,
                                start = Offset(x + h, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1.dp.toPx()
                            )
                            x += step
                        }
                    }

                    CardPatternType.COBALT_ARCS -> {
                        // Geometric tech arcs
                        val arcColor = Color.White.copy(alpha = 0.045f)
                        for (i in 1..4) {
                            val r = h * 0.65f * i
                            drawCircle(
                                color = arcColor,
                                radius = r,
                                center = Offset(w * 0.10f, h * 1.1f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    CardPatternType.CLOVER_GLOW -> {
                        // Subtle 4-petal geometric clover watermark
                        val center = Offset(w * 0.78f, h * 0.65f)
                        val petalR = 36.dp.toPx()
                        val color = Color.White.copy(alpha = 0.035f)
                        drawCircle(color = color, radius = petalR, center = Offset(center.x - petalR * 0.5f, center.y))
                        drawCircle(color = color, radius = petalR, center = Offset(center.x + petalR * 0.5f, center.y))
                        drawCircle(color = color, radius = petalR, center = Offset(center.x, center.y - petalR * 0.5f))
                        drawCircle(color = color, radius = petalR, center = Offset(center.x, center.y + petalR * 0.5f))
                    }

                    CardPatternType.BRUSHED_METAL -> {
                        // Subtle fine brushed satin streaks
                        val step = 16.dp.toPx()
                        val sheenColor = Color.White.copy(alpha = 0.022f)
                        var x = -h
                        while (x < w + h) {
                            drawLine(
                                color = sheenColor,
                                start = Offset(x, 0f),
                                end = Offset(x + h * 0.6f, h),
                                strokeWidth = 1.dp.toPx()
                            )
                            x += step
                        }
                    }

                    CardPatternType.MINIMAL_OBSIDIAN -> {
                        // Deep matte finish with subtle bottom vignette
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                                startY = h * 0.4f,
                                endY = h
                            )
                        )
                    }
                }
            }
        }

        // ================= STANDART KART KATMANLARI =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. ÜST SATIR (SOL ÜST: İkon + Banka Adı | SAĞ ÜST: Şema Logosu + Favori Yıldızı)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Üst: Banka İkonu ve Başlık
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
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = cleanBankTitle(bank.displayTitle),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sağ Üst: Ödeme Şeması Logosu (Mastercard, Visa, Troy) + Favori Yıldızı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProceduralNetworkLogo(network = effectiveNetwork)

                    if (onToggleFavorite != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (bank.isFavorite) Color(0xFFFFD60A).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .border(
                                    width = 0.8.dp,
                                    color = if (bank.isFavorite) Color(0xFFFFD60A).copy(alpha = 0.65f)
                                    else Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onToggleFavorite
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (bank.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (bank.isFavorite) "Favori" else "Favoriye Ekle",
                                tint = if (bank.isFavorite) Color(0xFFFFD60A) else Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. ORTA-SOL (Aktif/Ön Kartta): 3D EMV Çip + Temassız Dalga Simgesi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PhotorealisticEmvChip(isGold = cardStyle.isChipGold)
                CardContactlessSymbol(tint = Color.White.copy(alpha = 0.85f))
            }

            // 3. ALT-SOL (Aktif/Ön Kartta): Maskelenmiş Kart Numarası (•••• 2639)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "•••• $displaySuffix",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 2.5.sp
                    )
                )

                // Subtle corporate watermark branding
                Text(
                    text = cleanBankTitle(bank.displayTitle).uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.40f)
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
