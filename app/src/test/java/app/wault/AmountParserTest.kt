package app.wault

import app.wault.notify.AmountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {
    private fun p(s: String) = AmountParser.parse(s)?.display()

    @Test fun turkishSuffix() = assertEquals("₺245,90", p("Kartınızla 245,90 TL harcama yapıldı."))
    @Test fun lira_sign_prefix() = assertEquals("₺1.245,90", p("₺1.245,90 tutarında ödeme onaylandı"))
    @Test fun thousandsNoDecimals() = assertEquals("₺12.500,00", p("12.500 TL tutarlı işleminiz gerçekleşti"))
    @Test fun googleWalletEnglish() = assertEquals("₺89,50", p("₺89.50 paid at Migros"))
    @Test fun tryCode() = assertEquals("₺30,00", p("TRY 30,00 alışveriş"))
    @Test fun campaignIgnored() = assertNull(p("500 TL'ye varan puan kazan! Kampanyayı kaçırma"))
    @Test fun noSpendWordIgnored() = assertNull(p("Hesabınıza 250 TL yatırıldı"))
    @Test fun normalizeVariants() {
        assertEquals("1245.90", AmountParser.normalize("1.245,90").toString())
        assertEquals("1245.90", AmountParser.normalize("1,245.90").toString())
        assertEquals("245.9", AmountParser.normalize("245,9").toString())
        assertEquals("1245", AmountParser.normalize("1.245").toString())
    }
}
