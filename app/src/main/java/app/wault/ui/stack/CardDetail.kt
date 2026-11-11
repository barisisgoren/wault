package app.wault.ui.stack

import app.wault.ui.common.withCustomColor
import app.wault.ui.common.ColorSheet
import app.wault.ui.common.QuickColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.data.NfcCapability
import app.wault.data.WaultCard
import app.wault.design.Glyph
import app.wault.design.rememberCardArt
import app.wault.design.GlyphIcon
import app.wault.design.Group
import app.wault.design.GroupRow
import app.wault.design.PrimaryButton
import app.wault.design.Separator
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.nfc.EmulationState
import app.wault.nfc.NdefWriter
import app.wault.nfc.PaymentSwitcher
import app.wault.nfc.ShizukuBridge
import app.wault.nfc.SwitchOutcome
import app.wault.design.TintedButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import app.wault.nfc.TagAnalyzer
import app.wault.nfc.WriteResult
import app.wault.nfc.hexToBytes
import app.wault.notify.PaymentNotificationListener
import app.wault.ui.Environment
import app.wault.ui.code.BarcodeView
import app.wault.ui.code.canEncode
import app.wault.ui.common.AppIcon
import app.wault.ui.common.ReadyPulse

@Composable
fun CardDetail(activity: MainActivity, card: WaultCard, env: Environment, onEdit: () -> Unit, onChange: (WaultCard) -> Unit = {}) {
    val tint = rememberCardArt(card).accentOn(Wault.colors.isDark)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (card.kind) {
            CardKind.PAYMENT -> PaymentDetail(activity, card, env, tint, onEdit)
            CardKind.SHOWCASE -> ShowcaseDetail(activity, env, onEdit)
            CardKind.LOYALTY, CardKind.CODE -> CodeDetail(activity, card, onEdit)
            CardKind.NFC_COPY -> NfcDetail(activity, card, env, tint, onEdit)
        }
        Spacer(Modifier.height(20.dp))
        var sheet by remember { mutableStateOf(false) }
        Group(header = "Renk") {
            QuickColors(card, onChange, onCustom = { sheet = true })
        }
        if (sheet) {
            ColorSheet(current = rememberCardArt(card).base, onPick = { onChange(card.withCustomColor(it)) }, onDone = { sheet = false })
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Ortalanmış durum bloğu: ikon, başlık, açıklama. */
@Composable
private fun Status(title: String, body: String?, icon: @Composable () -> Unit = {}) {
    val c = Wault.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(14.dp))
        Text(title, style = WaultType.title, color = c.label, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(body, style = WaultType.subhead, color = c.secondary, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}

@Composable
private fun PaymentDetail(activity: MainActivity, card: WaultCard, env: Environment, tint: Color, onEdit: () -> Unit) {
    val c = Wault.colors
    val link = card.payment
    val isDefault = card.id in env.defaultIds
    val scope = rememberCoroutineScope()
    when {
        link == null -> Status(
            "Ödeme uygulaması seçilmedi",
            "Kartı düzenleyip bankanın uygulamasını seç. Ödemeyi o uygulama yapar, onayı Wault gösterir.",
        )
        !env.nfcAvailable -> Status("Bu cihazda NFC yok", "Temassız ödeme için NFC'li bir telefon gerekiyor.")
        !env.nfcEnabled -> {
            Status("NFC kapalı", "Temassız ödeme için NFC'yi aç.")
            Spacer(Modifier.height(16.dp))
            PrimaryButton("NFC'yi aç", { PaymentSwitcher.openNfcSettings(activity) }, Modifier.fillMaxWidth())
        }
        isDefault -> Status("Telefonu okuyucuya yaklaştır", "${link.appLabel} ile ödenir") { ReadyPulse(tint = tint) }
        else -> {
            Status("Bu kart varsayılan değil", "Android, ödeme uygulamasını değiştirmek için senden onay ister.")
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Bu kartla öde", {
                scope.launch {
                    if (PaymentSwitcher.switchTo(activity, link) == SwitchOutcome.Silent) activity.resumeTick.intValue++
                }
            }, Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(24.dp))
    Group {
        if (link != null) {
            GroupRow(
                link.appLabel, subtitle = "Ödeme uygulaması",
                leading = { AppIcon(link.packageName, 34.dp) },
                chevron = true, onClick = { PaymentSwitcher.openPaymentSettings(activity) },
            )
            Separator(64.dp)
        }
        if (link != null && !env.notificationAccess) {
            GroupRow(
                "Ödeme onayını göster", subtitle = "Bildirim erişimi gerekli",
                titleColor = c.accent, onClick = { PaymentNotificationListener.openAccessSettings(activity) },
            )
            Separator()
        }
        GroupRow("Kartı düzenle", chevron = true, onClick = onEdit)
    }
}

@Composable
private fun ShowcaseDetail(activity: MainActivity, env: Environment, onEdit: () -> Unit) {
    val prefs by activity.app.container.prefs.state.collectAsState()
    val scope = rememberCoroutineScope()
    val ready = env.shizuku == ShizukuBridge.State.READY
    val paymentOff = ready && prefs.showcaseTurnsOffPayment
    if (paymentOff) {
        Status("Ödeme kapalı", "Bu kart seçiliyken telefon okuyucuya hiçbir ödeme uygulaması sunmuyor. Başka bir ödeme kartı seçince ödeme yeniden açılır.")
        Spacer(Modifier.height(16.dp))
        TintedButton("Ödemeyi geri aç", {
            scope.launch {
                val pkg = prefs.lastWalletPackage.ifBlank { null }
                    ?: activity.app.container.store.cards.value.firstNotNullOfOrNull { it.payment?.packageName }
                if (pkg != null && ShizukuBridge.setWalletHolder(pkg)) activity.resumeTick.intValue++
            }
        }, Modifier.fillMaxWidth())
    } else {
        Status("Vitrin kartı", "Bu kart bir ödeme yöntemine bağlı değil. Okuyucuya yaklaştırınca ödeme yapmaz.")
    }
    Spacer(Modifier.height(24.dp))
    Group { GroupRow("Kartı düzenle", chevron = true, onClick = onEdit) }
}

@Composable
private fun CodeDetail(activity: MainActivity, card: WaultCard, onEdit: () -> Unit) {
    val c = Wault.colors
    val hasCode = card.codeValue.isNotBlank() && card.codeFormat != null
    val drawable = hasCode && canEncode(card.codeValue, card.codeFormat!!)
    if (hasCode && !drawable) {
        Status("Kod çizilemiyor", "Bu değer ${card.codeFormat!!.label} için geçerli değil. Kartı düzenleyip biçimi değiştir.")
        Spacer(Modifier.height(24.dp))
    } else if (hasCode) {
        DisposableEffect(card.id) {
            activity.setMaxBrightness(true)
            onDispose { activity.setMaxBrightness(false) }
        }
        // Okuyucular için her zaman beyaz zeminde siyah kod
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BarcodeView(card.codeValue, card.codeFormat!!)
            Spacer(Modifier.height(10.dp))
            Text(card.codeValue, style = WaultType.footnote, color = Color(0xFF3A3A3C))
        }
        Spacer(Modifier.height(8.dp))
        Text("Ekran parlaklığı kod görünürken en yüksekte.", style = WaultType.footnote, color = c.secondary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
    } else {
        Status("Bu kartta kod yok", "Kartı düzenleyip barkodunu tarayabilirsin.")
        Spacer(Modifier.height(24.dp))
    }
    Group {
        if (card.memberNo.isNotBlank()) { GroupRow("Üye numarası", value = card.memberNo); Separator() }
        if (card.points.isNotBlank()) { GroupRow("Puan", value = card.points); Separator() }
        if (card.note.isNotBlank()) { GroupRow("Not", subtitle = card.note); Separator() }
        GroupRow("Kartı düzenle", chevron = true, onClick = onEdit)
    }
}

@Composable
private fun NfcDetail(activity: MainActivity, card: WaultCard, env: Environment, tint: Color, onEdit: () -> Unit) {
    val c = Wault.colors
    val nfc = card.nfc ?: return
    val emulates = nfc.capability == NfcCapability.EMULATE && nfc.ndefHex != null
    var writing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WriteResult?>(null) }

    if (emulates && !writing) {
        DisposableEffect(card.id) {
            EmulationState.ndef = runCatching { nfc.ndefHex!!.hexToBytes() }.getOrNull()
            activity.nfc.setPreferWault(true)
            onDispose {
                EmulationState.ndef = null
                activity.nfc.setPreferWault(false)
            }
        }
    }
    if (writing) {
        DisposableEffect(Unit) {
            val bytes = nfc.ndefHex!!.hexToBytes()
            activity.nfc.startReading { tag ->
                val r = NdefWriter.write(tag, bytes)
                activity.runOnUiThread { result = r; writing = false }
            }
            onDispose { activity.nfc.stopReading() }
        }
    }

    when {
        !env.nfcAvailable -> Status("Bu cihazda NFC yok", "Kopya kartı kullanmak için NFC'li bir telefon gerekiyor.")
        writing -> Status("Boş etiketi telefona yaklaştır", "Yazma bitene kadar sabit tut.") {
            GlyphIcon(Glyph.NFC_PHONE, c.accent, size = 44.dp)
        }
        emulates -> Status("Telefonu okuyucuya yaklaştır", "Kart açık kaldıkça okuyucu telefonu bu kart olarak görür.") { ReadyPulse(tint = tint) }
        else -> Status("Sadece kayıt", "Bu kartın okuyucusu kart kimliğine ya da şifreli bölümlere bakıyor. Telefon bunu taklit edemez.")
    }
    result?.let {
        Spacer(Modifier.height(10.dp))
        Text(
            when (it) { WriteResult.Written -> "Etikete yazıldı."; is WriteResult.Failed -> it.reason },
            style = WaultType.subheadMedium,
            color = if (it is WriteResult.Written) c.label else c.danger,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(24.dp))
    Group {
        GroupRow("Tür", value = TagAnalyzer.techLabel(nfc.techs))
        Separator()
        GroupRow("Kart kimliği", value = nfc.uidHex.chunked(2).joinToString(" "))
        Separator()
        if (emulates && env.nfcAvailable) {
            GroupRow(if (writing) "Yazmayı durdur" else "Boş etikete yaz", titleColor = c.accent,
                onClick = { result = null; writing = !writing })
            Separator()
        }
        GroupRow("Kartı düzenle", chevron = true, onClick = onEdit)
    }
}
