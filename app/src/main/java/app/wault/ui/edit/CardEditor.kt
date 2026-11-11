package app.wault.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.wault.design.GlyphIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.data.CardNetwork
import app.wault.data.CodeFormat
import app.wault.data.NfcCapability
import app.wault.data.WaultCard
import app.wault.design.BarButton
import app.wault.design.CardArtSwatch
import app.wault.design.CardArts
import app.wault.design.CardSurface
import app.wault.design.CheckMark
import app.wault.design.FieldRow
import app.wault.design.Glyph
import app.wault.design.Group
import app.wault.design.GroupRow
import app.wault.design.IconTile
import app.wault.design.NavBar
import app.wault.design.Segmented
import app.wault.design.Separator
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.nfc.PaymentServices
import app.wault.nfc.TagAnalyzer
import app.wault.ui.add.ScannerOverlay
import app.wault.ui.add.scanImage
import app.wault.ui.code.canEncode
import app.wault.ui.code.codeFormatHint
import app.wault.ui.common.AppIcon
import app.wault.ui.onboarding.shortBankName
import kotlinx.coroutines.launch

fun CardKind.label() = when (this) {
    CardKind.PAYMENT -> "Ödeme kartı"
    CardKind.SHOWCASE -> "Vitrin kartı"
    CardKind.LOYALTY -> "Sadakat kartı"
    CardKind.CODE -> "QR veya barkod"
    CardKind.NFC_COPY -> "NFC kopya"
}

