package com.baris.wault.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.data.vault.WaultBarcodeFormat

@Composable
fun MainScreen(
    uiState: WaultUiState,
    snackbarHostState: SnackbarHostState,
    onSelectBank: (BankPaymentApp) -> Unit,
    onActivateTemp60s: (BankPaymentApp) -> Unit,
    onReorderBanks: (List<BankPaymentApp>) -> Unit = {},
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
    onMovePassToTop: (LoyaltyCardEntity) -> Unit = {},
    onReorderPasses: (List<LoyaltyCardEntity>) -> Unit = {},
    onOpenFullscreenPass: (LoyaltyCardEntity) -> Unit,
    onCloseFullscreenPass: () -> Unit
) {
    WaultMainScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSelectBank = onSelectBank,
        onActivateTemp60s = onActivateTemp60s,
        onReorderBanks = onReorderBanks,
        onRevertToPrimaryNow = onRevertToPrimaryNow,
        onCancelTempTimer = onCancelTempTimer,
        onRefreshBanks = onRefreshBanks,
        onOpenEditBankSheet = onOpenEditBankSheet,
        onCloseEditBankSheet = onCloseEditBankSheet,
        onSaveBankOverride = onSaveBankOverride,
        onSetBankHidden = onSetBankHidden,
        onOpenHiddenBanksSheet = onOpenHiddenBanksSheet,
        onCloseHiddenBanksSheet = onCloseHiddenBanksSheet,
        onShizukuPillClick = onShizukuPillClick,
        onRequestShizukuPermission = onRequestShizukuPermission,
        onDismissShizukuDialog = onDismissShizukuDialog,
        onOpenAddPassSheet = onOpenAddPassSheet,
        onCloseAddPassSheet = onCloseAddPassSheet,
        onSavePass = onSavePass,
        onDeletePass = onDeletePass,
        onMovePassToTop = onMovePassToTop,
        onReorderPasses = onReorderPasses,
        onOpenFullscreenPass = onOpenFullscreenPass,
        onCloseFullscreenPass = onCloseFullscreenPass
    )
}
