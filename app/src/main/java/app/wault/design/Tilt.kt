package app.wault.design

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/**
 * Telefonu eğince kart yüzeyindeki ışık yansıması kayar (Apple Wallet'taki gibi).
 * Sadece uygulama öndeyken dinler; çok düşük hızda örnekler ve değer ancak anlamlı değişince yayılır.
 * Telefonun dinlenme duruşu zamanla "nötr" sayılır; ışık sadece hareket ederken oynar.
 */
object Tilt {
    /** -1..1: sağa/sola eğim. */
    var x by mutableFloatStateOf(0f)
        private set

    /** -1..1: öne/arkaya eğim. */
    var y by mutableFloatStateOf(0f)
        private set

    private var manager: SensorManager? = null
    private var listener: SensorEventListener? = null
    private var primed = false
    private var fx = 0f
    private var fy = 0f
    private var bx = 0f
    private var by = 0f

    fun start(context: Context) {
        if (listener != null) return
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val gx = (e.values[0] / 9.81f).coerceIn(-1f, 1f)
                val gz = (e.values[2] / 9.81f).coerceIn(-1f, 1f)
                if (!primed) { fx = gx; fy = gz; bx = gx; by = gz; primed = true }
                fx += (gx - fx) * 0.18f
                fy += (gz - fy) * 0.18f
                bx += (fx - bx) * 0.012f
                by += (fy - by) * 0.012f
                // Ölü bölge: sensör gürültüsü ekranı sürekli yeniden çizdirmesin (durağan telefonda sıfır kare)
                val nx = deadband(((fx - bx) * 2.4f).coerceIn(-1f, 1f))
                val ny = deadband(((fy - by) * 2.4f).coerceIn(-1f, 1f))
                if (abs(nx - x) > 0.015f) x = nx
                if (abs(ny - y) > 0.015f) y = ny
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)) {
            manager = sm
            listener = l
        }
    }

    private fun deadband(v: Float): Float = if (abs(v) < 0.03f) 0f else v

    fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
        manager = null
        primed = false
        x = 0f
        y = 0f
    }
}
