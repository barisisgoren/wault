package com.baris.wault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ViewWeek
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.util.rememberStoredImageBitmap

@Composable
fun LoyaltyCardItem(
    card: LoyaltyCardEntity,
    isExpanded: Boolean = false,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customBitmap = rememberStoredImageBitmap(card.customImagePath)
    val showExpandedContent = isExpanded || isFocused

    val targetHeight by animateDpAsState(
        targetValue = if (showExpandedContent) 186.dp else 142.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PassCardHeight"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused || isDragging -> 26.dp
            isExpanded -> 18.dp
            else -> 10.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PassElevation"
    )

    val baseColor = Color(card.cardColorHex)
    val darkenedColor = Color(
        red = (baseColor.red * 0.50f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.50f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.50f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val contentColor = if (customBitmap != null) {
        Color.White
    } else if (baseColor.luminance() > 0.55f) {
        Color(0xFF0F172A)
    } else {
        Color.White
    }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(targetHeight)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (isFocused || isDragging || isExpanded) WaultPrimaryMint else Color.Black,
                spotColor = if (isFocused || isDragging || isExpanded) WaultPrimaryCyan else Color.Black
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, darkenedColor),
                    start = Offset(0f, 0f),
                    end = Offset(720f, 440f)
                )
            )
            .border(
                width = if (isFocused || isDragging) 2.dp else if (isExpanded) 1.5.dp else 1.dp,
                color = when {
                    isFocused || isDragging -> WaultPrimaryCyan
                    isExpanded -> WaultPrimaryMint.copy(alpha = 0.65f)
                    else -> Color.White.copy(alpha = 0.16f)
                },
                shape = shape
            )
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
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.58f),
                                Color.Black.copy(alpha = 0.32f),
                                Color.Black.copy(alpha = 0.74f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header (Always visible in 58.dp peek stack)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = contentColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = card.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = contentColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = contentColor,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (card.subtitle.isNotBlank()) {
                            Text(
                                text = card.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = contentColor.copy(alpha = 0.76f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (card.barcodeFormat.isSquare) {
                                    Icons.Rounded.QrCode2
                                } else {
                                    Icons.Rounded.ViewWeek
                                },
                                contentDescription = card.barcodeFormat.displayName,
                                tint = contentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = card.barcodeFormat.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Expanded Details on the Front / Focused Pass
            AnimatedVisibility(
                visible = showExpandedContent,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!card.notes.isNullOrBlank()) {
                        Text(
                            text = card.notes,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = contentColor.copy(alpha = 0.8f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.barcodeValue,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = contentColor,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            shape = CircleShape,
                            color = WaultPrimaryMint.copy(alpha = 0.24f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                WaultPrimaryMint.copy(alpha = 0.75f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = null,
                                    tint = WaultPrimaryMint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Barkodu Aç",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
