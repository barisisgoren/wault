package com.baris.wault.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.baris.wault.MainActivity
import com.baris.wault.R
import com.baris.wault.data.nfc.BankPaymentApp
import com.baris.wault.data.nfc.NfcPaymentRepository
import com.baris.wault.data.nfc.NfcSwitchResult
import com.baris.wault.data.vault.WaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile allowing users to cycle through their installed banking NFC HCE apps
 * directly from the Android notification shade with a single tap.
 */
class WaultTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val nfcRepository: NfcPaymentRepository by lazy {
        NfcPaymentRepository.getInstance(applicationContext)
    }

    private val bankOverrideDao by lazy {
        WaultDatabase.getInstance(applicationContext).bankCardOverrideDao()
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            val visibleBanks = loadVisibleCustomizedBanks()
            val activeBank = nfcRepository.getActiveBank(visibleBanks)
            updateQuickSettingsTile(visibleBanks, activeBank)
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val visibleBanks = loadVisibleCustomizedBanks()

            if (visibleBanks.isEmpty()) {
                val mainIntent = Intent(this@WaultTileService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                launchIntentFromTile(mainIntent)
                return@launch
            }

            val currentActive = nfcRepository.getActiveBank(visibleBanks)
            val currentIndex = visibleBanks.indexOfFirst {
                it.componentName == currentActive?.componentName
            }
            val nextBank = visibleBanks[(currentIndex + 1) % visibleBanks.size]

            when (val switchResult = nfcRepository.switchToBank(nextBank)) {
                is NfcSwitchResult.SwitchedSilently -> {
                    updateQuickSettingsTile(banks = visibleBanks, activeBank = nextBank)
                    Toast.makeText(
                        applicationContext,
                        "Wault NFC: ${nextBank.displayTitle} aktif",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is NfcSwitchResult.RequiresSystemDialog -> {
                    val dialogIntent = switchResult.fallbackIntent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    launchIntentFromTile(dialogIntent)
                }

                is NfcSwitchResult.Error -> {
                    Toast.makeText(
                        applicationContext,
                        switchResult.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun loadVisibleCustomizedBanks(): List<BankPaymentApp> {
        val rawBanks = nfcRepository.refreshInstalledBanks()
        val overrides = bankOverrideDao.getAllOverrides().associateBy { it.componentFlatten }
        return rawBanks
            .map { bank ->
                val ov = overrides[bank.flattenedComponent]
                    ?: overrides[bank.shortFlattenedComponent]
                bank.withOverride(ov)
            }
            .filterNot { it.isHidden }
            .sortedWith(
                compareByDescending<BankPaymentApp> { it.isPrimary }
                    .thenBy { it.sortOrder }
                    .thenBy { it.displayTitle.lowercase() }
            )
    }

    private fun updateQuickSettingsTile(
        banks: List<BankPaymentApp>,
        activeBank: BankPaymentApp?
    ) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_contactless_tile)

        when {
            banks.isEmpty() -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Banka bulunamadı"
                }
            }

            activeBank != null -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = activeBank.displayTitle
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (banks.size > 1) {
                        "Değiştir (${banks.size} banka)"
                    } else {
                        "NFC Ödemeye Hazır"
                    }
                }
            }

            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "${banks.first().displayTitle} için dokun"
                }
            }
        }

        tile.updateTile()
    }

    private fun launchIntentFromTile(intent: Intent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }.onFailure {
            runCatching { startActivity(intent) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
