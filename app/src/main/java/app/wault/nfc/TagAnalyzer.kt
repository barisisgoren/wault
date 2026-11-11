package app.wault.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import app.wault.data.NfcCapability

/** Okunan bir kartın özeti. */
data class TagReading(
    val uidHex: String,
    val techs: List<String>,
    val ndef: ByteArray?,
    val isPaymentCard: Boolean,
)

sealed interface TagVerdict {
    /** Banka kartı: kopyalanmaz. */
    data object PaymentCardRefused : TagVerdict
    data class Copyable(val capability: NfcCapability, val kindLabel: String, val explanation: String) : TagVerdict
}

object TagAnalyzer {

    fun techLabel(techs: List<String>): String = when {
        "MifareClassic" in techs -> "MIFARE Classic"
        "MifareUltralight" in techs -> "MIFARE Ultralight"
        "IsoDep" in techs -> "ISO 14443-4"
        "NfcF" in techs -> "FeliCa"
        "NfcV" in techs -> "ISO 15693"
        "NfcB" in techs -> "NFC-B"
        "NfcA" in techs -> "NFC-A"
        else -> "NFC etiketi"
    }

    /** Saf sınıflandırma: Android'e dokunmaz, test edilir. */
    fun classify(reading: TagReading): TagVerdict {
        if (reading.isPaymentCard) return TagVerdict.PaymentCardRefused
        val label = techLabel(reading.techs)
        val ndef = reading.ndef
        return if (ndef != null && ndef.isNotEmpty()) {
            TagVerdict.Copyable(
                NfcCapability.EMULATE, label,
                "Bu kart NDEF verisi taşıyor. Wault onu telefondan sunabilir ve boş bir etikete yazabilir.",
            )
        } else {
            TagVerdict.Copyable(
                NfcCapability.RECORD_ONLY, label,
                "Bu kartın okuyucusu kart kimliğine ya da şifreli bölümlere bakıyor. Telefon bunu taklit edemez; kart bilgisi sadece kayıt olarak saklanır.",
            )
        }
    }

    /** Okuma: reader mode geri çağrısında, arka plan iş parçacığında çalışır. */
    fun read(tag: Tag): TagReading {
        val techs = tag.techList.map { it.substringAfterLast('.') }
        var ndefBytes: ByteArray? = null
        Ndef.get(tag)?.let { ndef ->
            runCatching {
                ndef.connect()
                val msg: NdefMessage? = ndef.ndefMessage ?: ndef.cachedNdefMessage
                ndefBytes = msg?.toByteArray()
            }
            runCatching { ndef.close() }
        }
        var payment = false
        if (ndefBytes == null) {
            IsoDep.get(tag)?.let { iso ->
                runCatching {
                    iso.connect()
                    iso.timeout = 1500
                    val resp = iso.transceive(SELECT_PPSE)
                    payment = resp.size >= 2 && resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()
                }
                runCatching { iso.close() }
            }
        }
        return TagReading(tag.id.toHex(), techs, ndefBytes, payment)
    }

    /** "2PAY.SYS.DDF01" seçimi; yanıt veren kart temassız banka kartıdır. */
    private val SELECT_PPSE = "00A404000E325041592E5359532E444446303100".hexToBytes()
}

sealed interface WriteResult {
    data object Written : WriteResult
    data class Failed(val reason: String) : WriteResult
}

object NdefWriter {
    fun write(tag: Tag, message: ByteArray): WriteResult {
        val msg = runCatching { NdefMessage(message) }.getOrElse {
            return WriteResult.Failed("Kayıtlı veri bozuk, bu kart yazılamıyor.")
        }
        Ndef.get(tag)?.let { ndef ->
            return try {
                ndef.connect()
                when {
                    !ndef.isWritable -> WriteResult.Failed("Bu etiket kilitli. Yazılabilir boş bir etiket kullan.")
                    ndef.maxSize < message.size -> WriteResult.Failed("Etiket küçük: ${ndef.maxSize} bayt yer var, ${message.size} bayt gerekiyor.")
                    else -> { ndef.writeNdefMessage(msg); WriteResult.Written }
                }
            } catch (e: Exception) {
                WriteResult.Failed("Etiket yazma sırasında uzaklaştı. Telefonu sabit tutup tekrar dene.")
            } finally {
                runCatching { ndef.close() }
            }
        }
        NdefFormatable.get(tag)?.let { f ->
            return try {
                f.connect(); f.format(msg); WriteResult.Written
            } catch (e: Exception) {
                WriteResult.Failed("Etiket biçimlendirilemedi. Başka bir boş etiket dene.")
            } finally {
                runCatching { f.close() }
            }
        }
        return WriteResult.Failed("Bu etiket NDEF desteklemiyor.")
    }
}
