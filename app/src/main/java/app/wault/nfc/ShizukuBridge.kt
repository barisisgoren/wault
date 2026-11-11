package app.wault.nfc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.wault.BuildConfig
import app.wault.DevOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Shizuku köprüsü: sessiz ödeme geçişi için kabuk yetkisiyle `cmd role` komutları çalıştırır.
 * Shizuku kurulu, çalışıyor ve Wault'a izin verilmiş olmalı; aksi halde Android'in sistem seçicisi açılır.
 */
object ShizukuBridge {
    const val PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 4242

    enum class State { NOT_INSTALLED, NOT_RUNNING, NEEDS_PERMISSION, READY }

    fun state(context: Context): State {
        val installed = runCatching { context.packageManager.getPackageInfo(PACKAGE, 0) }.isSuccess
        if (!installed) return State.NOT_INSTALLED
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!running) return State.NOT_RUNNING
        val granted = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        return if (granted) State.READY else State.NEEDS_PERMISSION
    }

    /** Shizuku'nun kendi izin penceresini açar; sonuç, uygulamaya dönünce durum yeniden okunarak anlaşılır. */
    fun requestPermission() {
        runCatching { if (!Shizuku.shouldShowRequestPermissionRationale()) Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { runCatching { Shizuku.requestPermission(REQUEST_CODE) } }
    }

    fun openShizukuApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) runCatching { context.startActivity(intent) }
    }

    data class ShellResult(val exitCode: Int, val output: String)

    /** Kabukta komut çalıştırır. Shizuku 13'te newProcess gizli olduğu için yansıma ile çağrılır. */
    suspend fun run(command: List<String>): ShellResult = withContext(Dispatchers.IO) {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java,
        ).apply { isAccessible = true }
        val process = method.invoke(null, command.toTypedArray(), null, null) as Process
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        val code = process.waitFor()
        ShellResult(code, (out + err).trim())
    }

    suspend fun walletHolders(): List<String> {
        if (BuildConfig.DEBUG) DevOptions.pretended?.let { return listOfNotNull(it.packageName) }
        return runCatching { RoleShell.parseHolders(run(RoleShell.getHolders()).output) }.getOrDefault(emptyList())
    }

    /** Varsayılan ödeme uygulamasını verilen pakete geçirir. */
    suspend fun setWalletHolder(pkg: String): Boolean {
        if (BuildConfig.DEBUG && DevOptions.dryRun) { DevOptions.pretended = DevOptions.Pretended(pkg); return true }
        return runCatching { run(RoleShell.addHolder(pkg)).exitCode == 0 }.getOrDefault(false)
    }

    /** Hiçbir ödeme uygulaması varsayılan olmasın: mevcut cüzdan sahibini kaldırır. */
    suspend fun clearWalletHolder(): Boolean {
        if (BuildConfig.DEBUG && DevOptions.dryRun) { DevOptions.pretended = DevOptions.Pretended(null); return true }
        return runCatching {
            val holders = walletHolders()
            holders.all { run(RoleShell.removeHolder(it)).exitCode == 0 }
        }.getOrDefault(false)
    }
}
