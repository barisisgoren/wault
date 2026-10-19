package com.baris.wault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.DragHandle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.util.rememberStoredImageBitmap

@Composable
fun BankCardItem(
    bank: BankPaymentApp,
    isExpanded: Boolean,
    isDefaultNfc: Boolean,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val customCoverBitmap = rememberStoredImageBitmap(bank.customImagePath)

    val elevation by animateDpAsState(
        targetValue = when {
            isFocused || isDragging -> 28.dp
            isExpanded -> 20.dp
            else -> 10.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BankCardElevation"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused || isDragging -> WaultPrimaryCyan
            isDefaultNfc -> WaultPrimaryMint.copy(alpha = 0.8f)
            else -> Color.White.copy(alpha = 0.14f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BankCardBorderColor"
    )

    val gradientStart = bank.customColorHex?.let { Color(it) } ?: Color(bank.primaryColorArgb)
    val gradientEnd = bank.customColorHex?.let { base ->
        val c = Color(base)
        Color(
            red = (c.red * 0.42f).coerceIn(0f, 1f),
            green = (c.green * 0.42f).coerceIn(0f, 1f),
            blue = (c.blue * 0.42f).coerceIn(0f, 1f),
            alpha = 1f
        )
    } ?: Color(bank.secondaryColorArgb)

    val displaySuffix = bank.cardNumberSuffix
        ?: (bank.componentName.className.hashCode() and 0xFFFF)
            .toString()
            .padStart(4, '0')
            .takeLast(4)

    val showFullDetails = isExpanded || isFocused

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(216.dp)
            .shadow(
                elevation = elevation,
                shape = cardShape,
                ambientColor = if (isDefaultNfc || isFocused || isDragging) WaultPrimaryMint else Color.Black,
                spotColor = if (isDefaultNfc || isFocused || isDragging) WaultPrimaryCyan else Color.Black
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 750f)
                )
            )
            .border(
                width = if (isDefaultNfc || isFocused || isDragging) 2.dp else 1.dp,
                color = borderColor,
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
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.74f)
                            )
                        )
                    )
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.88f, size.height * 0.14f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = size.width * 0.38f,
                    center = Offset(size.width * 0.14f, size.height * 0.90f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header (Always visible in peek stack)
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
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        if (bank.iconBitmap != null) {
                            Image(
                                bitmap = bank.iconBitmap,
                                contentDescription = bank.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(7.dp))
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = bank.displayTitle,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = bank.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
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
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!showFullDetails) {
                        Text(
                            text = "•••• $displaySuffix",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.42f),
                        modifier = Modifier.size(18.dp)
                    )

                    Icon(
                        imageVector = Icons.Rounded.Contactless,
                        contentDescription = null,
                        tint = if (isDefaultNfc) WaultPrimaryMint else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Middle & Bottom Details (Shown on expanded top card or focused card)
            AnimatedVisibility(
                visible = showFullDetails,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EmvChipGraphic()

                        Text(
                            text = "•••• $displaySuffix",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.92f),
                                letterSpacing = 2.sp
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bank.appName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White.copy(alpha = 0.74f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isDefaultNfc) {
                            Surface(
                                shape = CircleShape,
                                color = WaultPrimaryMint.copy(alpha = 0.22f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    WaultPrimaryMint.copy(alpha = 0.75f)
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
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Varsayılan",
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
}

@Composable
private fun EmvChipGraphic() {
    Canvas(
        modifier = Modifier
            .width(42.dp)
            .height(30.dp)
    ) {
        val chipRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE2C878),
                    Color(0xFFB8963E),
                    Color(0xFFD9BE6C)
                )
            ),
            cornerRadius = chipRadius
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.25f),
            cornerRadius = chipRadius,
            style = Stroke(width = 1.dp.toPx())
        )
        val thirdH = size.height / 3f
        val thirdW = size.width / 3f
        drawLine(
            color = Color.Black.copy(alpha = 0.22f),
            start = Offset(0f, thirdH),
            end = Offset(size.width, thirdH),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.22f),
            start = Offset(0f, thirdH * 2f),
            end = Offset(size.width, thirdH * 2f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.22f),
            start = Offset(thirdW, 0f),
            end = Offset(thirdW, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.22f),
            start = Offset(thirdW * 2f, 0f),
            end = Offset(thirdW * 2f, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
}
