package app.wault.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/** Şu an seçili kopya kartın NDEF içeriği. Kart seçimi kalkınca null olur, servis yanıt vermez. */
object EmulationState {
    @Volatile
    var ndef: ByteArray? = null
}

class WaultApduService : HostApduService() {
    private val emulator = Type4TagEmulator { EmulationState.ndef }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray =
        emulator.process(commandApdu)

    override fun onDeactivated(reason: Int) = emulator.reset()
}
