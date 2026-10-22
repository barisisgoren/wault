package com.baris.wault.ui.theme

import androidx.compose.ui.graphics.Color

// Pure Apple Wallet AMOLED & Apple Dark Gray Surfaces
val WaultAmoledBlack = Color(0xFF000000)
val WaultObsidian = Color(0xFF000000)
val WaultSurfaceDark = Color(0xFF1C1C1E)
val WaultSurfaceContainerDark = Color(0xFF2C2C2E)
val WaultGlassBorderDark = Color.White.copy(alpha = 0.08f)

// Apple HIG Action & Pill Surfaces
val WaultTopActionBg = Color(0xFF1C1C1E)
val WaultTopActionBorder = Color.White.copy(alpha = 0.08f)
val WaultFloatingPillBg = Color(0xFF1C1C1E).copy(alpha = 0.88f)
val WaultFloatingPillBorder = Color.White.copy(alpha = 0.12f)

// Minimal Shizuku Capsule Colors
val WaultShizukuActiveBg = Color(0xFF1C1C1E)
val WaultShizukuActiveDot = Color(0xFF30D158)
val WaultShizukuInactiveBg = Color(0xFF1C1C1E)
val WaultShizukuInactiveDot = Color(0xFFFF9F0A)

// Accents (Apple iOS Dark Mode System Accents)
val WaultPrimaryMint = Color(0xFF30D158)
val WaultPrimaryCyan = Color(0xFF0A84FF)
val WaultAccentIndigo = Color(0xFF5E5CE6)
val WaultStatusGreen = Color(0xFF30D158)
val WaultStatusAmber = Color(0xFFFF9F0A)
val WaultMutedGray = Color(0xFF8E8E93)

// Light Palette (Fallback)
val WaultBackgroundLight = Color(0xFFF8FAFC)
val WaultSurfaceLight = Color(0xFFFFFFFF)
val WaultPrimaryLight = Color(0xFF0F172A)
val WaultSecondaryLight = Color(0xFF334155)

// Premium Bank Card Gradients
data class BankGradientPalette(
    val start: Color,
    val end: Color
)

object AppleWalletBankGradients {
    val AkbankCrimson = BankGradientPalette(
        start = Color(0xFFB91C1C),
        end = Color(0xFF3F070B)
    )
    val JuzdanRuby = BankGradientPalette(
        start = Color(0xFF9F1239),
        end = Color(0xFF2B0613)
    )
    val GarantiEmerald = BankGradientPalette(
        start = Color(0xFF059669),
        end = Color(0xFF042F2E)
    )
    val BonusTeal = BankGradientPalette(
        start = Color(0xFF0D9488),
        end = Color(0xFF062C2B)
    )
    val IsBankasiNavy = BankGradientPalette(
        start = Color(0xFF1D4ED8),
        end = Color(0xFF091536)
    )
    val YapiKrediSapphire = BankGradientPalette(
        start = Color(0xFF2563EB),
        end = Color(0xFF0F172A)
    )
    val WorldViolet = BankGradientPalette(
        start = Color(0xFF5B21B6),
        end = Color(0xFF1A0B36)
    )
    val ZiraatBurgundy = BankGradientPalette(
        start = Color(0xFF991B1B),
        end = Color(0xFF280507)
    )
    val VakifAmber = BankGradientPalette(
        start = Color(0xFFB45309),
        end = Color(0xFF2E1205)
    )
    val PaycellMidnight = BankGradientPalette(
        start = Color(0xFF1E3A8A),
        end = Color(0xFF0B132B)
    )
    val KentkartCoral = BankGradientPalette(
        start = Color(0xFFC2410C),
        end = Color(0xFF331004)
    )
    val QnbRoyalPurple = BankGradientPalette(
        start = Color(0xFF6D28D9),
        end = Color(0xFF1E103C)
    )
    val TitaniumBlack = BankGradientPalette(
        start = Color(0xFF2C2C2E),
        end = Color(0xFF0E0E10)
    )

    fun resolveForBank(titleOrPkg: String, fallbackStartArgb: Int, fallbackEndArgb: Int): BankGradientPalette {
        val key = titleOrPkg.lowercase()
        return when {
            key.contains("juzdan") || key.contains("axess") -> JuzdanRuby
            key.contains("akbank") || key.contains("wings") -> AkbankCrimson
            key.contains("bonus") -> BonusTeal
            key.contains("garanti") -> GarantiEmerald
            key.contains("iş") || key.contains("isbank") || key.contains("maximum") || key.contains("iscep") -> IsBankasiNavy
            key.contains("world") || key.contains("avm") -> WorldViolet
            key.contains("yapı") || key.contains("yapi") || key.contains("ykb") -> YapiKrediSapphire
            key.contains("ziraat") || key.contains("bankkart") -> ZiraatBurgundy
            key.contains("vakıf") || key.contains("vakif") -> VakifAmber
            key.contains("paycell") -> PaycellMidnight
            key.contains("kentkart") || key.contains("istanbulkart") -> KentkartCoral
            key.contains("qnb") || key.contains("enpara") || key.contains("finans") -> QnbRoyalPurple
            key.contains("papara") || key.contains("google") || key.contains("tapandpay") || key.contains("metal") -> TitaniumBlack
            else -> BankGradientPalette(Color(fallbackStartArgb), Color(fallbackEndArgb))
        }
    }
}

// Curated Pass / Loyalty & Bank Card Preset Colors
data class PassColorPreset(
    val name: String,
    val hex: Long
)

val WaultPassColorPresets = listOf(
    PassColorPreset("Starbucks Yeşili", 0xFF1E7045L),
    PassColorPreset("Gym Antrasit", 0xFF2C2C2EL),
    PassColorPreset("Market Mavisi", 0xFF0A84FFL),
    PassColorPreset("Transit Kırmızı", 0xFFFF3B30L),
    PassColorPreset("Titanyum Siyah", 0xFF1C1C1EL),
    PassColorPreset("Kraliyet Moru", 0xFF5E5CE6L),
    PassColorPreset("Kehribar", 0xFFFF9F0AL),
    PassColorPreset("Zümrüt", 0xFF059669L)
)