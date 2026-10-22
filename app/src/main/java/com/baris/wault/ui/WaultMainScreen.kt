package com.baris.wault.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.zIndex
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.SilentSwitchCapability
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.data.vault.WaultBarcodeFormat
import com.baris.wault.ui.components.AddCardBottomSheet
import com.baris.wault.ui.components.CategoryFilterBar
import com.baris.wault.ui.components.EditBankCardSheet
import com.baris.wault.ui.components.FullscreenBarcodeDialog
import com.baris.wault.ui.components.PassGridSection
import com.baris.wault.ui.components.PaymentStackSection
import com.baris.wault.ui.components.WaultCardCategory
import com.baris.wault.ui.components.WaultFloatingPill
import com.baris.wault.ui.components.WaultOtherSection
import com.baris.wault.ui.components.WaultTab
import com.baris.wault.ui.components.WaultTopBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.baris.wault.ui.theme.WaultAmoledBlack
import com.baris.wault.ui.theme.WaultGlassBorderDark
import com.baris.wault.ui.theme.WaultPrimaryMint
import com.baris.wault.ui.theme.WaultStatusAmber
import com.baris.wault.ui.theme.WaultStatusGreen
import com.baris.wault.ui.theme.WaultSurfaceContainerDark
import com.baris.wault.ui.theme.WaultSurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaultMainScreen(
    uiState: WaultUiState,
    snackbarHostState: SnackbarHostState,
    onSelectBank: (BankPaymentApp) -> Unit,
    onActivateTemp60s: (BankPaymentApp) -> Unit,
    onReorderBanks: (List<BankPaymentApp>) -> Unit,
    onRevertToPrimaryNow: () -> Unit,
    onCancelTempTimer: () -> Unit,
    onRefreshBanks: () -> Unit,
    onOpenEditBankSheet: (BankPaymentApp) -> Unit,
    onCloseEditBankSheet: () -> Unit,
    onSaveBankOverride: (
        bank: BankPaymentApp,
        customTitle: String?,
        cardNumberSuffix: String?,
        customColorHex: Long?,
        customImagePath: String?,
        paymentNetwork: String?,
        isPrimary: Boolean,
        isHidden: Boolean
    ) -> Unit,
    onSetBankHidden: (BankPaymentApp, Boolean) -> Unit,
    onOpenHiddenBanksSheet: () -> Unit,
    onCloseHiddenBanksSheet: () -> Unit,
    onShizukuPillClick: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onDismissShizukuDialog: () -> Unit,
    onOpenAddPassSheet: (LoyaltyCardEntity?) -> Unit,
    onCloseAddPassSheet: () -> Unit,
    onSavePass: (
        existingCard: LoyaltyCardEntity?,
        title: String,
        subtitle: String,
        notes: String?,
        barcodeValue: String,
        barcodeFormat: WaultBarcodeFormat,
        cardColorHex: Long,
        customImagePath: String?
    ) -> Unit,
    onDeletePass: (LoyaltyCardEntity) -> Unit,
    onMovePassToTop: (LoyaltyCardEntity) -> Unit,
    onReorderPasses: (List<LoyaltyCardEntity>) -> Unit,
    onOpenFullscreenPass: (LoyaltyCardEntity) -> Unit,
    onCloseFullscreenPass: () -> Unit,
    onToggleFavoriteBank: (BankPaymentApp) -> Unit = {}
) {
    var isUnifiedActionSheetVisible by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(WaultTab.ALL) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(WaultCardCategory.ALL) }

    val filteredBanks = remember(uiState.installedBanks, searchQuery, selectedCategory) {
        var list = uiState.installedBanks
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.displayTitle.contains(searchQuery, ignoreCase = true) ||
                    it.cardNumberSuffix?.contains(searchQuery) == true
            }
        }
        when (selectedCategory) {
            WaultCardCategory.ALL, WaultCardCategory.BANK_CREDIT -> list
            WaultCardCategory.TRANSIT -> list.filter {
                it.displayTitle.contains("kent", ignoreCase = true) ||
                    it.displayTitle.contains("kart", ignoreCase = true) ||
                    it.displayTitle.contains("ulaşım", ignoreCase = true) ||
                    it.packageName.contains("kentkart", ignoreCase = true)
            }
            WaultCardCategory.MEMBERSHIPS -> emptyList()
        }
    }

    val filteredPasses = remember(uiState.loyaltyCards, searchQuery, selectedCategory) {
        var list = uiState.loyaltyCards
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true) ||
                    it.barcodeValue.contains(searchQuery)
            }
        }
        when (selectedCategory) {
            WaultCardCategory.ALL -> list
            WaultCardCategory.BANK_CREDIT -> emptyList()
            WaultCardCategory.TRANSIT -> list.filter {
                it.title.contains("kart", ignoreCase = true) ||
                    it.title.contains("ulaşım", ignoreCase = true) ||
                    it.title.contains("bilet", ignoreCase = true) ||
                    it.title.contains("metro", ignoreCase = true) ||
                    it.title.contains("ego", ignoreCase = true)
            }
            WaultCardCategory.MEMBERSHIPS -> list.filterNot {
                it.title.contains("kentkart", ignoreCase = true) ||
                    it.title.contains("ulaşım", ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = WaultAmoledBlack,
        topBar = {
            WaultTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) searchQuery = ""
                },
                onQuickAddClick = { onOpenAddPassSheet(null) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Compact 60-Second Temporary NFC Switch Bar when active
                val showCountdown = uiState.tempSwitchSecondsRemaining != null
                val showQuickTrigger = !showCountdown &&
                    uiState.primaryBank != null &&
                    uiState.activeBank != null &&
                    uiState.primaryBank.componentName != uiState.activeBank.componentName

                AnimatedVisibility(
                    visible = showCountdown || showQuickTrigger,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TemporaryNfcSwitchBar(
                        primaryBank = uiState.primaryBank,
                        secondsRemaining = uiState.tempSwitchSecondsRemaining,
                        onStart60sCountdown = {
                            uiState.activeBank?.let(onActivateTemp60s)
                        },
                        onRevertNow = onRevertToPrimaryNow,
                        onCancelTimer = onCancelTempTimer,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val isForward = targetState.ordinal >= initialState.ordinal
                        val slideOffsetFraction = 0.16f
                        val durationIn = 300
                        val durationOut = 240

                        if (isForward) {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = durationIn, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> (fullWidth * slideOffsetFraction).toInt() }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            ) + scaleIn(
                                animationSpec = tween(durationMillis = durationIn, easing = FastOutSlowInEasing),
                                initialScale = 0.96f
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = durationOut, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> (-fullWidth * slideOffsetFraction).toInt() }
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                ) + scaleOut(
                                    animationSpec = tween(durationMillis = durationOut, easing = FastOutSlowInEasing),
                                    targetScale = 0.96f
                                )
                            )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = durationIn, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> (-fullWidth * slideOffsetFraction).toInt() }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            ) + scaleIn(
                                animationSpec = tween(durationMillis = durationIn, easing = FastOutSlowInEasing),
                                initialScale = 0.96f
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = durationOut, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> (fullWidth * slideOffsetFraction).toInt() }
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                ) + scaleOut(
                                    animationSpec = tween(durationMillis = durationOut, easing = FastOutSlowInEasing),
                                    targetScale = 0.96f
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "WaultTabTransition"
                ) { tab ->
                    when (tab) {
                        WaultTab.ALL -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Horizontal Category Pills (Tüm Kartlar, Banka & Kredi, Ulaşım, Üyelikler & Diğer)
                                CategoryFilterBar(
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = { selectedCategory = it }
                                )

                                PaymentStackSection(
                                    banks = filteredBanks,
                                    hiddenCount = uiState.hiddenBanks.size,
                                    activeBank = uiState.activeBank,
                                    isScanning = uiState.isScanningBanks,
                                    onSelectBank = onSelectBank,
                                    onEditBank = onOpenEditBankSheet,
                                    onActivateTemp60s = onActivateTemp60s,
                                    onReorderBanks = onReorderBanks,
                                    onToggleFavorite = onToggleFavoriteBank,
                                    onRefreshBanks = onRefreshBanks,
                                    onOpenHiddenBanks = onOpenHiddenBanksSheet,
                                    isScrollable = true,
                                    modifier = Modifier.weight(1f),
                                    bottomContent = {
                                        if (filteredPasses.isNotEmpty()) {
                                            PassGridSection(
                                                passes = filteredPasses,
                                                onPassClick = onOpenFullscreenPass,
                                                onPassEdit = { card -> onOpenAddPassSheet(card) },
                                                onAddPassClick = { onOpenAddPassSheet(null) }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        WaultTab.PAYMENT -> {
                            PaymentStackSection(
                                banks = uiState.installedBanks,
                                hiddenCount = uiState.hiddenBanks.size,
                                activeBank = uiState.activeBank,
                                isScanning = uiState.isScanningBanks,
                                onSelectBank = onSelectBank,
                                onEditBank = onOpenEditBankSheet,
                                onActivateTemp60s = onActivateTemp60s,
                                onReorderBanks = onReorderBanks,
                                onToggleFavorite = onToggleFavoriteBank,
                                onRefreshBanks = onRefreshBanks,
                                onOpenHiddenBanks = onOpenHiddenBanksSheet,
                                isScrollable = false,
                                modifier = Modifier.fillMaxSize(),
                                bottomContent = {}
                            )
                        }

                        WaultTab.OTHER -> {
                            WaultOtherSection(
                                capability = uiState.shizukuCapability,
                                hiddenBanksCount = uiState.hiddenBanks.size,
                                onOpenShizukuDialog = onShizukuPillClick,
                                onRequestShizukuPermission = onRequestShizukuPermission,
                                onOpenHiddenBanks = onOpenHiddenBanksSheet,
                                onRefreshBanks = onRefreshBanks,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Minimal Floating Notification Toast anchored directly above the Navigation Bar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp)
                    .padding(horizontal = 24.dp)
                    .zIndex(45f)
            ) { data ->
                MinimalStatusPill(message = data.visuals.message)
            }

            // Floating Frosted Glass Pill Dock anchored at BottomCenter
            WaultFloatingPill(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Unified Action Bottom Sheet triggered by '...' Menu button
    if (isUnifiedActionSheetVisible) {
        UnifiedQuickActionSheet(
            banks = uiState.installedBanks,
            hiddenCount = uiState.hiddenBanks.size,
            onDismiss = { isUnifiedActionSheetVisible = false },
            onAddLoyaltyPass = {
                isUnifiedActionSheetVisible = false
                onOpenAddPassSheet(null)
            },
            onCustomizeBank = { bank ->
                isUnifiedActionSheetVisible = false
                onOpenEditBankSheet(bank)
            },
            onOpenHiddenBanks = {
                isUnifiedActionSheetVisible = false
                onOpenHiddenBanksSheet()
            }
        )
    }

    // Full-Screen Optical Pass Modal
    uiState.selectedPassForFullscreen?.let { activePass ->
        FullscreenBarcodeDialog(
            card = activePass,
            onDismiss = onCloseFullscreenPass,
            onEdit = { cardToEdit -> onOpenAddPassSheet(cardToEdit) },
            onDelete = { cardToDelete -> onDeletePass(cardToDelete) }
        )
    }

    // Add / Edit Loyalty Pass Bottom Sheet
    if (uiState.isAddPassSheetVisible) {
        AddCardBottomSheet(
            existingCard = uiState.cardBeingEdited,
            onDismiss = onCloseAddPassSheet,
            onSave = onSavePass
        )
    }

    // Edit Bank Card Customization Bottom Sheet
    uiState.bankBeingEdited?.let { bankToEdit ->
        EditBankCardSheet(
            bank = bankToEdit,
            isActive = uiState.activeBank?.componentName == bankToEdit.componentName,
            onDismiss = onCloseEditBankSheet,
            onSetDefaultNow = onSelectBank,
            onActivateTemp60s = onActivateTemp60s,
            onSaveOverride = onSaveBankOverride
        )
    }

    // Hidden Banks Manager Sheet
    if (uiState.isHiddenBanksSheetVisible) {
        HiddenBanksBottomSheet(
            hiddenBanks = uiState.hiddenBanks,
            onRestoreBank = { bank -> onSetBankHidden(bank, false) },
            onDismiss = onCloseHiddenBanksSheet
        )
    }

    // Shizuku Status Dialog
    if (uiState.isShizukuDialogVisible) {
        ShizukuStatusDialog(
            capability = uiState.shizukuCapability,
            onRequestPermission = onRequestShizukuPermission,
            onDismiss = onDismissShizukuDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedQuickActionSheet(
    banks: List<BankPaymentApp>,
    hiddenCount: Int,
    onDismiss: () -> Unit,
    onAddLoyaltyPass: () -> Unit,
    onCustomizeBank: (BankPaymentApp) -> Unit,
    onOpenHiddenBanks: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WaultSurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hızlı İşlemler",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = WaultPrimaryMint.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    WaultPrimaryMint.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onAddLoyaltyPass)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = WaultPrimaryMint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Yeni Geçiş Kartı Ekle",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Kamera, Galeri veya Manuel Barkod",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = WaultPrimaryMint
                    )
                }
            }

            if (banks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Banka Kartlarını Özelleştir",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFF94A3B8)
                    )
                )

                banks.forEach { bank ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = WaultSurfaceContainerDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCustomizeBank(bank) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    if (bank.iconBitmap != null) {
                                        Image(
                                            bitmap = bank.iconBitmap,
                                            contentDescription = bank.displayTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountBalance,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = bank.displayTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Kartı Düzenle",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (hiddenCount > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WaultSurfaceContainerDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenHiddenBanks)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gizlenen Bankalar ($hiddenCount)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Text(
                            text = "Göster",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = WaultPrimaryMint
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemporaryNfcSwitchBar(
    primaryBank: BankPaymentApp?,
    secondsRemaining: Int?,
    onStart60sCountdown: () -> Unit,
    onRevertNow: () -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = WaultSurfaceContainerDark,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = WaultPrimaryMint.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (secondsRemaining != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = WaultPrimaryMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ana Karta dönüş: ${secondsRemaining}sn",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onRevertNow) {
                            Text("Şimdi Dön", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onCancelTimer) {
                            Text("İptal")
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { (secondsRemaining / 60f).coerceIn(0f, 1f) },
                    color = WaultPrimaryMint,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = WaultPrimaryMint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "60sn sonra ${primaryBank?.displayTitle ?: "Ana Kart"}'a dön",
                            style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(onClick = onStart60sCountdown) {
                        Text("Başlat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenBanksBottomSheet(
    hiddenBanks: List<BankPaymentApp>,
    onRestoreBank: (BankPaymentApp) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WaultSurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Gizlenen Bankalar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            hiddenBanks.forEach { bank ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WaultSurfaceContainerDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, WaultGlassBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (bank.iconBitmap != null) {
                                    Image(
                                        bitmap = bank.iconBitmap,
                                        contentDescription = bank.displayTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountBalance,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = bank.displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = { onRestoreBank(bank) },
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Göster")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShizukuStatusDialog(
    capability: SilentSwitchCapability,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val adbCommand = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WaultSurfaceDark,
        icon = {
            Icon(
                imageVector = if (capability.isSilentReady) Icons.Rounded.Bolt else Icons.Rounded.Security,
                contentDescription = null,
                tint = if (capability.isSilentReady) WaultStatusGreen else WaultStatusAmber
            )
        },
        title = {
            Text(
                text = capability.detailTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = capability.detailMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                )

                if (!capability.isSilentReady) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WaultSurfaceContainerDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, WaultGlassBorderDark, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ADB Komutu",
                                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(
                                            Context.CLIPBOARD_SERVICE
                                        ) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("ADB Komutu", adbCommand)
                                        )
                                        Toast.makeText(
                                            context,
                                            "ADB komutu kopyalandı",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "ADB Komutunu Kopyala",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = adbCommand,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (capability == SilentSwitchCapability.SHIZUKU_PERMISSION_NEEDED) {
                Button(
                    onClick = {
                        onRequestPermission()
                        onDismiss()
                    }
                ) {
                    Text("Shizuku İzni Ver")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Tamam")
                }
            }
        },
        dismissButton = {
            if (capability == SilentSwitchCapability.SHIZUKU_PERMISSION_NEEDED) {
                TextButton(onClick = onDismiss) {
                    Text("Kapat")
                }
            }
        }
    )
}

@Composable
private fun MinimalStatusPill(
    message: String,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
    val isError = message.contains("açılamadı", ignoreCase = true) || message.contains("hata", ignoreCase = true)
    val tintColor = if (isError) Color(0xFFFF453A) else Color(0xFF30D158)
    val icon = if (isError) Icons.Rounded.Security else Icons.Rounded.CheckCircle

    Surface(
        color = Color(0xFF1E1E22).copy(alpha = 0.95f),
        shape = pillShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.14f)
        ),
        shadowElevation = 14.dp,
        modifier = modifier.clip(pillShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.1).sp
                )
            )
        }
    }
}
