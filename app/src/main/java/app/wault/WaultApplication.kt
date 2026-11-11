package app.wault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import app.wault.data.CardStore
import app.wault.data.Prefs
import app.wault.notify.PaymentNotificationListener

class AppContainer(app: Application) {
    val store = CardStore(app)
    val prefs = Prefs(app)
}

class WaultApplication : Application() {
    lateinit var container: AppContainer
        private set

    @Volatile
    var isForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.store.load(seed = { emptyList() })
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                PaymentNotificationListener.CHANNEL,
                getString(R.string.channel_payments),
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }
}
