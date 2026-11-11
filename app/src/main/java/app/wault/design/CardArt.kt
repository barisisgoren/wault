package app.wault.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.wault.data.CardKind

/**
 * Kart yüzü: Apple Wallet gibi gerçek bir kart hissi veren renk geçişleri.
 * Her görsel iki durak renk + köşede ikinci bir ışık lekesi olarak çizilir (CardSurface).
 */
data class CardArt(
    val id: String,
    val name: String,
    val stops: List<Color>,
    /** true: koyu kart, üstüne açık metin. false: açık kart, koyu metin. */
    val dark: Boolean,
    /** ışık yansımasının tonu */
    val sheen: Color = Color.White,
    /** Köşede ikinci bir renk lekesi: düz gradyan yerine mesh gradyan hissi verir. */
    val glow: Color = stops.last(),
) {
    val ink: Color get() = if (dark) Color(0xFFFFFFFF) else Color(0xFF17181C)
    val inkSoft: Color get() = if (dark) Color(0xFFFFFFFF).copy(alpha = 0.74f) else Color(0xFF17181C).copy(alpha = 0.62f)

    /**
     * Kartın renginden türeyen, arayüz zemini üzerinde okunur bir vurgu (halka, ışık).
     * Koyu temada kartın parlak tonu, açık temada daha koyu ve doygun tonu.
     */
    fun accentOn(darkTheme: Boolean): Color = when {
        darkTheme && dark -> lerp(stops.first(), Color.White, 0.3f)
        darkTheme -> stops.last()
        dark -> stops.first()
        else -> lerp(stops.last(), Color.Black, 0.28f)
    }
}

object CardArts {
    val all: List<CardArt> = listOf(
        CardArt("auto", "Uygulama rengi", listOf(Color(0xFF536884), Color(0xFF26374D)), true),
        CardArt("midnight", "Gece", listOf(Color(0xFF34373E), Color(0xFF14151A)), dark = true, glow = Color(0xFF3B5BA8)),
        CardArt("graphite", "Grafit", listOf(Color(0xFF565B66), Color(0xFF22252B)), dark = true, glow = Color(0xFF7E92C0)),
        CardArt("ocean", "Okyanus", listOf(Color(0xFF3478F6), Color(0xFF1B3CB0)), dark = true, glow = Color(0xFF8B5CFF)),
        CardArt("indigo", "Çivit", listOf(Color(0xFF5B52E6), Color(0xFF231C78)), dark = true, glow = Color(0xFF4FC3F7)),
        CardArt("teal", "Turkuaz", listOf(Color(0xFF14B3A0), Color(0xFF0A5568)), dark = true, glow = Color(0xFF3FE0A0)),
        CardArt("forest", "Orman", listOf(Color(0xFF2F9168), Color(0xFF123F2E)), dark = true, glow = Color(0xFFA6E36E)),
        CardArt("plum", "Mürdüm", listOf(Color(0xFF8250D0), Color(0xFF3B1F70)), dark = true, glow = Color(0xFFFF6FA5)),
        CardArt("sunset", "Gün batımı", listOf(Color(0xFFFF8A5B), Color(0xFFE8505B)), dark = true, glow = Color(0xFFFFC857)),
        CardArt("crimson", "Bordo", listOf(Color(0xFFC23A45), Color(0xFF62121F)), dark = true, glow = Color(0xFFFF7A45)),
        CardArt("gold", "Altın", listOf(Color(0xFFEFCB70), Color(0xFFC9962F)), dark = false, glow = Color(0xFFFFF0B8)),
        CardArt("rose", "Pudra", listOf(Color(0xFFF8D6DC), Color(0xFFE8A9BC)), dark = false, glow = Color(0xFFC9B8FF)),
        CardArt("silver", "Gümüş", listOf(Color(0xFFE9EBEF), Color(0xFFC3C7CF)), dark = false, glow = Color(0xFFB5D3FF)),
        CardArt("ivory", "Fildişi", listOf(Color(0xFFFBFAF6), Color(0xFFE7E4DA)), dark = false, glow = Color(0xFFFFE2B8)),
    )

