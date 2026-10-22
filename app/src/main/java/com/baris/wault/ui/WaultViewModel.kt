 package com.baris.wault.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baris.wault.data.nfc.BankCardOverrideDao
import com.baris.wault.data.nfc.BankCardOverrideEntity
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.NfcPaymentRepository
import com.baris.wault.data.nfc.NfcSwitchResult
import com.baris.wault.data.nfc.SilentSwitchCapability
import com.baris.wault.data.vault.LoyaltyCardDao
import com.baris.wault.data.vault.LoyaltyCardEntity
import com.baris.wault.data.vault.WaultBarcodeFormat
import com.baris.wault.data.vault.WaultDatabase
import com.baris.wault.util.ImageStorageHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WaultUiState(
    val installedBanks: List<BankPaymentApp> = emptyList(),
    val hiddenBanks: List<BankPaymentApp> = emptyList(),
    val activeComponentRaw: String? = null,
    val activeBank: BankPaymentApp? = null,
    val primaryBank: BankPaymentApp? = null,
    val isScanningBanks: Boolean = true,
    val shizukuCapability: SilentSwitchCapability = SilentSwitchCapability.STANDARD_FALLBACK,
    val loyaltyCards: List<LoyaltyCardEntity> = emptyList(),
    val selectedPassForFullscreen: LoyaltyCardEntity? = null,
    val cardBeingEdited: LoyaltyCardEntity? = null,
    val bankBeingEdited: BankPaymentApp? = null,
    val isAddPassSheetVisible: Boolean = false,
    val isShizukuDialogVisible: Boolean = false,
    val isHiddenBanksSheetVisible: Boolean = false,
    val tempSwitchSecondsRemaining: Int? = null
)

sealed interface WaultUiEvent {
    data class LaunchFallbackNfcIntent(
        val intent: Intent,
        val bankName: String
    ) : WaultUiEvent

    data class ShowMessage(val message: String) : WaultUiEvent
}

private data class ModalState(
    val selectedPassForFullscreen: LoyaltyCardEntity? = null,
    val cardBeingEdited: LoyaltyCardEntity? = null,
    val bankBeingEditedComponent: String? = null,
    val isAddPassSheetVisible: Boolean = false,
    val isShizukuDialogVisible: Boolean = false,
    val isHiddenBanksSheetVisible: Boolean = false,
    val tempSwitchSecondsRemaining: Int? = null
)

