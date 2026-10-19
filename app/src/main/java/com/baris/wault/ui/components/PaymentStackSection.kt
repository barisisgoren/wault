package com.baris.wault.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import com.baris.wault.ui.theme.WaultSurfaceDark

private val BANK_CARD_HEIGHT = 216.dp
private val FIRST_STACK_GAP = 16.dp
private val STACK_PEEK_OFFSET = 60.dp
private val BOTTOM_NAV_RESERVE = 92.dp
private val SPOTLIGHT_GAP = 26.dp
private val FOCUS_ACTIONS_HEIGHT = 112.dp
private val PAYMENT_CONTENT_HEIGHT = 232.dp
private const val SPOTLIGHT_SCALE = 1.12f

@Composable
fun PaymentStackSection(
    banks: List<BankPaymentApp>,
    hiddenCount: Int,
    activeBank: BankPaymentApp?,
    isScanning: Boolean,
    onSelectBank: (BankPaymentApp) -> Unit,
    onEditBank: (BankPaymentApp) -> Unit,
    onActivateTemp60s: (BankPaymentApp) -> Unit,
    onReorderBanks: (List<BankPaymentApp>) -> Unit,
    onRefreshBanks: () -> Unit,
    onOpenHiddenBanks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val cardHeightPx = with(density) { BANK_CARD_HEIGHT.toPx() }
    val firstStackGapPx = with(density) { FIRST_STACK_GAP.toPx() }
    val peekStepPx = with(density) { STACK_PEEK_OFFSET.toPx() }
    val hysteresisPx = with(density) { 16.dp.toPx() }
    val spotlightGapPx = with(density) { SPOTLIGHT_GAP.toPx() }
    val focusActionsPx = with(density) { FOCUS_ACTIONS_HEIGHT.toPx() }
    val paymentContentPx = with(density) { PAYMENT_CONTENT_HEIGHT.toPx() }
    val minTopPx = with(density) { 12.dp.toPx() }
    val bottomReservePx = with(density) { BOTTOM_NAV_RESERVE.toPx() } +
        WindowInsets.navigationBars.getBottom(density)

    val motion = rememberCardStackMotionState<String>()
    val spotlight = rememberCardSpotlightState<String>()
    val metrics = remember { StackHostMetrics() }
    var isPlayingPaymentSuccess by remember { mutableStateOf(false) }
    var dragStartOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    val localBanks = remember { mutableStateListOf<BankPaymentApp>() }
    LaunchedEffect(banks) {
        if (motion.draggingKey == null) {
            localBanks.clear()
            localBanks.addAll(banks)
            val spotKey = spotlight.key
            if (spotKey != null && localBanks.none { it.flattenedComponent == spotKey }) {
                spotlight.dismissNow()
                isPlayingPaymentSuccess = false
            }
        }
    }

    fun slotTopPx(index: Int): Float =
        if (index <= 0) 0f else cardHeightPx + firstStackGapPx + (index - 1) * peekStepPx

    fun indexOfKey(key: String): Int = localBanks.indexOfFirst { it.flattenedComponent == key }

    fun moveToFront(key: String) {
        val idx = indexOfKey(key)
        if (idx > 0) {
            val item = localBanks.removeAt(idx)
            localBanks.add(0, item)
        }
    }

    fun spotlightTargetTop(mode: SpotlightMode): Float {
        val available = (metrics.hostHeightPx - bottomReservePx).coerceAtLeast(cardHeightPx + minTopPx * 2)
        return computeSpotlightTopPx(
            availableHeightPx = available,
            cardHeightPx = cardHeightPx,
            scale = SPOTLIGHT_SCALE,
            gapPx = spotlightGapPx,
            belowHeightPx = if (mode == SpotlightMode.PAYMENT) paymentContentPx else focusActionsPx,
            minTopPx = minTopPx,
            centerCardOnly = mode == SpotlightMode.FOCUS
        )
    }

    fun openSpotlight(key: String, mode: SpotlightMode) {
        val idx = indexOfKey(key)
        if (idx < 0) return
        val origin = metrics.stackTopInHostPx + motion.currentTopOf(key, slotTopPx(idx))
        isPlayingPaymentSuccess = false
        spotlight.open(
            key = key,
            mode = mode,
            originTopPx = origin,
            targetTopPx = spotlightTargetTop(mode),
            targetScale = SPOTLIGHT_SCALE
        )
    }

    fun closeSpotlight() {
        val key = spotlight.key ?: return
        val idx = indexOfKey(key).coerceAtLeast(0)
        spotlight.close(originTopPx = metrics.stackTopInHostPx + slotTopPx(idx)) {
            isPlayingPaymentSuccess = false
        }
    }

    // Listen for Android NFC RF field / transaction broadcasts while in double-tap payment mode
    val isPaymentModeActive = spotlight.key != null && spotlight.mode == SpotlightMode.PAYMENT
    DisposableEffect(isPaymentModeActive) {
        if (!isPaymentModeActive) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPlayingPaymentSuccess = true
                }
            }
            val filter = IntentFilter().apply {
                addAction("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED")
                addAction("android.nfc.action.TRANSACTION_DETECTED")
            }
            runCatching {
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            }
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    // Back button closes the spotlight and returns the card to the stack
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
            // Minimalist Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temassız Ödeme",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (localBanks.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = WaultSurfaceContainerDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark)
                        ) {
                            Text(
                                text = "${localBanks.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WaultPrimaryMint,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (hiddenCount > 0) {
                        IconButton(
                            onClick = onOpenHiddenBanks,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = "Gizlenen Bankalar",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isScanning && localBanks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BANK_CARD_HEIGHT)
                            .clip(RoundedCornerShape(24.dp))
                            .background(WaultSurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = WaultPrimaryMint,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                localBanks.isEmpty() -> {
                    EmptyPaymentStackCard(
                        hiddenCount = hiddenCount,
                        onRefreshBanks = onRefreshBanks,
                        onOpenHiddenBanks = onOpenHiddenBanks
                    )
                }

                else -> {
                    val totalStackHeight: Dp = if (localBanks.size <= 1) {
                        BANK_CARD_HEIGHT + 24.dp
                    } else {
                        BANK_CARD_HEIGHT + FIRST_STACK_GAP +
                            (STACK_PEEK_OFFSET * (localBanks.size - 2)) + BANK_CARD_HEIGHT + 16.dp
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalStackHeight)
                            .onGloballyPositioned {
                                metrics.stackTopInRootPx = it.positionInRoot().y
                            }
                            .stackContainerGestures(
                                findCardAtY = { yPx ->
                                    if (spotlight.key != null) {
                                        null
                                    } else {
                                        var hitResult: Pair<String, Float>? = null
                                        for (i in localBanks.lastIndex downTo 0) {
                                            val k = localBanks[i].flattenedComponent
                                            val topPx = motion.currentTopOf(k, slotTopPx(i))
                                            if (yPx >= topPx && yPx <= topPx + cardHeightPx) {
                                                hitResult = k to topPx
                                                break
                                            }
                                        }
                                        hitResult
                                    }
                                },
                                onCardTap = { k ->
                                    val bank = localBanks.firstOrNull { it.flattenedComponent == k }
                                    if (bank != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        moveToFront(k)
                                        onSelectBank(bank)
                                    }
                                },
                                onCardDoubleTap = { k ->
                                    val bank = localBanks.firstOrNull { it.flattenedComponent == k }
                                    if (bank != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openSpotlight(k, SpotlightMode.PAYMENT)
                                        moveToFront(k)
                                        onSelectBank(bank)
                                    }
                                },
                                onCardLongPress = { k ->
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    openSpotlight(k, SpotlightMode.FOCUS)
                                },
                                onCardDragStart = { k, initialTopPx ->
                                    if (spotlight.key == null && localBanks.size > 1) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        dragStartOrder = localBanks.map { it.flattenedComponent }
                                        motion.startDrag(k, initialTopPx)
                                    }
                                },
                                onCardDragUpdate = { k, newTopPx ->
                                    if (motion.draggingKey == k) {
                                        val minTop = -peekStepPx * 0.6f
                                        val maxTop = slotTopPx(localBanks.lastIndex) + peekStepPx * 0.6f
                                        val clamped = newTopPx.coerceIn(minTop, maxTop)
                                        motion.updateDrag(clamped)

                                        var idx = indexOfKey(k)
                                        while (idx != -1 && idx < localBanks.lastIndex) {
                                            val threshold = (slotTopPx(idx) + slotTopPx(idx + 1)) * 0.5f
                                            if (clamped > threshold + hysteresisPx) {
                                                val item = localBanks.removeAt(idx)
                                                localBanks.add(idx + 1, item)
                                                idx += 1
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } else {
                                                break
                                            }
                                        }
                                        while (idx > 0) {
                                            val threshold = (slotTopPx(idx - 1) + slotTopPx(idx)) * 0.5f
                                            if (clamped < threshold - hysteresisPx) {
                                                val item = localBanks.removeAt(idx)
                                                localBanks.add(idx - 1, item)
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
                                        val newOrder = localBanks.map { it.flattenedComponent }
                                        if (newOrder != dragStartOrder) {
                                            onReorderBanks(localBanks.toList())
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        localBanks.forEachIndexed { stackIndex, bank ->
                            key(bank.flattenedComponent) {
                                StackCardSlot(
                                    key = bank.flattenedComponent,
                                    motion = motion,
                                    targetTopPx = slotTopPx(stackIndex),
                                    zIndex = stackIndex.toFloat(),
                                    isHidden = spotlight.key == bank.flattenedComponent
                                ) { isDragging, slotModifier ->
                                    BankCardItem(
                                        bank = bank,
                                        isExpanded = stackIndex == 0,
                                        isDefaultNfc = activeBank?.componentName == bank.componentName,
                                        isFocused = false,
                                        isDragging = isDragging,
                                        modifier = slotModifier
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Spotlight overlay: dark scrim + card flying to screen center + actions / payment animation
        val spotlightBank = spotlight.key?.let { k -> localBanks.firstOrNull { it.flattenedComponent == k } }
        if (spotlightBank != null) {
            CardSpotlightOverlay(
                spotlight = spotlight,
                cardHeight = BANK_CARD_HEIGHT,
                belowGap = SPOTLIGHT_GAP,
                onDismissRequest = { closeSpotlight() },
                onCardTap = {
                    if (spotlight.mode == SpotlightMode.PAYMENT) {
                        if (!isPlayingPaymentSuccess) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isPlayingPaymentSuccess = true
                        }
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        moveToFront(spotlightBank.flattenedComponent)
                        onSelectBank(spotlightBank)
                        closeSpotlight()
                    }
                },
                onCardDoubleTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (spotlight.mode == SpotlightMode.FOCUS) {
                        moveToFront(spotlightBank.flattenedComponent)
                        onSelectBank(spotlightBank)
                        isPlayingPaymentSuccess = false
                        spotlight.changeMode(SpotlightMode.PAYMENT, spotlightTargetTop(SpotlightMode.PAYMENT))
                    } else {
                        closeSpotlight()
                    }
                },
                card = { cardModifier ->
                    BankCardItem(
                        bank = spotlightBank,
                        isExpanded = true,
                        isDefaultNfc = activeBank?.componentName == spotlightBank.componentName,
                        isFocused = true,
                        modifier = cardModifier
                    )
                },
                belowContent = {
                    if (spotlight.mode == SpotlightMode.PAYMENT) {
                        AnimatedContent(
                            targetState = isPlayingPaymentSuccess,
                            transitionSpec = {
                                (fadeIn(tween(220)) + scaleIn(initialScale = 0.9f))
                                    .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.92f))
                            },
                            label = "PaymentSpotlightState"
                        ) { successPlaying ->
                            if (successPlaying) {
                                PaymentSuccessAnimationCard(
                                    cardTitle = spotlightBank.displayTitle,
                                    onAnimationFinished = { closeSpotlight() }
                                )
                            } else {
                                PaymentReadyRadarIndicator(
                                    cardTitle = spotlightBank.displayTitle,
                                    onTriggerPaymentSuccess = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isPlayingPaymentSuccess = true
                                    }
                                )
                            }
                        }
                    } else {
                        val currentIdx = indexOfKey(spotlightBank.flattenedComponent)
                        FocusedBankActionsOverlay(
                            bank = spotlightBank,
                            canMoveUp = currentIdx > 0,
                            canMoveDown = currentIdx >= 0 && currentIdx < localBanks.lastIndex,
                            onMoveUp = {
                                if (currentIdx > 0) {
                                    val item = localBanks.removeAt(currentIdx)
                                    localBanks.add(currentIdx - 1, item)
                                    onReorderBanks(localBanks.toList())
                                }
                            },
                            onMoveDown = {
                                if (currentIdx >= 0 && currentIdx < localBanks.lastIndex) {
                                    val item = localBanks.removeAt(currentIdx)
                                    localBanks.add(currentIdx + 1, item)
                                    onReorderBanks(localBanks.toList())
                                }
                            },
                            onActivateNow = {
                                moveToFront(spotlightBank.flattenedComponent)
                                onSelectBank(spotlightBank)
                                closeSpotlight()
                            },
                            onActivateTemp60s = {
                                onActivateTemp60s(spotlightBank)
                                closeSpotlight()
                            },
                            onEdit = {
                                spotlight.dismissNow()
                                onEditBank(spotlightBank)
                            }
                        )
                    }
                },
                modifier = Modifier.zIndex(50f)
            )
        }
    }
}

@Composable
private fun FocusedBankActionsOverlay(
    bank: BankPaymentApp,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onActivateNow: () -> Unit,
    onActivateTemp60s: () -> Unit,
    onEdit: () -> Unit
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
                onClick = onActivateNow,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aktif Et")
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
            onClick = onActivateTemp60s,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("${bank.displayTitle} • 60sn Geçici Aktif Et")
        }
    }
}

@Composable
private fun EmptyPaymentStackCard(
    hiddenCount: Int,
    onRefreshBanks: () -> Unit,
    onOpenHiddenBanks: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = WaultSurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .height(BANK_CARD_HEIGHT)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.CreditCard,
                contentDescription = null,
                tint = WaultPrimaryMint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (hiddenCount > 0) "Kartlar Gizlendi" else "Ödeme Kartı Bulunamadı",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRefreshBanks,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yenile")
                }

                if (hiddenCount > 0) {
                    FilledTonalButton(
                        onClick = onOpenHiddenBanks,
                        shape = CircleShape
                    ) {
                        Text("Gizlenenler ($hiddenCount)")
                    }
                }
            }
        }
    }
}
