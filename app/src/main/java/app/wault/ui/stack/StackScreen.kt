package app.wault.ui.stack

import app.wault.DevOptions
import app.wault.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import kotlin.coroutines.cancellation.CancellationException
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.data.Reorder
import app.wault.data.WaultCard
import app.wault.design.BarButton
import app.wault.design.CARD_RATIO
import app.wault.design.CardPeek
import app.wault.design.CardSurface
import app.wault.design.CircleButton
import app.wault.design.Glyph
import app.wault.design.LargeTitleBar
import app.wault.design.PrimaryButton
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.design.rememberCardArt
import app.wault.nfc.PaymentSwitcher
import app.wault.nfc.ShizukuBridge
import app.wault.nfc.SwitchOutcome
import app.wault.ui.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val Gutter = 16.dp
private val TopPad = 6.dp
private val MaxPeek = 92.dp
private val DockHeight = 60.dp
private val DockGap = 12.dp
private const val BOTTOM_PEEKS = 4
private val PeekSliver = 20.dp
private val PeekStep = 11.dp

/**
 * Yığının tüm durumu. Konumların hepsi bu durumdan türetilir (layout aşamasında okunur), bu yüzden
 * parmak hareket ederken yeniden birleştirme (recomposition) olmaz, sadece yerleşim güncellenir.
 *
 * - scroll: yığın kaydırması (px). Sınır dışına biraz taşabilir, bırakınca yaylanır.
 * - sel: 0 = hepsi yığında, 1 = anchor kartı üstte. Parmakla çekerken parmağı izler, bırakınca yaylanır.
 * - anchor: yukarı çıkan kart (seçili ya da çekilmekte olan). Yığın tamamen kapanana kadar korunur.
 * - drag*: uzun basıp sürükleyerek sıralama.
 */
@Stable
private class StackState(selected: String?) {
    var scroll by mutableFloatStateOf(0f)
    var sel by mutableFloatStateOf(if (selected != null) 1f else 0f)
    var anchor by mutableStateOf(selected)
    var pulling by mutableStateOf(false)
    var pullTotal = 0f
    var pullDist = 1f
    var handoff = 0f

    var dragId by mutableStateOf<String?>(null)
    var dragTop by mutableFloatStateOf(0f)
    var dragGrab = 0f
    var dragOrigin = 0
    var dragTarget = 0
    var liveOrder by mutableStateOf<List<String>?>(null)

    var selJob: Job? = null
    var scrollJob: Job? = null

    private val slots = HashMap<String, Slot>()
    fun slot(id: String, index: Int): Slot = slots.getOrPut(id) { Slot(index.toFloat()) }
    fun forget(keep: Set<String>) { slots.keys.retainAll(keep) }

    fun animateSel(scope: CoroutineScope, target: Float, velocity: Float, reduced: Boolean, then: () -> Unit = {}) {
        selJob?.cancel()
        selJob = scope.launch {
            val spec: AnimationSpec<Float> = if (reduced) snap() else spring(dampingRatio = 0.84f, stiffness = 230f)
            animate(sel, target, velocity, spec) { v, _ -> sel = v }
            then()
        }
    }

    /** Yığını parmakla kaydırırken: sınırın dışında direnç artar. */
    fun scrollBy(delta: Float, max: Float, overshoot: Float) {
        val outside = scroll < 0f || scroll > max
        val further = (scroll <= 0f && delta < 0f) || (scroll >= max && delta > 0f)
        scroll = (scroll + if (outside && further) delta * 0.4f else delta).coerceIn(-overshoot, max + overshoot)
    }
}

@Stable
private class Slot(initial: Float) {
    var value by mutableFloatStateOf(initial)
}

