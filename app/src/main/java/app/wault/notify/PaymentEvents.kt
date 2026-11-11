package app.wault.notify

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PaidEvent(
    val cardId: String?,
    val amount: String,
    val sourcePackage: String,
    val at: Long = System.currentTimeMillis(),
)

object PaymentEvents {
    private val _events = MutableSharedFlow<PaidEvent>(replay = 0, extraBufferCapacity = 4)
    val events: SharedFlow<PaidEvent> = _events.asSharedFlow()

    /** Uygulama kapalıyken gelen son olay; açılınca bir kez gösterilir. */
    @Volatile
    var pending: PaidEvent? = null

    fun emit(e: PaidEvent) {
        if (!_events.tryEmit(e)) pending = e
    }
}
