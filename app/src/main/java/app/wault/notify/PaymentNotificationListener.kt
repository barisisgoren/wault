package app.wault.notify

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.wault.MainActivity
import app.wault.R
import app.wault.WaultApplication
import app.wault.data.CardKind
import app.wault.nfc.PaymentSwitcher

/**
 * Bağlı banka uygulamalarının bildirimlerini dinler. Harcama tutarı yakalanınca
 * Wault kendi "Ödendi" anını gösterir; banka uygulaması öne çıkarılmaz.
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val recent = LinkedHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = applicationContext as? WaultApplication ?: return
        val cards = app.container.store.cards.value.filter { it.kind == CardKind.PAYMENT && it.payment != null }
        val watched = cards.mapNotNull { it.payment?.packageName }.toSet() + GOOGLE_WALLET
        if (sbn.packageName !in watched) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > 15_000 }
        if (recent.containsKey(sbn.key)) return

        val ex = sbn.notification.extras
        val text = listOfNotNull(
            ex.getCharSequence(Notification.EXTRA_TITLE),
            ex.getCharSequence(Notification.EXTRA_TEXT),
            ex.getCharSequence(Notification.EXTRA_BIG_TEXT),
        ).joinToString(" ")
        val amount = AmountParser.parse(text) ?: return
        recent[sbn.key] = now

        val fromPkg = cards.filter { it.payment?.packageName == sbn.packageName }
        val card = fromPkg.firstOrNull { PaymentSwitcher.isDefault(this, it.payment!!) } ?: fromPkg.firstOrNull()
        val event = PaidEvent(card?.id, amount.display(), sbn.packageName)

        val replace = app.container.prefs.state.value.replaceBankNotification
        if (replace) runCatching { cancelNotification(sbn.key) }

        if (app.isForeground) {
            PaymentEvents.emit(event)
        } else {
            PaymentEvents.pending = event
            postOwn(this, card?.title, event.amount)
        }
    }

    companion object {
        const val CHANNEL = "payments"
        private const val GOOGLE_WALLET = "com.google.android.apps.walletnfcrel"

        fun hasAccess(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, PaymentNotificationListener::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun openAccessSettings(context: Context) {
            val i = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(i) }
        }

        fun postOwn(context: Context, cardTitle: String?, amount: String) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            val open = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_tile)
                .setContentTitle("Ödendi $amount")
                .setContentText(cardTitle?.let { "$it ile" } ?: "Varsayılan kartla")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { nm.notify(7001, n) }
        }
    }
}