private suspend fun flingScroll(st: StackState, velocity: Float, max: Float) {
    if (st.scroll < 0f || st.scroll > max) {
        animate(st.scroll, st.scroll.coerceIn(0f, max), velocity, spring(dampingRatio = 0.9f, stiffness = 260f)) { v, _ -> st.scroll = v }
        return
    }
    val state = AnimationState(st.scroll, velocity)
    var hitEdge = false
    state.animateDecay(exponentialDecay(frictionMultiplier = 1.3f, absVelocityThreshold = 20f)) {
        st.scroll = value
        if (value < 0f || value > max) { hitEdge = true; cancelAnimation() }
    }
    if (hitEdge) {
        animate(st.scroll, st.scroll.coerceIn(0f, max), state.velocity * 0.25f, spring(dampingRatio = 0.8f, stiffness = 320f)) { v, _ -> st.scroll = v }
    }
}

@Composable
fun StackScreen(
    activity: MainActivity,
    cards: List<WaultCard>,
    loaded: Boolean,
    env: Environment,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onAdd: () -> Unit,
    onMove: (List<String>) -> Unit,
    onSettings: () -> Unit,
    onEdit: (WaultCard) -> Unit,
) {
    val reduced = Wault.reducedMotion
    val selected = cards.firstOrNull { it.id == selectedId }
    var lastShown by remember { mutableStateOf(selected) }
    if (selected != null) lastShown = selected
    val st = remember { StackState(selectedId) }
    val glow = lastShown?.let { shown -> rememberCardArt(shown).let { lerp(it.stops.first(), it.glow, 0.35f) } }
    val strongGlow = Wault.colors.isDark
    val glowEnabled = Wault.appearance.lightEffects

    // Karta basıldığı anda: bağlı ödeme kartıysa varsayılan ödeme yöntemini bu karta geçir.
    LaunchedEffect(selectedId) {
        val card = selected ?: return@LaunchedEffect
        val prefs = activity.app.container.prefs
        when {
            // Ödeme kartı: Shizuku hazırsa sessizce, değilse sistem seçicisiyle bu karta geç
            card.kind == CardKind.PAYMENT && card.payment != null && card.id !in env.defaultIds -> {
                val outcome = PaymentSwitcher.switchTo(activity, card.payment)
                if (outcome == SwitchOutcome.Silent) activity.resumeTick.intValue++
            }
            // Vitrin kartı: Shizuku hazırsa varsayılan ödeme uygulamasını kaldır (okuyucuya ödeme sunulmasın)
            card.kind == CardKind.SHOWCASE && prefs.state.value.showcaseTurnsOffPayment &&
                env.shizuku == ShizukuBridge.State.READY -> {
                val holder = ShizukuBridge.walletHolders().firstOrNull()
                if (holder != null) {
                    // Deneme modunda (debug) benzetilen cüzdan, kullanıcının gerçek ayarına yazılmaz
                    if (!(BuildConfig.DEBUG && DevOptions.dryRun)) prefs.update { it.copy(lastWalletPackage = holder) }
                    if (ShizukuBridge.clearWalletHolder()) activity.resumeTick.intValue++
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Seçili kartın rengi, kartın altından yayılan yumuşak bir ışık olarak tüm zemine vurur
            .drawBehind {
                if (glowEnabled && glow != null && st.sel > 0.001f) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(glow.copy(alpha = (if (strongGlow) 0.34f else 0.2f) * st.sel.coerceIn(0f, 1f)), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.36f),
                            radius = size.width * 1.2f,
                        )
                    )
                }
            }
    ) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(60.dp)) {
            Crossfade(selected != null, animationSpec = tween(if (reduced) 0 else 180), label = "bar") { isSelected ->
                if (!isSelected) {
                    LargeTitleBar("Wault") {
                        CircleButton(Glyph.PLUS, "Kart ekle", onAdd)
                        CircleButton(Glyph.MORE, "Ayarlar", onSettings)
                    }
                } else {
                    Box(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        BarButton("Bitti", { onSelect(null) }, Modifier.align(Alignment.CenterEnd), bold = true)
                    }
                }
            }
        }

        if (loaded && cards.isEmpty()) {
            EmptyWallet(onAdd)
            return@Column
        }

        Stage(activity, st, cards, env, selectedId, lastShown, onSelect, onMove, onEdit)
    }
    }
}

