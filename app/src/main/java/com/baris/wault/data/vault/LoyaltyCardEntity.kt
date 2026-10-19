package com.baris.wault.data.vault

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.zxing.BarcodeFormat

enum class WaultBarcodeFormat(
    val displayName: String,
    val zxingFormat: BarcodeFormat,
    val isSquare: Boolean,
    val hint: String
) {
    QR_CODE(
        displayName = "QR Kod",
        zxingFormat = BarcodeFormat.QR_CODE,
        isSquare = true,
        hint = "Kare 2B matris (Spor salonu turnikeleri ve dijital geçişler)"
    ),
    CODE_128(
        displayName = "Kod 128",
        zxingFormat = BarcodeFormat.CODE_128,
        isSquare = false,
        hint = "Yüksek yoğunluklu 1B barkod (Harf ve rakam destekli)"
    ),
    EAN_13(
        displayName = "EAN-13",
        zxingFormat = BarcodeFormat.EAN_13,
        isSquare = false,
        hint = "13 haneli market ve perakende sadakat kartı barkodu"
    ),
    PDF_417(
        displayName = "PDF417",
        zxingFormat = BarcodeFormat.PDF_417,
        isSquare = false,
        hint = "Yığın 2B barkod (Ulaşım ve biniş kartları)"
    ),
    AZTEC(
        displayName = "Aztec",
        zxingFormat = BarcodeFormat.AZTEC,
        isSquare = true,
        hint = "Kompakt 2B matris (Etkinlik ve ulaşım biletleri)"
    ),
    CODE_39(
        displayName = "Kod 39",
        zxingFormat = BarcodeFormat.CODE_39,
        isSquare = false,
        hint = "Standart 1B barkod (Üye numaraları ve personel kartları)"
    );

    companion object {
        fun fromNameOrDefault(name: String?): WaultBarcodeFormat {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CODE_128
        }
    }
}

@Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val subtitle: String = "",
    val barcodeValue: String,
    val barcodeFormat: WaultBarcodeFormat = WaultBarcodeFormat.CODE_128,
    val cardColorHex: Long = 0xFF1E293BL,
    val customImagePath: String? = null,
    val notes: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
