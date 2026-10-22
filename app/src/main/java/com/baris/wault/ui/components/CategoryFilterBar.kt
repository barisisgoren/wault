package com.baris.wault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WaultCardCategory(val title: String) {
    ALL("Tüm Kartlar"),
    BANK_CREDIT("Banka & Kredi"),
    TRANSIT("Ulaşım"),
    MEMBERSHIPS("Üyelikler & Diğer")
}

/**
 * Horizontal scrollable category pill bar below Wault top bar:
 * - "Tüm Kartlar", "Banka & Kredi", "Ulaşım", "Üyelikler & Diğer"
 * - Active pill has luminous frosted background; inactive pills are subtle dark glass.
 */
@Composable
fun CategoryFilterBar(
    selectedCategory: WaultCardCategory,
    onCategorySelected: (WaultCardCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaultCardCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            val shape = RoundedCornerShape(12.dp)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0xFF1C1C1E),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                label = "CatBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0xFF2C2C2E),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                label = "CatBorder"
            )

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bgColor)
                    .border(width = 1.dp, color = borderColor, shape = shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCategorySelected(category)
                            }
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = (-0.1).sp
                    )
                )
            }
        }
    }
}

