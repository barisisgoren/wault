package com.baris.wault.data.nfc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.nfc.cardemulation.HostApduService
import android.nfc.cardemulation.OffHostApduService
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

data class BankPaymentApp(
    val appName: String,
    val packageName: String,
    val serviceName: String,
    val serviceDescription: String?,
    val componentName: ComponentName,
    val iconDrawable: Drawable?,
    val iconBitmap: ImageBitmap?,
    val primaryColorArgb: Int,
    val secondaryColorArgb: Int,
    val override: BankCardOverrideEntity? = null
) {
    val flattenedComponent: String
        get() = componentName.flattenToString()

    val shortFlattenedComponent: String
        get() = componentName.flattenToShortString()

    val displayTitle: String
        get() = override?.customTitle?.takeIf { it.isNotBlank() } ?: appName

    val cardNumberSuffix: String?
        get() = override?.cardNumberSuffix?.takeIf { it.isNotBlank() }

    val customColorHex: Long?
        get() = override?.customColorHex

    val customImagePath: String?
        get() = override?.customImagePath?.takeIf { it.isNotBlank() }

    val paymentNetwork: BankPaymentNetwork
        get() = BankPaymentNetwork.fromCode(override?.paymentNetwork)

    val isPrimary: Boolean
        get() = override?.isPrimary == true

    val isHidden: Boolean
        get() = override?.isHidden == true

    val sortOrder: Int
        get() = override?.sortOrder ?: 0

    fun withOverride(newOverride: BankCardOverrideEntity?): BankPaymentApp {
        return copy(override = newOverride)
    }

    fun matchesComponent(rawComponentString: String?): Boolean {
        if (rawComponentString.isNullOrBlank()) return false
        val parsed = ComponentName.unflattenFromString(rawComponentString.trim())
        return if (parsed != null) {
            parsed.packageName == componentName.packageName &&
                parsed.className == componentName.className
        } else {
            rawComponentString.trim() == flattenedComponent ||
                rawComponentString.trim() == shortFlattenedComponent
        }
    }
}

class NfcPaymentScanner(private val context: Context) {

    companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private const val TAG_AID_GROUP = "aid-group"
        private const val ATTR_CATEGORY = "category"
        private const val ATTR_DESCRIPTION = "description"
        private const val CATEGORY_PAYMENT = "payment"
        private const val CATEGORY_OTHER = "other"

        // Known non-payment / non-wallet system services that register HCE for Bluetooth/Nearby/FIDO/CarKey
        private val EXCLUDED_SERVICE_KEYWORDS = listOf(
            "com.google.android.gms.nearby",
            "com.google.android.gms.fido",
            "com.google.android.gms.dck",
            "com.google.android.gms.pay.hce.service.PayHceService",
            "com.yandex.go.movista.nfc"
        )

        private val EXCLUDED_PACKAGES = setOf(
            "com.android.nfc",
            "com.android.se",
            "com.google.android.nfc",
            "ru.yandex.taxi"
        )

