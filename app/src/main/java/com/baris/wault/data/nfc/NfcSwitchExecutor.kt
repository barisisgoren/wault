package com.baris.wault.data.nfc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.nfc.cardemulation.CardEmulation
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface NfcSwitchResult {
    data class SwitchedSilently(
        val componentName: ComponentName,
        val viaShizuku: Boolean
    ) : NfcSwitchResult

    data class RequiresSystemDialog(
        val componentName: ComponentName,
        val fallbackIntent: Intent
    ) : NfcSwitchResult

    data class Error(val message: String) : NfcSwitchResult
}

class NfcSwitchExecutor(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    companion object {
        const val NFC_PAYMENT_DEFAULT_COMPONENT_KEY = "nfc_payment_default_component"
    }

    /**
     * Switches Android's default contactless payment HCE service:
     * 1. Primary Path (Shizuku): Executes `settings put secure nfc_payment_default_component <flattened>`
     * 2. Secondary Path (Direct Permission): Uses `Settings.Secure.putString` if `WRITE_SECURE_SETTINGS` is held
     * 3. Fallback Path (Unprivileged): Builds `Intent(CardEmulation.ACTION_CHANGE_DEFAULT)`
     */
    suspend fun switchDefaultPaymentApp(componentName: ComponentName): NfcSwitchResult =
        withContext(Dispatchers.IO) {
            val flattened = componentName.flattenToString()

            // Primary Path: Shizuku shell execution
            if (shizukuManager.isShizukuReady()) {
                val cmd = arrayOf(
                    "settings",
                    "put",
                    "secure",
                    NFC_PAYMENT_DEFAULT_COMPONENT_KEY,
                    flattened
                )
                val shellResult = shizukuManager.executeCommand(cmd)
                if (shellResult.isSuccess) {
                    return@withContext NfcSwitchResult.SwitchedSilently(
                        componentName = componentName,
                        viaShizuku = true
                    )
                }
            }

            // Secondary Path: Direct WRITE_SECURE_SETTINGS via ADB
            if (shizukuManager.hasWriteSecureSettingsPermission()) {
                val wrote = runCatching {
                    Settings.Secure.putString(
                        context.contentResolver,
                        NFC_PAYMENT_DEFAULT_COMPONENT_KEY,
                        flattened
                    )
                }.getOrDefault(false)

                if (wrote) {
                    return@withContext NfcSwitchResult.SwitchedSilently(
                        componentName = componentName,
                        viaShizuku = false
                    )
                }
            }

            // Fallback Path: Standard Android CardEmulation intent
            val fallbackIntent = buildFallbackChangeDefaultIntent(componentName)
            NfcSwitchResult.RequiresSystemDialog(
                componentName = componentName,
                fallbackIntent = fallbackIntent
            )
        }

    @Suppress("DEPRECATION")
    fun buildFallbackChangeDefaultIntent(componentName: ComponentName): Intent {
        return Intent(CardEmulation.ACTION_CHANGE_DEFAULT).apply {
            putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT)
            putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, componentName)
        }
    }
}
