package app.wault.ui.add

import androidx.compose.foundation.layout.widthIn
import app.wault.design.ContentMaxWidth
import app.wault.design.cardMaxWidth
import app.wault.design.horizontalSafePadding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.wault.data.CodeFormat
import app.wault.design.PrimaryButton
import app.wault.design.BarButton
import app.wault.design.NavBar
import app.wault.design.TintedButton
import app.wault.design.WaultType
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

fun mapFormat(f: Int): CodeFormat = when (f) {
    Barcode.FORMAT_QR_CODE -> CodeFormat.QR
    Barcode.FORMAT_EAN_13 -> CodeFormat.EAN_13
    Barcode.FORMAT_PDF417 -> CodeFormat.PDF_417
    Barcode.FORMAT_AZTEC -> CodeFormat.AZTEC
    else -> CodeFormat.CODE_128
}

suspend fun scanImage(context: Context, uri: Uri): Pair<CodeFormat, String>? = suspendCancellableCoroutine { cont ->
    val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
    if (image == null) { cont.resume(null); return@suspendCancellableCoroutine }
    BarcodeScanning.getClient().process(image)
        .addOnSuccessListener { list ->
            val b = list.firstOrNull { !it.rawValue.isNullOrEmpty() }
            cont.resume(b?.let { mapFormat(it.format) to it.rawValue!! })
        }
        .addOnFailureListener { cont.resume(null) }
}

/** Tam ekran kamera: koyu zemin, kart oranında tarama çerçevesi. */
@Composable
fun ScannerOverlay(onResult: (CodeFormat, String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it; denied = !it
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1626))) {
        if (granted) CameraPreview(onResult)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().horizontalSafePadding()) {
            NavBar("", trailing = { BarButton("Vazgeç", onCancel, bold = true) })
            Spacer(Modifier.weight(1f))
            if (granted) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 32.dp)
                        // Yatayda çerçeve ekrandan taşmasın
                        .widthIn(max = cardMaxWidth(0.45f))
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(20.dp))
                Text("Kodu çerçevenin içine al", style = WaultType.headline, color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (denied) {
                Column(Modifier.align(Alignment.CenterHorizontally).widthIn(max = ContentMaxWidth).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kamera izni kapalı.", style = WaultType.title, color = Color.White)
                    Text("Kodu taramak için kamera izni ver ya da kodu görselden seç.", style = WaultType.subhead, color = Color.White.copy(alpha = 0.8f))
                    PrimaryButton("İzin ver", { launcher.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth())
                    TintedButton("Vazgeç", onCancel, Modifier.fillMaxWidth(), color = Color.White)
                }
            }
            Spacer(Modifier.weight(1.4f))
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onResult: (CodeFormat, String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val done = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val preview = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val p = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || done.get()) { proxy.close(); return@setAnalyzer }
                val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(img)
                    .addOnSuccessListener { list ->
                        val b = list.firstOrNull { !it.rawValue.isNullOrEmpty() }
                        if (b != null && done.compareAndSet(false, true)) {
                            ContextCompat.getMainExecutor(context).execute { onResult(mapFormat(b.format), b.rawValue!!) }
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, p, analysis)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView({ preview }, Modifier.fillMaxSize())
}
