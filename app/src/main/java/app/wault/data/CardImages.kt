package app.wault.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Only verified front-facing artwork, bundled for offline use. */
data class CatalogFace(val id: String, val product: String, val bank: String, val source: String, val network: CardNetwork, val packageHint: String)
/**
 * Hazır kart görselleri. Şimdilik boş: görseller sonradan eklenecek.
 * Eklemek için `app/src/main/assets/card_faces/<id>.webp` dosyasını koy (üzerinde ad/numara olmasın)
 * ve buraya bir satır yaz, örn.
 * `CatalogFace("ornek", "Ürün adı", "Banka", "kaynak adresi", CardNetwork.VISA, "paket-adı-parçası")`.
 */
object CardCatalog {
    val all = emptyList<CatalogFace>()
}
object CardImages {
    private val validRef = Regex("[a-f0-9-]{36}\\.bin")
    private fun file(context: Context, ref: String): File? = if (validRef.matches(ref)) File(File(context.filesDir, "card-images"), ref) else null
    fun load(context: Context, source: FaceSource, ref: String?): Bitmap? = runCatching {
        if (ref == null) return null
        when (source) {
            FaceSource.THEME -> null
            FaceSource.CATALOG -> if (CardCatalog.all.any { it.id == ref }) context.assets.open("card_faces/$ref.webp").use { BitmapFactory.decodeStream(it) } else null
            FaceSource.CUSTOM -> file(context, ref)?.takeIf { it.exists() }?.let { val data = VaultCipher.decrypt(it.readBytes()); BitmapFactory.decodeByteArray(data, 0, data.size) }
        }
    }.getOrNull()
    fun decodePhoto(context: Context, uri: Uri): Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        val scale = minOf(1f, 2400f / maxOf(info.size.width, info.size.height))
        decoder.setTargetSize(maxOf(1,(info.size.width*scale).toInt()), maxOf(1,(info.size.height*scale).toInt()))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    fun save(context: Context, bitmap: Bitmap): String {
        val width = minOf(1200, bitmap.width)
        val normalized = Bitmap.createScaledBitmap(bitmap, width, (width.toFloat()*bitmap.height/bitmap.width).toInt(), true)
        val bytes = ByteArrayOutputStream().use { stream ->
            @Suppress("DEPRECATION")
            check(normalized.compress(Bitmap.CompressFormat.WEBP, 92, stream))
            stream.toByteArray()
        }
        val ref = "${UUID.randomUUID()}.bin"
        val target = requireNotNull(file(context, ref))
        target.parentFile!!.mkdirs()
        target.writeBytes(VaultCipher.encrypt(bytes))
        return ref
    }
    fun delete(context: Context, ref: String) { file(context, ref)?.delete() }
}
