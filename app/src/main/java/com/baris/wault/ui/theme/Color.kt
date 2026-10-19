package com.baris.wault.ui.theme

import androidx.compose.ui.graphics.Color

// Wault v2.5 Pure AMOLED & Frosted Container Palette
val WaultAmoledBlack = Color(0xFF050505)
val WaultObsidian = Color(0xFF050505)
val WaultSurfaceDark = Color(0xFF121316)
val WaultSurfaceContainerDark = Color(0xFF1A1C20)
val WaultGlassBorderDark = Color(0xFF262930)

val WaultPrimaryMint = Color(0xFF22C55E)
val WaultPrimaryCyan = Color(0xFF38BDF8)
val WaultAccentIndigo = Color(0xFF818CF8)
val WaultStatusGreen = Color(0xFF22C55E)
val WaultStatusAmber = Color(0xFFF59E0B)

// Light Palette
val WaultBackgroundLight = Color(0xFFF8FAFC)
val WaultSurfaceLight = Color(0xFFFFFFFF)
val WaultPrimaryLight = Color(0xFF0F172A)
val WaultSecondaryLight = Color(0xFF334155)

// Curated Pass / Loyalty & Bank Card Preset Colors
data class PassColorPreset(
    val name: String,
    val hex: Long
)

val WaultPassColorPresets = listOf(
    PassColorPreset("Obsidyen", 0xFF18181BL),
    PassColorPreset("Zümrüt", 0xFF065F46L),
    PassColorPreset("Kraliyet Mavisi", 0xFF1E40AFL),
    PassColorPreset("Gün Batımı", 0xFFC2410CL),
    PassColorPreset("Kızıl", 0xFF9F1239L),
    PassColorPreset("Mor", 0xFF5B21B6L),
    PassColorPreset("Turkuaz", 0xFF155E75L),
    PassColorPreset("Grafit", 0xFF27272AL)
)