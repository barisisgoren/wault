package app.wault.data

import android.content.Context
import android.util.Log
import app.wault.DevOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Kartların tek doğruluk kaynağı. Liste sırası yığındaki sıradır (ilk eleman en üstte).
 * Disk: filesDir/vault.bin, VaultCipher ile şifreli JSON.
 */
class CardStore(private val context: Context) {
    private val file = File(context.filesDir, "vault.bin")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private var persistedImages = emptySet<String>()

    private val _cards = MutableStateFlow<List<WaultCard>>(emptyList())
    val cards: StateFlow<List<WaultCard>> = _cards.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun load(seed: () -> List<WaultCard>) {
        scope.launch {
            val list = if (file.exists()) {
                runCatching { CardCodec.decode(String(VaultCipher.decrypt(file.readBytes()))) }
                    .onFailure { Log.e("Wault", "Kasa okunamadı", it) }
                    .getOrDefault(emptyList())
            } else {
                seed().also { if (it.isNotEmpty()) persist(it) }
            }
            persistedImages = list.filter { it.faceSource == FaceSource.CUSTOM }.mapNotNull { it.faceRef }.toSet()
            if (DevOptions.demoActive) return@launch // örnek kartlar gösteriliyorsa gerçek kasa üstüne yazılmaz
            _cards.value = list
            _loaded.value = true
        }
    }

    /** Yalnızca debug: kasaya dokunmadan örnek kartları gösterir (bkz. DevOptions.demoActive). */
    fun showDemo(cards: List<WaultCard>) {
        DevOptions.demoActive = true
        _cards.value = cards
        _loaded.value = true
    }

    /** Yalnızca debug: örnek kartları kapatıp gerçek kasayı yeniden okur. */
    fun leaveDemo() {
        DevOptions.demoActive = false
        load(seed = { emptyList() })
    }

    fun byId(id: String): WaultCard? = _cards.value.firstOrNull { it.id == id }

    fun upsert(card: WaultCard) = mutate { list ->
        val i = list.indexOfFirst { it.id == card.id }
        if (i >= 0) list.toMutableList().also { it[i] = card } else listOf(card) + list
    }

    fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun reorder(ids: List<String>) = mutate { list -> Reorder.applyOrder(list, ids) }

    private fun mutate(block: (List<WaultCard>) -> List<WaultCard>) {
        _cards.update(block)
        val snapshot = _cards.value
        scope.launch { persist(snapshot) }
    }

    private suspend fun persist(list: List<WaultCard>) = writeLock.withLock {
        if (DevOptions.demoActive) return@withLock // örnek kartlar asla kasaya yazılmaz
        runCatching {
            val tmp = File(file.parentFile, "vault.tmp")
            tmp.writeBytes(VaultCipher.encrypt(CardCodec.encode(list).toByteArray()))
            check(tmp.renameTo(file)) { "Kasa atomik olarak kaydedilemedi" }
            val images = list.filter { it.faceSource == FaceSource.CUSTOM }.mapNotNull { it.faceRef }.toSet()
            (persistedImages - images).forEach { CardImages.delete(context, it) }
            persistedImages = images
        }.onFailure { Log.e("Wault", "Kasa yazılamadı", it) }
    }
}