@Composable
private fun Stage(
    activity: MainActivity,
    st: StackState,
    cards: List<WaultCard>,
    env: Environment,
    selectedId: String?,
    lastShown: WaultCard?,
    onSelect: (String?) -> Unit,
    onMove: (List<String>) -> Unit,
    onEdit: (WaultCard) -> Unit,
) {
    val reduced = Wault.reducedMotion
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Seçim değişince: yığını aç/kapat. Parmakla başlayan çekme kaldığı yerden yayla devam eder.
    LaunchedEffect(selectedId) {
        if (selectedId != null) st.anchor = selectedId
        val target = if (selectedId != null) 1f else 0f
        val velocity = st.handoff
        st.handoff = 0f
        st.animateSel(scope, target, velocity, reduced) { if (target == 0f && !st.pulling) st.anchor = null }
    }

    // Geri hareketi: kenardan çekerken seçili kart parmakla çekilmiş gibi yığına iner; bırakınca tamamlanır
    PredictiveBackHandler(enabled = selectedId != null) { progress ->
        try {
            progress.collect { event -> st.selJob?.cancel(); st.sel = (1f - event.progress * 0.8f).coerceIn(0f, 1f) }
            st.handoff = 0f
            onSelect(null)
        } catch (e: CancellationException) {
            st.animateSel(scope, 1f, 0f, reduced)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val viewportH = constraints.maxHeight.toFloat()
        val gutter = with(density) { Gutter.toPx() }
        val cardH = (constraints.maxWidth - 2 * gutter) / CARD_RATIO
        val topPad = with(density) { TopPad.toPx() }
        val navInset = WindowInsets.navigationBars.getBottom(density).toFloat()
        val dockSpace = navInset + with(density) { (DockHeight + DockGap * 2).toPx() }
        val n = cards.size
        val minPeek = with(density) { CardPeek.toPx() }
        val maxPeek = with(density) { MaxPeek.toPx() }
        // Az kart varsa şeritleri genişlet: ekran boş kalmasın. Çok kart varsa en az CardPeek, gerisi kaydırılır.
        val peek = if (n <= 1) 0f else ((viewportH - topPad - cardH - dockSpace) / (n - 1)).coerceIn(minPeek, maxPeek)
        val contentH = topPad + peek * (n - 1) + cardH + dockSpace
        val maxScroll = (contentH - viewportH).coerceAtLeast(0f)
        val overshoot = with(density) { 120.dp.toPx() }

        val order = remember(cards) { cards.map { it.id } }
        val anchor = st.anchor
        val others = remember(order, anchor) { order.filter { it != anchor } }
        val othersIdx = remember(others) { others.withIndex().associate { it.value to it.index } }
        val orderIdx = remember(order) { order.withIndex().associate { it.value to it.index } }
        val peeks = min(others.size, BOTTOM_PEEKS)
        val firstVisible = others.size - peeks
        val sliver = with(density) { PeekSliver.toPx() }
        val step = with(density) { PeekStep.toPx() }
        val bottomBase = viewportH - navInset - with(density) { 4.dp.toPx() } - sliver - step * (peeks - 1).coerceAtLeast(0)
        val detailTop = topPad + cardH + with(density) { 22.dp.toPx() }
        val detailHeight = with(density) { (bottomBase - detailTop - 14.dp.toPx()).coerceAtLeast(160.dp.toPx()).toDp() }

        LaunchedEffect(order) { st.forget(order.toSet()) }
        LaunchedEffect(maxScroll) { if (st.dragId == null && st.scroll > maxScroll) st.scroll = maxScroll }
        // Sürükleme bırakıldıktan sonra kartlar yeni sıraya oturunca canlı sırayı bırak
        LaunchedEffect(order, st.dragId, st.liveOrder) { if (st.dragId == null && st.liveOrder == order) st.liveOrder = null }

        fun collapsedY(id: String, index: Int): Float = topPad + st.slot(id, index).value * peek - st.scroll

        fun updateTarget() {
            if (n <= 1 || peek <= 0f) return
            val slotF = ((st.dragTop + st.scroll - topPad) / peek).coerceIn(0f, (n - 1).toFloat())
            val t = slotF.roundToInt()
            if (t != st.dragTarget) {
                st.dragTarget = t
                st.liveOrder = Reorder.move(order, st.dragOrigin, t)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        // Sürüklerken ekranın üst/alt kenarına yaklaşınca yığını kendiliğinden kaydır
        LaunchedEffect(st.dragId, maxScroll) {
            if (st.dragId == null || maxScroll <= 0f) return@LaunchedEffect
            val zone = with(density) { 84.dp.toPx() }
            val maxSpeed = with(density) { 900.dp.toPx() }
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (isActive) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                val dt = (now - last) / 1_000_000_000f
                last = now
                val finger = st.dragTop + st.dragGrab
                val lower = viewportH - dockSpace
                val speed = when {
                    finger < zone -> -((zone - finger) / zone).coerceIn(0f, 1f) * maxSpeed
                    finger > lower - zone -> ((finger - (lower - zone)) / zone).coerceIn(0f, 1f) * maxSpeed
                    else -> 0f
                }
                if (speed != 0f) {
                    val next = (st.scroll + speed * dt).coerceIn(0f, maxScroll)
                    if (next != st.scroll) { st.scroll = next; updateTarget() }
                }
            }
        }

        fun startReorder(id: String, grabY: Float) {
            val i = order.indexOf(id)
            if (i < 0) return
            st.scrollJob?.cancel()
            st.dragOrigin = i
            st.dragTarget = i
            st.dragGrab = grabY
            st.dragTop = collapsedY(id, i)
            st.liveOrder = order
            st.dragId = id
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        fun endReorder(id: String, cancelled: Boolean) {
            if (st.dragId != id) return
            val slotF = if (n <= 1 || peek <= 0f) 0f else ((st.dragTop + st.scroll - topPad) / peek).coerceIn(0f, (n - 1).toFloat())
            st.slot(id, st.dragOrigin).value = slotF
            val ids = if (cancelled) order else (st.liveOrder ?: order)
            st.dragId = null
            if (cancelled || ids == order) st.liveOrder = null else onMove(ids)
        }

        // Kart çekme: parmak izleyen etkileşimli geçiş
        fun startPull(card: WaultCard, index: Int, fromSelected: Boolean) {
            st.selJob?.cancel()
            st.pullTotal = 0f
            st.pullDist = max(collapsedY(card.id, index) - topPad, 0.3f * viewportH)
            if (!fromSelected) { st.anchor = card.id; st.pulling = true }
        }

        fun pull(dy: Float, fromSelected: Boolean) {
            st.pullTotal += dy
            st.sel = if (fromSelected) (1f - st.pullTotal / st.pullDist).coerceIn(0f, 1f)
            else (-st.pullTotal / st.pullDist).coerceIn(0f, 1f)
        }

        fun finishPull(card: WaultCard, velocity: Float, fromSelected: Boolean) {
            val dist = st.pullDist
            if (!fromSelected) {
                if (st.sel > 0.38f || velocity < -1100f) {
                    st.handoff = (-velocity / dist).coerceAtLeast(0f)
                    st.pulling = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(card.id)
                } else {
                    st.animateSel(scope, 0f, -velocity / dist, reduced) { st.pulling = false; st.anchor = null }
                }
            } else {
                if (st.sel < 0.62f || velocity > 1100f) {
                    st.handoff = (-velocity / dist).coerceAtMost(0f)
                    onSelect(null)
                } else {
                    st.animateSel(scope, 1f, -velocity / dist, reduced)
                }
            }
        }

        val scrollEnabled = selectedId == null && maxScroll > 0f
        val detailShown by remember { derivedStateOf { st.sel > 0.55f } }
        val collapsedAtRest by remember { derivedStateOf { st.sel < 0.02f } }

        Box(
            Modifier
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = scrollEnabled,
                    state = rememberDraggableState { dy -> st.scrollBy(-dy, maxScroll, overshoot) },
                    onDragStarted = { st.scrollJob?.cancel() },
                    onDragStopped = { v -> st.scrollJob = scope.launch { flingScroll(st, -v, maxScroll) } },
                )
        ) {
            cards.forEachIndexed { i, card ->
                androidx.compose.runtime.key(card.id) {
                    val isSel = card.id == selectedId
                    val isDragged = st.dragId == card.id
                    val isAnchor = card.id == st.anchor
                    val motion = st.slot(card.id, i)
                    val liveIdx = (st.liveOrder ?: order).indexOf(card.id).coerceAtLeast(0)

                    LaunchedEffect(liveIdx, isDragged) {
                        if (!isDragged) {
                            val spec: AnimationSpec<Float> = if (reduced) snap() else spring(dampingRatio = 0.8f, stiffness = 380f)
                            animate(motion.value, liveIdx.toFloat(), 0f, spec) { v, _ -> motion.value = v }
                        }
                    }

                    val rank = if (isAnchor) 0 else kotlin.math.abs((orderIdx[card.id] ?: 0) - (orderIdx[anchor] ?: 0))
                    val otherJ = othersIdx[card.id] ?: -1
                    val visibleJ = otherJ - firstVisible

                    // seçim ilerlemesi: sıradaki kartlar çekilen karttan biraz geriden gelir
                    fun progress(): Float {
                        val sel = st.sel
                        if (sel <= 0.0005f || st.anchor == null) return 0f
                        val d = if (isAnchor) 0f else (0.05f * rank).coerceAtMost(0.28f)
                        val raw = ((sel - d) / (1f - d)).coerceAtLeast(0f)
                        return if (isAnchor) min(raw, 1f) else min(raw, 1.04f)
                    }

                    fun yNow(): Float {
                        if (isDragged) return st.dragTop
                        val collapsed = collapsedY(card.id, i)
                        val p = progress()
                        if (p == 0f) return collapsed
                        val target = if (isAnchor) topPad else bottomBase + step * max(0, visibleJ)
                        return collapsed + (target - collapsed) * p
                    }

                    val tapLabel = if (selectedId == null) "Kartı aç" else if (isSel) null else "Kartları göster"
                    // Sürüklerken kaldırılan kart: ölçek ve gölge yumuşakça artar, bırakınca yumuşakça iner
                    val lift by animateFloatAsState(if (isDragged) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "lift")
                    val raised = isAnchor || isDragged || lift > 0.02f
                    val press = remember { MutableInteractionSource() }
                    val pressed by press.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(if (pressed && st.dragId == null) 0.985f else 1f, spring(stiffness = 900f), label = "press")
                    val pullUp = false // yukarı çekmek ödemeye geçmez: yalnızca dokunmak açar, sürüklemek sıralar
                    val pullDown = false

                    Box(
                        Modifier
                            .zIndex(if (isAnchor) 1000f else if (raised) 900f else i.toFloat())
                            .padding(horizontal = Gutter)
                            .offset { IntOffset(0, yNow().roundToInt()) }
                            .graphicsLayer {
                                val p = progress()
                                val target = if (isAnchor) 1f else 0.95f
                                val s = (1f + (target - 1f) * p) * (1f + 0.035f * lift) * pressScale
                                scaleX = s; scaleY = s
                                alpha = if (!isAnchor && p > 0f && visibleJ < 0) (1f - min(p, 1f)) else 1f
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("Yukarı taşı") {
                                        if (i > 0) { onMove(Reorder.move(order, i, i - 1)); true } else false
                                    },
                                    CustomAccessibilityAction("Aşağı taşı") {
                                        if (i < n - 1) { onMove(Reorder.move(order, i, i + 1)); true } else false
                                    },
                                )
                            }
                            .clickable(
                                interactionSource = press,
                                indication = null,
                                role = Role.Button,
                                enabled = tapLabel != null,
                                onClickLabel = tapLabel,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (selectedId == null) { if (st.sel < 0.02f) onSelect(card.id) } else onSelect(null)
                            }
                            .draggable(
                                orientation = Orientation.Vertical,
                                enabled = pullUp || pullDown,
                                state = rememberDraggableState { dy -> pull(dy, pullDown) },
                                onDragStarted = { startPull(card, i, pullDown) },
                                onDragStopped = { v -> finishPull(card, v, pullDown) },
                            )
                            .pointerInput(card.id, selectedId == null, n, peek, maxScroll, order) {
                                if (selectedId != null) return@pointerInput
                                if (maxScroll <= 0f) {
                                    // Apple Wallet gibi: kart sığıyorsa yukarı/aşağı sürüklemek doğrudan sırayı değiştirir
                                    detectVerticalDragGestures(
                                        onDragStart = { offset -> startReorder(card.id, offset.y) },
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            st.dragTop += amount
                                            updateTarget()
                                        },
                                        onDragEnd = { endReorder(card.id, cancelled = false) },
                                        onDragCancel = { endReorder(card.id, cancelled = true) },
                                    )
                                } else {
                                    // Kartlar ekrana sığmıyorsa sürükleme yığını kaydırır; sıralamak için basılı tutulur
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset -> startReorder(card.id, offset.y) },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            st.dragTop += amount.y
                                            updateTarget()
                                        },
                                        onDragEnd = { endReorder(card.id, cancelled = false) },
                                        onDragCancel = { endReorder(card.id, cancelled = true) },
                                    )
                                }
                            }
                    ) {
                        CardSurface(
                            card,
                            isDefault = card.id in env.defaultIds,
                            elevation = if (isAnchor) 14.dp else 6.dp + 16.dp * lift,
                            // alttaki yığında sadece kartların kenarları görünsün
                            showFace = (selectedId == null && collapsedAtRest) || isAnchor || isDragged,
                        )
                    }
                }
            }

            // Yığın başlığın altına kayarken sert kesik yerine yumuşak bir geçiş
            val fadeColor = Wault.colors.bg
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(3000f)
                    .graphicsLayer { alpha = (st.scroll / 40f).coerceIn(0f, 1f) * (1f - st.sel.coerceIn(0f, 1f)) }
                    .background(Brush.verticalGradient(listOf(fadeColor, Color.Transparent)))
            )

            if (detailShown) {
                lastShown?.let { shown ->
                    Box(
                        Modifier
                            .zIndex(999f)
                            .offset { IntOffset(0, detailTop.roundToInt()) }
                            .graphicsLayer {
                                val a = ((st.sel - 0.7f) / 0.3f).coerceIn(0f, 1f)
                                alpha = a
                                translationY = (1f - a) * 28.dp.toPx()
                            }
                            .padding(horizontal = Gutter)
                            .height(detailHeight)
                    ) {
                        CardDetail(activity, shown, env, onEdit = { onEdit(shown) }, onChange = { activity.app.container.store.upsert(it) })
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedId == null,
                enter = fadeIn(tween(if (reduced) 0 else 220, delayMillis = if (reduced) 0 else 80)),
                exit = fadeOut(tween(if (reduced) 0 else 90)),
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(2000f),
            ) {
                ReadyBar(
                    cards, env, onNfcSettings = { PaymentSwitcher.openNfcSettings(activity) },
                    modifier = Modifier
                        .padding(horizontal = Gutter)
                        .padding(bottom = with(density) { navInset.toDp() } + DockGap)
                        .graphicsLayer { alpha = (1f - st.sel * 1.8f).coerceIn(0f, 1f) },
                )
            }
        }
    }
}

/** Boş cüzdan: üst üste duran üç silik kart ve tek bir eylem. */
@Composable
private fun EmptyWallet(onAdd: () -> Unit) {
    val c = Wault.colors
    val ghosts = remember {
        listOf("silver", "ocean", "sunset").map { WaultCard(kind = CardKind.SHOWCASE, title = "", artId = it) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        Box(Modifier.width(260.dp).height(220.dp)) {
            ghosts.forEachIndexed { i, card ->
                Box(Modifier.offset(y = (i * 44).dp).graphicsLayer { alpha = 0.35f + i * 0.25f }) {
                    CardSurface(card.copy(title = if (i == 2) "Wault" else ""), elevation = 4.dp, showFace = i == 2)
                }
            }
        }
        Spacer(Modifier.height(36.dp))
        Text("Kartların burada duracak", style = WaultType.title, color = c.label, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ödeme kartlarını, sadakat kartlarını, kodlarını ve NFC kartlarını tek yerde topla.",
            style = WaultType.subhead, color = c.secondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Kart ekle", onAdd, Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
    }
}
