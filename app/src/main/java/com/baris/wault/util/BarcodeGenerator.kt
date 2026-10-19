package com.baris.wault.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.baris.wault.data.vault.WaultBarcodeFormat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

object BarcodeGenerator {

    private val bitmapCache = object : LruCache<String, ImageBitmap>(36) {}

    suspend fun generateBarcode(
        value: String,
        format: WaultBarcodeFormat,
        widthPx: Int = if (format.isSquare) 900 else 1200,
        heightPx: Int = if (format.isSquare) 900 else 420
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        val sanitizedValue = value.trim()
        if (sanitizedValue.isEmpty()) return@withContext null

        val cacheKey = "${format.name}:${widthPx}x${heightPx}:$sanitizedValue"
        synchronized(bitmapCache) {
            bitmapCache.get(cacheKey)?.let { return@withContext it }
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, if (format.isSquare) 1 else 2)
        }

        val bitMatrix = encodeWithFallback(
            contents = sanitizedValue,
            targetFormat = format.zxingFormat,
            width = widthPx,
            height = heightPx,
            hints = hints
        ) ?: return@withContext null

        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val pixels = IntArray(matrixWidth * matrixHeight)

        for (y in 0 until matrixHeight) {
            val offset = y * matrixWidth
            for (x in 0 until matrixWidth) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
        val imageBitmap = bitmap.asImageBitmap()

        synchronized(bitmapCache) {
            bitmapCache.put(cacheKey, imageBitmap)
        }
        imageBitmap
    }

    private fun encodeWithFallback(
        contents: String,
        targetFormat: BarcodeFormat,
        width: Int,
        height: Int,
        hints: Map<EncodeHintType, Any>
    ): BitMatrix? {
        val writer = MultiFormatWriter()
        return try {
            val normalizedInput = if (targetFormat == BarcodeFormat.EAN_13) {
                normalizeEan13OrNull(contents) ?: contents
            } else {
                contents
            }
            writer.encode(normalizedInput, targetFormat, width, height, hints)
        } catch (_: Exception) {
            // Fallback to CODE_128 for 1D formats or QR_CODE for 2D formats if checksum/charset fails
            try {
                val fallbackFormat = when (targetFormat) {
                    BarcodeFormat.QR_CODE, BarcodeFormat.AZTEC -> BarcodeFormat.QR_CODE
                    else -> BarcodeFormat.CODE_128
                }
                val fallbackHints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                    put(EncodeHintType.MARGIN, 1)
                }
                writer.encode(contents, fallbackFormat, width, height, fallbackHints)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Normalizes 12 or 13 digit numeric strings for EAN-13 so ZXing never rejects a valid retail card.
     */
    private fun normalizeEan13OrNull(raw: String): String? {
        val digitsOnly = raw.filter { it.isDigit() }
        if (digitsOnly.length == 12) {
            val checkDigit = calculateEan13CheckDigit(digitsOnly)
            return digitsOnly + checkDigit
        }
        if (digitsOnly.length == 13) {
            val prefix12 = digitsOnly.substring(0, 12)
            val expectedCheck = calculateEan13CheckDigit(prefix12)
            if (digitsOnly.last().digitToInt() == expectedCheck) {
                return digitsOnly
            }
        }
        return null
    }

    private fun calculateEan13CheckDigit(twelveDigits: String): Int {
        var sum = 0
        for (i in 0 until 12) {
            val digit = twelveDigits[i].digitToInt()
            sum += if (i % 2 == 0) digit else digit * 3
        }
        return (10 - (sum % 10)) % 10
    }
}
