package com.baris.wault.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.SilentSwitchCapability
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark

/**
 * Dedicated "Diğer" (Settings, Shizuku Status, Hidden Cards & Preferences) Screen.
 * Relocates all technical status indicators from the header to this clean, modular settings hub.
 */
@Composable
fun WaultOtherSection(
    capability: SilentSwitchCapability,
    hiddenBanksCount: Int,
    onOpenShizukuDialog: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onOpenHiddenBanks: () -> Unit,
    onRefreshBanks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ayarlar & Tercihler",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
        )

        // Card 1: Shizuku Background Engine Status
        val isReady = capability.isSilentReady
        val statusColor = if (isReady) Color(0xFF30D158) else Color(0xFFFF9F0A)
        val statusText = capability.badgeLabel

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WaultSurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isReady) Icons.Rounded.Bolt else Icons.Rounded.Security,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Shizuku Motoru",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = statusColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }

                Text(
                    text = capability.detailMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        lineHeight = 18.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (capability == SilentSwitchCapability.SHIZUKU_PERMISSION_NEEDED) {
                        Button(
                            onClick = onRequestShizukuPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = WaultPrimaryMint),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("İzin Ver", color = Color(0xFF03140A), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onOpenShizukuDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Durum Detayları", color = Color.White)
                        }
                    }

                    Button(
                        onClick = onRefreshBanks,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Yenile",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Card 2: NFC & System Preferences
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WaultSurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                SettingsOptionRow(
                    icon = Icons.Rounded.Nfc,
                    iconTint = Color(0xFF0A84FF),
                    title = "Android Temassız Ödeme Ayarları",
                    subtitle = "Varsayılan NFC cüzdan servisini seçin",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS))
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        }
                    }
                )

                if (hiddenBanksCount > 0) {
                    SettingsOptionRow(
                        icon = Icons.Rounded.VisibilityOff,
                        iconTint = Color(0xFFFF9F0A),
                        title = "Gizlenen Kartlar",
                        subtitle = "$hiddenBanksCount kart gizlendi",
                        onClick = onOpenHiddenBanks
                    )
                }

                SettingsOptionRow(
                    icon = Icons.Rounded.Timer,
                    iconTint = Color(0xFF5E5CE6),
                    title = "60sn Geçici Ödeme",
                    subtitle = "Kart seçildiğinde otomatik 1 dakikalık geçici mod",
                    onClick = {}
                )
            }
        }

        // Card 3: Vault Security & Privacy
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WaultSurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Güvenli ve Çevrimdışı Kasa",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Wault, tüm verilerinizi yalnızca cihazınızda şifreli tutar. Sunucu veya internet bağımlılığı yoktur.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.60f),
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

