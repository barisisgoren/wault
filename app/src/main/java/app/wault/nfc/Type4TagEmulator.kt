package app.wault.nfc

/**
 * NFC Forum Type 4 Tag (salt okunur) APDU durum makinesi.
 * Kopyalanan bir NDEF kartının içeriğini telefondan sunmak için kullanılır.
 * Android'den bağımsız; birim testleri doğrudan bu sınıfı sürer.
 */
class Type4TagEmulator(private val ndefMessage: () -> ByteArray?) {

    private enum class File { NONE, CC, NDEF }

    private var appSelected = false
    private var file = File.NONE

    fun reset() {
        appSelected = false
        file = File.NONE
    }

    fun process(apdu: ByteArray): ByteArray {
        if (apdu.size < 4) return SW_WRONG_LENGTH
        val ins = apdu[1].toInt() and 0xFF
        val p1 = apdu[2].toInt() and 0xFF
        val p2 = apdu[3].toInt() and 0xFF
        return when (ins) {
            0xA4 -> select(apdu, p1, p2)
            0xB0 -> readBinary(apdu, (p1 shl 8) or p2)
            else -> SW_INS_NOT_SUPPORTED
        }
    }

    private fun select(apdu: ByteArray, p1: Int, p2: Int): ByteArray {
        if (apdu.size < 5) return SW_WRONG_LENGTH
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return SW_WRONG_LENGTH
        val data = apdu.copyOfRange(5, 5 + lc)
        return when {
            p1 == 0x04 -> {
                if (data.contentEquals(NDEF_AID) && ndefMessage() != null) {
                    appSelected = true
                    file = File.NONE
                    SW_OK
                } else {
                    reset()
                    SW_NOT_FOUND
                }
            }
            p1 == 0x00 && p2 == 0x0C && appSelected -> when {
                data.contentEquals(CC_FILE_ID) -> { file = File.CC; SW_OK }
                data.contentEquals(NDEF_FILE_ID) -> { file = File.NDEF; SW_OK }
                else -> SW_NOT_FOUND
            }
            else -> SW_NOT_FOUND
        }
    }

    private fun readBinary(apdu: ByteArray, offset: Int): ByteArray {
        val content = when (file) {
            File.CC -> capabilityContainer()
            File.NDEF -> ndefFile() ?: return SW_NOT_FOUND
            File.NONE -> return SW_NOT_FOUND
        }
        val le = if (apdu.size >= 5) (apdu[4].toInt() and 0xFF).let { if (it == 0) 256 else it } else 256
        if (offset > content.size) return SW_WRONG_P1P2
        val end = minOf(content.size, offset + le)
        return content.copyOfRange(offset, end) + SW_OK
    }

    private fun ndefFile(): ByteArray? {
        val msg = ndefMessage() ?: return null
        return byteArrayOf((msg.size shr 8).toByte(), msg.size.toByte()) + msg
    }

    private fun capabilityContainer(): ByteArray {
        val max = (ndefMessage()?.size ?: 0) + 2
        return byteArrayOf(
            0x00, 0x0F,             // CCLEN
            0x20,                   // eşleme sürümü 2.0
            0x00, 0x3B,             // MLe
            0x00, 0x34,             // MLc
            0x04, 0x06,             // NDEF dosya kontrol TLV
            0xE1.toByte(), 0x04,    // dosya kimliği
            (max shr 8).toByte(), max.toByte(),
            0x00,                   // okuma serbest
            0xFF.toByte(),          // yazma yok
        )
    }

    companion object {
        val NDEF_AID = "D2760000850101".hexToBytes()
        val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
        val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)
        val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        val SW_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        val SW_WRONG_P1P2 = byteArrayOf(0x6B, 0x00)
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
    }
}
