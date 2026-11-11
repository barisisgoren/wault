package app.wault

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import app.wault.data.DemoCards
import app.wault.design.Tilt
import app.wault.design.WaultTheme
import app.wault.nfc.NfcController
import app.wault.ui.WaultApp

class MainActivity : FragmentActivity() {

    lateinit var nfc: NfcController
        private set

    /** Her onResume'da artar; varsayılan kart, izinler gibi dış durumu yeniden okumak için. */
    val resumeTick = mutableIntStateOf(0)
    val openActiveRequest = mutableIntStateOf(0)
    val locked = mutableStateOf(false)

    private var pausedAt = 0L

    val app: WaultApplication get() = application as WaultApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        nfc = NfcController(this)
        locked.value = app.container.prefs.state.value.biometricLock
        handle(intent)
        setContent {
            WaultTheme(dark = (if (BuildConfig.DEBUG) DevOptions.darkOverride else null) ?: isSystemInDarkTheme()) {
                WaultApp(this)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ACTIVE, false) == true) openActiveRequest.intValue++
        if (BuildConfig.DEBUG && intent != null) {
            // Yalnızca debug: adb ile arayüz denerken telefonun gerçek ayarlarına dokunmamak için.
            if (intent.hasExtra("dry_run")) DevOptions.dryRun = intent.getBooleanExtra("dry_run", false)
            if (intent.hasExtra("dry_run") && !DevOptions.dryRun) DevOptions.pretended = null
            intent.getStringExtra("theme")?.let {
                DevOptions.darkOverride = when (it) { "dark" -> true; "light" -> false; else -> null }
            }
            if (intent.hasExtra("demo")) {
                if (intent.getBooleanExtra("demo", false)) {
                    DevOptions.dryRun = true
                    DevOptions.pretended = DevOptions.Pretended("com.garanti.cepsubesi")
                    app.container.store.showDemo(DemoCards.all(intent.getIntExtra("demo_count", 9)))
                } else {
                    app.container.store.leaveDemo()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.isForeground = true
        Tilt.start(this)
        nfc.onResume()
        val away = SystemClock.elapsedRealtime() - pausedAt
        if (pausedAt != 0L && away > LOCK_AFTER_MS && app.container.prefs.state.value.biometricLock) locked.value = true
        resumeTick.intValue++
    }

    override fun onPause() {
        super.onPause()
        app.isForeground = false
        Tilt.stop()
        nfc.onPause()
        pausedAt = SystemClock.elapsedRealtime()
    }

    /** Kod gösterilirken okuyucunun görebilmesi için ekranı en parlak hale getirir. */
    fun setMaxBrightness(on: Boolean) {
        val lp = window.attributes
        lp.screenBrightness = if (on) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }

    companion object {
        const val EXTRA_OPEN_ACTIVE = "open_active"
        private const val LOCK_AFTER_MS = 30_000L
    }
}
