package app.wault.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.wault.data.CardKind
import app.wault.data.InkMode

/**
 * Kartın rengi. Varsayılan yüzey düz tek renktir (base); gradyan stilinde aynı renkten
 * türetilen açık ve koyu tonlar kullanılır, yani her renk iki stilde de çalışır.
 */
data class CardArt(
    val id: String,
    val name: String,
    val base: Color,
    /** true: beyaz yazı, false: koyu yazı. Varsayılan olarak zeminin parlaklığından seçilir. */
    val lightInk: Boolean = prefersLightInk(base),
) {
    /** Eski adı: koyu/orta tonlu kart, üstüne beyaz yazı. */
    val dark: Boolean get() = lightInk

    val ink: Color get() = if (lightInk) Color.White else Color(0xFF17181C)
    val inkSoft: Color get() = if (lightInk) Color.White.copy(alpha = 0.74f) else Color(0xFF17181C).copy(alpha = 0.62f)

    // Gradyan stili için türetilen tonlar
    val top: Color get() = lerp(base, Color.White, if (lightInk) 0.18f else 0.55f)
    val bottom: Color get() = lerp(base, Color.Black, if (lightInk) 0.32f else 0.10f)
    val glow: Color get() = base.toHsv().let { hsvColor(it.h + 28f, (it.s * 0.75f).coerceAtLeast(0.35f), 1f) }
    val stops: List<Color> get() = listOf(top, bottom)

    fun withInk(mode: InkMode): CardArt = when (mode) {
        InkMode.AUTO -> this
        InkMode.LIGHT -> copy(lightInk = true)
        InkMode.DARK -> copy(lightInk = false)
    }

    /**
     * Kartın renginden türeyen, arayüz zemini üzerinde okunur bir vurgu (halka, ışık).
     * Koyu temada kartın parlak tonu, açık temada daha koyu ve doygun tonu.
     */
    fun accentOn(darkTheme: Boolean): Color = when {
        darkTheme && lightInk -> lerp(base, Color.White, 0.25f)
        darkTheme -> base
        lightInk -> base
        else -> lerp(base, Color.Black, 0.3f)
    }
}

object CardArts {
    /** Ödeme uygulamasının ikonundan çıkarılan renk; gerçek değer fromIcon ile bulunur. */
    val auto = CardArt("auto", "Uygulama rengi", Color(0xFF536884))

    /** Kullanıcının seçtiği renk; gerçek değer kartın customColor alanındadır. */
    val custom = CardArt("custom", "Özel", Color(0xFF536884))

    /** Düz renk paleti. Kimlikler eski gradyan görsellerle aynı; kayıtlı kartlar bozulmaz. */
    val presets: List<CardArt> = listOf(
        CardArt("ink", "Siyah", Color(0xFF121316)),
        CardArt("midnight", "Gece", Color(0xFF262A33)),
        CardArt("graphite", "Grafit", Color(0xFF474D5A)),
        CardArt("silver", "Gümüş", Color(0xFFD6DAE1)),
        CardArt("ivory", "Fildişi", Color(0xFFF2EFE5)),
        CardArt("white", "Beyaz", Color(0xFFFFFFFF)),
        CardArt("rose", "Pudra", Color(0xFFF2C4CE)),
        CardArt("pink", "Pembe", Color(0xFFE2467C)),
        CardArt("red", "Kırmızı", Color(0xFFE4474D)),
        CardArt("crimson", "Bordo", Color(0xFFA3283A)),
        CardArt("sunset", "Mercan", Color(0xFFEC6740)),
        CardArt("gold", "Altın", Color(0xFFEDB93C)),
        CardArt("olive", "Zeytin", Color(0xFF7A8A3A)),
        CardArt("forest", "Orman", Color(0xFF2D8F66)),
        CardArt("teal", "Turkuaz", Color(0xFF119E9A)),
        CardArt("mint", "Nane", Color(0xFFA7E3C6)),
        CardArt("sky", "Gök", Color(0xFF2C96D8)),
        CardArt("ocean", "Okyanus", Color(0xFF2F6BE0)),
        CardArt("indigo", "Çivit", Color(0xFF4B46D4)),
        CardArt("plum", "Mürdüm", Color(0xFF7B4CCB)),
        CardArt("brown", "Kahve", Color(0xFF7A5648)),
    )

    val all: List<CardArt> = listOf(auto) + presets

    val default: CardArt get() = byId("midnight")

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
        val sat = (satSum[best] / weight[best]).coerceIn(0.55f, 0.92f)
        val value = (valSum[best] / weight[best]).coerceIn(0.5f, 0.92f)
        return CardArt("auto", "Uygulama rengi", hsvColor(hue, sat, value))
    }

    /** Yeni kartın başlangıç görseli: türüne göre canlı bir renk. */
    fun defaultFor(kind: CardKind): String = when (kind) {
        CardKind.PAYMENT -> "auto"
        CardKind.SHOWCASE -> "plum"
        CardKind.LOYALTY -> "sunset"
        CardKind.CODE -> "forest"
        CardKind.NFC_COPY -> "graphite"
    }

    fun byId(id: String?): CardArt = when (id) {
        "auto" -> auto
        "custom" -> custom
        else -> presets.firstOrNull { it.id == id } ?: presets.first { it.id == "midnight" }
    }
}
