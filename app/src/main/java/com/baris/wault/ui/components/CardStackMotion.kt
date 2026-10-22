package com.baris.wault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Holds per-card vertical positions for a stacked card list.
 *
 * All animated values are read exclusively inside `graphicsLayer {}` lambdas, so dragging or settling
 * cards only invalidates GPU layers – it never triggers recomposition of the stack or the cards.
 */
@Stable
class CardStackMotionState<K : Any> {
    var draggingKey by mutableStateOf<K?>(null)
        private set

    internal val dragTopPx = mutableFloatStateOf(0f)
    internal val pullDownFanFactor = mutableFloatStateOf(0f)
    internal val topAnimatables = HashMap<K, Animatable<Float, AnimationVector1D>>()

    /** Current on-screen top of a card (live drag position, in-flight animation value, or fallback slot). */
    fun currentTopOf(key: K, fallbackPx: Float): Float =
        if (draggingKey == key) dragTopPx.floatValue else topAnimatables[key]?.value ?: fallbackPx

    fun startDrag(key: K, topPx: Float) {
        dragTopPx.floatValue = topPx
        pullDownFanFactor.floatValue = 0f
        draggingKey = key
    }

    fun updateDrag(topPx: Float, fanFactor: Float = 0f) {
        dragTopPx.floatValue = topPx
        pullDownFanFactor.floatValue = fanFactor.coerceIn(0f, 1.5f)
    }

    fun endDrag() {
        val finishedKey = draggingKey
        if (finishedKey != null) {
            val anim = topAnimatables[finishedKey]
            if (anim != null) {
                // Keep last drag coordinate available for immediate snap
            }
        }
        pullDownFanFactor.floatValue = 0f
        draggingKey = null
    }
}

@Composable
fun <K : Any> rememberCardStackMotionState(): CardStackMotionState<K> =
    remember { CardStackMotionState() }

private val StackSettleSpring = spring(
    dampingRatio = 0.82f,
    stiffness = 380f,
    visibilityThreshold = 0.5f
)

/**
 * A single card slot inside the stack. Position is driven by an [Animatable] that:
 * - follows the finger 1:1 while dragging (layer-only reads, zero coroutine overhead),
 * - springs smoothly to [targetTopPx] whenever the slot changes or the drag is released.
 */
@Composable
fun <K : Any> StackCardSlot(
    key: K,
    motion: CardStackMotionState<K>,
    targetTopPx: Float,
    zIndex: Float,
    isHidden: Boolean,
    stackScaleX: Float = 1f,
    content: @Composable (isDragging: Boolean, modifier: Modifier) -> Unit
) {
    val isDragging = motion.draggingKey == key
    val topAnim = remember(key) { Animatable(targetTopPx, visibilityThreshold = 0.5f) }

    DisposableEffect(key, topAnim) {
        motion.topAnimatables[key] = topAnim
        onDispose {
            if (motion.topAnimatables[key] === topAnim) {
                motion.topAnimatables.remove(key)
            }
        }
    }

    var wasDragging by remember(key) { mutableStateOf(false) }
    LaunchedEffect(isDragging, targetTopPx) {
        if (isDragging) {
            wasDragging = true
            topAnim.snapTo(motion.dragTopPx.floatValue)
        } else {
            if (wasDragging) {
                wasDragging = false
                topAnim.snapTo(motion.dragTopPx.floatValue)
            }
            topAnim.animateTo(targetTopPx, StackSettleSpring)
        }
    }

    val animatedScaleX = animateFloatAsState(
        targetValue = if (isDragging) 1.03f else stackScaleX,
        animationSpec = StackSettleSpring,
        label = "StackCardScaleX"
    )
    val animatedScaleY = animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = StackSettleSpring,
        label = "StackCardScaleY"
    )

    content(
        isDragging,
        Modifier
            .zIndex(if (isDragging) 100f else zIndex)
            .graphicsLayer {
                translationY = if (isDragging) motion.dragTopPx.floatValue else topAnim.value
                scaleX = animatedScaleX.value
                scaleY = animatedScaleY.value
                alpha = if (isHidden) 0f else 1f
            }
    )
}

/** Plain (non-state) layout metrics used to map stack coordinates into the overlay host. */
class StackHostMetrics {
    var hostTopInRootPx = 0f
    var hostHeightPx = 0f
    var stackTopInRootPx = 0f
    val stackTopInHostPx: Float get() = stackTopInRootPx - hostTopInRootPx
}

enum class SpotlightMode { FOCUS, PAYMENT }

/**
 * Spotlight (focus / payment) state. The selected card springs to the top of the screen
 * (`topOffset = 24.dp` right below the top bar) with `spring(dampingRatio = 0.82f, stiffness = 380f)`.
 */
