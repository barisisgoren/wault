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
    }

    /**
     * Scans installed applications for HostApduService implementations that declare
     * `<aid-group android:category="payment">` in their `android.nfc.cardemulation.host_apdu_service` XML metadata.
     */
    suspend fun scanInstalledPaymentApps(): List<BankPaymentApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val queryIntent = Intent(HostApduService.SERVICE_INTERFACE)

        val resolveInfos: List<ResolveInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(
                    queryIntent,
                    PackageManager.ResolveInfoFlags.of(
                        (PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(
                    queryIntent,
                    PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }

        val discovered = mutableListOf<BankPaymentApp>()
        val seenComponents = HashSet<ComponentName>()

        for (resolveInfo in resolveInfos) {
            val serviceInfo = resolveInfo.serviceInfo ?: continue
            val componentName = ComponentName(serviceInfo.packageName, serviceInfo.name)
            if (componentName in seenComponents) continue

            val parsedMeta = parsePaymentAidMetadata(pm, serviceInfo)
            if (!parsedMeta.hasPaymentCategory) continue

            seenComponents.add(componentName)

            val appInfo = serviceInfo.applicationInfo
            val appName = try {
                pm.getApplicationLabel(appInfo).toString().ifBlank { serviceInfo.packageName }
            } catch (_: Throwable) {
                serviceInfo.packageName
            }

            val iconDrawable: Drawable? = try {
                serviceInfo.loadIcon(pm) ?: pm.getApplicationIcon(serviceInfo.packageName)
            } catch (_: Throwable) {
                null
            }

            val bitmap = iconDrawable?.let { drawableToBitmap(it, 160) }
            val (primaryColor, secondaryColor) = extractCardGradientColors(
                bitmap = bitmap,
                packageName = serviceInfo.packageName
            )

            discovered.add(
                BankPaymentApp(
                    appName = appName,
                    packageName = serviceInfo.packageName,
                    serviceName = serviceInfo.name,
                    serviceDescription = parsedMeta.description,
                    componentName = componentName,
                    iconDrawable = iconDrawable,
                    iconBitmap = bitmap?.asImageBitmap(),
                    primaryColorArgb = primaryColor,
                    secondaryColorArgb = secondaryColor
                )
            )
        }

        discovered.sortedWith(
            compareBy<BankPaymentApp> { it.appName.lowercase() }
                .thenBy { it.serviceName }
        )
    }

    private data class HceMetadataResult(
        val hasPaymentCategory: Boolean,
        val description: String?
    )

    private fun parsePaymentAidMetadata(
        pm: PackageManager,
        serviceInfo: ServiceInfo
    ): HceMetadataResult {
        var parser = serviceInfo.loadXmlMetaData(pm, HostApduService.SERVICE_META_DATA)
        if (parser == null) {
            val metaBundle = serviceInfo.metaData
            if (metaBundle != null) {
                for (key in metaBundle.keySet()) {
                    if (key.contains("apdu", ignoreCase = true) || key.contains("hce", ignoreCase = true)) {
                        parser = serviceInfo.loadXmlMetaData(pm, key)
                        if (parser != null) break
                    }
                }
            }
        }
        if (parser == null) {
            return HceMetadataResult(hasPaymentCategory = false, description = null)
        }

        var hasPayment = false
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
                    }
                    eventType = xml.next()
                }
            }
        } catch (_: Throwable) {
        }

        return HceMetadataResult(
            hasPaymentCategory = hasPayment,
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
