package com.baris.wault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.ui.theme.WaultFloatingPillBg
import com.baris.wault.ui.theme.WaultFloatingPillBorder

enum class WaultTab(
    val label: String,
    val icon: ImageVector
) {
    ALL(
        label = "Tümü",
        icon = Icons.Rounded.GridView
    ),
    PAYMENT(
        label = "Temassız Ödeme",
        icon = Icons.Rounded.Contactless
    ),
    PASSES(
        label = "Geçiş Kartları",
        icon = Icons.Rounded.QrCode2
    )
}

@Composable
fun WaultFloatingPill(
    onAllClick: () -> Unit = {},
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val pillShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = WaultFloatingPillBg,
            shape = pillShape,
            modifier = Modifier
                .shadow(
                    elevation = 24.dp,
                    shape = pillShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(pillShape)
                .border(
                    width = 1.dp,
                    color = WaultFloatingPillBorder,
                    shape = pillShape
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left: Active View Pill ("Tümü")
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAllClick()
                            }
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Tümü",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Tümü",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Right: Quick Barcode / Card Add (+) Circular Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.16f),
                            shape = CircleShape
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Yeni Kart Ekle",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