class WaultViewModel(
    private val app: Application,
    private val nfcRepository: NfcPaymentRepository,
    private val loyaltyCardDao: LoyaltyCardDao,
    private val bankOverrideDao: BankCardOverrideDao
) : AndroidViewModel(app) {

    private val modalState = MutableStateFlow(ModalState())
    private val selectedBankComponent = MutableStateFlow<String?>(null)
    private var tempSwitchCountdownJob: Job? = null

    private val _events = MutableSharedFlow<WaultUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WaultUiEvent> = _events.asSharedFlow()

    private val nfcSnapshotFlow = combine(
        nfcRepository.installedBanks,
        bankOverrideDao.observeAllOverrides(),
        nfcRepository.activeComponentRaw,
        nfcRepository.isScanning,
        nfcRepository.shizukuManager.capability
    ) { rawBanks, overrides, activeRaw, isScanning, capability ->
        val overrideMap = overrides.associateBy { it.componentFlatten }
        val mergedBanks = rawBanks.mapIndexed { idx, bank ->
            val ov = overrideMap[bank.flattenedComponent]
                ?: overrideMap[bank.shortFlattenedComponent]
            bank.withOverride(ov ?: BankCardOverrideEntity(
                componentFlatten = bank.flattenedComponent,
                sortOrder = 1000 + idx
            ))
        }.sortedWith(
            compareByDescending<BankPaymentApp> { it.isFavorite }
                .thenBy { it.sortOrder }
                .thenByDescending { it.isPrimary }
                .thenBy { it.displayTitle.lowercase() }
        )

        val visible = mergedBanks.filterNot { it.isHidden }
        val hidden = mergedBanks.filter { it.isHidden }
        val active = mergedBanks.firstOrNull { it.matchesComponent(activeRaw) }
        val primary = mergedBanks.firstOrNull { it.isPrimary }

        NfcMergedSnapshot(
            allMerged = mergedBanks,
            visibleBanks = visible,
            hiddenBanks = hidden,
            activeRaw = activeRaw,
            activeBank = active,
            primaryBank = primary,
            isScanning = isScanning,
            capability = capability
        )
    }

    val uiState: StateFlow<WaultUiState> = combine(
        nfcSnapshotFlow,
        loyaltyCardDao.observeAllCards(),
        modalState,
        selectedBankComponent
    ) { nfc, cards, modals, manualSelectedComponent ->
        val syncedFullscreenPass = modals.selectedPassForFullscreen?.let { current ->
            cards.firstOrNull { it.id == current.id }
        }
        val syncedBankBeingEdited = modals.bankBeingEditedComponent?.let { comp ->
            nfc.allMerged.firstOrNull { it.flattenedComponent == comp }
        }
        val effectiveActiveBank = nfc.activeBank
            ?: manualSelectedComponent?.let { comp ->
                nfc.visibleBanks.firstOrNull { it.flattenedComponent == comp }
            }
            ?: nfc.visibleBanks.firstOrNull()

        WaultUiState(
            installedBanks = nfc.visibleBanks,
            hiddenBanks = nfc.hiddenBanks,
            activeComponentRaw = nfc.activeRaw,
            activeBank = effectiveActiveBank,
            primaryBank = nfc.primaryBank,
            isScanningBanks = nfc.isScanning,
            shizukuCapability = nfc.capability,
            loyaltyCards = cards,
            selectedPassForFullscreen = syncedFullscreenPass,
            cardBeingEdited = modals.cardBeingEdited,
            bankBeingEdited = syncedBankBeingEdited,
            isAddPassSheetVisible = modals.isAddPassSheetVisible,
            isShizukuDialogVisible = modals.isShizukuDialogVisible,
            isHiddenBanksSheetVisible = modals.isHiddenBanksSheetVisible,
            tempSwitchSecondsRemaining = modals.tempSwitchSecondsRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WaultUiState()
    )

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            nfcRepository.refreshInstalledBanks()
        }
    }

    fun refreshActiveNfcStatus() {
        nfcRepository.refreshActiveComponent()
        nfcRepository.shizukuManager.refreshStatus()
    }

    fun selectBankAsDefault(
        bank: BankPaymentApp,
        forceTemporary60s: Boolean = false,
        allowSystemFallback: Boolean = false
    ) {
        viewModelScope.launch {
            selectedBankComponent.value = bank.flattenedComponent
            // Move the tapped bank card to the top of the stack (sortOrder = 0)
            persistBankOrderWithTopCard(bank)

            val currentState = uiState.value
            val primary = currentState.primaryBank
            val isSystemAlreadyActive = currentState.installedBanks
                .firstOrNull { it.matchesComponent(currentState.activeComponentRaw) }
                ?.componentName == bank.componentName

            if (isSystemAlreadyActive && !forceTemporary60s) {
                return@launch
            }

            // Only attempt silent NFC switch if Shizuku or ADB WRITE_SECURE_SETTINGS is ready,
            // or if user explicitly requested system fallback dialog.
            val canSwitchSilently = nfcRepository.shizukuManager.isShizukuReady() ||
                nfcRepository.shizukuManager.hasWriteSecureSettingsPermission()

            if (!canSwitchSilently && !allowSystemFallback) {
                if (forceTemporary60s && primary != null && primary.componentName != bank.componentName) {
                    startTemporary60sCountdown(primaryBank = primary, tempBank = bank)
                }
                return@launch
            }

            when (val result = nfcRepository.switchToBank(bank)) {
                is NfcSwitchResult.SwitchedSilently -> {
                    val shouldStartTempTimer = forceTemporary60s ||
                        (primary != null && primary.componentName != bank.componentName)

                    if (shouldStartTempTimer && primary != null && primary.componentName != bank.componentName) {
                        startTemporary60sCountdown(primaryBank = primary, tempBank = bank)
                    } else {
                        cancelTemporaryCountdown(silent = true)
                        _events.emit(
                            WaultUiEvent.ShowMessage("${bank.displayTitle} varsayılan ödeme kartı yapıldı")
                        )
                    }
                }

                is NfcSwitchResult.RequiresSystemDialog -> {
                    if (allowSystemFallback) {
                        _events.emit(
                            WaultUiEvent.LaunchFallbackNfcIntent(
                                intent = result.fallbackIntent,
                                bankName = bank.displayTitle
                            )
                        )
                    }
                }

                is NfcSwitchResult.Error -> {
                    _events.emit(WaultUiEvent.ShowMessage(result.message))
                }
            }
        }
    }

    fun reorderBankCards(reorderedBanks: List<BankPaymentApp>) {
        viewModelScope.launch {
            val updatedOverrides = reorderedBanks.mapIndexed { index, bank ->
                val existing = bankOverrideDao.getOverride(bank.flattenedComponent)
                (existing ?: BankCardOverrideEntity(componentFlatten = bank.flattenedComponent))
                    .copy(sortOrder = index)
            }
            bankOverrideDao.upsertOverrides(updatedOverrides)
            reorderedBanks.firstOrNull()?.let { topBank ->
                selectedBankComponent.value = topBank.flattenedComponent
                val canSwitchSilently = nfcRepository.shizukuManager.isShizukuReady() ||
                    nfcRepository.shizukuManager.hasWriteSecureSettingsPermission()
                if (canSwitchSilently) {
                    nfcRepository.switchToBank(topBank)
                }
            }
        }
    }

    private suspend fun persistBankOrderWithTopCard(topBank: BankPaymentApp) {
        val currentVisible = uiState.value.installedBanks
        if (currentVisible.isEmpty()) return
        val newOrder = buildList {
            add(topBank)
            addAll(currentVisible.filter { it.componentName != topBank.componentName })
        }
        val updatedOverrides = newOrder.mapIndexed { index, item ->
            val existing = bankOverrideDao.getOverride(item.flattenedComponent)
            (existing ?: BankCardOverrideEntity(componentFlatten = item.flattenedComponent))
                .copy(sortOrder = index)
        }
        bankOverrideDao.upsertOverrides(updatedOverrides)
    }

    fun reorderLoyaltyCards(reorderedCards: List<LoyaltyCardEntity>) {
        viewModelScope.launch {
            val updated = reorderedCards.mapIndexed { index, card ->
                card.copy(sortOrder = index)
            }
            loyaltyCardDao.updateCards(updated)
        }
    }

    fun moveLoyaltyCardToTop(card: LoyaltyCardEntity) {
        viewModelScope.launch {
            val currentCards = uiState.value.loyaltyCards
            if (currentCards.isEmpty() || currentCards.first().id == card.id) return@launch
            val reordered = buildList {
                add(card)
                addAll(currentCards.filter { it.id != card.id })
            }.mapIndexed { index, item ->
                item.copy(sortOrder = index)
            }
            loyaltyCardDao.updateCards(reordered)
        }
    }

    fun startTemporary60sSwitchTo(bank: BankPaymentApp) {
        val currentPrimary = uiState.value.primaryBank
        val currentActive = uiState.value.activeBank
        viewModelScope.launch {
            if (currentPrimary == null && currentActive != null && currentActive.componentName != bank.componentName) {
                saveBankCardOverride(
                    bank = currentActive,
                    customTitle = currentActive.override?.customTitle,
                    cardNumberSuffix = currentActive.override?.cardNumberSuffix,
                    customColorHex = currentActive.override?.customColorHex,
                    customImagePath = currentActive.override?.customImagePath,
                    paymentNetwork = currentActive.override?.paymentNetwork,
                    isPrimary = true,
                    isHidden = false,
                    showToast = false
                )
            }
            selectBankAsDefault(bank = bank, forceTemporary60s = true)
        }
    }

    private fun startTemporary60sCountdown(
        primaryBank: BankPaymentApp,
        tempBank: BankPaymentApp
    ) {
        tempSwitchCountdownJob?.cancel()
        tempSwitchCountdownJob = viewModelScope.launch {
            _events.emit(
                WaultUiEvent.ShowMessage(
                    "${tempBank.displayTitle} 60 saniyeliğine aktif edildi"
                )
            )
            for (remaining in 60 downTo 1) {
                modalState.update { it.copy(tempSwitchSecondsRemaining = remaining) }
                delay(1_000L)
            }
            modalState.update { it.copy(tempSwitchSecondsRemaining = null) }

            selectedBankComponent.value = primaryBank.flattenedComponent
            persistBankOrderWithTopCard(primaryBank)
            val canSwitchSilently = nfcRepository.shizukuManager.isShizukuReady() ||
                nfcRepository.shizukuManager.hasWriteSecureSettingsPermission()
            if (canSwitchSilently) {
                when (val revertResult = nfcRepository.switchToBank(primaryBank)) {
                    is NfcSwitchResult.SwitchedSilently -> {
                        _events.emit(
                            WaultUiEvent.ShowMessage(
                                "Süre doldu • Ana Kart (${primaryBank.displayTitle}) tekrar aktif edildi"
                            )
                        )
                    }

                    is NfcSwitchResult.RequiresSystemDialog -> Unit

                    is NfcSwitchResult.Error -> {
                        _events.emit(WaultUiEvent.ShowMessage(revertResult.message))
                    }
                }
            } else {
                _events.emit(
                    WaultUiEvent.ShowMessage(
                        "Süre doldu • Ana Kart (${primaryBank.displayTitle}) seçildi"
                    )
                )
            }
        }
    }

    fun revertToPrimaryNow() {
        val primary = uiState.value.primaryBank ?: return
        cancelTemporaryCountdown(silent = true)
        selectBankAsDefault(bank = primary, forceTemporary60s = false)
    }

    fun cancelTemporaryCountdown(silent: Boolean = false) {
        tempSwitchCountdownJob?.cancel()
        tempSwitchCountdownJob = null
        modalState.update { it.copy(tempSwitchSecondsRemaining = null) }
        if (!silent) {
            viewModelScope.launch {
                val activeTitle = uiState.value.activeBank?.displayTitle ?: "Kart"
                _events.emit(
                    WaultUiEvent.ShowMessage("$activeTitle kalıcı olarak varsayılan bırakıldı")
                )
            }
        }
    }

    fun openEditBankSheet(bank: BankPaymentApp) {
        modalState.update {
            it.copy(bankBeingEditedComponent = bank.flattenedComponent)
        }
    }

    fun closeEditBankSheet() {
        modalState.update {
            it.copy(bankBeingEditedComponent = null)
        }
    }

    fun saveBankCardOverride(
        bank: BankPaymentApp,
        customTitle: String?,
        cardNumberSuffix: String?,
        customColorHex: Long?,
        customImagePath: String?,
        paymentNetwork: String?,
        isPrimary: Boolean,
        isHidden: Boolean,
        showToast: Boolean = true
    ) {
        viewModelScope.launch {
            val flattened = bank.flattenedComponent
            val previousOverride = bankOverrideDao.getOverride(flattened)

            if (previousOverride?.customImagePath != null &&
                previousOverride.customImagePath != customImagePath
            ) {
                ImageStorageHelper.deleteStoredImageIfOwned(app, previousOverride.customImagePath)
            }

            if (isPrimary) {
                bankOverrideDao.clearOtherPrimaryFlags(flattened)
            }

            val cleanSuffix = cardNumberSuffix
                ?.filter { it.isDigit() }
                ?.takeLast(4)
                ?.takeIf { it.isNotBlank() }

            val entity = BankCardOverrideEntity(
                componentFlatten = flattened,
                customTitle = customTitle?.trim()?.takeIf { it.isNotBlank() },
                cardNumberSuffix = cleanSuffix,
                customColorHex = customColorHex,
                customImagePath = customImagePath?.takeIf { it.isNotBlank() },
                paymentNetwork = paymentNetwork?.takeIf { it.isNotBlank() },
                isPrimary = isPrimary && !isHidden,
                isHidden = isHidden,
                sortOrder = previousOverride?.sortOrder ?: bank.sortOrder,
                isFavorite = previousOverride?.isFavorite ?: bank.isFavorite
            )
            bankOverrideDao.upsertOverride(entity)
            closeEditBankSheet()

            if (showToast) {
                val msg = if (isHidden) {
                    "${entity.customTitle ?: bank.appName} gizlendi"
                } else {
                    "${entity.customTitle ?: bank.appName} güncellendi"
                }
                _events.emit(WaultUiEvent.ShowMessage(msg))
            }
        }
    }

    fun toggleFavoriteBank(bank: BankPaymentApp) {
        viewModelScope.launch {
            val flattened = bank.flattenedComponent
            val prev = bankOverrideDao.getOverride(flattened)
                ?: bankOverrideDao.getOverride(bank.shortFlattenedComponent)
            val newFav = !(prev?.isFavorite ?: bank.isFavorite)
            val entity = (prev ?: BankCardOverrideEntity(
                componentFlatten = flattened,
                sortOrder = bank.sortOrder
            )).copy(isFavorite = newFav)
            bankOverrideDao.upsertOverride(entity)

            val msg = if (newFav) {
                "${bank.displayTitle} favorilere eklendi"
            } else {
                "${bank.displayTitle} favorilerden çıkarıldı"
            }
            _events.emit(WaultUiEvent.ShowMessage(msg))
        }
    }

    fun setBankHidden(bank: BankPaymentApp, hidden: Boolean) {
        val current = bank.override
        saveBankCardOverride(
            bank = bank,
            customTitle = current?.customTitle,
            cardNumberSuffix = current?.cardNumberSuffix,
            customColorHex = current?.customColorHex,
            customImagePath = current?.customImagePath,
            paymentNetwork = current?.paymentNetwork,
            isPrimary = if (hidden) false else (current?.isPrimary == true),
            isHidden = hidden
        )
    }

    fun openHiddenBanksSheet() {
        modalState.update { it.copy(isHiddenBanksSheetVisible = true) }
    }

    fun closeHiddenBanksSheet() {
        modalState.update { it.copy(isHiddenBanksSheetVisible = false) }
    }

    fun onShizukuStatusPillClicked() {
        val currentCapability = nfcRepository.shizukuManager.refreshStatus()
        if (currentCapability == SilentSwitchCapability.SHIZUKU_PERMISSION_NEEDED) {
            val requested = nfcRepository.shizukuManager.requestShizukuPermission()
            if (!requested) {
                modalState.update { it.copy(isShizukuDialogVisible = true) }
            }
        } else {
            modalState.update { it.copy(isShizukuDialogVisible = true) }
        }
    }

    fun requestShizukuPermissionFromDialog() {
        val requested = nfcRepository.shizukuManager.requestShizukuPermission()
        if (!requested) {
            viewModelScope.launch {
                _events.emit(
                    WaultUiEvent.ShowMessage("Önce Shizuku uygulamasını açıp servisin çalıştığından emin olun.")
                )
            }
        }
    }

    fun dismissShizukuDialog() {
        modalState.update { it.copy(isShizukuDialogVisible = false) }
    }

    fun openAddPassSheet(cardToEdit: LoyaltyCardEntity? = null) {
        modalState.update {
            it.copy(
                cardBeingEdited = cardToEdit,
                isAddPassSheetVisible = true
            )
        }
    }

    fun closeAddPassSheet() {
        modalState.update {
            it.copy(
                cardBeingEdited = null,
                isAddPassSheetVisible = false
            )
        }
    }

    fun openFullscreenPass(card: LoyaltyCardEntity) {
        modalState.update {
            it.copy(selectedPassForFullscreen = card)
        }
    }

    fun closeFullscreenPass() {
        modalState.update {
            it.copy(selectedPassForFullscreen = null)
        }
    }

    fun saveLoyaltyCard(
        existingCard: LoyaltyCardEntity?,
        title: String,
        subtitle: String,
        notes: String?,
        barcodeValue: String,
        barcodeFormat: WaultBarcodeFormat,
        cardColorHex: Long,
        customImagePath: String?
    ) {
        val cleanTitle = title.trim()
        val cleanSubtitle = subtitle.trim()
        val cleanNotes = notes?.trim()?.takeIf { it.isNotBlank() }
        val cleanBarcode = barcodeValue.trim()
        if (cleanTitle.isEmpty() || cleanBarcode.isEmpty()) return

        viewModelScope.launch {
            if (existingCard != null) {
                if (existingCard.customImagePath != null &&
                    existingCard.customImagePath != customImagePath
                ) {
                    ImageStorageHelper.deleteStoredImageIfOwned(app, existingCard.customImagePath)
                }
                val updated = existingCard.copy(
                    title = cleanTitle,
                    subtitle = cleanSubtitle,
                    notes = cleanNotes,
                    barcodeValue = cleanBarcode,
                    barcodeFormat = barcodeFormat,
                    cardColorHex = cardColorHex,
                    customImagePath = customImagePath
                )
                loyaltyCardDao.updateCard(updated)
                _events.emit(WaultUiEvent.ShowMessage("\"$cleanTitle\" kartı güncellendi"))
            } else {
                // New card goes to the top or next sortOrder
                val nextOrder = loyaltyCardDao.getNextSortOrder()
                val newCard = LoyaltyCardEntity(
                    title = cleanTitle,
                    subtitle = cleanSubtitle,
                    notes = cleanNotes,
                    barcodeValue = cleanBarcode,
                    barcodeFormat = barcodeFormat,
                    cardColorHex = cardColorHex,
                    customImagePath = customImagePath,
                    sortOrder = nextOrder
                )
                loyaltyCardDao.insertCard(newCard)
                _events.emit(WaultUiEvent.ShowMessage("\"$cleanTitle\" cüzdana eklendi"))
            }
            closeAddPassSheet()
        }
    }

    fun deleteLoyaltyCard(card: LoyaltyCardEntity) {
        viewModelScope.launch {
            ImageStorageHelper.deleteStoredImageIfOwned(app, card.customImagePath)
            loyaltyCardDao.deleteCard(card)
            modalState.update { current ->
                if (current.selectedPassForFullscreen?.id == card.id) {
                    current.copy(selectedPassForFullscreen = null)
                } else {
                    current
                }
            }
            _events.emit(WaultUiEvent.ShowMessage("\"${card.title}\" kartı silindi"))
        }
    }

    private data class NfcMergedSnapshot(
        val allMerged: List<BankPaymentApp>,
        val visibleBanks: List<BankPaymentApp>,
        val hiddenBanks: List<BankPaymentApp>,
        val activeRaw: String?,
        val activeBank: BankPaymentApp?,
        val primaryBank: BankPaymentApp?,
        val isScanning: Boolean,
        val capability: SilentSwitchCapability
    )

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val nfcRepo = NfcPaymentRepository.getInstance(application)
                    val db = WaultDatabase.getInstance(application)
                    return WaultViewModel(
                        app = application,
                        nfcRepository = nfcRepo,
                        loyaltyCardDao = db.loyaltyCardDao(),
                        bankOverrideDao = db.bankCardOverrideDao()
                    ) as T
                }
            }
    }
}
