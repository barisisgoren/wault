package com.baris.wault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultMutedGray
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceDark

@Composable
fun PassGridSection(
    passes: List<LoyaltyCardEntity>,
    onPassClick: (LoyaltyCardEntity) -> Unit,
    onPassEdit: (LoyaltyCardEntity) -> Unit,
    onAddPassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(22.dp))

        if (passes.isEmpty()) {
            EmptyPassGridCard(onAddPassClick = onAddPassClick)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rows = passes.chunked(2)
                rows.forEach { rowPasses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPasses.forEach { card ->
                            key(card.id) {
                                Box(modifier = Modifier.weight(1f)) {
                                    LoyaltyCardItem(
                                        card = card,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onPassClick(card)
                                        },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPassEdit(card)
                                        }
                                    )
                                }
                            }
                        }

                        if (rowPasses.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassStackSection(
    passes: List<LoyaltyCardEntity>,
    onPassClick: (LoyaltyCardEntity) -> Unit,
    onMovePassToTop: (LoyaltyCardEntity) -> Unit = {},
    onReorderPasses: (List<LoyaltyCardEntity>) -> Unit = {},
    onPassEdit: (LoyaltyCardEntity) -> Unit,
    onPassDelete: (LoyaltyCardEntity) -> Unit = {},
    onAddPassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PassGridSection(
        passes = passes,
        onPassClick = onPassClick,
        onPassEdit = onPassEdit,
        onAddPassClick = onAddPassClick,
        modifier = modifier
    )
}

@Composable
private fun EmptyPassGridCard(
    onAddPassClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WaultSurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onAddPassClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                tint = WaultPrimaryMint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Sadakat veya Geçiş Kartı Ekle",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Barkod veya QR kodunuzu kasaya kaydedin",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = WaultMutedGray
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Yeni Kart Ekle",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
