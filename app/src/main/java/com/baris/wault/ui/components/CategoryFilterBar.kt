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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.baris.wault.ui.theme.WaultTokens

enum class WaultCardCategory(val title: String) {
    ALL("Tüm Kartlar"),
    BANK_CREDIT("Banka & Kredi"),
    TRANSIT("Ulaşım"),
    MEMBERSHIPS("Üyelikler & Diğer")
}

/**
 * Horizontal category chips.
 * Selected = solid light chip with dark text; unselected = quiet raised chip.
 * 44dp touch height and a fading edge that signals more content when scrollable.
 */
@Composable
fun CategoryFilterBar(
    selectedCategory: WaultCardCategory,
    onCategorySelected: (WaultCardCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = WaultTokens.Space16 + 4.dp),
            horizontalArrangement = Arrangement.spacedBy(WaultTokens.Space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaultCardCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                val bg by animateColorAsState(
                    targetValue = if (isSelected) WaultTokens.TextPrimary else WaultTokens.Bg2,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f),
                    label = "CatBg"
                )
                val border by animateColorAsState(
                    targetValue = if (isSelected) Color.Transparent else WaultTokens.Stroke,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f),
                    label = "CatBorder"
                )
                val shape = RoundedCornerShape(WaultTokens.ControlRadius)

                // Outer box guarantees the 44dp touch target; inner pill is the visual chip.
                Box(
                    modifier = Modifier
                        .heightIn(min = WaultTokens.MinTouch)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCategorySelected(category)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(bg)
                            .border(1.dp, border, shape)
                            .padding(horizontal = WaultTokens.Space16, vertical = WaultTokens.Space8 + 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.title,
                            style = WaultTokens.Label.copy(
                                color = if (isSelected) Color.Black else WaultTokens.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Fading edge: hints that the row scrolls further.
        if (scrollState.canScrollForward) {
            val fade = listOf(Color.Transparent, WaultTokens.Bg0)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(36.dp)
                    .background(
                        Brush.horizontalGradient(if (isRtl) fade.reversed() else fade)
                    )
            )
        }
    }
}
