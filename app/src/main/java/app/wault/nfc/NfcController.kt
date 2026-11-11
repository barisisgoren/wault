package app.wault.nfc

import android.app.Activity
import android.content.ComponentName
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle

/**
 * Etkinliğe bağlı NFC kontrolü.
 * Okuyucu modu sadece kopyalama ve yazma ekranlarında açılır; açıkken telefon kart taklidi yapmaz,
 * bu yüzden ödeme ekranında asla açık bırakılmaz.
 */
class NfcController(private val activity: Activity) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private var readerCallback: ((Tag) -> Unit)? = null
    private var preferWault = false
    private var resumed = false

    val available: Boolean get() = adapter != null
    val enabled: Boolean get() = adapter?.isEnabled == true

    fun startReading(onTag: (Tag) -> Unit) {
        readerCallback = onTag
        if (resumed) enableReader()
    }

    fun stopReading() {
        readerCallback = null
        runCatching { adapter?.disableReaderMode(activity) }
    }

    /** Kopya kart seçiliyken Wault'un servisi diğer "other" servislerin önüne geçer. */
    fun setPreferWault(on: Boolean) {
        preferWault = on
        if (resumed) applyPreferred()
    }

    fun onResume() {
        resumed = true
        if (readerCallback != null) enableReader()
        applyPreferred()
    }

    fun onPause() {
        resumed = false
        runCatching { adapter?.disableReaderMode(activity) }
        val ce = emulation() ?: return
        runCatching { ce.unsetPreferredService(activity) }
    }

    private fun enableReader() {
        val a = adapter ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 300) }
        runCatching { a.enableReaderMode(activity, { tag -> readerCallback?.invoke(tag) }, flags, extras) }
    }

    private fun applyPreferred() {
        val ce = emulation() ?: return
        runCatching {
            if (preferWault) ce.setPreferredService(activity, ComponentName(activity, WaultApduService::class.java))
            else ce.unsetPreferredService(activity)
        }
    }

    private fun emulation(): CardEmulation? = adapter?.let { runCatching { CardEmulation.getInstance(it) }.getOrNull() }
}
