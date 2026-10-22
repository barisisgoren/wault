package com.baris.wault.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageStorageHelper {

    private const val CARD_IMAGES_DIR = "card_images"
    private const val MAX_IMAGE_DIMENSION_PX = 1440
    private const val JPEG_QUALITY = 88

    private val bitmapCache = object : LruCache<String, ImageBitmap>(32) {}

    /**
     * Copies, downscales, and compresses a gallery image selected via PickVisualMedia
     * into `context.filesDir/card_images/` so it persists permanently without URI permission expiry.
     */
    suspend fun saveGalleryImageToInternalStorage(
        context: Context,
        sourceUri: Uri,
        prefix: String = "card"
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 1. Decode bounds first
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0) return@runCatching null

            var sampleSize = 1
            while (max(rawW / sampleSize, rawH / sampleSize) > MAX_IMAGE_DIMENSION_PX * 2) {
                sampleSize *= 2
            }

            // 2. Decode with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decodedBitmap = resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@runCatching null

            // 3. Scale accurately to MAX_IMAGE_DIMENSION_PX if still larger
            val finalBitmap = scaleDownIfNeeded(decodedBitmap, MAX_IMAGE_DIMENSION_PX)

            // 4. Write to internal filesDir/card_images/
            val directory = File(context.filesDir, CARD_IMAGES_DIR).apply {
                if (!exists()) mkdirs()
            }
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(directory, fileName)

            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }

            val path = destFile.absolutePath
            synchronized(bitmapCache) {
                bitmapCache.put(path, finalBitmap.asImageBitmap())
            }
            path
        }.getOrNull()
    }

    /**
     * Decodes a Bitmap from [sourceUri], respecting its EXIF orientation tag,
     * downscaling to [maxDimension] to keep memory reasonable for interactive cropping.
     */
    suspend fun decodeBitmapFromUri(
        context: Context,
        sourceUri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 1. Read EXIF orientation
            var rotationDegrees = 0
            try {
                resolver.openInputStream(sourceUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (_: Exception) { }

            // 2. Decode bounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0) return@runCatching null

            var sampleSize = 1
            while (max(rawW / sampleSize, rawH / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var decoded = resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@runCatching null

            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated != decoded) {
                    decoded.recycle()
                    decoded = rotated
                }
            }

            scaleDownIfNeeded(decoded, maxDimension)
        }.getOrNull()
    }

    /**
     * Loads an existing internal storage image as a mutable Bitmap for re-cropping.
     */
    suspend fun loadOriginalBitmap(path: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        runCatching {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return@runCatching null
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    /**
     * Saves a user-cropped Bitmap into `filesDir/card_images/`, updates LRU cache,
     * and returns the absolute file path.
     */
    suspend fun saveCroppedBitmap(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "card"
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.filesDir, CARD_IMAGES_DIR).apply {
                if (!exists()) mkdirs()
            }
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val destFile = File(directory, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }

            val path = destFile.absolutePath
            synchronized(bitmapCache) {
                bitmapCache.put(path, bitmap.asImageBitmap())
            }
            path
        }.getOrNull()
    }

    suspend fun loadStoredImageBitmap(imagePath: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        if (imagePath.isNullOrBlank()) return@withContext null

        synchronized(bitmapCache) {
            bitmapCache.get(imagePath)?.let { return@withContext it }
        }

        runCatching {
            val file = File(imagePath)
            if (!file.exists() || !file.canRead()) return@runCatching null
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
            val imageBitmap = bitmap.asImageBitmap()
            synchronized(bitmapCache) {
                bitmapCache.put(imagePath, imageBitmap)
            }
            imageBitmap
        }.getOrNull()
    }

    suspend fun deleteStoredImageIfOwned(
        context: Context,
        imagePath: String?
    ) = withContext(Dispatchers.IO) {
        if (imagePath.isNullOrBlank()) return@withContext
        runCatching {
            synchronized(bitmapCache) {
                bitmapCache.remove(imagePath)
            }
            val imagesDir = File(context.filesDir, CARD_IMAGES_DIR).canonicalPath
            val targetFile = File(imagePath)
            if (targetFile.exists() && targetFile.canonicalPath.startsWith(imagesDir)) {
                targetFile.delete()
            }
        }
    }

    private fun scaleDownIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val largest = max(width, height)
        if (largest <= maxDimension) return source

        val ratio = maxDimension.toFloat() / largest.toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}

@Composable
fun rememberStoredImageBitmap(imagePath: String?): ImageBitmap? {
    var bitmap by remember(imagePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imagePath) {
        bitmap = ImageStorageHelper.loadStoredImageBitmap(imagePath)
    }
    return bitmap
}

