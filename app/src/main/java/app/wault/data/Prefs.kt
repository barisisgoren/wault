package app.wault.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WaultPrefs(
    val biometricLock: Boolean = false,
    val replaceBankNotification: Boolean = false,
    val onboarded: Boolean = false,
    /** Vitrin kartı seçilince (Shizuku hazırsa) varsayılan ödeme uygulamasını kaldır. */
    val showcaseTurnsOffPayment: Boolean = true,
    /** Ödemeyi kapatmadan önceki cüzdan uygulaması; "Ödemeyi geri aç" bunu geri verir. */
    val lastWalletPackage: String = "",
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wault", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<WaultPrefs> = _state.asStateFlow()

    private fun read() = WaultPrefs(
        biometricLock = sp.getBoolean("biometricLock", false),
        replaceBankNotification = sp.getBoolean("replaceBankNotification", false),
        onboarded = sp.getBoolean("onboarded", false),
        showcaseTurnsOffPayment = sp.getBoolean("showcaseTurnsOffPayment", true),
        lastWalletPackage = sp.getString("lastWalletPackage", "") ?: "",
    )

    fun update(block: (WaultPrefs) -> WaultPrefs) {
        val next = block(_state.value)
        sp.edit()
            .putBoolean("biometricLock", next.biometricLock)
            .putBoolean("replaceBankNotification", next.replaceBankNotification)
            .putBoolean("onboarded", next.onboarded)
            .putBoolean("showcaseTurnsOffPayment", next.showcaseTurnsOffPayment)
            .putString("lastWalletPackage", next.lastWalletPackage)
            .apply()
        _state.value = next
    }
}
