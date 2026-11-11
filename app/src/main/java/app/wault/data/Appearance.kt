package app.wault.data

/** Kartın yüzeyi: tek renk ya da renk geçişi. */
enum class CardStyle(val label: String) {
    FLAT("Düz"),
    GRADIENT("Gradyan"),
}

/** Kart üstündeki yazının rengi. AUTO: kart rengine göre beyaz ya da siyah seçilir. */
enum class InkMode(val label: String) {
    AUTO("Otomatik"),
    LIGHT("Beyaz"),
    DARK("Siyah"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem"),
    LIGHT("Açık"),
    DARK("Koyu"),
}

/** Arayüzün vurgu rengi (düğmeler, bağlantılar, seçili öğeler). Açık ve koyu tema için ayrı ton. */
enum class AccentChoice(val label: String, val light: Long, val dark: Long) {
    BLUE("Mavi", 0xFF007AFF, 0xFF409CFF),
    INDIGO("Çivit", 0xFF5856D6, 0xFF7D7AFF),
    PURPLE("Mor", 0xFF9B4DCA, 0xFFBF5AF2),
    PINK("Pembe", 0xFFD6336C, 0xFFFF5C8D),
    RED("Kırmızı", 0xFFD93A3F, 0xFFFF6B6B),
    ORANGE("Turuncu", 0xFFD9650A, 0xFFFF9F0A),
    GREEN("Yeşil", 0xFF1F8F4E, 0xFF30D158),
    TEAL("Turkuaz", 0xFF0A8F94, 0xFF40C8C0),
}
