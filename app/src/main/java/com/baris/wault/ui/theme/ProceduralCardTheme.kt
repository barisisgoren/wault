package com.baris.wault.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class ProceduralCardStyle(
    val backgroundBrush: Brush,
    val isChipGold: Boolean = true,
    val isLightContent: Boolean = true,
    val patternType: CardPatternType = CardPatternType.BRUSHED_METAL
)

enum class CardPatternType {
    BRUSHED_METAL,
    TRANSIT_WAVES,
    CARBON_WEAVE,
    COBALT_ARCS,
    CLOVER_GLOW,
    MINIMAL_OBSIDIAN
}

object ProceduralCardThemes {

    fun resolve(titleOrPkg: String, fallbackStart: Int, fallbackEnd: Int): ProceduralCardStyle {
        val key = titleOrPkg.lowercase()
        return when {
            key.contains("kentkart") || key.contains("istanbulkart") || key.contains("ulaşım") -> {
                // High-Tech Transit Card: Warm ember to dark bronze with transit waves
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEA580C),
                            Color(0xFFC2410C),
                            Color(0xFF7C2D12),
                            Color(0xFF2E0C04)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = true,
                    patternType = CardPatternType.TRANSIT_WAVES
                )
            }

            key.contains("garanti") || key.contains("bonus") -> {
                // Garanti Emerald / Obsidian
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF059669),
                            Color(0xFF047857),
                            Color(0xFF064E3B),
                            Color(0xFF02231A)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = true,
                    patternType = CardPatternType.CLOVER_GLOW
                )
            }

            key.contains("akbank") || key.contains("axess") || key.contains("wings") -> {
                // Akbank Deep Crimson to Carbon
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE11D48),
                            Color(0xFFBE123C),
                            Color(0xFF881337),
                            Color(0xFF28030C)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = false,
                    patternType = CardPatternType.CARBON_WEAVE
                )
            }

            key.contains("yapı") || key.contains("yapi") || key.contains("ykb") -> {
                // Yapı Kredi Royal Sapphire
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2563EB),
                            Color(0xFF1D4ED8),
                            Color(0xFF1E3A8A),
                            Color(0xFF0A1435)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = false,
                    patternType = CardPatternType.COBALT_ARCS
                )
            }

            key.contains("world") -> {
                // World Mobil Velvet Purple
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7C3AED),
                            Color(0xFF6D28D9),
                            Color(0xFF4C1D95),
                            Color(0xFF1C0638)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = true,
                    patternType = CardPatternType.COBALT_ARCS
                )
            }

            key.contains("ziraat") -> {
                // Ziraat Imperial Burgundy
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB91C1C),
                            Color(0xFF991B1B),
                            Color(0xFF7F1D1D),
                            Color(0xFF260505)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = true,
                    patternType = CardPatternType.BRUSHED_METAL
                )
            }

            key.contains("vakıf") || key.contains("vakif") -> {
                // VakıfBank Warm Amber Bronze
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD97706),
                            Color(0xFFB45309),
                            Color(0xFF78350F),
                            Color(0xFF2E1204)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = true,
                    patternType = CardPatternType.BRUSHED_METAL
                )
            }

            key.contains("iş") || key.contains("isbank") || key.contains("maximum") || key.contains("iscep") -> {
                // İş Bankası Aegean Navy
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1D4ED8),
                            Color(0xFF1E40AF),
                            Color(0xFF1E3A8A),
                            Color(0xFF09122C)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = false,
                    patternType = CardPatternType.COBALT_ARCS
                )
            }

            key.contains("papara") || key.contains("google") || key.contains("tapandpay") -> {
                // Stealth Obsidian Black
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3F3F46),
                            Color(0xFF27272A),
                            Color(0xFF18181B),
                            Color(0xFF09090B)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = false,
                    patternType = CardPatternType.MINIMAL_OBSIDIAN
                )
            }

            else -> {
                val start = Color(fallbackStart)
                val end = Color(fallbackEnd)
                ProceduralCardStyle(
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(
                            start,
                            Color(
                                (start.red * 0.7f).coerceIn(0f, 1f),
                                (start.green * 0.7f).coerceIn(0f, 1f),
                                (start.blue * 0.7f).coerceIn(0f, 1f)
                            ),
                            end,
                            Color(
                                (end.red * 0.35f).coerceIn(0f, 1f),
                                (end.green * 0.35f).coerceIn(0f, 1f),
                                (end.blue * 0.35f).coerceIn(0f, 1f)
                            )
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    ),
                    isChipGold = true,
                    patternType = CardPatternType.BRUSHED_METAL
                )
            }
        }
    }
}
