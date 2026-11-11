package app.wault.ui.code

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.wault.data.CodeFormat
import app.wault.design.WaultType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

private fun CodeFormat.zxing(): BarcodeFormat = when (this) {
    CodeFormat.QR -> BarcodeFormat.QR_CODE
    CodeFormat.CODE_128 -> BarcodeFormat.CODE_128
    CodeFormat.EAN_13 -> BarcodeFormat.EAN_13
    CodeFormat.PDF_417 -> BarcodeFormat.PDF_417
    CodeFormat.AZTEC -> BarcodeFormat.AZTEC
}

private fun CodeFormat.isSquare() = this == CodeFormat.QR || this == CodeFormat.AZTEC

/** Değer bu biçimde çizilebiliyor mu? (EAN-13 için kontrol hanesi dahil.) */
fun canEncode(value: String, format: CodeFormat): Boolean = value.isNotBlank() && runCatching {
    MultiFormatWriter().encode(value, format.zxing(), 200, 200, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
}.isSuccess

/** Girişte kullanıcıya gösterilecek, biçime özel kısa açıklama. */
fun codeFormatHint(format: CodeFormat): String = when (format) {
    CodeFormat.EAN_13 -> "EAN-13 için 13 haneli ve kontrol hanesi doğru bir sayı gerekir."
    CodeFormat.QR, CodeFormat.AZTEC, CodeFormat.PDF_417 -> "Bu değer ${format.label} olarak çizilemiyor. Başka bir biçim dene."
    CodeFormat.CODE_128 -> "Code 128 sadece düz harf, rakam ve işaret kabul eder."
}

fun encodeBarcode(value: String, format: CodeFormat): Bitmap? = runCatching {
    val (w, h) = if (format.isSquare()) 600 to 600 else 900 to 300
    val matrix = MultiFormatWriter().encode(
        value, format.zxing(), w, h,
        mapOf(EncodeHintType.MARGIN to if (format.isSquare()) 1 else 4, EncodeHintType.CHARACTER_SET to "UTF-8"),
    )
    val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    val px = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
        px[y * matrix.width + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
    bmp.setPixels(px, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    bmp
}.getOrNull()

@Composable
fun BarcodeView(value: String, format: CodeFormat, modifier: Modifier = Modifier) {
    val bmp = remember(value, format) { encodeBarcode(value, format) }
    if (bmp == null) {
        Box(modifier.fillMaxWidth().height(80.dp)) {
            Text("Bu değer ${format.label} olarak çizilemiyor. Kartı düzenleyip biçimi değiştir.",
                style = WaultType.footnote, color = Color(0xFFA8322D))
        }
        return
    }
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = "${format.label} kodu",
        filterQuality = FilterQuality.None,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth(if (format.isSquare()) 0.7f else 1f)
            .aspectRatio(bmp.width / bmp.height.toFloat()),
    )
}
