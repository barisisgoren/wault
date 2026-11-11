package app.wault.nfc

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.delay
import app.wault.BuildConfig
import app.wault.DevOptions
import app.wault.data.PaymentLink
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

/** Telefonda kurulu, "payment" kategorisinde AID kaydı olan bir NFC servisi. */
data class PaymentService(
    val component: ComponentName,
    val appLabel: String,
) {
    fun toLink() = PaymentLink(component.packageName, component.className, appLabel)
}

object PaymentServices {
    private const val HOST = "android.nfc.cardemulation.action.HOST_APDU_SERVICE"
    private const val OFF_HOST = "android.nfc.cardemulation.action.OFF_HOST_APDU_SERVICE"
    private const val HOST_META = "android.nfc.cardemulation.host_apdu_service"
    private const val OFF_HOST_META = "android.nfc.cardemulation.off_host_apdu_service"
    private const val ATTR_CATEGORY = 0x010103e8

    /** Kullanıcıya kart olarak sunulmayacak sistem servisleri (ör. SIM cüzdanı yer tutucusu). */
    private val hiddenPackages = setOf("com.android.nfc")

    /** Uygulama adı yerine daha tanıdık gelen adlar. */
    private val friendlyNames = mapOf("com.google.android.gms" to "Google Cüzdan")

    fun list(context: Context): List<PaymentService> {
        val pm = context.packageManager
        val out = LinkedHashMap<ComponentName, PaymentService>()
        for ((action, meta) in listOf(HOST to HOST_META, OFF_HOST to OFF_HOST_META)) {
            val found = runCatching { pm.queryIntentServices(Intent(action), PackageManager.GET_META_DATA) }
                .getOrDefault(emptyList())
            for (ri in found) {
                val si = ri.serviceInfo ?: continue
                if (si.packageName == context.packageName || si.packageName in hiddenPackages) continue
                val parser = runCatching { si.loadXmlMetaData(pm, meta) }.getOrNull() ?: continue
                val res = runCatching { pm.getResourcesForApplication(si.applicationInfo) }.getOrNull()
                val isPayment = runCatching {
                    hasPaymentGroup(parser) { id -> res?.getString(id) }
                }.getOrDefault(false)
                parser.close()
                val cn = ComponentName(si.packageName, si.name)
                if (!isPayment) continue
                val label = friendlyNames[si.packageName]
                    ?: runCatching { pm.getApplicationLabel(si.applicationInfo).toString() }.getOrDefault(si.packageName)
                out[cn] = PaymentService(cn, label)
            }
        }
        return out.values.sortedBy { it.appLabel.lowercase() }
    }

    /**
     * aid-group'lardan biri "payment" kategorisindeyse true.
     * Kategori düz yazı ya da başka bir uygulamanın string kaynağına referans (@string/...) olabilir.
     */
    fun hasPaymentGroup(parser: XmlPullParser, resolveString: (Int) -> String? = { null }): Boolean {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "aid-group") {
                var category: String? = null
                for (i in 0 until parser.attributeCount) {
                    val byName = parser.getAttributeName(i) == "category"
                    // Kod karartıcılar öznitelik adlarını siler; android:category kaynak numarası 0x010103e8 kalır
                    val byId = parser is XmlResourceParser && parser.getAttributeNameResource(i) == ATTR_CATEGORY
                    if (byName || byId) { category = parser.getAttributeValue(i); break }
                }
                if (category != null && category.startsWith("@")) {
                    category = category.drop(1).toIntOrNull()?.let(resolveString) ?: category
                }
                if (category == CardEmulation.CATEGORY_PAYMENT) return true
            }
            event = parser.next()
        }
        return false
    }
}

sealed interface SwitchOutcome {
    data object AlreadyDefault : SwitchOutcome
    /** Shizuku ile ekran açılmadan değiştirildi. */
    data object Silent : SwitchOutcome
    /** Sistem onayı açıldı; dönüşte varsayılan yeniden kontrol edilmeli. */
    data object AskedSystem : SwitchOutcome
    /** Doğrudan onay yok; ödeme ayarları açıldı. */
    data object OpenedSettings : SwitchOutcome
    data object NfcUnavailable : SwitchOutcome
    data object NfcOff : SwitchOutcome
}

/**
 * Varsayılan NFC ödeme yöntemini değiştirir.
 * Sıradan bir uygulama bunu sessizce yapamaz (Android 15+ cüzdan rolü). Shizuku hazırsa rol komutuyla ekran
 * açmadan değiştirilir; değilse Android'in sistem seçicisi açılır.
 */
object PaymentSwitcher {

    private fun emulation(context: Context): CardEmulation? =
        NfcAdapter.getDefaultAdapter(context)?.let { runCatching { CardEmulation.getInstance(it) }.getOrNull() }

    fun isDefault(context: Context, link: PaymentLink): Boolean {
        // Deneme modunda (yalnızca debug) sistem değil, simüle edilen durum gösterilir.
        DevOptions.pretended?.let { return it.packageName == link.packageName }
        return runCatching {
            emulation(context)?.isDefaultServiceForCategory(
                ComponentName(link.packageName, link.serviceClass), CardEmulation.CATEGORY_PAYMENT,
            ) == true
        }.getOrDefault(false)
    }

    /** Önce Shizuku ile sessiz geçiş, olmazsa sistem seçicisi. */
    suspend fun switchTo(activity: Activity, link: PaymentLink): SwitchOutcome {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return SwitchOutcome.NfcUnavailable
        if (!adapter.isEnabled) return SwitchOutcome.NfcOff
        if (isDefault(activity, link)) return SwitchOutcome.AlreadyDefault
        if (BuildConfig.DEBUG && DevOptions.dryRun) {
            DevOptions.pretended = DevOptions.Pretended(link.packageName)
            return SwitchOutcome.Silent
        }
        if (ShizukuBridge.state(activity) == ShizukuBridge.State.READY && ShizukuBridge.setWalletHolder(link.packageName)) {
            repeat(12) {
                if (isDefault(activity, link)) return SwitchOutcome.Silent
                delay(200)
            }
        }
        return request(activity, link)
    }

    fun request(activity: Activity, link: PaymentLink): SwitchOutcome {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return SwitchOutcome.NfcUnavailable
        if (!adapter.isEnabled) return SwitchOutcome.NfcOff
        if (isDefault(activity, link)) return SwitchOutcome.AlreadyDefault
        if (BuildConfig.DEBUG && DevOptions.dryRun) return SwitchOutcome.AskedSystem
        val change = Intent(CardEmulation.ACTION_CHANGE_DEFAULT)
            .putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT)
            .putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, ComponentName(link.packageName, link.serviceClass))
        return try {
            activity.startActivity(change)
            SwitchOutcome.AskedSystem
        } catch (e: ActivityNotFoundException) {
            Log.i("Wault", "Varsayılan değiştirme onayı yok, ayarlar açılıyor")
            openPaymentSettings(activity)
            SwitchOutcome.OpenedSettings
        } catch (e: SecurityException) {
            openPaymentSettings(activity)
            SwitchOutcome.OpenedSettings
        }
    }

    fun openPaymentSettings(activity: Activity) {
        val intents = listOf(
            Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS),
            Intent(Settings.ACTION_NFC_SETTINGS),
        )
        for (i in intents) {
            if (runCatching { activity.startActivity(i) }.isSuccess) return
        }
    }

    fun openNfcSettings(activity: Activity) {
        runCatching { activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
    }
}
