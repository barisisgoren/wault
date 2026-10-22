package com.baris.wault.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.baris.wault.data.nfc.BankPaymentApp

@Composable
fun PaymentStackSection(
    banks: List<BankPaymentApp>,
    hiddenCount: Int = 0,
    activeBank: BankPaymentApp?,
    isScanning: Boolean = false,
    onSelectBank: (BankPaymentApp) -> Unit,
    onEditBank: (BankPaymentApp) -> Unit,
    onActivateTemp60s: (BankPaymentApp) -> Unit,
    onReorderBanks: (List<BankPaymentApp>) -> Unit,
    onRefreshBanks: () -> Unit = {},
    onOpenHiddenBanks: () -> Unit = {},
    isScrollable: Boolean = true,
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {}
) {
    AppleCardStack(
        banks = banks,
        activeBank = activeBank,
        onSelectBank = onSelectBank,
        onEditBank = onEditBank,
        onActivateTemp60s = onActivateTemp60s,
        onReorderBanks = onReorderBanks,
        isScrollable = isScrollable,
        modifier = modifier,
        bottomContent = bottomContent
    )
}