    val default: CardArt get() = all.first { it.id == "midnight" }

    /** Aynı uygulama için ikon taraması bir kez yapılır. */
    private val iconCache = HashMap<String, CardArt>()

    /**
     * Uygulama ikonunun baskın rengini bulur ve kartı o markanın rengine boyar.
     * Doygun pikseller 24 renk dilimine ayrılır; en ağır dilim kazanır (ortalama değil: iki renkli ikon çamurlaşmasın).
     */
    fun fromIcon(context: Context, packageName: String?): CardArt {
        if (packageName == null) return default
        iconCache[packageName]?.let { return it }
        val art = runCatching { scanIcon(context, packageName) }.getOrNull() ?: default
        iconCache[packageName] = art
        return art
    }

    private fun scanIcon(context: Context, packageName: String): CardArt? {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        val buckets = 24
        val weight = FloatArray(buckets)
        val hueSum = FloatArray(buckets)
        val satSum = FloatArray(buckets)
        val valSum = FloatArray(buckets)
        val count = FloatArray(buckets)
        val hsv = FloatArray(3)
        for (y in 0 until size) for (x in 0 until size) {
            val px = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(px) < 200) continue
            android.graphics.Color.colorToHSV(px, hsv)
            if (hsv[1] < 0.30f || hsv[2] < 0.22f) continue
            val b = ((hsv[0] / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
            val w = hsv[1] * hsv[2]
            weight[b] += w; hueSum[b] += hsv[0] * w; satSum[b] += hsv[1] * w; valSum[b] += hsv[2] * w; count[b] += 1f
        }
        bitmap.recycle()
        // komşu dilimlerle yumuşat: bir rengin iki dilime bölünmesi onu yenmesin
        var best = -1
        var bestScore = 0f
        for (b in 0 until buckets) {
            val score = weight[b] + 0.5f * (weight[(b + 1) % buckets] + weight[(b + buckets - 1) % buckets])
            if (score > bestScore) { bestScore = score; best = b }
        }
        if (best < 0 || count[best] < 24f || weight[best] <= 0f) return null
        val hue = hueSum[best] / weight[best]
        val sat = (satSum[best] / weight[best]).coerceIn(0.52f, 0.92f)
        val value = valSum[best] / weight[best]

        fun hsv(h: Float, s: Float, v: Float) = Color(android.graphics.Color.HSVToColor(floatArrayOf((h + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

        // sarı-turuncu gibi açık markalar açık kart + koyu yazı olur
        val light = hue in 34f..68f && value > 0.7f
        return if (light) {
            val top = hsv(hue, sat * 0.62f, 1f)
            val bottom = hsv(hue - 4f, sat * 0.95f, 0.86f)
            CardArt("auto", "Uygulama rengi", listOf(top, bottom), dark = false, glow = hsv(hue + 26f, 0.35f, 1f))
        } else {
            val top = hsv(hue, sat * 0.92f, (value * 0.9f).coerceIn(0.62f, 0.86f))
            val bottom = hsv(hue + 8f, (sat * 1.05f), 0.36f)
            CardArt("auto", "Uygulama rengi", listOf(top, bottom), dark = true, glow = hsv(hue + 30f, 0.62f, 1f))
        }
    }

    /** Yeni kartın başlangıç görseli: türüne göre canlı bir renk. */
    fun defaultFor(kind: CardKind): String = when (kind) {
        CardKind.PAYMENT -> "auto"
        CardKind.SHOWCASE -> "plum"
        CardKind.LOYALTY -> "sunset"
        CardKind.CODE -> "forest"
        CardKind.NFC_COPY -> "graphite"
    }

    fun byId(id: String?): CardArt = all.firstOrNull { it.id == id } ?: default
}
