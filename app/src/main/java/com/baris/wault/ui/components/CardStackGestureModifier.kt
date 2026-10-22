package com.baris.wault.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Stationary parent-container gesture detector for stacked cards.
 *
 * Attached to the stationary stack container (not to the moving cards), so pointer coordinates never
 * shift while a card is dragged or when cards swap slots. Supports tap, double tap, long press and
 * vertical drag-to-reorder.
 *
 * - [waitForDoubleTapBeforeTap] = false: single taps fire immediately; a second tap near the first one
 *   (within the double-tap window) is routed to the SAME card even if it moved to a new slot.
 * - [waitForDoubleTapBeforeTap] = true: single tap fires only after the double-tap window expires.
 */
@Composable
fun <K : Any> Modifier.stackContainerGestures(
    findCardAtY: (yPx: Float) -> Pair<K, Float>?,
    onCardTap: (K) -> Unit,
    onCardDoubleTap: (K) -> Unit,
    onCardLongPress: (K) -> Unit,
    onCardDragStart: (cardKey: K, initialTopPx: Float) -> Unit,
    onCardDragUpdate: (cardKey: K, newTopPx: Float) -> Unit,
    onCardDragEnd: (cardKey: K) -> Unit,
    onCardTouchDown: (K) -> Unit = {},
    onEmptySpaceTap: () -> Unit = {},
    waitForDoubleTapBeforeTap: Boolean = false
): Modifier {
    val currentFindCardAtY by rememberUpdatedState(findCardAtY)
    val currentOnEmptySpaceTap by rememberUpdatedState(onEmptySpaceTap)
    val currentOnCardTap by rememberUpdatedState(onCardTap)
    val currentOnCardDoubleTap by rememberUpdatedState(onCardDoubleTap)
    val currentOnCardLongPress by rememberUpdatedState(onCardLongPress)
    val currentOnCardDragStart by rememberUpdatedState(onCardDragStart)
    val currentOnCardDragUpdate by rememberUpdatedState(onCardDragUpdate)
    val currentOnCardDragEnd by rememberUpdatedState(onCardDragEnd)
    val currentOnCardTouchDown by rememberUpdatedState(onCardTouchDown)

    return this.pointerInput(waitForDoubleTapBeforeTap) {
        val touchSlop = viewConfiguration.touchSlop
        val dragStartSlop = (touchSlop * 0.5f).coerceAtLeast(6f)
        val longPressTimeout = 420L
        val doubleTapWindowMs = 280L
        val doubleTapRadius = touchSlop * 3f

        var lastTapKey: K? = null
        var lastTapUpTime = 0L
        var lastTapX = 0f
        var lastTapY = 0f

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            val downX = down.position.x
            val downY = down.position.y
            val downTimeMs = System.currentTimeMillis()

            val previousKey = lastTapKey
            val stickyKey: K? = if (
                !waitForDoubleTapBeforeTap &&
                previousKey != null &&
                down.uptimeMillis - lastTapUpTime <= doubleTapWindowMs &&
                abs(downX - lastTapX) <= doubleTapRadius &&
                abs(downY - lastTapY) <= doubleTapRadius
            ) previousKey else null

            val hit = currentFindCardAtY(downY)

            if (hit == null && stickyKey == null) {
                // Finger touched empty space outside any card
                lastTapKey = null
                var movedTooFar = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: event.changes.firstOrNull()
                    if (change == null || !change.pressed) {
                        if (!movedTooFar) currentOnEmptySpaceTap()
                        break
                    }
                    if (abs(change.position.x - downX) > touchSlop ||
                        abs(change.position.y - downY) > touchSlop
                    ) {
                        movedTooFar = true
                    }
                }
                return@awaitEachGesture
            }

            val cardKey: K = stickyKey ?: hit!!.first
            val canDrag = hit != null && hit.first == cardKey
            val initialCardTopPx = hit?.second ?: 0f

            currentOnCardTouchDown(cardKey)

            var isDragging = false
            var longPressTriggered = false
            var movedBeyondTapSlop = false

            while (true) {
                val elapsed = System.currentTimeMillis() - downTimeMs
                val waitMs = if (!isDragging && !longPressTriggered && !movedBeyondTapSlop) {
                    (longPressTimeout - elapsed).coerceAtLeast(16L)
                } else {
                    1000L
                }

                val event = withTimeoutOrNull(waitMs) {
                    awaitPointerEvent(PointerEventPass.Main)
                }

                if (event == null) {
                    if (!isDragging && !longPressTriggered && !movedBeyondTapSlop) {
                        longPressTriggered = true
                        lastTapKey = null
                        currentOnCardLongPress(cardKey)
                    }
                    continue
                }

                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.firstOrNull()

                if (change == null || !change.pressed) {
                    if (isDragging) {
                        currentOnCardDragEnd(cardKey)
                    } else if (!longPressTriggered && !movedBeyondTapSlop) {
                        when {
                            stickyKey != null -> {
                                lastTapKey = null
                                currentOnCardDoubleTap(cardKey)
                            }

                            waitForDoubleTapBeforeTap -> {
                                val secondDown = withTimeoutOrNull(doubleTapWindowMs) {
                                    awaitFirstDown(requireUnconsumed = false)
                                }
                                if (secondDown == null ||
                                    abs(secondDown.position.x - downX) > doubleTapRadius ||
                                    abs(secondDown.position.y - downY) > doubleTapRadius
                                ) {
                                    currentOnCardTap(cardKey)
                                } else {
                                    var validSecondTap = true
                                    val released = withTimeoutOrNull(longPressTimeout) {
                                        while (true) {
                                            val e = awaitPointerEvent(PointerEventPass.Main)
                                            val c = e.changes.firstOrNull { it.id == secondDown.id }
                                                ?: e.changes.firstOrNull()
                                            if (c == null || !c.pressed) break
                                            if (abs(c.position.x - secondDown.position.x) > touchSlop ||
                                                abs(c.position.y - secondDown.position.y) > touchSlop
                                            ) {
                                                validSecondTap = false
                                            }
                                        }
                                        true
                                    }
                                    if (released == true && validSecondTap) {
                                        currentOnCardDoubleTap(cardKey)
                                    } else {
                                        currentOnCardTap(cardKey)
                                    }
                                }
                            }

                            else -> {
                                lastTapKey = cardKey
                                lastTapUpTime = change?.uptimeMillis ?: System.currentTimeMillis()
                                lastTapX = downX
                                lastTapY = downY
                                currentOnCardTap(cardKey)
                            }
                        }
                    }
                    break
                }

                val totalDx = change.position.x - downX
                val totalDy = change.position.y - downY

                if (abs(totalDx) > touchSlop || abs(totalDy) > touchSlop) {
                    movedBeyondTapSlop = true
                }

                if (!isDragging && !longPressTriggered) {
                    if (canDrag && abs(totalDy) >= dragStartSlop && abs(totalDy) >= abs(totalDx)) {
                        isDragging = true
                        lastTapKey = null
                        change.consume()
                        val newTopPx = initialCardTopPx + totalDy
                        currentOnCardDragStart(cardKey, newTopPx)
                        currentOnCardDragUpdate(cardKey, newTopPx)
                    }
                } else if (isDragging) {
                    change.consume()
                    currentOnCardDragUpdate(cardKey, initialCardTopPx + totalDy)
                }
            }
        }
    }
}
