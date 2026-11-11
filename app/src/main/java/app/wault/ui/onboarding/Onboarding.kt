package app.wault.ui.onboarding

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import app.wault.ui.pay.PaidSummary
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import app.wault.design.CardSurface
import app.wault.design.CARD_RATIO
import androidx.compose.ui.zIndex
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.data.WaultCard
import app.wault.design.BarButton
import app.wault.design.CardArts
import app.wault.design.CheckMark
import app.wault.design.Glyph
import app.wault.design.Group
import app.wault.design.GroupRow
import app.wault.design.IconTile
import app.wault.design.PrimaryButton
import app.wault.design.Separator
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.nfc.PaymentService
import app.wault.nfc.PaymentServices
import app.wault.nfc.PaymentSwitcher
import app.wault.notify.PaymentNotificationListener
import app.wault.ui.Environment
import app.wault.ui.common.AppIcon

/** Banka dışı cüzdanlar: varsayılan olarak seçilmez. */
private val walletPackages = setOf("com.google.android.gms", "com.samsung.android.spay.offline", "com.samsung.android.spay")

/** Kurum adını kartta kısa göstermek için uygulama adındaki ekleri atar. */
fun shortBankName(label: String): String =
    label.replace(Regex("\\s+(Mobil|Mobile|Cep Şubesi|Cep Subesi|Direkt|BBVA Mobile|BBVA)$", RegexOption.IGNORE_CASE), "").trim()

fun cardFromService(s: PaymentService): WaultCard = WaultCard(
    kind = CardKind.PAYMENT,
    title = shortBankName(s.appLabel),
    artId = "auto",
    payment = s.toLink(),
)

/**
 * İlk açılış. 1) Telefondaki NFC ödeme uygulamalarını kart olarak ekle.
 * 2) Ödeme onayını gösterebilmek için bildirim erişimi iste.
 */
@Composable
fun Onboarding(activity: MainActivity, env: Environment, onFinish: (List<WaultCard>) -> Unit) {
    val services = remember { PaymentServices.list(activity) }
    var step by remember { mutableStateOf(0) }
    var picked by remember {
        mutableStateOf(services.filter { it.component.packageName !in walletPackages }.map { it.component }.toSet())
    }
    var chosen by remember { mutableStateOf<List<WaultCard>>(emptyList()) }

    if (step == 0) {
        PickApps(
            activity = activity,
            services = services,
            picked = picked,
            onToggle = { cn -> picked = if (cn in picked) picked - cn else picked + cn },
            onNext = {
                chosen = services.filter { it.component in picked }.map(::cardFromService)
                step = 1
            },
        )
    } else {
        // Erişim zaten verilmişse ikinci adıma gerek yok
        LaunchedEffect(env.notificationAccess) { if (env.notificationAccess) onFinish(chosen) }
        AskNotifications(activity, sample = chosen.firstOrNull(), onDone = { onFinish(chosen) })
    }
}

@Composable
private fun PickApps(
    activity: MainActivity,
    services: List<PaymentService>,
    picked: Set<android.content.ComponentName>,
    onToggle: (android.content.ComponentName) -> Unit,
    onNext: () -> Unit,
) {
    val c = Wault.colors
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(36.dp))
            Text("Kartlarını ekle", style = WaultType.largeTitle, color = c.label, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                if (services.isEmpty()) "Bu cihazda NFC ile ödeme yapan bir uygulama bulunamadı. Sadakat kartlarını, kodlarını ve vitrin kartlarını yine de ekleyebilirsin."
                else "Telefonunda temassız ödeme yapabilen uygulamalar bunlar. Seçtiklerin Wault'ta kart olur; karta dokununca ödeme o uygulamaya geçer.",
                style = WaultType.callout, color = c.secondary, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
            StackPreview(services.filter { it.component in picked }.map(::cardFromService))
            Spacer(Modifier.height(20.dp))
            if (services.isNotEmpty()) {
                Group(footer = "Bir uygulamada birden fazla kartın varsa sonra elle ekleyebilirsin.") {
                    services.forEachIndexed { i, s ->
                        val isDefault = PaymentSwitcher.isDefault(activity, s.toLink())
                        GroupRow(
                            s.appLabel,
                            subtitle = if (isDefault) "Şu an varsayılan" else null,
                            leading = { AppIcon(s.component.packageName, 34.dp) },
                            trailing = { CheckMark(s.component in picked) },
                            onClick = { onToggle(s.component) },
                        )
                        if (i < services.lastIndex) Separator(64.dp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(16.dp)) {
            PrimaryButton(
                when {
                    services.isEmpty() -> "Devam"
                    picked.isEmpty() -> "Kart eklemeden devam et"
                    else -> "${picked.size} kartı ekle"
                },
                onNext, Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Seçilenlerin cüzdanda nasıl duracağını canlı gösterir: her seçimde yığına bir kart eklenir.
 * Sadece ilk dört kart çizilir; asıl yığın ana ekranda.
 */
@Composable
private fun StackPreview(cards: List<WaultCard>) {
    val shown = cards.take(4)
    if (shown.isEmpty()) return
    val peek = 54.dp
    BoxWithConstraints(Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.85f, stiffness = 380f))) {
        val cardW = maxWidth * 0.8f
        val cardH = cardW / CARD_RATIO
        Box(Modifier.align(Alignment.TopCenter).width(cardW).height(cardH + peek * (shown.size - 1))) {
            shown.forEachIndexed { i, card ->
                key(card.id) {
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = visible,
                        enter = fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it / 3 },
                        modifier = Modifier.offset(y = peek * i).zIndex(i.toFloat()),
                    ) {
                        CardSurface(card, elevation = 6.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AskNotifications(activity: MainActivity, sample: WaultCard?, onDone: () -> Unit) {
    val c = Wault.colors
    val reduced = Wault.reducedMotion
    val ring = remember { Animatable(if (reduced) 1f else 0f) }
    val check = remember { Animatable(if (reduced) 1f else 0f) }
    val text = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(250)
            ring.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            launch { check.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
            text.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 400f))
        }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.8f))
        // Ödeme yapılınca ne göreceğinin küçük bir örneği
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface).padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            PaidSummary(
                ring.value, check.value, text.value, "₺245,90", sample,
                badgeSize = 84.dp, amountStyle = WaultType.largeTitle,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text("Ödeme onayını Wault göstersin", style = WaultType.title, color = c.label, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Ödeme yapınca bankanın gönderdiği bildirimi Wault okur ve tutarı kendi ekranında gösterir. " +
                "Wault sadece bağladığın bankaların bildirimlerine bakar; hiçbir şeyi telefondan dışarı göndermez.",
            style = WaultType.callout, color = c.secondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Bildirim erişimi ver", { PaymentNotificationListener.openAccessSettings(activity) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        BarButton("Şimdi değil", onDone)
        Spacer(Modifier.height(12.dp))
    }
}

