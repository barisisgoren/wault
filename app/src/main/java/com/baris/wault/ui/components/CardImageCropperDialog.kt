package com.baris.wault.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Standard ID-1 Credit Card Aspect Ratio (85.60 mm x 53.98 mm = ~1.58577)
 */
private const val CARD_ASPECT_RATIO = 1.58577f

/**
 * High-precision, Apple-style interactive Card Image Cropper Dialog.
 * Allows panning, pinch-to-zoom, 90-degree rotations, and precise alignment
 * to Wault's 20.dp rounded credit card frame.
 */
@Composable
fun CardImageCropperDialog(
    bitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var userZoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action & Title Bar
                CropTopBar(
                    isProcessing = isProcessing,
                    onDismiss = onDismiss,
                    onConfirm = {
                        // Confirmed trigger
                    }
                )

                // Viewport Box with Constraints
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val density = LocalDensity.current
                    val maxWidthPx = constraints.maxWidth.toFloat()
                    val maxHeightPx = constraints.maxHeight.toFloat()

                    val marginHorizontalPx = with(density) { 24.dp.toPx() }
                    val maxAllowedWidthPx = with(density) { 380.dp.toPx() }
                    val frameWidthPx = (maxWidthPx - marginHorizontalPx * 2).coerceAtMost(maxAllowedWidthPx)
                    val frameHeightPx = frameWidthPx / CARD_ASPECT_RATIO
                    val frameLeftPx = (maxWidthPx - frameWidthPx) / 2f
                    val frameTopPx = (maxHeightPx - frameHeightPx) / 2f

                    val frameRect = remember(frameLeftPx, frameTopPx, frameWidthPx, frameHeightPx) {
                        Rect(frameLeftPx, frameTopPx, frameLeftPx + frameWidthPx, frameTopPx + frameHeightPx)
                    }
                    val cornerRadiusPx = with(density) { 20.dp.toPx() }

                    // Effective dimensions of the rotated bitmap
                    val effW = if (rotationDegrees % 180 == 0) bitmap.width.toFloat() else bitmap.height.toFloat()
                    val effH = if (rotationDegrees % 180 == 0) bitmap.height.toFloat() else bitmap.width.toFloat()

                    val baseScale = max(frameWidthPx / effW, frameHeightPx / effH)
                    val totalScale = baseScale * userZoom

                    val maxPanX = ((effW * totalScale - frameWidthPx) / 2f).coerceAtLeast(0f)
                    val maxPanY = ((effH * totalScale - frameHeightPx) / 2f).coerceAtLeast(0f)

                    // Ensure pan offset stays clamped
                    panOffset = Offset(
                        x = panOffset.x.coerceIn(-maxPanX, maxPanX),
                        y = panOffset.y.coerceIn(-maxPanY, maxPanY)
                    )

                    // Canvas & Gesture Receiver
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap, rotationDegrees) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextZoom = (userZoom * zoom).coerceIn(1.0f, 4.0f)
                                    val nextScale = baseScale * nextZoom
                                    val curMaxX = ((effW * nextScale - frameWidthPx) / 2f).coerceAtLeast(0f)
                                    val curMaxY = ((effH * nextScale - frameHeightPx) / 2f).coerceAtLeast(0f)

                                    userZoom = nextZoom
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-curMaxX, curMaxX),
                                        y = (panOffset.y + pan.y).coerceIn(-curMaxY, curMaxY)
                                    )
                                }
                            }
                            .pointerInput(bitmap, rotationDegrees) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        userZoom = if (userZoom > 1.25f) 1.0f else 2.0f
                                    }
                                )
                            }
                    ) {
                        // 1. Draw transformed image
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val matrix = android.graphics.Matrix().apply {
                                postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                                postRotate(rotationDegrees.toFloat())
                                postScale(totalScale, totalScale)
                                postTranslate(panOffset.x, panOffset.y)
                                postTranslate(frameRect.center.x, frameRect.center.y)
                            }
                            val paint = android.graphics.Paint(
                                android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
                            )
                            nativeCanvas.drawBitmap(bitmap, matrix, paint)
                        }

                        // 2. Translucent Mask with Card Frame Cutout
                        val fullPath = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                        }
                        val cardCutoutPath = Path().apply {
                            addRoundRect(RoundRect(frameRect, CornerRadius(cornerRadiusPx)))
                        }
                        val maskPath = Path.combine(PathOperation.Difference, fullPath, cardCutoutPath)
                        drawPath(maskPath, color = Color.Black.copy(alpha = 0.82f))

                        // 3. Card Frame Stroke
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = frameRect.topLeft,
                            size = frameRect.size,
                            cornerRadius = CornerRadius(cornerRadiusPx),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 4. Subtle 3x3 Grid
                        val line1X = frameRect.left + frameRect.width / 3f
                        val line2X = frameRect.left + (frameRect.width / 3f) * 2f
                        val line1Y = frameRect.top + frameRect.height / 3f
                        val line2Y = frameRect.top + (frameRect.height / 3f) * 2f
                        val gridColor = Color.White.copy(alpha = 0.18f)
                        val gridStroke = 1.dp.toPx()

                        drawLine(gridColor, Offset(line1X, frameRect.top), Offset(line1X, frameRect.bottom), strokeWidth = gridStroke)
                        drawLine(gridColor, Offset(line2X, frameRect.top), Offset(line2X, frameRect.bottom), strokeWidth = gridStroke)
                        drawLine(gridColor, Offset(frameRect.left, line1Y), Offset(frameRect.right, line1Y), strokeWidth = gridStroke)
                        drawLine(gridColor, Offset(frameRect.left, line2Y), Offset(frameRect.right, line2Y), strokeWidth = gridStroke)

                        // 5. Card Watermark Reference (Chip & Contactless placement hint)
                        val chipW = 38.dp.toPx()
                        val chipH = 28.dp.toPx()
                        val chipLeft = frameRect.left + 22.dp.toPx()
                        val chipTop = frameRect.top + frameRect.height * 0.42f
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.22f),
                            topLeft = Offset(chipLeft, chipTop),
                            size = androidx.compose.ui.geometry.Size(chipW, chipH),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Confirm handler hooked into the frame metrics
                    CropActionOverlay(
                        isProcessing = isProcessing,
                        onConfirmCrop = {
                            if (isProcessing) return@CropActionOverlay
                            isProcessing = true
                            scope.launch(Dispatchers.Default) {
                                val targetW = 1200
                                val targetH = (targetW / CARD_ASPECT_RATIO).toInt()
                                val targetBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(targetBitmap)

                                val renderScale = totalScale * (targetW.toFloat() / frameWidthPx)
                                val scaledPanX = panOffset.x * (targetW.toFloat() / frameWidthPx)
                                val scaledPanY = panOffset.y * (targetW.toFloat() / frameWidthPx)

                                val matrix = android.graphics.Matrix().apply {
                                    postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                                    postRotate(rotationDegrees.toFloat())
                                    postScale(renderScale, renderScale)
                                    postTranslate(scaledPanX, scaledPanY)
                                    postTranslate(targetW / 2f, targetH / 2f)
                                }
                                val paint = android.graphics.Paint(
                                    android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
                                )
                                canvas.drawBitmap(bitmap, matrix, paint)

                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    onCropConfirmed(targetBitmap)
                                }
                            }
                        }
                    )
                }

                // Bottom Controls: Rotation, Zoom Slider, Reset
                CropBottomControls(
                    userZoom = userZoom,
                    onZoomChange = { nextZoom ->
                        userZoom = nextZoom
                    },
                    onRotate90 = {
                        rotationDegrees = (rotationDegrees + 90) % 360
                    },
                    onReset = {
                        rotationDegrees = 0
                        userZoom = 1.0f
                        panOffset = Offset.Zero
                    }
                )
            }
        }
    }
}

