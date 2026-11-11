package app.wault.ui

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

/** Ekran görüntüleri app/build/test-screenshots altına yazılır; test sonrası gözle de bakılabilir. */
object Shots {
    private val dir = File("build/test-screenshots").apply { mkdirs() }
    fun path(name: String): String = File(dir, "$name.png").path
    fun read(name: String): BufferedImage = ImageIO.read(File(path(name)))
}

data class Rgb(val r: Int, val g: Int, val b: Int) {
    fun maxDiff(o: Rgb) = maxOf(abs(r - o.r), abs(g - o.g), abs(b - o.b))
}

fun BufferedImage.rgbAt(fx: Float, fy: Float): Rgb {
    val px = getRGB((width * fx).toInt().coerceIn(0, width - 1), (height * fy).toInt().coerceIn(0, height - 1))
    return Rgb((px shr 16) and 0xFF, (px shr 8) and 0xFF, px and 0xFF)
}

/** Kartın yazı ve ikon içermeyen orta bandından örnek noktaları. */
fun BufferedImage.quietSamples(): List<Rgb> =
    listOf(0.40f, 0.50f, 0.60f).flatMap { y -> listOf(0.15f, 0.50f, 0.85f).map { x -> rgbAt(x, y) } }
