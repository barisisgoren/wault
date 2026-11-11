package app.wault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.data.WaultCard
import app.wault.design.CardArts
import app.wault.design.Wault
import app.wault.notify.PaidEvent
import app.wault.notify.PaymentEvents
import app.wault.nfc.PaymentSwitcher
import app.wault.nfc.ShizukuBridge
import app.wault.notify.PaymentNotificationListener
import app.wault.ui.add.AddSheet
import app.wault.ui.add.NfcCopyScreen
import app.wault.ui.edit.CardEditor
import app.wault.ui.edit.ReorderScreen
import app.wault.ui.pay.PaidMoment
import app.wault.ui.settings.LockScreen
import app.wault.ui.settings.SettingsScreen
import app.wault.ui.stack.StackScreen
import app.wault.ui.onboarding.Onboarding

sealed interface Screen {
    data object Stack : Screen
    data object Reorder : Screen
    data object Settings : Screen
    data object NfcCopy : Screen
    data object OnboardingPreview : Screen
    data class Editor(val card: WaultCard, val isNew: Boolean) : Screen
}

/** Dış dünyadan okunan durum: hangi kart varsayılan, izinler, NFC. */
class Environment(
    val defaultIds: Set<String> = emptySet(),
    val notificationAccess: Boolean = false,
    val nfcAvailable: Boolean = false,
    val nfcEnabled: Boolean = false,
    val shizuku: ShizukuBridge.State = ShizukuBridge.State.NOT_INSTALLED,
)

@Composable
fun WaultApp(activity: MainActivity) {
    val container = activity.app.container
    val cards by container.store.cards.collectAsState()
    val loaded by container.store.loaded.collectAsState()
    val tick = activity.resumeTick.intValue
    val env = remember(cards, tick) {
        Environment(
            defaultIds = cards.filter { it.kind == CardKind.PAYMENT && it.payment != null && PaymentSwitcher.isDefault(activity, it.payment) }
                .map { it.id }.toSet(),
            notificationAccess = PaymentNotificationListener.hasAccess(activity),
            nfcAvailable = activity.nfc.available,
            nfcEnabled = activity.nfc.enabled,
            shizuku = ShizukuBridge.state(activity),
        )
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Stack) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    // Seçili kart silindiyse ya da kasa yeniden okunduysa yığın "seçili" modda takılı kalmasın
    LaunchedEffect(cards, loaded, selectedId) {
        if (loaded && selectedId != null && cards.none { it.id == selectedId }) selectedId = null
    }
    var paid by remember { mutableStateOf<PaidEvent?>(null) }

    // Kutucuktan açılış: varsayılan kartı seç
    val openReq = activity.openActiveRequest.intValue
    LaunchedEffect(openReq, env) {
        if (openReq > 0) {
            screen = Screen.Stack
            selectedId = env.defaultIds.firstOrNull() ?: cards.firstOrNull { it.kind == CardKind.PAYMENT }?.id
        }
    }

    LaunchedEffect(Unit) {
        PaymentEvents.pending?.let { paid = it; PaymentEvents.pending = null }
        PaymentEvents.events.collect { paid = it }
    }
    LaunchedEffect(tick) {
        PaymentEvents.pending?.let { paid = it; PaymentEvents.pending = null }
    }

    BackHandler(enabled = adding || screen != Screen.Stack || selectedId != null) {
        when {
            adding -> adding = false
            screen != Screen.Stack -> screen = Screen.Stack
            else -> selectedId = null
        }
    }

    val prefs by container.prefs.state.collectAsState()
    if (loaded && !prefs.onboarded) {
        if (cards.isNotEmpty()) {
            LaunchedEffect(Unit) { container.prefs.update { it.copy(onboarded = true) } }
        } else {
            Box(Modifier.fillMaxSize().background(Wault.colors.bg)) {
                Onboarding(activity, env) { picked ->
                    // upsert başa ekler; sırayı korumak için tersten ekle
                    picked.asReversed().forEach { container.store.upsert(it) }
                    container.prefs.update { it.copy(onboarded = true) }
                }
            }
            return
        }
    }

    val reduced = Wault.reducedMotion
    Box(Modifier.fillMaxSize().background(Wault.colors.bg)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (reduced) fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                else if (targetState == Screen.Stack) {
                    (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(320)) { it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(200)))
                }
            },
            contentKey = { it::class },
            label = "screen",
        ) { s ->
            when (s) {
                Screen.Stack -> StackScreen(
                    activity = activity,
                    cards = cards,
                    loaded = loaded,
                    env = env,
                    selectedId = selectedId,
                    onSelect = { selectedId = it },
                    onAdd = { adding = true },
                    onMove = { ids -> container.store.reorder(ids) },
                    onSettings = { screen = Screen.Settings },
                    onEdit = { screen = Screen.Editor(it, isNew = false) },
                )
                Screen.Reorder -> ReorderScreen(
                    cards = cards,
                    onDone = { ids -> container.store.reorder(ids); screen = Screen.Stack },
                    onOpen = { screen = Screen.Editor(it, isNew = false) },
                )
                Screen.Settings -> SettingsScreen(
                    activity, env,
                    onBack = { screen = Screen.Stack },
                    onReorder = { screen = Screen.Reorder },
                    onPreviewOnboarding = { screen = Screen.OnboardingPreview },
                )
                Screen.OnboardingPreview -> Box(Modifier.fillMaxSize().background(Wault.colors.bg)) {
                    Onboarding(activity, env) { screen = Screen.Settings }
                }
                Screen.NfcCopy -> NfcCopyScreen(
                    activity = activity,
                    env = env,
                    onCancel = { screen = Screen.Stack },
                    onCopied = { screen = Screen.Editor(it, isNew = true) },
                    onAddPayment = { screen = Screen.Editor(WaultCard(kind = CardKind.PAYMENT, title = "", artId = CardArts.defaultFor(CardKind.PAYMENT)), isNew = true) },
                )
                is Screen.Editor -> CardEditor(
                    activity = activity,
                    initial = s.card,
                    isNew = s.isNew,
                    onCancel = { screen = Screen.Stack },
                    onSave = { card ->
                        container.store.upsert(card)
                        selectedId = card.id
                        screen = Screen.Stack
                    },
                    onDelete = { id ->
                        container.store.delete(id)
                        if (selectedId == id) selectedId = null
                        screen = Screen.Stack
                    },
                )
            }
        }

        if (adding) {
            AddSheet(
                onDismiss = { adding = false },
                onPick = { kind ->
                    adding = false
                    selectedId = null
                    screen = if (kind == CardKind.NFC_COPY) Screen.NfcCopy
                    else Screen.Editor(WaultCard(kind = kind, title = "", artId = CardArts.defaultFor(kind)), isNew = true)
                },
            )
        }

        paid?.let { event ->
            PaidMoment(
                event = event,
                card = cards.firstOrNull { it.id == event.cardId },
                onDone = { paid = null },
            )
        }

        if (activity.locked.value) {
            LockScreen(activity)
        }
    }
}