@Composable
private fun CropTopBar(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDismiss,
            enabled = !isProcessing
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Vazgeç",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kart Görselini Kırp",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Yakınlaştırıp çerçeveye hizalayın",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93)
            )
        }

        // Dummy spacer for balance; confirm button is rendered cleanly in action overlay
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun CropActionOverlay(
    isProcessing: Boolean,
    onConfirmCrop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, end = 16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        ElevatedButton(
            onClick = onConfirmCrop,
            enabled = !isProcessing,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF0A84FF),
                contentColor = Color.White
            )
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kırp ve Kullan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CropBottomControls(
    userZoom: Float,
    onZoomChange: (Float) -> Unit,
    onRotate90: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1C1C1E).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zoom Slider Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomOut,
                    contentDescription = "Uzaklaştır",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value = userZoom,
                    onValueChange = onZoomChange,
                    valueRange = 1.0f..4.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF0A84FF),
                        inactiveTrackColor = Color(0xFF3A3A3C)
                    )
                )
                Icon(
                    imageVector = Icons.Rounded.ZoomIn,
                    contentDescription = "Yakınlaştır",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Quick Actions: Rotate 90 & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRotate90,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF2C2C2E),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "90° Döndür",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                FilledTonalButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF2C2C2E),
                        contentColor = Color(0xFF8E8E93)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sıfırla",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

