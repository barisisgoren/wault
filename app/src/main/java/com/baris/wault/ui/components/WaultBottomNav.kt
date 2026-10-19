package com.baris.wault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import com.baris.wault.ui.theme.WaultSurfaceDark

enum class WaultTab(
    val label: String,
    val icon: ImageVector
) {
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
fun WaultBottomNav(
    selectedTab: WaultTab,
    onSelectTab: (WaultTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        color = WaultSurfaceDark.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = WaultGlassBorderDark,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaultTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab

                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        WaultPrimaryMint.copy(alpha = 0.16f)
                    } else {
                        WaultSurfaceContainerDark.copy(alpha = 0.45f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TabPillBg"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        WaultPrimaryMint.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TabPillBorder"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) WaultPrimaryMint else Color(0xFF94A3B8),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TabContentColor"
                )

                val horizontalPad by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 14.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TabHorizontalPad"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(pillColor)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (selectedTab != tab) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectTab(tab)
                                }
                            }
                        )
                        .padding(horizontal = horizontalPad),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isSelected) Color.White else contentColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