@Stable
class CardSpotlightState<K : Any>(private val scope: CoroutineScope) {
    var key by mutableStateOf<K?>(null)
        private set
    var mode by mutableStateOf(SpotlightMode.FOCUS)
        private set
    var isClosing by mutableStateOf(false)
        private set
    var originTopPx by mutableFloatStateOf(0f)
        private set
    var targetTopPx by mutableFloatStateOf(0f)
        private set
    var targetScale by mutableFloatStateOf(1.02f)
        private set

    val progress = Animatable(0f, visibilityThreshold = 0.0005f)
    private var job: Job? = null

    val isActive: Boolean get() = key != null

    fun open(key: K, mode: SpotlightMode, originTopPx: Float, targetTopPx: Float, targetScale: Float = 1.02f) {
        job?.cancel()
        if (this.key != key) {
            this.originTopPx = originTopPx
        }
        this.key = key
        this.mode = mode
        this.isClosing = false
        this.targetTopPx = targetTopPx
        this.targetScale = targetScale
        job = scope.launch {
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 380f, visibilityThreshold = 0.0005f))
        }
    }

    fun changeMode(mode: SpotlightMode, targetTopPx: Float) {
        this.mode = mode
        this.targetTopPx = targetTopPx
    }

    fun close(originTopPx: Float, onClosed: () -> Unit = {}) {
        if (key == null || isClosing) return
        job?.cancel()
        this.originTopPx = originTopPx
        isClosing = true
        job = scope.launch {
            progress.animateTo(0f, spring(dampingRatio = 0.88f, stiffness = 380f, visibilityThreshold = 0.0005f))
            key = null
            isClosing = false
            onClosed()
        }
    }

    /** Removes the spotlight instantly (e.g. before opening a dialog / sheet). */
    fun dismissNow() {
        job?.cancel()
        key = null
        isClosing = false
        job = scope.launch { progress.snapTo(0f) }
    }
}

@Composable
fun <K : Any> rememberCardSpotlightState(): CardSpotlightState<K> {
    val scope = rememberCoroutineScope()
    return remember(scope) { CardSpotlightState(scope) }
}

/**
 * Computes the spotlight card's top offset at the TOP section of the screen (`24.dp` below top bar).
 */
fun computeSpotlightTopPx(
    availableHeightPx: Float,
    cardHeightPx: Float,
    scale: Float,
    gapPx: Float,
    belowHeightPx: Float,
    minTopPx: Float,
    centerCardOnly: Boolean
): Float {
    val scaleOverflow = (scale - 1f) * cardHeightPx / 2f
    return (minTopPx + scaleOverflow).coerceAtLeast(0f)
}

/**
 * Full-size overlay: 92% darkened scrim + active card at the TOP (`topOffset = 24.dp`) +
 * a SINGLE "Okuyucuya Yaklaştırın" & action buttons block at `cardBottom + 24.dp`.
 */
@Composable
fun <K : Any> CardSpotlightOverlay(
    spotlight: CardSpotlightState<K>,
    cardHeight: Dp,
    belowGap: Dp,
    onDismissRequest: () -> Unit,
    onCardTap: () -> Unit,
    onCardDoubleTap: () -> Unit,
    card: @Composable (modifier: Modifier) -> Unit,
    belowContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    if (spotlight.key == null) return

    val density = LocalDensity.current
    val cardHeightPx = with(density) { cardHeight.toPx() }
    val gapPx = with(density) { belowGap.toPx() }
    val slidePx = with(density) { 24.dp.toPx() }

    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val currentTap by rememberUpdatedState(onCardTap)
    val currentDoubleTap by rememberUpdatedState(onCardDoubleTap)

    val animatedTarget = animateFloatAsState(
        targetValue = spotlight.targetTopPx,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "SpotlightTargetTop"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        // 92% Dark Scrim (alpha read in draw phase only)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val a = (0.92f * spotlight.progress.value).coerceIn(0f, 0.94f)
                    drawRect(Color.Black.copy(alpha = a))
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!spotlight.isClosing) currentDismiss()
                    }
                }
        )

        // Active card springs to topOffset = 24.dp
        card(
            Modifier
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    val p = spotlight.progress.value
                    val origin = spotlight.originTopPx
                    translationY = origin + (animatedTarget.value - origin) * p
                    val s = 1f + (spotlight.targetScale - 1f) * p
                    scaleX = s
                    scaleY = s
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (!spotlight.isClosing) currentTap() },
                        onDoubleTap = { if (!spotlight.isClosing) currentDoubleTap() }
                    )
                }
        )

        // Content directly below the card (`cardBottom + 24.dp`)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset {
                    val y = animatedTarget.value +
                        cardHeightPx * (1f + spotlight.targetScale) / 2f +
                        gapPx
                    IntOffset(0, y.roundToInt())
                }
                .graphicsLayer {
                    val a = ((spotlight.progress.value - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - a) * slidePx
                }
        ) {
            belowContent()
        }
    }
}
