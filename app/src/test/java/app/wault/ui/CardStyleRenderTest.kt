package app.wault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wault.data.CardKind
import app.wault.data.CardStyle
import app.wault.data.WaultCard
import app.wault.design.Appearance
import app.wault.design.CardArts
import app.wault.design.CardSurface
import app.wault.design.WaultTheme
import app.wault.design.toArgbLong
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage

/**
 * "Gradyan kalmasın" isteğinin otomatik kanıtı: kartı gerçekten çizip piksellere bakar.
 * Düz kartın yüzeyi her noktada aynı renk olmalı; gradyan kartta renk yüzey boyunca değişmeli.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CardStyleRenderTest {
    @get:Rule val rule = createComposeRule()

    private fun render(card: WaultCard, appearance: Appearance, name: String, dark: Boolean = true): BufferedImage {
        rule.setContent {
            WaultTheme(dark = dark, appearance = appearance) {
                Box(Modifier.padding(16.dp)) { CardSurface(card, Modifier.testTag("card")) }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("card").captureRoboImage(Shots.path(name))
        return Shots.read(name)
    }

    private fun ocean(style: CardStyle? = null) =
        WaultCard(kind = CardKind.SHOWCASE, title = "Deneme", artId = "ocean", last4 = "4417", style = style)

    private fun expected(id: String): Rgb {
        val c = CardArts.byId(id).base
        return Rgb((c.red * 255f + 0.5f).toInt(), (c.green * 255f + 0.5f).toInt(), (c.blue * 255f + 0.5f).toInt())
    }

    @Test fun flatCardIsOneSolidColour() {
        val img = render(ocean(), Appearance(cardStyle = CardStyle.FLAT), "style_flat")
        val want = expected("ocean")
        img.quietSamples().forEach { assertTrue("düz kartta $it, beklenen $want", it.maxDiff(want) <= 2) }
    }

    @Test fun gradientCardChangesAcrossTheSurface() {
        val img = render(ocean(), Appearance(cardStyle = CardStyle.GRADIENT), "style_gradient")
        val a = img.rgbAt(0.15f, 0.40f)
        val b = img.rgbAt(0.85f, 0.60f)
        assertTrue("gradyan kartta iki uç aynı renk: $a $b", a.maxDiff(b) >= 15)
    }

    @Test fun cardOwnStyleBeatsTheAppDefault() {
        // Uygulama genelinde gradyan seçili ama bu kart "Düz" dedi
        val img = render(ocean(CardStyle.FLAT), Appearance(cardStyle = CardStyle.GRADIENT), "style_card_override")
        val want = expected("ocean")
        img.quietSamples().forEach { assertTrue("kartın kendi stili geçerli olmalı: $it", it.maxDiff(want) <= 2) }
    }

    @Test fun customColourIsPaintedExactly() {
        val card = WaultCard(kind = CardKind.CODE, title = "Özel", artId = "custom", customColor = Color(0xFFFF8800).toArgbLong())
        val img = render(card, Appearance(), "style_custom")
        img.quietSamples().forEach { assertTrue("özel renk bozuldu: $it", it.maxDiff(Rgb(255, 136, 0)) <= 2) }
    }

    @Test fun flatCardsStayFlatInLightTheme() {
        val img = render(ocean(), Appearance(), "style_flat_light", dark = false)
        val want = expected("ocean")
        img.quietSamples().forEach { assertTrue("açık temada $it", it.maxDiff(want) <= 2) }
    }

    @Test fun lightCardsKeepDarkText() {
        // Beyaz kartta yazı koyu olmalı: kartın sol üstündeki başlık bölgesinde koyu piksel bulunmalı
        val card = WaultCard(kind = CardKind.SHOWCASE, title = "Beyaz kart", artId = "white")
        val img = render(card, Appearance(), "style_white")
        var darkest = 255
        for (x in (img.width * 0.2f).toInt()..(img.width * 0.5f).toInt()) for (y in (img.height * 0.08f).toInt()..(img.height * 0.22f).toInt()) {
            val p = img.getRGB(x, y)
            darkest = minOf(darkest, (p shr 16) and 0xFF)
        }
        assertTrue("başlık koyu olmalı (en koyu piksel $darkest)", darkest < 90)
    }
}
