package com.baris.wault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.baris.wault.data.vault.LoyaltyCardEntity

@Composable
fun PassGridSection(
    passes: List<LoyaltyCardEntity>,
    onPassClick: (LoyaltyCardEntity) -> Unit,
    onPassEdit: (LoyaltyCardEntity) -> Unit,
    onAddPassClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    if (passes.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

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

@Composable
fun PassStackSection(
    passes: List<LoyaltyCardEntity>,
    onPassClick: (LoyaltyCardEntity) -> Unit,
    onMovePassToTop: (LoyaltyCardEntity) -> Unit = {},
    onReorderPasses: (List<LoyaltyCardEntity>) -> Unit = {},
    onPassEdit: (LoyaltyCardEntity) -> Unit,
    onPassDelete: (LoyaltyCardEntity) -> Unit = {},
    onAddPassClick: () -> Unit = {},
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
