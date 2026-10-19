package com.baris.wault.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.baris.wault.ui.theme.WaultPassColorPresets
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.util.BarcodeGenerator
import com.baris.wault.util.ImageStorageHelper
import com.baris.wault.util.rememberStoredImageBitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private enum class EntryMode {
    MANUAL,
    SCANNER
}

private val QuickBrandSuggestions = listOf(
    "MACFit" to 0xFF065F46L,
    "Migros Money" to 0xFFC2410CL,
    "İstanbulkart" to 0xFF9F1239L,
    "Starbucks" to 0xFF065F46L,
    "Decathlon" to 0xFF1E40AFL,
    "Gratis" to 0xFF5B21B6L,
    "Watsons" to 0xFF155E75L,
    "CarrefourSA" to 0xFF1E40AFL
)

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var entryMode by remember {
        mutableStateOf(EntryMode.MANUAL)
    }
    var title by remember(existingCard) {
        mutableStateOf(existingCard?.title ?: "")
    }
    var subtitle by remember(existingCard) {
        mutableStateOf(existingCard?.subtitle ?: "")
    }
    var notes by remember(existingCard) {
        mutableStateOf(existingCard?.notes ?: "")
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

    val coverBitmap = rememberStoredImageBitmap(customImagePath)

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

    // 1. Launcher for scanning barcode/QR from Gallery Screenshot ("Galeriden Barkod Tara")
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
                            entryMode = EntryMode.MANUAL
                            Toast.makeText(
                                context,
                                "Barkod galeriden başarıyla okundu",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Seçilen görselde barkod veya QR kod bulunamadı",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            "Görsel taranırken bir hata oluştu",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnCompleteListener {
                        scanner.close()
                    }
            }
        }
    }

    // 2. Launcher for picking custom cover image ("Görsel Seç")
    val pickCoverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = ImageStorageHelper.saveGalleryImageToInternalStorage(
                    context = context,
                    sourceUri = uri,
                    prefix = "pass"
                )
                if (savedPath != null) {
                    customImagePath = savedPath
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
        containerColor = MaterialTheme.colorScheme.surface
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
                text = if (existingCard == null) "Yeni Kart Ekle" else "Kartı Düzenle",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kamerayla, galerideki ekran görüntüsünden veya manuel olarak üyelik kartı ekleyin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 1 (Kamerayla Tara) vs Option 2 (Manuel Giriş)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = entryMode == EntryMode.SCANNER,
                    onClick = {
                        entryMode = EntryMode.SCANNER
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Kamerayla Tara")
                }

                SegmentedButton(
                    selected = entryMode == EntryMode.MANUAL,
                    onClick = { entryMode = EntryMode.MANUAL },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Bilgileri Gir")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scan Barcode from Gallery Image ("Galeriden Görsel/Ekran Görüntüsü Tara")
            FilledTonalButton(
                onClick = {
                    scanBarcodeFromGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Galeriden Görsel/Ekran Görüntüsü Tara")
            }

            // Live CameraX + ML Kit Scanner Section
            AnimatedVisibility(visible = entryMode == EntryMode.SCANNER) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    if (hasCameraPermission) {
                        CameraBarcodeScannerBox(
                            onBarcodeDetected = { rawValue, detectedFormat ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                barcodeValue = rawValue
                                selectedFormat = detectedFormat
                                entryMode = EntryMode.MANUAL
                            }
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kamera İzni Gerekli",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fiziksel kartları veya QR kodları otomatik okumak için kamera erişimine izin verin.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                ) {
                                    Text("Kamera İzni Ver")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Brand Suggestions
            if (existingCard == null) {
                Text(
                    text = "Hızlı Marka Şablonları",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickBrandSuggestions.forEach { (presetName, presetColor) ->
                        FilterChip(
                            selected = title.equals(presetName, ignoreCase = true),
                            onClick = {
                                title = presetName
                                selectedColorHex = presetColor
                            },
                            label = { Text(presetName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Custom Cover Image Picker ("Görsel Seç")
            Text(
                text = "Kart Kapak Görseli / Logo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .width(96.dp)
                        .height(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            pickCoverImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = "Kart Görseli",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = {
                            pickCoverImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Görsel Seç")
                    }

                    if (customImagePath != null) {
                        TextButton(
                            onClick = { customImagePath = null }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Görseli Kaldır")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title, Subtitle, Barcode Value & Notes Inputs
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Kart / Marka Adı *") },
                placeholder = { Text("Örn: MACFit, Migros, Kütüphane") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                label = { Text("Alt Başlık / Üyelik Tipi (İsteğe Bağlı)") },
                placeholder = { Text("Örn: Gold Üyelik • Kulüp Kartı") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                label = { Text("Barkod / QR Kod Değeri *") },
                placeholder = { Text("Kart numarasını girin veya tarayın") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            entryMode = EntryMode.SCANNER
                            if (!hasCameraPermission) {
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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notlar (İsteğe Bağlı)") },
                placeholder = { Text("Örn: Şube bilgisi veya ek açıklama") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barcode Format Selector
            Text(
                text = "Barkod Formatı",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WaultBarcodeFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        label = { Text(format.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Minimalist Preset Color Picker
            Text(
                text = "Kart Tema Rengi",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaultPassColorPresets.forEach { preset ->
                    val isSelected = selectedColorHex == preset.hex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Live Barcode Preview
            AnimatedVisibility(visible = previewBitmap != null) {
                previewBitmap?.let { bmp ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .height(if (selectedFormat.isSquare) 120.dp else 74.dp),
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
            }

            Spacer(modifier = Modifier.height(22.dp))

            val canSave = title.isNotBlank() && barcodeValue.isNotBlank()
            Button(
                onClick = {
                    onSave(
                        existingCard,
                        title,
                        subtitle,
                        notes,
                        barcodeValue,
                        selectedFormat,
                        selectedColorHex,
                        customImagePath
                    )
                },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (existingCard == null) "Kartı Kaydet" else "Değişiklikleri Kaydet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
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
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .border(
                width = 1.5.dp,
                color = WaultPrimaryMint.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
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
                .height(110.dp)
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
                text = "Barkodu veya QR kodu çerçeveye hizalayın",
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
