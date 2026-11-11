package app.wault.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WaultPrefs(
    val biometricLock: Boolean = false,
    /** Ayarlar > Geliştirici modu: açıkken geliştirici araçları görünür. */
    val developerMode: Boolean = false,
    val replaceBankNotification: Boolean = false,
    val onboarded: Boolean = false,
    /** Vitrin kartı seçilince (Shizuku hazırsa) varsayılan ödeme uygulamasını kaldır. */
    val showcaseTurnsOffPayment: Boolean = true,
    /** Ödemeyi kapatmadan önceki cüzdan uygulaması; "Ödemeyi geri aç" bunu geri verir. */
    val lastWalletPackage: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Kartlar için varsayılan yüzey; kartın kendi stili varsa o geçerli. */
    val cardStyle: CardStyle = CardStyle.FLAT,
    val accent: AccentChoice = AccentChoice.BLUE,
    /** Seçili kartın zemine yayılan ışığı ve gradyan kartlarda eğimle parlama. Varsayılan kapalı. */
    val lightEffects: Boolean = false,
)

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wault", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<WaultPrefs> = _state.asStateFlow()

    private fun read() = WaultPrefs(
        biometricLock = sp.getBoolean("biometricLock", false),
        developerMode = sp.getBoolean("developerMode", false),
        replaceBankNotification = sp.getBoolean("replaceBankNotification", false),
        onboarded = sp.getBoolean("onboarded", false),
        showcaseTurnsOffPayment = sp.getBoolean("showcaseTurnsOffPayment", true),
        lastWalletPackage = sp.getString("lastWalletPackage", "") ?: "",
        themeMode = enumOr(sp.getString("themeMode", null), ThemeMode.SYSTEM),
        cardStyle = enumOr(sp.getString("cardStyle", null), CardStyle.FLAT),
        accent = enumOr(sp.getString("accent", null), AccentChoice.BLUE),
        lightEffects = sp.getBoolean("lightEffects", false),
    )

    fun update(block: (WaultPrefs) -> WaultPrefs) {
        val next = block(_state.value)
        sp.edit()
            .putBoolean("biometricLock", next.biometricLock)
            .putBoolean("developerMode", next.developerMode)
            .putBoolean("replaceBankNotification", next.replaceBankNotification)
            .putBoolean("onboarded", next.onboarded)
            .putBoolean("showcaseTurnsOffPayment", next.showcaseTurnsOffPayment)
            .putString("lastWalletPackage", next.lastWalletPackage)
            .putString("themeMode", next.themeMode.name)
            .putString("cardStyle", next.cardStyle.name)
            .putString("accent", next.accent.name)
            .putBoolean("lightEffects", next.lightEffects)
            .apply()
        _state.value = next
    }
}
