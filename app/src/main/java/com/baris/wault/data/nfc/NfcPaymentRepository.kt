package com.baris.wault.data.nfc

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-source-of-truth repository coordinating HCE bank discovery, live default NFC payment
 * component observation via ContentObserver, and Shizuku / fallback execution.
 */
class NfcPaymentRepository private constructor(private val appContext: Context) {

    val shizukuManager = ShizukuManager(appContext)
    private val scanner = NfcPaymentScanner(appContext)
    private val switchExecutor = NfcSwitchExecutor(appContext, shizukuManager)

    private val _installedBanks = MutableStateFlow<List<BankPaymentApp>>(emptyList())
    val installedBanks: StateFlow<List<BankPaymentApp>> = _installedBanks.asStateFlow()

    private val _activeComponentRaw = MutableStateFlow(readCurrentDefaultPaymentComponent())
    val activeComponentRaw: StateFlow<String?> = _activeComponentRaw.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val secureSettingsUri: Uri? = runCatching {
        Settings.Secure.getUriFor(NfcSwitchExecutor.NFC_PAYMENT_DEFAULT_COMPONENT_KEY)
    }.getOrNull()

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            refreshActiveComponent()
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            refreshActiveComponent()
        }
    }

    init {
        registerSecureSettingsObserver()
    }

    private fun registerSecureSettingsObserver() {
        val uri = secureSettingsUri ?: return
        runCatching {
            appContext.contentResolver.registerContentObserver(uri, false, settingsObserver)
        }
    }

    fun refreshActiveComponent(): String? {
        val current = readCurrentDefaultPaymentComponent()
        _activeComponentRaw.value = current
        return current
    }

    fun readCurrentDefaultPaymentComponent(): String? {
        return runCatching {
            Settings.Secure.getString(
                appContext.contentResolver,
                NfcSwitchExecutor.NFC_PAYMENT_DEFAULT_COMPONENT_KEY
            )?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    suspend fun refreshInstalledBanks(): List<BankPaymentApp> {
        _isScanning.value = true
        return try {
            val scanned = scanner.scanInstalledPaymentApps()
            _installedBanks.value = scanned
            refreshActiveComponent()
            shizukuManager.refreshStatus()
            scanned
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun switchToBank(bank: BankPaymentApp): NfcSwitchResult {
        return switchToComponent(bank.componentName)
    }

    suspend fun switchToComponent(componentName: ComponentName): NfcSwitchResult {
        val result = switchExecutor.switchDefaultPaymentApp(componentName)
        if (result is NfcSwitchResult.SwitchedSilently) {
            // Update state immediately for zero-latency UI transition and also verify from Settings.Secure
            _activeComponentRaw.value = componentName.flattenToString()
            refreshActiveComponent()
        }
        return result
    }

    fun getActiveBank(banks: List<BankPaymentApp> = _installedBanks.value): BankPaymentApp? {
        val activeRaw = _activeComponentRaw.value ?: readCurrentDefaultPaymentComponent()
        return banks.firstOrNull { it.matchesComponent(activeRaw) }
    }

    companion object {
        @Volatile
        private var INSTANCE: NfcPaymentRepository? = null

        fun getInstance(context: Context): NfcPaymentRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NfcPaymentRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
