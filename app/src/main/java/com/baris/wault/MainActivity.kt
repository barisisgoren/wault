package com.baris.wault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baris.wault.ui.WaultMainScreen
import com.baris.wault.ui.WaultUiEvent
import com.baris.wault.ui.WaultViewModel
import com.baris.wault.ui.theme.WaultTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WaultViewModel by viewModels {
        WaultViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            WaultTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                val changeDefaultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.refreshActiveNfcStatus()
                }

                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is WaultUiEvent.LaunchFallbackNfcIntent -> {
                                runCatching {
                                    changeDefaultLauncher.launch(event.intent)
                                }.onFailure {
                                    snackbarHostState.showSnackbar(
                                        "${event.bankName} için NFC seçim ekranı açılamadı"
                                    )
                                }
                            }

                            is WaultUiEvent.ShowMessage -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                        }
                    }
                }

                WaultMainScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSelectBank = { bank -> viewModel.selectBankAsDefault(bank) },
                    onActivateTemp60s = viewModel::startTemporary60sSwitchTo,
                    onReorderBanks = viewModel::reorderBankCards,
                    onRevertToPrimaryNow = viewModel::revertToPrimaryNow,
                    onCancelTempTimer = { viewModel.cancelTemporaryCountdown(silent = false) },
                    onRefreshBanks = viewModel::refreshAll,
                    onOpenEditBankSheet = viewModel::openEditBankSheet,
                    onCloseEditBankSheet = viewModel::closeEditBankSheet,
                    onSaveBankOverride = { bank, title, suffix, color, imagePath, isPrimary, isHidden ->
                        viewModel.saveBankCardOverride(
                            bank = bank,
                            customTitle = title,
                            cardNumberSuffix = suffix,
                            customColorHex = color,
                            customImagePath = imagePath,
                            isPrimary = isPrimary,
                            isHidden = isHidden
                        )
                    },
                    onSetBankHidden = viewModel::setBankHidden,
                    onOpenHiddenBanksSheet = viewModel::openHiddenBanksSheet,
                    onCloseHiddenBanksSheet = viewModel::closeHiddenBanksSheet,
                    onShizukuPillClick = viewModel::onShizukuStatusPillClicked,
                    onRequestShizukuPermission = viewModel::requestShizukuPermissionFromDialog,
                    onDismissShizukuDialog = viewModel::dismissShizukuDialog,
                    onOpenAddPassSheet = viewModel::openAddPassSheet,
                    onCloseAddPassSheet = viewModel::closeAddPassSheet,
                    onSavePass = viewModel::saveLoyaltyCard,
                    onDeletePass = viewModel::deleteLoyaltyCard,
                    onMovePassToTop = viewModel::moveLoyaltyCardToTop,
                    onReorderPasses = viewModel::reorderLoyaltyCards,
                    onOpenFullscreenPass = viewModel::openFullscreenPass,
                    onCloseFullscreenPass = viewModel::closeFullscreenPass
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshActiveNfcStatus()
    }
}