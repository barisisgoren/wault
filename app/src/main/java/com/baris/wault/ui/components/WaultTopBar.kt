package com.baris.wault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.SilentSwitchCapability
import com.baris.wault.ui.theme.WaultAmoledBlack
import com.baris.wault.ui.theme.WaultShizukuActiveBg
import com.baris.wault.ui.theme.WaultShizukuActiveDot
import com.baris.wault.ui.theme.WaultShizukuInactiveBg
import com.baris.wault.ui.theme.WaultShizukuInactiveDot
import com.baris.wault.ui.theme.WaultTopActionBg
import com.baris.wault.ui.theme.WaultTopActionBorder

@Composable
fun WaultTopBar(
    capability: SilentSwitchCapability,
    onStatusPillClick: () -> Unit,
    onQuickAddClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isReady = capability.isSilentReady

    val pillBgColor by animateColorAsState(
        targetValue = if (isReady) WaultShizukuActiveBg else WaultShizukuInactiveBg,
        animationSpec = tween(durationMillis = 260),
        label = "ShizukuPillBg"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isReady) WaultShizukuActiveDot else WaultShizukuInactiveDot,
        animationSpec = tween(durationMillis = 260),
        label = "ShizukuDotColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isReady) {
            WaultShizukuActiveDot.copy(alpha = 0.42f)
        } else {
            WaultShizukuInactiveDot.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 260),
        label = "ShizukuPillBorder"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ShizukuPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isReady) 1.65f else 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WaultAmoledBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Section: Bold "Wault" title + Compact Shizuku status pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = "Wault",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = pillBgColor,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .clickable(onClick = onStatusPillClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(dotColor.copy(alpha = pulseAlpha))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isReady) "Shizuku Aktif" else "İzin Gerekli",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReady) Color(0xFF34C759) else Color(0xFFFFB340)
                        )
                    )
                }
            }
        }

        // Right Section: Two circular 36.dp action buttons (+ and ...)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopCircularActionButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Hızlı Ekle",
                onClick = onQuickAddClick
            )
            TopCircularActionButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = "Menü ve Ayarlar",
                onClick = onMenuClick
            )
        }
    }
}

@Composable
private fun TopCircularActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(WaultTopActionBg)
            .border(1.dp, WaultTopActionBorder, CircleShape)
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
