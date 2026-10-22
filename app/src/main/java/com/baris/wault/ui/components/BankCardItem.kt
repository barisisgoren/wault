package com.baris.wault.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.BankPaymentNetwork
import com.baris.wault.ui.theme.AppleWalletBankGradients
import com.baris.wault.ui.theme.CardGrain
import com.baris.wault.ui.theme.LocalCardSheen
import com.baris.wault.ui.theme.WalletCardStyle
import com.baris.wault.ui.theme.WalletCardStyles
import com.baris.wault.ui.theme.WaultTokens
import com.baris.wault.ui.theme.rememberReduceMotion
import com.baris.wault.util.rememberStoredImageBitmap
import com.baris.wault.util.rememberStoredImageLuminance
import kotlin.math.abs

/**
 * Wallet card (presentation only).
 * - Collapsed (peek strip): 40dp logo tile, name, network badge.
 * - Expanded: + chip, contactless, masked number, network badge bottom-right, small favourite icon.
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
    val cardShape = RoundedCornerShape(WaultTokens.CardRadius)
    val customCoverBitmap = rememberStoredImageBitmap(bank.customImagePath)
    val customCoverLuminance = rememberStoredImageLuminance(bank.customImagePath)
    val reduceMotion = rememberReduceMotion()
    val sheen = LocalCardSheen.current

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused || isDragging -> 24.dp
            isExpanded -> 14.dp
            else -> 6.dp
        },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
        label = "CardElevation"
    )

    // One tonal system: brand hue, constrained saturation/lightness. Custom colour is honoured.
    val style: WalletCardStyle = remember(bank.customColorHex, bank.displayTitle, bank.packageName, bank.primaryColorArgb) {
        if (bank.customColorHex != null) {
            WalletCardStyles.fromCustom(Color(bank.customColorHex!!))
        } else {
            val brand = AppleWalletBankGradients.resolveForBank(
                titleOrPkg = "${bank.displayTitle} ${bank.packageName}",
                fallbackStartArgb = bank.primaryColorArgb,
                fallbackEndArgb = bank.secondaryColorArgb
            ).start
            WalletCardStyles.fromBrand(brand)
        }
    }
    val backgroundBrush = remember(style) { WalletCardStyles.brush(style) }

    val hasImage = customCoverBitmap != null
    val onCard = if (hasImage) Color(0xFFF5F5F7) else style.onCard
    val onCardSecondary = if (hasImage) Color(0xFFF5F5F7).copy(alpha = 0.78f) else style.onCardSecondary

    val displaySuffix = bank.cardNumberSuffix
        ?: when {
            bank.packageName.contains("kentkart") || bank.displayTitle.contains("kentkart", ignoreCase = true) -> "2639"
            bank.packageName.contains("akbank") -> "0454"
            bank.packageName.contains("garanti") -> "8219"
            bank.packageName.contains("iscep") -> "3940"
            bank.packageName.contains("ykb") -> "6108"
            bank.packageName.contains("ziraat") -> "1923"
            else -> abs(bank.componentName.className.hashCode() % 9000 + 1000).toString().takeLast(4)
        }
    val network = resolveEffectivePaymentNetwork(bank)

    val expandedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 220),
        label = "ExpandedAlpha"
    )
    val titleSize by animateFloatAsState(
        targetValue = if (isExpanded) 18f else 16f,
        animationSpec = tween(if (reduceMotion) 0 else 250),
        label = "TitleSize"
    )

    // Top + left specular edge (1dp) like a physical card edge
    val edgeBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f)),
            start = Offset(0f, 0f),
            end = Offset(500f, 700f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WaultTokens.CardHeight)
            .shadow(
                elevation = elevation,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black
            )
            .clip(cardShape)
            .background(backgroundBrush)
            .border(1.dp, edgeBrush, cardShape)
    ) {
        if (customCoverBitmap != null) {
            Image(
                bitmap = customCoverBitmap,
                contentDescription = bank.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val top = if (customCoverLuminance > 0.45f) 0.60f else 0.42f
            val mid = if (customCoverLuminance > 0.55f) 0.22f else 0.12f
            val bottom = if (customCoverLuminance > 0.45f) 0.76f else 0.64f
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = top), Color.Black.copy(alpha = mid), Color.Black.copy(alpha = bottom))
                        )
                    )
            )
        } else {
            // Fine grain (cached shader) + reactive light sheen. State read only in the draw phase.
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(CardGrain.brush, alpha = 0.55f)
                        val pos = if (reduceMotion) 0.25f else sheen.value
                        val cx = size.width * (0.5f + 0.5f * pos)
                        val half = size.width * 0.42f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.07f * (0.4f + 0.6f * expandedAlpha)), Color.Transparent),
                                startX = cx - half,
                                endX = cx + half
                            )
                        )
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = WaultTokens.Space16, end = WaultTokens.Space16, top = WaultTokens.Space12, bottom = WaultTokens.Space16),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---- Header row: logo tile + name | network (collapsed) / favourite (expanded) ----
            Row(
                modifier = Modifier.fillMaxWidth().height(WaultTokens.LogoTile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogoTile(bank = bank)
                Spacer(Modifier.width(WaultTokens.Space12))
                Text(
                    text = cleanBankTitle(bank.displayTitle),
                    style = WaultTokens.CardTitle.copy(fontSize = titleSize.sp, color = onCard),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(contentAlignment = Alignment.CenterEnd) {
                    ProceduralNetworkLogo(
                        network = network,
                        tint = onCard,
                        modifier = Modifier.alpha(1f - expandedAlpha)
                    )
                    if (onToggleFavorite != null && isExpanded) {
                        Box(
                            modifier = Modifier
                                .size(WaultTokens.MinTouch)
                                .alpha(expandedAlpha)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onToggleFavorite
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (bank.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (bank.isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                                tint = if (bank.isFavorite) WaultTokens.Favorite else onCardSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ---- Chip + contactless (expanded content) ----
            Row(
                modifier = Modifier.alpha(expandedAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WaultTokens.Space12)
            ) {
                PhotorealisticEmvChip(isGold = !style.isNeutralChip)
                CardContactlessSymbol(tint = onCardSecondary)
            }

            // ---- Masked number (left) + network badge (right) ----
            Row(
                modifier = Modifier.fillMaxWidth().alpha(expandedAlpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "•••• $displaySuffix",
                    style = WaultTokens.CardDigits.copy(color = onCard, fontWeight = FontWeight.Medium)
                )
                ProceduralNetworkLogo(network = network, tint = onCard)
            }
        }
    }
}

/** Uniform 40dp brand tile — logo is always fitted (never cropped) on a neutral plate. */
@Composable
private fun LogoTile(bank: BankPaymentApp) {
    Box(
        modifier = Modifier
            .size(WaultTokens.LogoTile)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F7)),
        contentAlignment = Alignment.Center
    ) {
        if (bank.iconBitmap != null) {
            Image(
                bitmap = bank.iconBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(5.dp)
            )
        } else {
            Text(
                text = cleanBankTitle(bank.displayTitle).take(1).uppercase(),
                style = WaultTokens.CardTitle.copy(color = Color(0xFF1C1C1E))
            )
        }
    }
}

private fun cleanBankTitle(rawTitle: String): String {
    val trimmed = rawTitle.trim()
    if (trimmed.contains('.') && !trimmed.contains(' ')) {
        return trimmed.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
    return trimmed
}

private fun resolveEffectivePaymentNetwork(bank: BankPaymentApp): BankPaymentNetwork {
    if (bank.paymentNetwork != BankPaymentNetwork.AUTO) return bank.paymentNetwork
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
        else -> if (abs(bank.flattenedComponent.hashCode()) % 2 == 1) BankPaymentNetwork.MASTERCARD else BankPaymentNetwork.VISA
    }
}
