package app.wault

import app.wault.data.NfcCapability
import app.wault.nfc.TagAnalyzer
import app.wault.nfc.TagReading
import app.wault.nfc.TagVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagClassifyTest {
    @Test fun bankCardRefused() {
        val v = TagAnalyzer.classify(TagReading("0102", listOf("IsoDep", "NfcA"), null, isPaymentCard = true))
        assertTrue(v is TagVerdict.PaymentCardRefused)
    }

    @Test fun ndefTagIsEmulatable() {
        val v = TagAnalyzer.classify(TagReading("04AA", listOf("NfcA", "MifareUltralight", "Ndef"), byteArrayOf(1, 2), false))
        assertEquals(NfcCapability.EMULATE, (v as TagVerdict.Copyable).capability)
        assertEquals("MIFARE Ultralight", v.kindLabel)
    }

    @Test fun mifareClassicIsRecordOnly() {
        val v = TagAnalyzer.classify(TagReading("04A2193B", listOf("NfcA", "MifareClassic"), null, false))
        assertEquals(NfcCapability.RECORD_ONLY, (v as TagVerdict.Copyable).capability)
        assertEquals("MIFARE Classic", v.kindLabel)
    }
}
