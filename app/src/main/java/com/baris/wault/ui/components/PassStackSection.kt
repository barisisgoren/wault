package com.baris.wault.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import com.baris.wault.ui.theme.WaultSurfaceDark

private val PASS_PEEK_OFFSET = 58.dp
private val FIRST_PASS_GAP = 16.dp
private val EXPANDED_PASS_HEIGHT = 186.dp
private val STACKED_PASS_HEIGHT = 142.dp
private val PASS_BOTTOM_NAV_RESERVE = 92.dp
private val PASS_SPOTLIGHT_GAP = 26.dp
private val PASS_ACTIONS_HEIGHT = 112.dp
private const val PASS_SPOTLIGHT_SCALE = 1.12f

@Composable
fun PassStackSection(
    passes: List<LoyaltyCardEntity>,
    onPassClick: (LoyaltyCardEntity) -> Unit,
    onMovePassToTop: (LoyaltyCardEntity) -> Unit,
    onReorderPasses: (List<LoyaltyCardEntity>) -> Unit,
    onPassEdit: (LoyaltyCardEntity) -> Unit,
    onPassDelete: (LoyaltyCardEntity) -> Unit,
    onAddPassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val expandedPassHeightPx = with(density) { EXPANDED_PASS_HEIGHT.toPx() }
    val stackedPassHeightPx = with(density) { STACKED_PASS_HEIGHT.toPx() }
    val firstPassGapPx = with(density) { FIRST_PASS_GAP.toPx() }
    val peekStepPx = with(density) { PASS_PEEK_OFFSET.toPx() }
    val hysteresisPx = with(density) { 16.dp.toPx() }
    val spotlightGapPx = with(density) { PASS_SPOTLIGHT_GAP.toPx() }
    val actionsHeightPx = with(density) { PASS_ACTIONS_HEIGHT.toPx() }
    val minTopPx = with(density) { 12.dp.toPx() }
    val bottomReservePx = with(density) { PASS_BOTTOM_NAV_RESERVE.toPx() } +
        WindowInsets.navigationBars.getBottom(density)

    val motion = rememberCardStackMotionState<Long>()
    val spotlight = rememberCardSpotlightState<Long>()
    val metrics = remember { StackHostMetrics() }
    var dragStartOrder by remember { mutableStateOf<List<Long>>(emptyList()) }

    val localPasses = remember { mutableStateListOf<LoyaltyCardEntity>() }
    LaunchedEffect(passes) {
        if (motion.draggingKey == null) {
            localPasses.clear()
            localPasses.addAll(passes)
            val spotKey = spotlight.key
            if (spotKey != null && localPasses.none { it.id == spotKey }) {
                spotlight.dismissNow()
            }
        }
    }

    fun slotTopPx(index: Int): Float =
        if (index <= 0) 0f else expandedPassHeightPx + firstPassGapPx + (index - 1) * peekStepPx

    fun indexOfKey(id: Long): Int = localPasses.indexOfFirst { it.id == id }

    fun spotlightTargetTop(): Float {
        val available = (metrics.hostHeightPx - bottomReservePx)
            .coerceAtLeast(expandedPassHeightPx + minTopPx * 2)
        return computeSpotlightTopPx(
            availableHeightPx = available,
            cardHeightPx = expandedPassHeightPx,
            scale = PASS_SPOTLIGHT_SCALE,
            gapPx = spotlightGapPx,
            belowHeightPx = actionsHeightPx,
            minTopPx = minTopPx,
            centerCardOnly = true
        )
    }

    fun openSpotlight(id: Long) {
        val idx = indexOfKey(id)
        if (idx < 0) return
        val origin = metrics.stackTopInHostPx + motion.currentTopOf(id, slotTopPx(idx))
        spotlight.open(
            key = id,
            mode = SpotlightMode.FOCUS,
            originTopPx = origin,
            targetTopPx = spotlightTargetTop(),
            targetScale = PASS_SPOTLIGHT_SCALE
        )
    }

    fun closeSpotlight() {
        val id = spotlight.key ?: return
        val idx = indexOfKey(id).coerceAtLeast(0)
        spotlight.close(originTopPx = metrics.stackTopInHostPx + slotTopPx(idx))
    }

    // Back button closes the spotlight and returns the pass to the stack
    BackHandler(enabled = spotlight.key != null) {
        closeSpotlight()
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                metrics.hostTopInRootPx = it.positionInRoot().y
                metrics.hostHeightPx = it.size.height.toFloat()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = scrollState,
                    enabled = motion.draggingKey == null && spotlight.key == null
                )
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Geçiş Kartları",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                if (localPasses.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = WaultSurfaceContainerDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark)
                    ) {
                        Text(
                            text = "${localPasses.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WaultPrimaryMint,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (localPasses.isEmpty()) {
                EmptyPassStackCard(onAddPassClick = onAddPassClick)
            } else {
                val totalStackHeight: Dp = if (localPasses.size == 1) {
                    EXPANDED_PASS_HEIGHT + 24.dp
                } else {
                    EXPANDED_PASS_HEIGHT + FIRST_PASS_GAP +
                        (PASS_PEEK_OFFSET * (localPasses.size - 2)) + STACKED_PASS_HEIGHT + 20.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalStackHeight)
                        .onGloballyPositioned {
                            metrics.stackTopInRootPx = it.positionInRoot().y
                        }
                        .stackContainerGestures(
                            waitForDoubleTapBeforeTap = true,
                            findCardAtY = { yPx ->
                                if (spotlight.key != null) {
                                    null
                                } else {
                                    var hitResult: Pair<Long, Float>? = null
                                    for (i in localPasses.lastIndex downTo 0) {
                                        val id = localPasses[i].id
                                        val topPx = motion.currentTopOf(id, slotTopPx(i))
                                        val heightPx = if (i == 0) expandedPassHeightPx else stackedPassHeightPx
                                        if (yPx >= topPx && yPx <= topPx + heightPx) {
                                            hitResult = id to topPx
                                            break
                                        }
                                    }
                                    hitResult
                                }
                            },
                            onCardTap = { id ->
                                val card = localPasses.firstOrNull { it.id == id }
                                if (card != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val idx = indexOfKey(id)
                                    if (idx > 0) {
                                        val item = localPasses.removeAt(idx)
                                        localPasses.add(0, item)
                                        onMovePassToTop(card)
                                    } else {
                                        onPassClick(card)
                                    }
                                }
                            },
                            onCardDoubleTap = { id ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                openSpotlight(id)
                            },
                            onCardLongPress = { id ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                openSpotlight(id)
                            },
                            onCardDragStart = { id, initialTopPx ->
                                if (spotlight.key == null && localPasses.size > 1) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dragStartOrder = localPasses.map { it.id }
                                    motion.startDrag(id, initialTopPx)
                                }
                            },
                            onCardDragUpdate = { id, newTopPx ->
                                if (motion.draggingKey == id) {
                                    val minTop = -peekStepPx * 0.6f
                                    val maxTop = slotTopPx(localPasses.lastIndex) + peekStepPx * 0.6f
                                    val clamped = newTopPx.coerceIn(minTop, maxTop)
                                    motion.updateDrag(clamped)

                                    var idx = indexOfKey(id)
                                    while (idx != -1 && idx < localPasses.lastIndex) {
                                        val threshold = (slotTopPx(idx) + slotTopPx(idx + 1)) * 0.5f
                                        if (clamped > threshold + hysteresisPx) {
                                            val item = localPasses.removeAt(idx)
                                            localPasses.add(idx + 1, item)
                                            idx += 1
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else {
                                            break
                                        }
                                    }
                                    while (idx > 0) {
                                        val threshold = (slotTopPx(idx - 1) + slotTopPx(idx)) * 0.5f
                                        if (clamped < threshold - hysteresisPx) {
                                            val item = localPasses.removeAt(idx)
                                            localPasses.add(idx - 1, item)
                                            idx -= 1
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else {
                                            break
                                        }
                                    }
                                }
                            },
                            onCardDragEnd = {
                                if (motion.draggingKey != null) {
                                    motion.endDrag()
                                    if (localPasses.map { it.id } != dragStartOrder) {
                                        onReorderPasses(localPasses.toList())
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    localPasses.forEachIndexed { stackIndex, card ->
                        key(card.id) {
                            StackCardSlot(
                                key = card.id,
                                motion = motion,
                                targetTopPx = slotTopPx(stackIndex),
                                zIndex = stackIndex.toFloat(),
                                isHidden = spotlight.key == card.id
                            ) { isDragging, slotModifier ->
                                LoyaltyCardItem(
                                    card = card,
                                    isExpanded = stackIndex == 0,
                                    isFocused = false,
                                    isDragging = isDragging,
                                    modifier = slotModifier
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Spotlight overlay: dark scrim + pass flying to screen center + actions
        val spotlightCard = spotlight.key?.let { id -> localPasses.firstOrNull { it.id == id } }
        if (spotlightCard != null) {
            CardSpotlightOverlay(
                spotlight = spotlight,
                cardHeight = EXPANDED_PASS_HEIGHT,
                belowGap = PASS_SPOTLIGHT_GAP,
                onDismissRequest = { closeSpotlight() },
                onCardTap = {
                    spotlight.dismissNow()
                    onPassClick(spotlightCard)
                },
                onCardDoubleTap = { closeSpotlight() },
                card = { cardModifier ->
                    LoyaltyCardItem(
                        card = spotlightCard,
                        isExpanded = true,
                        isFocused = true,
                        modifier = cardModifier
                    )
                },
                belowContent = {
                    val currentIdx = indexOfKey(spotlightCard.id)
                    FocusedPassActionsOverlay(
                        canMoveUp = currentIdx > 0,
                        canMoveDown = currentIdx >= 0 && currentIdx < localPasses.lastIndex,
                        onMoveUp = {
                            if (currentIdx > 0) {
                                val item = localPasses.removeAt(currentIdx)
                                localPasses.add(currentIdx - 1, item)
                                onReorderPasses(localPasses.toList())
                            }
                        },
                        onMoveDown = {
                            if (currentIdx >= 0 && currentIdx < localPasses.lastIndex) {
                                val item = localPasses.removeAt(currentIdx)
                                localPasses.add(currentIdx + 1, item)
                                onReorderPasses(localPasses.toList())
                            }
                        },
                        onOpenBarcode = {
                            spotlight.dismissNow()
                            onPassClick(spotlightCard)
                        },
                        onEdit = {
                            spotlight.dismissNow()
                            onPassEdit(spotlightCard)
                        },
                        onDelete = {
                            spotlight.dismissNow()
                            onPassDelete(spotlightCard)
                        }
                    )
                },
                modifier = Modifier.zIndex(50f)
            )
        }
    }
}

@Composable
private fun FocusedPassActionsOverlay(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpenBarcode: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onMoveUp,
                enabled = canMoveUp
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = "Yukarı Taşı"
                )
            }

            FilledTonalButton(
                onClick = onOpenBarcode,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Barkodu Aç")
            }

            FilledTonalButton(
                onClick = onEdit,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Düzenle")
            }

            FilledTonalIconButton(
                onClick = onMoveDown,
                enabled = canMoveDown
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = "Aşağı Taşı"
                )
            }
        }

        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kartı Sil")
        }
    }
}

@Composable
private fun EmptyPassStackCard(
    onAddPassClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WaultSurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                tint = WaultPrimaryMint,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Kayıtlı Geçiş Kartı Yok",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddPassClick,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yeni Kart Ekle", fontWeight = FontWeight.Bold)
            }
        }
    }
}
