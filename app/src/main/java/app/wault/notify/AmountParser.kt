package app.wault.notify

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Banka ve cüzdan bildirimlerinden harcama tutarını çıkarır.
 * Kampanya bildirimlerini ("500 TL'ye varan puan") ayırmak için bir harcama sözcüğü de arar.
 */
object AmountParser {

    private val spendWords = Regex(
        "harcama|ödeme|odeme|işlem|islem|alışveriş|alisveris|onaylandı|onaylandi|çekildi|cekildi|tahsil|payment|paid|purchase|spent|transaction",
        RegexOption.IGNORE_CASE,
    )

    private val noiseWords = Regex("varan|kazan|fırsat|firsat|kampanya|indirim|hediye", RegexOption.IGNORE_CASE)

    private const val NUM = """\d{1,3}(?:[.  ]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?"""
    private val prefixed = Regex("""(?:₺|\bTL|\bTRY)\s?($NUM)""")
    private val suffixed = Regex("""($NUM)\s?(?:₺|TL\b|TRY\b)""")

    data class Amount(val value: BigDecimal) {
        /** "₺1.245,90" */
        fun display(): String {
            val f = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")))
            return "₺" + f.format(value)
        }
    }

    fun parse(text: String): Amount? {
        if (!spendWords.containsMatchIn(text)) return null
        if (noiseWords.containsMatchIn(text) && !Regex("harcama|ödeme|payment|paid", RegexOption.IGNORE_CASE).containsMatchIn(text)) return null
        val raw = (prefixed.find(text) ?: suffixed.find(text))?.groupValues?.get(1) ?: return null
        return normalize(raw)?.takeIf { it.signum() > 0 }?.let(::Amount)
    }

    /** "1.245,90" / "1,245.90" / "245.90" / "245,9" / "1 245,90" -> BigDecimal */
    fun normalize(raw: String): BigDecimal? {
        val s = raw.replace(' ', ' ').replace(" ", "")
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        val decimalSep: Char? = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> if (s.length - lastComma - 1 in 1..2) ',' else null
            lastDot >= 0 -> if (s.length - lastDot - 1 in 1..2) '.' else null
            else -> null
        }
        val canonical = buildString {
            s.forEachIndexed { i, ch ->
                when {
                    ch.isDigit() -> append(ch)
                    decimalSep != null && ch == decimalSep && i == (if (ch == ',') lastComma else lastDot) -> append('.')
                }
            }
        }
        return canonical.toBigDecimalOrNull()
    }
}
