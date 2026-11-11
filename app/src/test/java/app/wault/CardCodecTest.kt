package app.wault

import app.wault.data.CardCodec
import app.wault.data.CardKind
import app.wault.data.CardNetwork
import app.wault.data.CodeFormat
import app.wault.data.DemoCards
import app.wault.data.FaceSource
import app.wault.data.NfcCapability
import app.wault.data.NfcCopy
import app.wault.data.PaymentLink
import app.wault.data.WaultCard
import app.wault.ui.code.canEncode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardCodecTest {
    private val cards = listOf(
        WaultCard(
            kind = CardKind.PAYMENT, title = "Garanti", subtitle = "Bonus",
            artId = "forest", network = CardNetwork.VISA, last4 = "4417", holder = "A KAYA", expiry = "09/29",
            payment = PaymentLink("com.garanti.cepsubesi", "x.HCEService", "Garanti"),
        ),
        WaultCard(kind = CardKind.PAYMENT, title = "Ziraat", artId = "auto", payment = PaymentLink("com.ziraat.ziraatmobil", "x.Svc", "Ziraat Mobil")),
        WaultCard(kind = CardKind.SHOWCASE, title = "Metal", artId = "midnight", network = CardNetwork.MASTERCARD, last4 = "0001"),
        WaultCard(kind = CardKind.LOYALTY, title = "Migros", artId = "gold", memberNo = "530011", points = "1.240", codeFormat = CodeFormat.EAN_13, codeValue = "8690000012345"),
        WaultCard(kind = CardKind.CODE, title = "Spor", artId = "ocean", codeFormat = CodeFormat.QR, codeValue = "GYM-22841"),
        WaultCard(kind = CardKind.NFC_COPY, title = "Kapı", artId = "graphite", nfc = NfcCopy("04A2193B", listOf("NfcA", "MifareClassic"), null, NfcCapability.RECORD_ONLY)),
        WaultCard(kind = CardKind.PAYMENT, title = "Foto", faceSource = FaceSource.CUSTOM, faceRef = "0b7e6f4a-1c22-4f3a-9d1e-2a7f5c1d9e10.bin"),
        WaultCard(kind = CardKind.PAYMENT, title = "Model", faceSource = FaceSource.CATALOG, faceRef = "world"),
    )

    @Test fun roundTripKeepsEverything() {
        assertEquals(cards, CardCodec.decode(CardCodec.encode(cards)))
    }

    @Test fun oldFilesWithoutFaceFieldsStillOpen() {
        // faceSource/faceRef eklenmeden önce yazılmış kayıt: tema olarak açılmalı
        val json = """{"v":2,"cards":[{"id":"a","kind":"LOYALTY","title":"Eski","artId":"ocean","memberNo":"1"}]}"""
        val card = CardCodec.decode(json).single()
        assertEquals(FaceSource.THEME, card.faceSource)
        assertEquals(null, card.faceRef)
        assertEquals("ocean", card.artId)
    }

    @Test fun brokenCardIsSkippedNotFatal() {
        val json = """{"v":3,"cards":[{"id":"a","kind":"NOPE","title":"x"},{"id":"b","kind":"CODE","title":"ok"}]}"""
        assertEquals(listOf("b"), CardCodec.decode(json).map { it.id })
    }

    @Test fun demoCardsHaveScannableCodesAndRequestedCount() {
        DemoCards.all().filter { it.codeValue.isNotBlank() }.forEach {
            assertTrue("${it.title} kodu çizilebilmeli", canEncode(it.codeValue, requireNotNull(it.codeFormat)))
        }
        assertEquals(0, DemoCards.all(0).size)
        assertEquals(3, DemoCards.all(3).size)
        assertEquals(15, DemoCards.all(15).size)
        assertEquals(15, DemoCards.all(15).map { it.id }.toSet().size)
    }
}
