package com.baris.wault.ui.theme

import androidx.compose.ui.graphics.Color

// Pure AMOLED & Glassmorphism Palette
val WaultAmoledBlack = Color(0xFF000000)
val WaultObsidian = Color(0xFF000000)
val WaultSurfaceDark = Color(0xFF121212)
val WaultSurfaceContainerDark = Color(0xFF1A1A1A)
val WaultGlassBorderDark = Color.White.copy(alpha = 0.08f)

// TopBar & Floating Pill Dock Surfaces
val WaultTopActionBg = Color(0xFF1C1C1E)
val WaultTopActionBorder = Color.White.copy(alpha = 0.10f)
val WaultFloatingPillBg = Color(0xFF262629).copy(alpha = 0.85f)
val WaultFloatingPillBorder = Color.White.copy(alpha = 0.15f)

// Shizuku Status Pill Colors
val WaultShizukuActiveBg = Color(0xFF0D2E1E)
val WaultShizukuActiveDot = Color(0xFF34C759)
val WaultShizukuInactiveBg = Color(0xFF2B1911)
val WaultShizukuInactiveDot = Color(0xFFFF9F0A)

// Accents
val WaultPrimaryMint = Color(0xFF34C759)
val WaultPrimaryCyan = Color(0xFF38BDF8)
val WaultAccentIndigo = Color(0xFF818CF8)
val WaultStatusGreen = Color(0xFF34C759)
val WaultStatusAmber = Color(0xFFFF9F0A)
val WaultMutedGray = Color(0xFF8E8E93)

// Light Palette (Fallback)
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
    PassColorPreset("Starbucks Yeşili", 0xFF1E7045L),
    PassColorPreset("Gym Antrasit", 0xFF3A3D42L),
    PassColorPreset("Market Mavisi", 0xFF2B6AD0L),
    PassColorPreset("Transit Kırmızı", 0xFFD9383AL),
    PassColorPreset("Obsidyen", 0xFF18181BL),
    PassColorPreset("Kraliyet Moru", 0xFF5B21B6L),
    PassColorPreset("Gün Batımı", 0xFFC2410CL),
    PassColorPreset("Okyanus", 0xFF155E75L)
)