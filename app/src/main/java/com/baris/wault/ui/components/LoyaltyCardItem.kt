package com.baris.wault.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.util.rememberStoredImageBitmap

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoyaltyCardItem(
    card: LoyaltyCardEntity,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isExpanded: Boolean = false,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customBitmap = rememberStoredImageBitmap(card.customImagePath)
    val shape = RoundedCornerShape(16.dp)

    val baseColor = Color(card.cardColorHex)
    val darkenedColor = Color(
        red = (baseColor.red * 0.72f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.72f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.72f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val categoryIcon = resolveLoyaltyIcon(card)

    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, darkenedColor),
                    start = Offset(0f, 0f),
                    end = Offset(420f, 340f)
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.46f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 44.dp translucent circular pill with category/brand icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = card.title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun resolveLoyaltyIcon(card: LoyaltyCardEntity): ImageVector {
    val combined = (card.title + " " + card.subtitle).lowercase()
    return when {
        combined.contains("starbucks") || combined.contains("kahve") ||
            combined.contains("cafe") || combined.contains("coffee") ||
            combined.contains("espresso") -> Icons.Rounded.LocalCafe

        combined.contains("gym") || combined.contains("spor") ||
            combined.contains("fitness") || combined.contains("macfit") -> Icons.Rounded.FitnessCenter

        combined.contains("market") || combined.contains("migros") ||
            combined.contains("carrefour") || combined.contains("a101") ||
            combined.contains("şok") || combined.contains("bim") -> Icons.Rounded.ShoppingBag

        combined.contains("kent") || combined.contains("istanbulkart") ||
            combined.contains("otobüs") || combined.contains("metro") ||
            combined.contains("transit") || combined.contains("ulaşım") -> Icons.Rounded.DirectionsTransit

        combined.contains("sinema") || combined.contains("bilet") ||
            combined.contains("etkinlik") -> Icons.Rounded.ConfirmationNumber

        card.barcodeFormat.isSquare -> Icons.Rounded.QrCode2
        else -> Icons.Rounded.Storefront
    }
}
