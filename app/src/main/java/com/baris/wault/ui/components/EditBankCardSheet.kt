package com.baris.wault.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baris.wault.data.nfc.BankCardOverrideEntity
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.BankPaymentNetwork
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPassColorPresets
import com.baris.wault.ui.theme.WaultPrimaryCyan
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import com.baris.wault.ui.theme.WaultSurfaceDark
import com.baris.wault.util.ImageStorageHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBankCardSheet(
    bank: BankPaymentApp,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onSetDefaultNow: (BankPaymentApp) -> Unit,
    onActivateTemp60s: (BankPaymentApp) -> Unit,
    onSaveOverride: (
        bank: BankPaymentApp,
        customTitle: String?,
        cardNumberSuffix: String?,
        customColorHex: Long?,
        customImagePath: String?,
        paymentNetwork: String?,
        isPrimary: Boolean,
        isHidden: Boolean,
        isFavorite: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customTitle by remember(bank) {
        mutableStateOf(bank.override?.customTitle ?: bank.appName)
    }
    var cardNumberSuffix by remember(bank) {
        mutableStateOf(bank.override?.cardNumberSuffix ?: "")
    }
    var customColorHex by remember(bank) {
        mutableStateOf(bank.override?.customColorHex)
    }
    var customImagePath by remember(bank) {
        mutableStateOf(bank.override?.customImagePath)
    }
    var selectedNetwork by remember(bank) {
        mutableStateOf(bank.paymentNetwork)
    }
    var isPrimary by remember(bank) {
        mutableStateOf(bank.override?.isPrimary == true)
    }
    var isFavorite by remember(bank) {
        mutableStateOf(bank.isFavorite)
    }

    val livePreviewBank = remember(
        bank,
        customTitle,
        cardNumberSuffix,
        customColorHex,
        customImagePath,
        selectedNetwork
    ) {
        bank.withOverride(
            BankCardOverrideEntity(
                componentFlatten = bank.flattenedComponent,
                customTitle = customTitle.takeIf { it.isNotBlank() },
                cardNumberSuffix = cardNumberSuffix.takeIf { it.isNotBlank() },
                customColorHex = customColorHex,
                customImagePath = customImagePath,
                paymentNetwork = selectedNetwork.code
            )
        )
    }

    var bitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }

    val pickCardCoverLauncher = rememberLauncherForActivityResult(
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
                text = "Kartı Özelleştir",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Apple Wallet Card Preview
            BankCardItem(
                bank = livePreviewBank,
                isExpanded = true,
                isDefaultNfc = isActive
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick NFC Actions Row: Varsayılan Yap & 60sn Geçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        onSetDefaultNow(bank)
                        onDismiss()
                    },
                    enabled = !isActive,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "Varsayılan Kart" else "Varsayılan Yap",
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        onActivateTemp60s(bank)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "60sn Geçici",
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Launch Bank App (QR / Mobile Banking Deep Link)
            OutlinedButton(
                onClick = {
                    bank.launchBankApp(context)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${bank.displayTitle} Uygulamasını Aç (QR / Mobil)",
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Network Selector (VISA / Mastercard / TROY / Amex)
            Text(
                text = "Ödeme Ağı (Kart Logosu)",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF94A3B8)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BankPaymentNetwork.entries.forEach { network ->
                    FilterChip(
                        selected = selectedNetwork == network,
                        onClick = { selectedNetwork = network },
                        label = { Text(network.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Card Title & Last 4 Digits (Side by Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Kart Adı") },
                    placeholder = { Text("Örn: Akbank") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.45f)
                )

                OutlinedTextField(
                    value = cardNumberSuffix,
                    onValueChange = { input ->
                        cardNumberSuffix = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Son 4 Hane") },
                    placeholder = { Text("0454") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Color Picker
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = customColorHex == null,
                    onClick = { customColorHex = null },
                    label = { Text("Otomatik") }
                )

                WaultPassColorPresets.forEach { preset ->
                    val isSelected = customColorHex == preset.hex
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
                            .clickable { customColorHex = preset.hex },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Compact Cover Image & Primary Card Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        pickCardCoverLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (customImagePath == null) "Kapak Görseli Seç" else "Görseli Değiştir")
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kırp")
                    }

                    TextButton(onClick = { customImagePath = null }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kaldır")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Card ("Ana Kart") Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = WaultSurfaceContainerDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Nfc,
                            contentDescription = null,
                            tint = WaultPrimaryMint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ana Kart Olarak Ayarla",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Geçici ödemelerden 60 sn sonra bu karta dönülür",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Favorite ("Favorilere Ekle") Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = WaultSurfaceContainerDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD60A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Favorilere Ekle",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Kartı 'Favoriler' filtresinde gösterir",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save & Hide Card Buttons
            Button(
                onClick = {
                    onSaveOverride(
                        bank,
                        customTitle,
                        cardNumberSuffix,
                        customColorHex,
                        customImagePath,
                        selectedNetwork.code,
                        isPrimary,
                        false,
                        isFavorite
                    )
                },
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
                    text = "Kaydet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    onSaveOverride(
                        bank,
                        customTitle,
                        cardNumberSuffix,
                        customColorHex,
                        customImagePath,
                        selectedNetwork.code,
                        false,
                        true,
                        isFavorite
                    )
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFF453A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bu Kartı Cüzdandan Gizle")
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
                        prefix = "bank"
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
