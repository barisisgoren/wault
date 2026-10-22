package com.baris.wault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.SilentSwitchCapability
import com.baris.wault.ui.theme.WaultAmoledBlack
import com.baris.wault.ui.theme.WaultShizukuActiveDot
import com.baris.wault.ui.theme.WaultShizukuInactiveDot
import com.baris.wault.ui.theme.WaultTopActionBg

@Composable
fun WaultTopBar(
    capability: SilentSwitchCapability,
    onStatusPillClick: () -> Unit,
    onQuickAddClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isReady = capability.isSilentReady
    val dotColor by animateColorAsState(
        targetValue = if (isReady) {
            WaultShizukuActiveDot.copy(alpha = 0.9f)
        } else {
            WaultShizukuInactiveDot.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "ShizukuDotColor"
    )

    val capsuleShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WaultAmoledBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Large, clean, bold "Wault" title (32.sp) + Minimal 24.dp "Shizuku" capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = "Wault",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.9).sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .height(24.dp)
                    .clip(capsuleShape)
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.85f))
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = capsuleShape
                    )
                    .clickable(onClick = onStatusPillClick)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Shizuku",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                )
            }
        }

        // Right: Two Apple-style 36.dp circular action buttons (+ and ...)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppleTopActionButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Hızlı Ekle",
                onClick = onQuickAddClick
            )
            AppleTopActionButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = "Menü",
                onClick = onMenuClick
            )
        }
    }
}

@Composable
private fun AppleTopActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(WaultTopActionBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