        // Known Turkish banking, fintech, and transit package prefixes/keywords
        private val KNOWN_WALLET_OR_BANK_KEYWORDS = listOf(
            "akbank", "axess", "juzdan",
            "garanti", "bonus",
            "iscep", "isbank", "maximum", "nays", "pozitron", "softtech",
            "ykb", "yapikredi", "world",
            "ziraat", "vakif", "halkbank", "tmobtech",
            "finansbank", "qnb", "enpara",
            "denizbank", "mobildeniz",
            "kuveytturk", "turkiyefinans", "albaraka", "emlakkatilim", "vakifkatilim", "ziraatkatilim",
            "teb", "cepteteb", "ingbank", "odeabank", "fibabanka", "hsbc", "anadolubank", "sekerbank", "burgan", "onmobil",
            "papara", "paycell", "tosla", "hadi", "param", "ininal", "fastpay", "pokus", "sipay", "ozan", "oldubil", "bkm", "passo",
            "kentkart", "istanbulkart", "belbim", "eshot", "izmirim", "ankarakart", "ego", "bursakart", "antalyakart", "kocaelikart",
            "tapandpay", "wallet", "samsung.android.spay"
        )
    }

    /**
     * Scans installed applications for HostApduService and OffHostApduService implementations,
     * including both `category="payment"` and Turkish banking/transit apps using `category="other"`
     * or dynamic AID groups.
     */
    suspend fun scanInstalledPaymentApps(): List<BankPaymentApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val candidateServices = LinkedHashMap<ComponentName, ServiceInfo>()

        // 1. Query both HOST_APDU_SERVICE and OFF_HOST_APDU_SERVICE intents (including disabled components)
        val actions = listOf(
            HostApduService.SERVICE_INTERFACE,
            OffHostApduService.SERVICE_INTERFACE
        )
        val flags = PackageManager.GET_META_DATA or
            PackageManager.GET_RESOLVED_FILTER or
            PackageManager.MATCH_DISABLED_COMPONENTS

        for (action in actions) {
            val queryIntent = Intent(action)
            val resolveInfos: List<ResolveInfo> = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentServices(
                        queryIntent,
                        PackageManager.ResolveInfoFlags.of(flags.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentServices(queryIntent, flags)
                }
            } catch (_: Throwable) {
                emptyList()
            }

            for (resolveInfo in resolveInfos) {
                val serviceInfo = resolveInfo.serviceInfo ?: continue
                val comp = ComponentName(serviceInfo.packageName, serviceInfo.name)
                candidateServices.putIfAbsent(comp, serviceInfo)
            }
        }

        // 2. Fallback package scan for banking apps whose HCE service might not match standard intent filters
        try {
            val pkgFlags = PackageManager.GET_SERVICES or
                PackageManager.GET_META_DATA or
                PackageManager.MATCH_DISABLED_COMPONENTS
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(pkgFlags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(pkgFlags)
            }

            for (pkg in packages) {
                val pkgName = pkg.packageName ?: continue
                if (pkgName == context.packageName || pkgName in EXCLUDED_PACKAGES) continue
                val services = pkg.services ?: continue
                for (svc in services) {
                    val comp = ComponentName(svc.packageName, svc.name)
                    if (comp in candidateServices) continue
                    val hasHcePermission = svc.permission == "android.permission.BIND_NFC_SERVICE"
                    val hasHceMeta = svc.metaData?.keySet()?.any { key ->
                        key.contains("apdu", ignoreCase = true) || key.contains("hce", ignoreCase = true)
                    } == true
                    if (hasHcePermission || hasHceMeta) {
                        candidateServices[comp] = svc
                    }
                }
            }
        } catch (_: Throwable) {
        }

        val discovered = mutableListOf<BankPaymentApp>()

        for ((componentName, serviceInfo) in candidateServices) {
            val pkgName = serviceInfo.packageName
            val svcName = serviceInfo.name
            if (pkgName == context.packageName || pkgName in EXCLUDED_PACKAGES) continue
            if (EXCLUDED_SERVICE_KEYWORDS.any { svcName.contains(it, ignoreCase = true) }) continue

            val parsedMeta = parsePaymentAidMetadata(pm, serviceInfo)
            val isKnownBankOrWallet = isKnownPaymentOrTransitApp(pkgName, svcName, parsedMeta.description)

            // Include if:
            // - It declares category="payment", OR
            // - It declares category="other" / valid APDU XML and belongs to a known bank/wallet/transit app or third-party user app
            val isSystemGms = pkgName == "com.google.android.gms"
            val shouldInclude = when {
                parsedMeta.hasPaymentCategory -> true
                isSystemGms -> svcName.contains("tapandpay", ignoreCase = true)
                parsedMeta.hasOtherCategory || parsedMeta.hasApduXml -> isKnownBankOrWallet || !isInternalSystemPackage(pkgName)
                else -> isKnownBankOrWallet
            }

            if (!shouldInclude) continue

            val appInfo = serviceInfo.applicationInfo
            val rawAppLabel = try {
                pm.getApplicationLabel(appInfo).toString().ifBlank { pkgName }
            } catch (_: Throwable) {
                pkgName
            }

            val resolvedTitle = resolveCleanBankName(
                rawAppLabel = rawAppLabel,
                serviceDescription = parsedMeta.description,
                packageName = pkgName,
                serviceName = svcName
            )

            val iconDrawable: Drawable? = try {
                serviceInfo.loadIcon(pm) ?: pm.getApplicationIcon(pkgName)
            } catch (_: Throwable) {
                null
            }

            val bitmap = iconDrawable?.let { drawableToBitmap(it, 160) }
            val (primaryColor, secondaryColor) = extractCardGradientColors(
                bitmap = bitmap,
                packageName = pkgName
            )

            discovered.add(
                BankPaymentApp(
                    appName = resolvedTitle,
                    packageName = pkgName,
                    serviceName = svcName,
                    serviceDescription = parsedMeta.description,
                    componentName = componentName,
                    iconDrawable = iconDrawable,
                    iconBitmap = bitmap?.asImageBitmap(),
                    primaryColorArgb = primaryColor,
                    secondaryColorArgb = secondaryColor
                )
            )
        }

        if (discovered.isEmpty()) {
            return@withContext defaultShowcaseBankCards()
        }

        discovered
            .sortedWith(
                compareBy<BankPaymentApp> { it.appName.lowercase() }
                    .thenBy { it.serviceName }
            )
            .distinctBy { it.packageName }
    }

    private fun isKnownPaymentOrTransitApp(
        packageName: String,
        serviceName: String,
        description: String?
    ): Boolean {
        val combined = "$packageName $serviceName ${description.orEmpty()}".lowercase()
        return KNOWN_WALLET_OR_BANK_KEYWORDS.any { combined.contains(it) }
    }

    private fun isInternalSystemPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.startsWith("com.android.") ||
            lower.startsWith("android.") ||
            lower.startsWith("com.qualcomm.") ||
            lower.startsWith("com.xiaomi.") ||
            lower.startsWith("com.miui.") ||
            lower.startsWith("com.mediatek.")
    }

    private fun resolveCleanBankName(
        rawAppLabel: String,
        serviceDescription: String?,
        packageName: String,
        serviceName: String
    ): String {
        val desc = serviceDescription?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("@") }
        if (packageName == "com.google.android.gms" && serviceName.contains("tapandpay", ignoreCase = true)) {
            return desc ?: "Google Pay"
        }
        // Prefer specific HCE description if rawAppLabel is overly generic or system-like
        if (!desc.isNullOrBlank() && (
                rawAppLabel.contains("hizmetleri", ignoreCase = true) ||
                    rawAppLabel.contains("services", ignoreCase = true) ||
                    rawAppLabel == packageName
                )
        ) {
            return desc
        }
        return rawAppLabel
    }

    private fun defaultShowcaseBankCards(): List<BankPaymentApp> = listOf(
        BankPaymentApp(
            appName = "Akbank",
            packageName = "com.akbank.android.apps.akbank_direkt",
            serviceName = "com.akbank.hce.PaymentService",
            serviceDescription = null,
            componentName = ComponentName(
                "com.akbank.android.apps.akbank_direkt",
                "com.akbank.hce.PaymentService"
            ),
            iconDrawable = null,
            iconBitmap = null,
            primaryColorArgb = 0xFFB91C1C.toInt(),
            secondaryColorArgb = 0xFF3F070B.toInt()
        ),
        BankPaymentApp(
            appName = "Garanti BBVA",
            packageName = "com.garanti.cepsubesi",
            serviceName = "com.garanti.hce.PaymentService",
            serviceDescription = null,
            componentName = ComponentName(
                "com.garanti.cepsubesi",
                "com.garanti.hce.PaymentService"
            ),
            iconDrawable = null,
            iconBitmap = null,
            primaryColorArgb = 0xFF059669.toInt(),
            secondaryColorArgb = 0xFF042F2E.toInt()
        ),
        BankPaymentApp(
            appName = "İş Bankası",
            packageName = "com.pozitron.iscep",
            serviceName = "com.pozitron.iscep.hce.PaymentService",
            serviceDescription = null,
            componentName = ComponentName(
                "com.pozitron.iscep",
                "com.pozitron.iscep.hce.PaymentService"
            ),
            iconDrawable = null,
            iconBitmap = null,
            primaryColorArgb = 0xFF1D4ED8.toInt(),
            secondaryColorArgb = 0xFF091536.toInt()
        ),
        BankPaymentApp(
            appName = "Yapı Kredi",
            packageName = "com.ykb.android",
            serviceName = "com.ykb.android.hce.PaymentService",
            serviceDescription = null,
            componentName = ComponentName(
                "com.ykb.android",
                "com.ykb.android.hce.PaymentService"
            ),
            iconDrawable = null,
            iconBitmap = null,
            primaryColorArgb = 0xFF2563EB.toInt(),
            secondaryColorArgb = 0xFF0F172A.toInt()
        )
    )

    private data class HceMetadataResult(
        val hasPaymentCategory: Boolean,
        val hasOtherCategory: Boolean,
        val hasApduXml: Boolean,
        val description: String?
    )

    private fun parsePaymentAidMetadata(
        pm: PackageManager,
        serviceInfo: ServiceInfo
    ): HceMetadataResult {
        var parser = serviceInfo.loadXmlMetaData(pm, HostApduService.SERVICE_META_DATA)
            ?: serviceInfo.loadXmlMetaData(pm, OffHostApduService.SERVICE_META_DATA)

        if (parser == null) {
            val metaBundle = serviceInfo.metaData
            if (metaBundle != null) {
                for (key in metaBundle.keySet()) {
                    if (key.contains("apdu", ignoreCase = true) ||
                        key.contains("hce", ignoreCase = true) ||
                        key.contains("nfc", ignoreCase = true)
                    ) {
                        parser = serviceInfo.loadXmlMetaData(pm, key)
                        if (parser != null) break
                    }
                }
            }
        }
        if (parser == null) {
            return HceMetadataResult(
                hasPaymentCategory = false,
                hasOtherCategory = false,
                hasApduXml = false,
                description = null
            )
        }

        var hasPayment = false
        var hasOther = false
        var hasApduTag = false
        var serviceDescription: String? = null

        try {
            parser.use { xml ->
                val appResources = try {
                    pm.getResourcesForApplication(serviceInfo.applicationInfo)
                } catch (_: Throwable) {
                    null
                }

                var eventType = xml.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tagName = xml.name
                        if (tagName == "host-apdu-service" || tagName == "offhost-apdu-service") {
                            hasApduTag = true
                            val rawDesc = xml.getAttributeValue(ANDROID_NAMESPACE, ATTR_DESCRIPTION)
                            val descResId = xml.getAttributeResourceValue(ANDROID_NAMESPACE, ATTR_DESCRIPTION, 0)
                            if (descResId != 0 && appResources != null) {
                                serviceDescription = runCatching { appResources.getString(descResId) }.getOrNull()
                            } else if (!rawDesc.isNullOrBlank() && !rawDesc.startsWith("@")) {
                                serviceDescription = rawDesc
                            }
                        } else if (tagName == TAG_AID_GROUP) {
                            val category = xml.getAttributeValue(ANDROID_NAMESPACE, ATTR_CATEGORY)
                                ?: xml.getAttributeValue(null, ATTR_CATEGORY)
                            if (category.equals(CATEGORY_PAYMENT, ignoreCase = true)) {
                                hasPayment = true
                            } else if (category.equals(CATEGORY_OTHER, ignoreCase = true)) {
                                hasOther = true
                            }
                            if (serviceDescription.isNullOrBlank()) {
                                val groupDescRes = xml.getAttributeResourceValue(ANDROID_NAMESPACE, ATTR_DESCRIPTION, 0)
                                val rawGroupDesc = xml.getAttributeValue(ANDROID_NAMESPACE, ATTR_DESCRIPTION)
                                if (groupDescRes != 0 && appResources != null) {
                                    serviceDescription = runCatching { appResources.getString(groupDescRes) }.getOrNull()
                                } else if (!rawGroupDesc.isNullOrBlank() && !rawGroupDesc.startsWith("@")) {
                                    serviceDescription = rawGroupDesc
                                }
                            }
                        }
                    }
                    eventType = xml.next()
                }
            }
        } catch (_: Throwable) {
        }

        return HceMetadataResult(
            hasPaymentCategory = hasPayment,
            hasOtherCategory = hasOther,
            hasApduXml = hasApduTag,
            description = serviceDescription
        )
    }

    private fun drawableToBitmap(drawable: Drawable, maxSizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceIn(64, maxSizePx)
        val height = drawable.intrinsicHeight.coerceIn(64, maxSizePx)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun extractCardGradientColors(bitmap: Bitmap?, packageName: String): Pair<Int, Int> {
        val fallbackHue = (packageName.hashCode() and 0x7FFFFFFF) % 360
        val defaultPrimary = ColorUtils.HSLToColor(floatArrayOf(fallbackHue.toFloat(), 0.55f, 0.22f))
        val defaultSecondary = ColorUtils.HSLToColor(floatArrayOf(((fallbackHue + 28) % 360).toFloat(), 0.48f, 0.11f))

        if (bitmap == null) return defaultPrimary to defaultSecondary

        return try {
            val palette = Palette.from(bitmap).clearFilters().generate()
            val seedColor = palette.getVibrantColor(
                palette.getDominantColor(
                    palette.getMutedColor(defaultPrimary)
                )
            )

            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(seedColor, hsl)
            val startHsl = floatArrayOf(
                hsl[0],
                hsl[1].coerceIn(0.35f, 0.78f),
                hsl[2].coerceIn(0.18f, 0.28f)
            )
            val endHsl = floatArrayOf(
                (hsl[0] + 22f) % 360f,
                (hsl[1] * 0.85f).coerceIn(0.25f, 0.65f),
                0.09f
            )
            ColorUtils.HSLToColor(startHsl) to ColorUtils.HSLToColor(endHsl)
        } catch (_: Throwable) {
            defaultPrimary to defaultSecondary
        }
    }
}
