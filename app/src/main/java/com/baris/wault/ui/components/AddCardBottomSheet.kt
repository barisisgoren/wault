package com.baris.wault.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.data.vault.WaultBarcodeFormat
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPassColorPresets
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import com.baris.wault.ui.theme.WaultSurfaceDark
import com.baris.wault.util.BarcodeGenerator
import com.baris.wault.util.ImageStorageHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardBottomSheet(
    existingCard: LoyaltyCardEntity?,
    onDismiss: () -> Unit,
    onSave: (
        existingCard: LoyaltyCardEntity?,
        title: String,
        subtitle: String,
        notes: String?,
        barcodeValue: String,
        barcodeFormat: WaultBarcodeFormat,
        cardColorHex: Long,
        customImagePath: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isScannerOpen by remember { mutableStateOf(false) }
    var title by remember(existingCard) {
        mutableStateOf(existingCard?.title ?: "")
    }
    var barcodeValue by remember(existingCard) {
        mutableStateOf(existingCard?.barcodeValue ?: "")
    }
    var selectedFormat by remember(existingCard) {
        mutableStateOf(existingCard?.barcodeFormat ?: WaultBarcodeFormat.QR_CODE)
    }
    var selectedColorHex by remember(existingCard) {
        mutableLongStateOf(
            existingCard?.cardColorHex ?: WaultPassColorPresets.first().hex
        )
    }
    var customImagePath by remember(existingCard) {
        mutableStateOf(existingCard?.customImagePath)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val scanBarcodeFromGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                )
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val match = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                        if (match != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            barcodeValue = match.rawValue!!.trim()
                            selectedFormat = mapMlKitFormat(match.format)
                            isScannerOpen = false
                            Toast.makeText(
                                context,
                                "Barkod okundu",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Görselde barkod veya QR kod bulunamadı",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            "Görsel taranırken hata oluştu",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnCompleteListener {
                        scanner.close()
                    }
            }
        }
    }

    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }

    val pickCoverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = ImageStorageHelper.decodeBitmapFromUri(context, uri)
                if (decoded != null) {
                    bitmapToCrop = decoded
                }
            }
        }
    }

    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(barcodeValue, selectedFormat) {
        previewBitmap = if (barcodeValue.isNotBlank()) {
            BarcodeGenerator.generateBarcode(
                value = barcodeValue,
                format = selectedFormat,
                widthPx = if (selectedFormat.isSquare) 420 else 640,
                heightPx = if (selectedFormat.isSquare) 420 else 200
            )
        } else {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WaultSurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existingCard == null) "Kart veya Bilet Ekle" else "Kartı Düzenle",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Top 2 Clean Apple Action Tiles: Kamerayla Tara & Galeriden Seç
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isScannerOpen) WaultPrimaryCyan.copy(alpha = 0.22f) else WaultSurfaceContainerDark,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isScannerOpen) WaultPrimaryCyan else WaultGlassBorderDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            isScannerOpen = !isScannerOpen
                            if (isScannerOpen && !hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kamerayla Tara",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WaultSurfaceContainerDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            scanBarcodeFromGalleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Galeriden Seç",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Live CameraX + ML Kit Scanner Section
            AnimatedVisibility(visible = isScannerOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (hasCameraPermission) {
                        CameraBarcodeScannerBox(
                            onBarcodeDetected = { rawValue, detectedFormat ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                barcodeValue = rawValue
                                selectedFormat = detectedFormat
                                isScannerOpen = false
                            }
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WaultSurfaceContainerDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = WaultPrimaryCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kamera İzni Gerekli",
                                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                ) {
                                    Text("İzin Ver")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Essential Input 1: Card / Brand Name
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Kart Adı") },
                placeholder = { Text("Kart veya bilet adını girin") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Essential Input 2: Barcode / QR Code Value
            OutlinedTextField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                label = { Text("Barkod / QR Kod Numarası") },
                placeholder = { Text("Kart numarasını girin veya tarayın") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            isScannerOpen = !isScannerOpen
                            if (isScannerOpen && !hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Kamerayla Tara"
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Minimal 2-Option Code Type Selector (QR Kod vs Çizgili Barkod)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isQrSelected = selectedFormat.isSquare
                FilterChip(
                    selected = isQrSelected,
                    onClick = { selectedFormat = WaultBarcodeFormat.QR_CODE },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("QR Kod") }
                )

                FilterChip(
                    selected = !isQrSelected,
                    onClick = {
                        if (selectedFormat.isSquare) {
                            selectedFormat = WaultBarcodeFormat.CODE_128
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ViewWeek,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("Çizgili Barkod") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Color Swatches + Optional Compact Cover Image Button
            Text(
                text = "Kart Rengi",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF94A3B8)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaultPassColorPresets.forEach { preset ->
                    val isSelected = selectedColorHex == preset.hex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(preset.hex))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) WaultPrimaryMint else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = preset.hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = preset.name,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compact Optional Cover Image Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        pickCoverImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (customImagePath == null) "Özel Görsel Ekle (İsteğe Bağlı)" else "Görseli Değiştir")
                }

                if (customImagePath != null) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val existing = ImageStorageHelper.loadOriginalBitmap(customImagePath)
                                if (existing != null) {
                                    bitmapToCrop = existing
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Crop,
                            contentDescription = "Kırp",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kırp")
                    }

                    TextButton(onClick = { customImagePath = null }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kaldır")
                    }
                }
            }

            // Live Barcode Preview
            AnimatedVisibility(visible = previewBitmap != null) {
                previewBitmap?.let { bmp ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .height(if (selectedFormat.isSquare) 116.dp else 70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Barkod Önizleme",
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.None,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val canSave = title.isNotBlank() && barcodeValue.isNotBlank()
            Button(
                onClick = {
                    onSave(
                        existingCard,
                        title,
                        existingCard?.subtitle.orEmpty(),
                        existingCard?.notes,
                        barcodeValue,
                        selectedFormat,
                        selectedColorHex,
                        customImagePath
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WaultPrimaryCyan,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (existingCard == null) "Cüzdana Ekle" else "Kaydet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    // Interactive Card Image Cropper
    bitmapToCrop?.let { bmp ->
        CardImageCropperDialog(
            bitmap = bmp,
            onCropConfirmed = { cropped ->
                scope.launch {
                    val savedPath = ImageStorageHelper.saveCroppedBitmap(
                        context = context,
                        bitmap = cropped,
                        prefix = "pass"
                    )
                    if (savedPath != null) {
                        customImagePath = savedPath
                    }
                    bitmapToCrop = null
                }
            },
            onDismiss = {
                bitmapToCrop = null
            }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBarcodeScannerBox(
    onBarcodeDetected: (String, WaultBarcodeFormat) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var hasEmittedResult by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val barcodeScanner = BarcodeScanning.getClient(scannerOptions)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull()
                ?: return@addListener

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            @Suppress("DEPRECATION")
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null && !hasEmittedResult) {
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val firstValid = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                            if (firstValid != null && !hasEmittedResult) {
                                hasEmittedResult = true
                                val raw = firstValid.rawValue!!.trim()
                                val mappedFormat = mapMlKitFormat(firstValid.format)
                                onBarcodeDetected(raw, mappedFormat)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            runCatching {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
            runCatching {
                barcodeScanner.close()
                analysisExecutor.shutdown()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .border(
                width = 1.5.dp,
                color = WaultPrimaryMint.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.74f)
                .height(104.dp)
                .border(
                    width = 2.dp,
                    color = WaultPrimaryMint.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp)
                )
        )

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            IconButton(
                onClick = {
                    val next = !isTorchOn
                    isTorchOn = next
                    camera?.cameraControl?.enableTorch(next)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = "Flaşı Aç/Kapat",
                    tint = Color.White
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Text(
                text = "Barkodu çerçeveye hizalayın",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private fun mapMlKitFormat(mlKitFormat: Int): WaultBarcodeFormat {
    return when (mlKitFormat) {
        Barcode.FORMAT_QR_CODE -> WaultBarcodeFormat.QR_CODE
        Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A -> WaultBarcodeFormat.EAN_13
        Barcode.FORMAT_PDF417 -> WaultBarcodeFormat.PDF_417
        Barcode.FORMAT_AZTEC -> WaultBarcodeFormat.AZTEC
        Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93 -> WaultBarcodeFormat.CODE_39
        else -> WaultBarcodeFormat.CODE_128
    }
}
