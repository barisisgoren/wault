package com.baris.wault.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.baris.wault.data.nfc.BankPaymentApp
import kotlinx.coroutines.delay

private val BANK_CARD_HEIGHT = 216.dp
private val STACK_PEEK_OFFSET = 56.dp
private val PULL_DOWN_ACCORDION_DISTANCE = 60.dp
private val SPOTLIGHT_TOP_OFFSET = 24.dp
private val SPOTLIGHT_GAP = 24.dp
private val SPOTLIGHT_BELOW_HEIGHT = 176.dp
private val BOTTOM_NAV_RESERVE = 96.dp
private const val SPOTLIGHT_SCALE = 1.02f

@Composable
fun AppleCardStack(
    banks: List<BankPaymentApp>,
    activeBank: BankPaymentApp?,
    onSelectBank: (BankPaymentApp) -> Unit,
    onEditBank: (BankPaymentApp) -> Unit,
    onActivateTemp60s: (BankPaymentApp) -> Unit,
    onReorderBanks: (List<BankPaymentApp>) -> Unit,
    onToggleFavorite: (BankPaymentApp) -> Unit = {},
    isScrollable: Boolean = true,
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val cardHeightPx = with(density) { BANK_CARD_HEIGHT.toPx() }
    val peekStepPx = with(density) { STACK_PEEK_OFFSET.toPx() }
    val accordionPullPx = with(density) { PULL_DOWN_ACCORDION_DISTANCE.toPx() }
    val hysteresisPx = with(density) { 14.dp.toPx() }
    val spotlightTopPx = with(density) { SPOTLIGHT_TOP_OFFSET.toPx() }
    val spotlightGapPx = with(density) { SPOTLIGHT_GAP.toPx() }
    val spotlightBelowPx = with(density) { SPOTLIGHT_BELOW_HEIGHT.toPx() }
    val bottomReservePx = with(density) { BOTTOM_NAV_RESERVE.toPx() } +
        WindowInsets.navigationBars.getBottom(density)

    val motion = rememberCardStackMotionState<String>()
    val spotlight = rememberCardSpotlightState<String>()
    val metrics = remember { StackHostMetrics() }
    var isPlayingPaymentSuccess by remember { mutableStateOf(false) }
    var isNfcRadarPulsing by remember { mutableStateOf(false) }
    var dragStartOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    val localBanks = remember { mutableStateListOf<BankPaymentApp>() }
    LaunchedEffect(banks) {
        if (motion.draggingKey == null) {
            localBanks.clear()
            localBanks.addAll(banks.distinctBy { it.packageName })
            val spotKey = spotlight.key
            if (spotKey != null && localBanks.none { it.packageName == spotKey }) {
                spotlight.dismissNow()
                isPlayingPaymentSuccess = false
            }
        }
    }

    LaunchedEffect(isNfcRadarPulsing) {
        if (isNfcRadarPulsing) {
            delay(4500L)
            isNfcRadarPulsing = false
        }
    }

    /**
     * Real Apple Wallet Stack Math:
     * - Cards cascade downwards all the way down the screen with generous 56.dp peek per card.
     * - The active card (index 0) sits in the foreground with full 216.dp height.
     * - Pulling down on the front card spreads cards out like an accordion fan.
     */
    fun baseSlotTopPx(index: Int, count: Int = localBanks.size): Float {
        if (count <= 1) return 0f
        val depthFromBack = (count - 1 - index).coerceIn(0, count - 1)
        return depthFromBack * peekStepPx
    }

    fun slotTopPx(index: Int): Float {
        val base = baseSlotTopPx(index)
        val fan = motion.pullDownFanFactor.floatValue
        if (fan <= 0f || localBanks.size <= 1) return base
        val depthFromBack = (localBanks.size - 1 - index).coerceAtLeast(0)
        return base + depthFromBack * (peekStepPx * 0.45f * fan)
    }

    fun indexOfKey(pkg: String): Int = localBanks.indexOfFirst { it.packageName == pkg }

    fun moveToFront(pkg: String) {
        val idx = indexOfKey(pkg)
        if (idx > 0) {
            val item = localBanks.removeAt(idx)
            localBanks.add(0, item)
        }
    }

    fun spotlightTargetTop(): Float {
        val available = (metrics.hostHeightPx - bottomReservePx).coerceAtLeast(cardHeightPx + spotlightTopPx * 2)
        return computeSpotlightTopPx(
            availableHeightPx = available,
            cardHeightPx = cardHeightPx,
            scale = SPOTLIGHT_SCALE,
            gapPx = spotlightGapPx,
            belowHeightPx = spotlightBelowPx,
            minTopPx = spotlightTopPx,
            centerCardOnly = false
        )
    }

    fun openSpotlight(pkg: String, mode: SpotlightMode) {
        val idx = indexOfKey(pkg)
        if (idx < 0) return
        val origin = metrics.stackTopInHostPx + motion.currentTopOf(pkg, slotTopPx(idx))
        isPlayingPaymentSuccess = false
        spotlight.open(
            key = pkg,
            mode = mode,
            originTopPx = origin,
            targetTopPx = spotlightTargetTop(),
            targetScale = SPOTLIGHT_SCALE
        )
    }

    fun closeSpotlight() {
        val pkg = spotlight.key ?: return
        val idx = indexOfKey(pkg).coerceAtLeast(0)
        spotlight.close(originTopPx = metrics.stackTopInHostPx + slotTopPx(idx)) {
            isPlayingPaymentSuccess = false
        }
    }

    val isSpotlightActive = spotlight.key != null
    DisposableEffect(isSpotlightActive) {
        if (!isSpotlightActive) {
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
                .then(
                    if (isScrollable) {
                        Modifier.verticalScroll(
                            state = scrollState,
                            enabled = motion.draggingKey == null && spotlight.key == null
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (localBanks.isNotEmpty()) {
                val frontCardTopDp: Dp = with(density) { baseSlotTopPx(0).toDp() }
                val totalStackHeight: Dp = BANK_CARD_HEIGHT + frontCardTopDp

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
                                    for (i in 0..localBanks.lastIndex) {
                                        val pkg = localBanks[i].packageName
                                        val topPx = motion.currentTopOf(pkg, slotTopPx(i))
                                        if (yPx >= topPx && yPx <= topPx + cardHeightPx) {
                                            hitResult = pkg to topPx
                                            break
                                        }
                                    }
                                    hitResult
                                }
                            },
                            onCardTap = { pkg ->
                                val bank = localBanks.firstOrNull { it.packageName == pkg }
                                if (bank != null) {
                                    val wasAlreadyFront = localBanks.firstOrNull()?.packageName == pkg
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    moveToFront(pkg)
                                    onSelectBank(bank)
                                    if (wasAlreadyFront) {
                                        isNfcRadarPulsing = true
                                        openSpotlight(pkg, SpotlightMode.FOCUS)
                                    }
                                }
                            },
                            onCardDoubleTap = { pkg ->
                                val bank = localBanks.firstOrNull { it.packageName == pkg }
                                if (bank != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isNfcRadarPulsing = true
                                    moveToFront(pkg)
                                    onSelectBank(bank)
                                    openSpotlight(pkg, SpotlightMode.PAYMENT)
                                }
                            },
                            onCardLongPress = { pkg ->
                                val bank = localBanks.firstOrNull { it.packageName == pkg }
                                if (bank != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isNfcRadarPulsing = true
                                    moveToFront(pkg)
                                    onSelectBank(bank)
                                    openSpotlight(pkg, SpotlightMode.FOCUS)
                                }
                            },
                            onCardDragStart = { pkg, initialTopPx ->
                                if (spotlight.key == null && localBanks.size > 1) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dragStartOrder = localBanks.map { it.packageName }
                                    motion.startDrag(pkg, initialTopPx)
                                }
                            },
                            onCardDragUpdate = { pkg, newTopPx ->
                                if (motion.draggingKey == pkg) {
                                    val baseFrontTop = baseSlotTopPx(0)
                                    val minTop = -peekStepPx * 0.8f
                                    val maxTop = baseFrontTop + accordionPullPx * 1.35f
                                    val clamped = newTopPx.coerceIn(minTop, maxTop)

                                    // Accordion Fan-out when pulling the active front card downward
                                    val idxBefore = indexOfKey(pkg)
                                    val fanFactor = if (idxBefore == 0 && clamped > baseFrontTop) {
                                        ((clamped - baseFrontTop) / accordionPullPx).coerceIn(0f, 1.25f)
                                    } else {
                                        0f
                                    }
                                    motion.updateDrag(clamped, fanFactor)

                                    var idx = idxBefore
                                    while (idx > 0) {
                                        val threshold = (baseSlotTopPx(idx) + baseSlotTopPx(idx - 1)) * 0.5f
                                        if (clamped > threshold + hysteresisPx) {
                                            val item = localBanks.removeAt(idx)
                                            localBanks.add(idx - 1, item)
                                            idx -= 1
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else {
                                            break
                                        }
                                    }
                                    while (idx != -1 && idx < localBanks.lastIndex) {
                                        val threshold = (baseSlotTopPx(idx) + baseSlotTopPx(idx + 1)) * 0.5f
                                        if (clamped < threshold - hysteresisPx) {
                                            val item = localBanks.removeAt(idx)
                                            localBanks.add(idx + 1, item)
                                            idx += 1
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
                                    val newOrder = localBanks.map { it.packageName }
                                    if (newOrder != dragStartOrder) {
                                        onReorderBanks(localBanks.toList())
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    localBanks.forEachIndexed { stackIndex, bank ->
                        key(bank.packageName) {
                            val cardScaleX = 1f
                            val cardZIndex = (localBanks.size - stackIndex).toFloat()

                            StackCardSlot(
                                key = bank.packageName,
                                motion = motion,
                                targetTopPx = slotTopPx(stackIndex),
                                zIndex = cardZIndex,
                                isHidden = spotlight.key == bank.packageName,
                                stackScaleX = cardScaleX
                            ) { isDragging, slotModifier ->
                                BankCardItem(
                                    bank = bank,
                                    isExpanded = stackIndex == 0,
                                    isDefaultNfc = activeBank?.packageName == bank.packageName,
                                    isFocused = false,
                                    isDragging = isDragging,
                                    onToggleFavorite = { onToggleFavorite(bank) },
                                    modifier = slotModifier
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            bottomContent()

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Mode 1: Apple Pay Focus / Payment Mode (Top-aligned card at 24.dp, 92% scrim, single NFC icon + actions)
        val spotlightBank = spotlight.key?.let { pkg -> localBanks.firstOrNull { it.packageName == pkg } }
        if (spotlightBank != null) {
            CardSpotlightOverlay(
                spotlight = spotlight,
                cardHeight = BANK_CARD_HEIGHT,
                belowGap = SPOTLIGHT_GAP,
                onDismissRequest = { closeSpotlight() },
                onCardTap = {
                    if (!isPlayingPaymentSuccess) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isPlayingPaymentSuccess = true
                    }
                },
                onCardDoubleTap = {
                    closeSpotlight()
                },
                card = { cardModifier ->
                    BankCardItem(
                        bank = spotlightBank,
                        isExpanded = true,
                        isDefaultNfc = true,
                        isFocused = true,
                        modifier = cardModifier
                    )
                },
                belowContent = {
                    AnimatedContent(
                        targetState = isPlayingPaymentSuccess,
                        transitionSpec = {
                            (fadeIn(tween(220)) + scaleIn(initialScale = 0.9f))
                                .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.92f))
                        },
                        label = "ApplePaySpotlightState"
                    ) { successPlaying ->
                        if (successPlaying) {
                            PaymentSuccessAnimationCard(
                                cardTitle = spotlightBank.displayTitle,
                                onAnimationFinished = { closeSpotlight() }
                            )
                        } else {
                            ApplePaySpotlightPrompt(
                                showQuickEditActions = true,
                                onTriggerPaymentSuccess = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isPlayingPaymentSuccess = true
                                },
                                onEdit = {
                                    spotlight.dismissNow()
                                    onEditBank(spotlightBank)
                                },
                                onTemp60s = {
                                    onActivateTemp60s(spotlightBank)
                                    closeSpotlight()
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.zIndex(50f)
            )
        }
    }
}

@Composable
private fun ApplePaySpotlightPrompt(
    showQuickEditActions: Boolean,
    onTriggerPaymentSuccess: () -> Unit,
    onEdit: () -> Unit,
    onTemp60s: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpotlightNfcRing")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpotlightRingScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpotlightRingAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .clickable(onClick = onTriggerPaymentSuccess)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF0A84FF).copy(alpha = ringAlpha),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFF0A84FF).copy(alpha = 0.85f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = "Okuyucuya Yaklaştırın",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = "Okuyucuya Yaklaştırın",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                letterSpacing = (-0.2).sp
            )
        )

        if (showQuickEditActions) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C1C1E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kartı Özelleştir",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C1C1E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onTemp60s)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "60sn Geçici",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
