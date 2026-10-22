package com.baris.wault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
        label = "Ödeme",
        icon = Icons.Rounded.Contactless
    ),
    PASSES(
        label = "Diğer",
        icon = Icons.Rounded.QrCode2
    )
}

@Composable
fun WaultFloatingPill(
    currentTab: WaultTab = WaultTab.ALL,
    onTabSelected: (WaultTab) -> Unit,
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
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WaultTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
                        label = "TabBgColor"
                    )
                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
                        label = "TabBorderColor"
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(animatedBgColor)
                            .border(
                                width = 0.8.dp,
                                color = animatedBorderColor,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isSelected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(tab)
                                    }
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
