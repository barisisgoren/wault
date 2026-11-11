package app.wault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Geliştirici modunda (Ayarlar > Geliştirici modu) kullanılan ayarlar. Kalıcı değildir; uygulama süreci kapanınca sıfırlanır.
 * adb ile komut gönderme (MainActivity.handle) yalnızca debug derlemede açıktır.
 */
object DevOptions {
    /**
     * true iken karta dokunmak telefonun gerçek ödeme ayarını değiştirmez; geçiş yalnızca uygulama içinde simüle edilir.
     * Arayüzü gerçek telefonda denerken kullanıcının varsayılan ödeme uygulaması bozulmasın diye var.
     */
    var dryRun by mutableStateOf(false)

    /** true iken kasa yerine örnek kartlar gösterilir ve hiçbir şey diske yazılmaz. */
    var demoActive by mutableStateOf(false)

    /** null: sistem temasını izle. */
    var darkOverride by mutableStateOf<Boolean?>(null)

    /** Deneme modunda varsayılan sanılan cüzdan uygulaması. null: gerçek durum geçerli. */
    var pretended by mutableStateOf<Pretended?>(null)

    data class Pretended(val packageName: String?)
}
