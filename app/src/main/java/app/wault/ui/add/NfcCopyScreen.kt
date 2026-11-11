package app.wault.ui.add

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.data.NfcCopy
import app.wault.data.WaultCard
import app.wault.design.BarButton
import app.wault.design.CardArts
import app.wault.design.NavBar
import app.wault.design.PrimaryButton
import app.wault.design.TintedButton
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.nfc.PaymentSwitcher
import app.wault.nfc.TagAnalyzer
import app.wault.nfc.TagReading
import app.wault.nfc.TagVerdict
import app.wault.nfc.toHex
import app.wault.ui.Environment

/** Bir NFC kartını okur, telefonun onunla ne yapabileceğini açıkça söyler, kopyayı düzenleyiciye verir. */
@Composable
fun NfcCopyScreen(
    activity: MainActivity,
    env: Environment,
    onCancel: () -> Unit,
    onCopied: (WaultCard) -> Unit,
    onAddPayment: () -> Unit,
) {
    val c = Wault.colors
    var reading by remember { mutableStateOf<TagReading?>(null) }
    var failed by remember { mutableStateOf(false) }

    if (env.nfcEnabled && reading == null) {
        DisposableEffect(Unit) {
            activity.nfc.startReading { tag ->
                val r = runCatching { TagAnalyzer.read(tag) }.getOrNull()
                activity.runOnUiThread { if (r == null) failed = true else reading = r }
            }
            onDispose { activity.nfc.stopReading() }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        NavBar("NFC kartı kopyala", leading = { BarButton("Vazgeç", onCancel) })
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val r = reading
            Spacer(Modifier.weight(0.5f))
            when {
                !env.nfcAvailable -> Message("Bu cihazda NFC yok", "Kart kopyalamak için NFC'li bir telefon gerekiyor.")
                !env.nfcEnabled -> {
                    Message("NFC kapalı", "Kartı okuyabilmek için NFC'yi aç, sonra buraya dön.")
                    Spacer(Modifier.height(28.dp))
                    PrimaryButton("NFC'yi aç", { PaymentSwitcher.openNfcSettings(activity) }, Modifier.fillMaxWidth())
                }
                r == null -> {
                    PhoneAndCard(Modifier.size(220.dp))
                    Spacer(Modifier.height(28.dp))
                    Message(
                        "Kartı telefonun arkasına yaklaştır",
                        if (failed) "Kart okunurken uzaklaştı. Tekrar yaklaştır ve sabit tut."
                        else "Okunana kadar sabit tut. Anten çoğu telefonda kameranın hemen altında.",
                    )
                }
                else -> when (val v = TagAnalyzer.classify(r)) {
                    TagVerdict.PaymentCardRefused -> {
                        Message(
                            "Bu bir banka kartı",
                            "Banka kartlarının ödeme anahtarları kartın çipinde kilitli ve kopyalanamaz. " +
                                "Bankanın uygulamasını Wault'a ödeme kartı olarak ekle.",
                        )
                        Spacer(Modifier.height(28.dp))
                        PrimaryButton("Ödeme kartı ekle", onAddPayment, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        TintedButton("Başka kart oku", { reading = null; failed = false }, Modifier.fillMaxWidth())
                    }
                    is TagVerdict.Copyable -> {
                        Message(v.kindLabel, v.explanation)
                        Spacer(Modifier.height(28.dp))
                        PrimaryButton("Devam", {
                            onCopied(
                                WaultCard(
                                    kind = CardKind.NFC_COPY, title = "", subtitle = v.kindLabel, artId = "graphite",
                                    nfc = NfcCopy(r.uidHex, r.techs, r.ndef?.toHex(), v.capability),
                                )
                            )
                        }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        TintedButton("Başka kart oku", { reading = null; failed = false }, Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Message(title: String, body: String) {
    val c = Wault.colors
    Text(title, style = WaultType.title, color = c.label, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(body, style = WaultType.callout, color = c.secondary, textAlign = TextAlign.Center)
}

/** Telefonun arkası ve ona yaslanmış bir kart: nereye tutulacağını gösteren sabit çizim. */
@Composable
private fun PhoneAndCard(modifier: Modifier) {
    val c = Wault.colors
    val art = CardArts.byId("ocean")
    Canvas(modifier) {
        val w = size.width
        val phoneSize = Size(w * 0.46f, w * 0.88f)
        val phoneTop = Offset(w * 0.16f, w * 0.06f)
        drawRoundRect(c.surface, phoneTop, phoneSize, CornerRadius(w * 0.07f))
        drawRoundRect(c.separator, phoneTop, phoneSize, CornerRadius(w * 0.07f), style = Stroke(w * 0.01f))
        drawRoundRect(c.surfaceAlt, phoneTop + Offset(w * 0.04f, w * 0.04f), Size(w * 0.15f, w * 0.2f), CornerRadius(w * 0.035f))
        val antenna = phoneTop + Offset(phoneSize.width / 2, w * 0.4f)
        drawCircle(c.accent.copy(alpha = 0.5f), w * 0.08f, antenna, style = Stroke(w * 0.008f))
        drawCircle(c.accent.copy(alpha = 0.25f), w * 0.13f, antenna, style = Stroke(w * 0.008f))
        rotate(-12f, pivot = Offset(w * 0.64f, w * 0.5f)) {
            val tl = Offset(w * 0.4f, w * 0.36f)
            val cs = Size(w * 0.54f, w * 0.34f)
            drawRoundRect(Brush.linearGradient(art.stops, tl, tl + Offset(cs.width, cs.height)), tl, cs, CornerRadius(w * 0.03f))
            drawRoundRect(art.ink.copy(alpha = 0.7f), tl + Offset(w * 0.05f, w * 0.24f), Size(w * 0.16f, w * 0.025f), CornerRadius(w * 0.01f))
        }
    }
}
