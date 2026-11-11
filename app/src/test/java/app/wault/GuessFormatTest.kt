package app.wault

import app.wault.data.CodeFormat
import app.wault.ui.edit.guessFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class GuessFormatTest {
    @Test fun validEanBecomesEan13() = assertEquals(CodeFormat.EAN_13, guessFormat("8690000000012"))
    @Test fun shortMemberNumberBecomesCode128() = assertEquals(CodeFormat.CODE_128, guessFormat("530011827741"))
    @Test fun longOrNonAsciiTextBecomesQr() {
        assertEquals(CodeFormat.QR, guessFormat("https://ornek.com/bilet/4412-S4-K12-çok-uzun-bir-bağlantı"))
        assertEquals(CodeFormat.QR, guessFormat("Çay"))
    }
}
