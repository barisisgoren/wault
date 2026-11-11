package app.wault

import app.wault.nfc.Type4TagEmulator
import app.wault.nfc.hexToBytes
import app.wault.nfc.toHex
import org.junit.Assert.assertEquals
import org.junit.Test

class Type4TagEmulatorTest {
    // "wault" metinli tek kayıtlı NDEF mesajı
    private val ndef = "D1010854026577617574".hexToBytes()
    private val emu = Type4TagEmulator { ndef }

    private fun send(hex: String) = emu.process(hex.hexToBytes()).toHex()

    @Test fun fullReadFlow() {
        assertEquals("9000", send("00A4040007D276000085010100"))
        assertEquals("9000", send("00A4000C02E103"))
        val cc = send("00B000000F")
        assertEquals("000F20003B00340406E104000CFF00" .substring(0, 22), cc.substring(0, 22))
        assertEquals("9000", cc.takeLast(4))
        assertEquals("9000", send("00A4000C02E104"))
        assertEquals("000A9000", send("00B0000002"))
        assertEquals(ndef.toHex() + "9000", send("00B000020A"))
    }

    @Test fun fileSelectBeforeAppIsRejected() = assertEquals("6A82", send("00A4000C02E103"))

    @Test fun unknownAidRejected() = assertEquals("6A82", send("00A4040007A000000003101000"))

    @Test fun nothingToServeRejectsAid() {
        val empty = Type4TagEmulator { null }
        assertEquals("6A82", empty.process("00A4040007D276000085010100".hexToBytes()).toHex())
    }

    @Test fun unsupportedInstruction() = assertEquals("6D00", send("00D6000001AA"))
}
