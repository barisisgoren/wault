package app.wault.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.wault.MainActivity
import app.wault.WaultApplication
import app.wault.data.CardKind
import app.wault.nfc.PaymentSwitcher

/** Hızlı ayarlar kutucuğu: Wault'u varsayılan ödeme kartı seçili halde açar. */
class WaultTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        val app = application as WaultApplication
        val active = app.container.store.cards.value.firstOrNull {
            it.kind == CardKind.PAYMENT && it.payment != null && PaymentSwitcher.isDefault(this, it.payment)
        }
        tile.state = if (active != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = active?.title ?: "Kart seç"
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ACTIVE, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
