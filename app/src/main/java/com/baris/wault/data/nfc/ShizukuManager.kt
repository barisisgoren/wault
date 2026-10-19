package com.baris.wault.data.nfc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

enum class SilentSwitchCapability(
    val isSilentReady: Boolean,
    val badgeLabel: String,
    val detailTitle: String,
    val detailMessage: String
) {
    SHIZUKU_READY(
        isSilentReady = true,
        badgeLabel = "Shizuku Hazır",
        detailTitle = "Shizuku Bağlantısı Aktif",
        detailMessage = "Wault, Shizuku üzerinden sistem yetkisine sahip. Herhangi bir banka kartına dokunduğunuzda varsayılan NFC ödeme uygulaması anında değiştirilir."
    ),
    ADB_WRITE_SECURE_READY(
        isSilentReady = true,
        badgeLabel = "Shizuku Hazır",
        detailTitle = "ADB Güvenli Ayar Yetkisi Aktif",
        detailMessage = "Wault, WRITE_SECURE_SETTINGS iznine sahip. Tek dokunuşla sessiz ve anında NFC banka geçişi aktiftir."
    ),
    SHIZUKU_PERMISSION_NEEDED(
        isSilentReady = false,
        badgeLabel = "İzin Gerekli",
        detailTitle = "Shizuku Yetkisi Gerekli",
        detailMessage = "Shizuku çalışıyor ancak Wault'un varsayılan temassız ödeme uygulamasını sessizce değiştirebilmesi için izin vermeniz gerekiyor."
    ),
    STANDARD_FALLBACK(
        isSilentReady = false,
        badgeLabel = "Standart Mod",
        detailTitle = "Standart Android NFC Modu",
        detailMessage = "Shizuku şu anda çalışmıyor. Sıfır gecikmeli sessiz geçiş için Shizuku'yu başlatın veya ADB komutunu kullanın."
    )
}

class ShizukuManager(private val context: Context) {

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

    private val _capability = MutableStateFlow(evaluateCapability())
    val capability: StateFlow<SilentSwitchCapability> = _capability.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                refreshStatus()
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
        }
        refreshStatus()
    }

    fun refreshStatus(): SilentSwitchCapability {
        val current = evaluateCapability()
        _capability.value = current
        return current
    }

    private fun evaluateCapability(): SilentSwitchCapability {
        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

        if (binderAlive) {
            val granted = try {
                if (Shizuku.isPreV11()) {
                    false
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
            } catch (_: Throwable) {
                false
            }
            if (granted) {
                return SilentSwitchCapability.SHIZUKU_READY
            }
        }

        val hasWriteSecureSettings = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasWriteSecureSettings) {
            return SilentSwitchCapability.ADB_WRITE_SECURE_READY
        }

        return if (binderAlive) {
            SilentSwitchCapability.SHIZUKU_PERMISSION_NEEDED
        } else {
            SilentSwitchCapability.STANDARD_FALLBACK
        }
    }

    fun isShizukuReady(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun hasWriteSecureSettingsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestShizukuPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) {
                refreshStatus()
                false
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshStatus()
                true
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                false
            } else {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun executeCommand(command: Array<String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            check(isShizukuReady()) { "Shizuku hazır değil veya izin verilmedi." }

            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply {
                isAccessible = true
            }

            val process = newProcessMethod.invoke(null, command, null, null) as Process
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stdout.appendLine(line)
                }
            }
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stderr.appendLine(line)
                }
            }

            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                error("Shizuku komutu zaman aşımına uğradı")
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                error("Shizuku hata kodu ($exitCode): ${stderr.toString().trim()}")
            }
            stdout.toString().trim()
        }
    }
}
