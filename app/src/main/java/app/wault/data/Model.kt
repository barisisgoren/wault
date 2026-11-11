package app.wault.data

import java.util.UUID

enum class FaceSource { THEME, CATALOG, CUSTOM }

enum class CardKind { PAYMENT, SHOWCASE, LOYALTY, CODE, NFC_COPY }

/** Kartın üzerindeki ödeme ağı rozeti. */
enum class CardNetwork(val label: String) {
    NONE(""),
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    TROY("Troy"),
    AMEX("Amex"),
}

enum class CodeFormat(val label: String) {
    QR("QR"),
    CODE_128("Code 128"),
    EAN_13("EAN-13"),
    PDF_417("PDF417"),
    AZTEC("Aztec"),
}

/**
 * Kopyalanan bir NFC kartının telefonda ne yapabileceği.
 * EMULATE: NDEF içeriği Type 4 etiketi olarak sunulabilir.
 * RECORD_ONLY: okuyucu kart kimliğine/şifreli sektöre bakıyor; telefon taklit edemez.
 */
enum class NfcCapability { EMULATE, RECORD_ONLY }

data class PaymentLink(
    val packageName: String,
    val serviceClass: String,
    val appLabel: String,
)

data class NfcCopy(
    val uidHex: String,
    val techs: List<String>,
    val ndefHex: String?,
    val capability: NfcCapability,
)

data class WaultCard(
    val id: String = UUID.randomUUID().toString(),
    val kind: CardKind,
    val title: String,
    val subtitle: String = "",
    val artId: String = "midnight",
    val faceSource: FaceSource = FaceSource.THEME,
    val faceRef: String? = null,
    // ödeme / vitrin
    val network: CardNetwork = CardNetwork.NONE,
    val last4: String = "",
    val holder: String = "",
    val expiry: String = "",
    val payment: PaymentLink? = null,
    // sadakat ve kod
    val memberNo: String = "",
    val points: String = "",
    val note: String = "",
    val codeFormat: CodeFormat? = null,
    val codeValue: String = "",
    // nfc kopya
    val nfc: NfcCopy? = null,
)