/** Kart ekleme ve düzenleme: üstte canlı önizleme, altında gruplu form. */
@Composable
fun CardEditor(
    activity: MainActivity,
    initial: WaultCard,
    isNew: Boolean,
    onCancel: () -> Unit,
    onSave: (WaultCard) -> Unit,
    onDelete: (String) -> Unit,
) {
    val c = Wault.colors
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var tried by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val last4Ok = draft.last4.isEmpty() || Regex("\\d{4}").matches(draft.last4)
    val codeFormat = draft.codeFormat ?: CodeFormat.QR
    val codeFilled = draft.codeValue.isNotBlank()
    val codeDrawable = !codeFilled || canEncode(draft.codeValue, codeFormat)
    val codeOk = codeDrawable && !(draft.kind == CardKind.CODE && !codeFilled)
    val valid = draft.title.isNotBlank() && last4Ok && codeOk
    val save = { if (valid) onSave(draft.copy(title = draft.title.trim())) else tried = true }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            NavBar(
                if (isNew) draft.kind.label() else "Kartı düzenle",
                leading = { BarButton("Vazgeç", onCancel) },
                trailing = { BarButton(if (isNew) "Ekle" else "Kaydet", save, bold = true) },
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.padding(horizontal = 12.dp)) {
                    CardSurface(draft.copy(title = draft.title.ifBlank { "Kart adı" }), isDefault = true, elevation = 14.dp)
                }
                Spacer(Modifier.height(28.dp))

                if (draft.kind == CardKind.PAYMENT) {
                    PaymentApps(draft) { draft = it }
                    Spacer(Modifier.height(28.dp))
                }

                Group(
                    header = "Kart",
                    footer = when {
                        tried && draft.title.isBlank() -> "Kart adını yaz; kart cüzdanda bu adla görünür."
                        tried && !last4Ok -> "Son dört hane 4 rakam olmalı."
                        draft.kind == CardKind.SHOWCASE -> "Vitrin kartı sadece görünüştür; okuyucuya yaklaştırınca ödeme yapmaz."
                        draft.kind == CardKind.PAYMENT -> "Wault kart numarası, güvenlik kodu ya da ödeme anahtarı saklamaz."
                        else -> null
                    },
                ) {
                    FieldRow("Kart adı", draft.title, { draft = draft.copy(title = it) },
                        placeholder = placeholderFor(draft.kind), caps = KeyboardCapitalization.Words,
                        error = tried && draft.title.isBlank())
                    Separator()
                    FieldRow("Alt satır", draft.subtitle, { draft = draft.copy(subtitle = it) }, placeholder = "İsteğe bağlı")
                    when (draft.kind) {
                        CardKind.PAYMENT, CardKind.SHOWCASE -> {
                            Separator()
                            FieldRow("Son dört hane", draft.last4, { s -> draft = draft.copy(last4 = s.filter(Char::isDigit).take(4)) },
                                placeholder = "4417", keyboard = KeyboardType.NumberPassword, error = tried && !last4Ok)
                            Separator()
                            FieldRow("Son kullanma", draft.expiry, { s -> draft = draft.copy(expiry = formatExpiry(s)) },
                                placeholder = "AA/YY", keyboard = KeyboardType.Number)
                            Separator()
                            FieldRow("Kart sahibi", draft.holder, { draft = draft.copy(holder = it) },
                                placeholder = "İsteğe bağlı", caps = KeyboardCapitalization.Characters)
                        }
                        CardKind.LOYALTY -> {
                            Separator()
                            FieldRow("Üye numarası", draft.memberNo, { draft = draft.copy(memberNo = it) }, placeholder = "5300 1182 7741", keyboard = KeyboardType.Number)
                            Separator()
                            FieldRow("Puan", draft.points, { draft = draft.copy(points = it) }, placeholder = "1.240")
                            Separator()
                            FieldRow("Not", draft.note, { draft = draft.copy(note = it) }, placeholder = "İsteğe bağlı")
                        }
                        CardKind.CODE -> {
                            Separator()
                            FieldRow("Not", draft.note, { draft = draft.copy(note = it) }, placeholder = "Salon 4, koltuk 12")
                        }
                        CardKind.NFC_COPY -> Unit
                    }
                }

                if (draft.kind == CardKind.PAYMENT || draft.kind == CardKind.SHOWCASE) {
                    Spacer(Modifier.height(28.dp))
                    Group(header = "Ağ") {
                        Box(Modifier.padding(10.dp)) {
                            Segmented(
                                CardNetwork.entries, draft.network,
                                { if (it == CardNetwork.NONE) "Yok" else if (it == CardNetwork.MASTERCARD) "MC" else it.label },
                                { draft = draft.copy(network = it) },
                            )
                        }
                    }
                }

                if (draft.kind == CardKind.LOYALTY || draft.kind == CardKind.CODE) {
                    Spacer(Modifier.height(28.dp))
                    CodeGroup(activity, draft, { draft = it }, required = draft.kind == CardKind.CODE, showError = tried && !codeOk,
                        onScan = { scanning = true })
                }

                if (draft.kind == CardKind.NFC_COPY) draft.nfc?.let { nfc ->
                    Spacer(Modifier.height(28.dp))
                    Group(
                        header = "Okunan kart",
                        footer = if (nfc.capability == NfcCapability.EMULATE) "Telefon bu kartın verisini sunabilir ve boş etikete yazabilir."
                        else "Telefon bu kartı taklit edemez; sadece kayıt olarak saklanır.",
                    ) {
                        GroupRow("Tür", value = TagAnalyzer.techLabel(nfc.techs))
                        Separator()
                        GroupRow("Kart kimliği", value = nfc.uidHex.chunked(2).joinToString(" "))
                    }
                }

                Spacer(Modifier.height(28.dp))
                AppearancePicker(draft) { draft = it }

                if (!isNew) {
                    Spacer(Modifier.height(28.dp))
                    Group {
                        GroupRow(
                            if (confirmDelete) "Silmeyi onayla" else "Kartı sil",
                            titleColor = c.danger,
                            onClick = { if (confirmDelete) onDelete(draft.id) else confirmDelete = true },
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }

        if (scanning) {
            ScannerOverlay(
                onResult = { format, value -> draft = draft.copy(codeFormat = format, codeValue = value); scanning = false },
                onCancel = { scanning = false },
            )
        }
    }
}

private fun placeholderFor(kind: CardKind) = when (kind) {
    CardKind.PAYMENT, CardKind.SHOWCASE -> "Garanti"
    CardKind.LOYALTY -> "Migros"
    CardKind.CODE -> "Sinema bileti"
    CardKind.NFC_COPY -> "Ofis kapısı"
}

@Composable
private fun PaymentApps(draft: WaultCard, update: (WaultCard) -> Unit) {
    val context = LocalContext.current
    val services = remember { PaymentServices.list(context) }
    val c = Wault.colors
    Group(
        header = "Ödeme uygulaması",
        footer = if (services.isEmpty()) "Bu cihazda NFC ödeme uygulaması yok. Bankanın uygulamasında temassız ödemeyi açınca burada görünür."
        else "Karta dokununca telefonun NFC ödeme yöntemi bu uygulamaya geçer.",
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(services, key = { it.component.flattenToShortString() }) { s ->
                val on = draft.payment?.packageName == s.component.packageName && draft.payment.serviceClass == s.component.className
                Column(
                    Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .selectable(on, role = Role.RadioButton) {
                            // Başlık uygulamadan otomatik gelmişse yeni uygulamayı izler; kullanıcı yazdıysa dokunulmaz
                            val autoTitle = draft.title.isBlank() || draft.title == draft.payment?.let { shortBankName(it.appLabel) }
                            update(
                                draft.copy(
                                    payment = s.toLink(),
                                    title = if (autoTitle) shortBankName(s.appLabel) else draft.title,
                                    artId = if (autoTitle || draft.artId == "auto") "auto" else draft.artId,
                                )
                            )
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .border(if (on) 2.dp else 0.dp, if (on) c.accent else Color.Transparent, RoundedCornerShape(16.dp))
                            .padding(3.dp)
                    ) {
                        AppIcon(s.component.packageName, 52.dp)
                        if (on) {
                            Box(
                                Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(20.dp).clip(CircleShape).background(c.accent),
                                contentAlignment = Alignment.Center,
                            ) { GlyphIcon(Glyph.CHECK, Color.White, size = 13.dp) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.appLabel, style = WaultType.caption, color = if (on) c.label else c.secondary,
                        maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeGroup(
    activity: MainActivity,
    draft: WaultCard,
    update: (WaultCard) -> Unit,
    required: Boolean,
    showError: Boolean,
    onScan: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var galleryMessage by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val found = scanImage(activity, uri)
            if (found == null) galleryMessage = "Bu görselde kod bulunamadı. Kodu daha yakından gösteren bir görsel seç."
            else { galleryMessage = null; update(draft.copy(codeFormat = found.first, codeValue = found.second)) }
        }
    }
    Group(
        header = if (required) "Kod" else "Barkod",
        footer = galleryMessage ?: when {
            draft.codeValue.isNotBlank() && !canEncode(draft.codeValue, draft.codeFormat ?: CodeFormat.QR) ->
                codeFormatHint(draft.codeFormat ?: CodeFormat.QR)
            showError -> "Kodu tara, görselden seç ya da elle yaz."
            else -> null
        },
    ) {
        GroupRow("Kamerayla tara", leading = { IconTile(Glyph.CAMERA, Color(0xFF0A84FF)) }, chevron = true, onClick = onScan)
        Separator(60.dp)
        GroupRow("Görselden seç", leading = { IconTile(Glyph.IMAGE, Color(0xFF30B158)) }, chevron = true,
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
        Separator()
        FieldRow("Kod", draft.codeValue,
            { update(draft.copy(codeValue = it, codeFormat = draft.codeFormat ?: CodeFormat.QR)) },
            placeholder = if (required) "Gerekli" else "İsteğe bağlı", caps = KeyboardCapitalization.None,
            error = showError || (draft.codeValue.isNotBlank() && !canEncode(draft.codeValue, draft.codeFormat ?: CodeFormat.QR)))
        Separator()
        Box(Modifier.padding(10.dp)) {
            Segmented(
                CodeFormat.entries, draft.codeFormat ?: CodeFormat.QR,
                { when (it) { CodeFormat.CODE_128 -> "128"; CodeFormat.EAN_13 -> "EAN"; else -> it.label } },
                { update(draft.copy(codeFormat = it)) },
            )
        }
    }
}

private fun formatExpiry(input: String): String {
    val d = input.filter { it.isDigit() }.take(4)
    return if (d.length > 2) d.substring(0, 2) + "/" + d.substring(2) else d
}

