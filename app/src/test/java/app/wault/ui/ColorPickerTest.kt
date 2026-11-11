package app.wault.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wault.data.CardKind
import app.wault.data.FaceSource
import app.wault.data.PaymentLink
import app.wault.data.WaultCard
import app.wault.design.WaultTheme
import app.wault.design.hsvColor
import app.wault.design.toHex
import app.wault.ui.common.ColorSheetContent
import app.wault.ui.common.PaletteGrid
import app.wault.ui.common.QuickColors
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ColorPickerTest {
    @get:Rule val rule = createComposeRule()

    private val base = WaultCard(kind = CardKind.LOYALTY, title = "Migros", artId = "ocean")

    @Test fun hexFieldAppliesASixDigitCode() {
        var picked: Color? = null
        rule.setContent { WaultTheme(dark = true) { ColorSheetContent(current = null, onPick = { picked = it }, onDone = {}) } }
        rule.onNode(hasSetTextAction()).performTextInput("FF8800")
        assertEquals("#FF8800", picked?.toHex())
    }

    @Test fun hexFieldKeepsTheCursorAtTheEndWhenTypedOneKeyAtATime() {
        // Telefonda her tuş ayrı gelir; imleç '#' yanında kalırsa haneler döner ("8A2BE2" -> "A2BE28")
        var picked: Color? = null
        rule.setContent { WaultTheme(dark = true) { ColorSheetContent(current = null, onPick = { picked = it }, onDone = {}) } }
        "8A2BE2".forEach { rule.onNode(hasSetTextAction()).performTextInput(it.toString()) }
        assertEquals("#8A2BE2", picked?.toHex())
    }

    @Test fun hexFieldIgnoresIncompleteOrInvalidInput() {
        var picked: Color? = null
        rule.setContent { WaultTheme(dark = true) { ColorSheetContent(current = null, onPick = { picked = it }, onDone = {}) } }
        rule.onNode(hasSetTextAction()).performTextInput("ZZ12")   // geçersiz karakterler atılır, 2 hane kalır
        assertNull("eksik kod renk uygulamamalı", picked)
    }

    @Test fun paletteCellPicksItsOwnColour() {
        var picked: Color? = null
        val target = hsvColor(205f, 0.9f, 0.75f) // mavi ailesi, 4. kademe
        rule.setContent { WaultTheme(dark = true) { ColorSheetContent(current = null, onPick = { picked = it }, onDone = {}) } }
        rule.onNodeWithContentDescription(target.toHex()).performClick()
        assertEquals(target.toHex(), picked?.toHex())
    }

    @Test fun sheetShowsTheCurrentColourCode() {
        rule.setContent { WaultTheme(dark = true) { ColorSheetContent(current = Color(0xFF2F6BE0), onPick = {}, onDone = {}) } }
        rule.onNodeWithText("#2F6BE0").assertExists()
        rule.onRoot().captureRoboImage(Shots.path("color_sheet"))
    }

    @Test fun doneButtonCloses() {
        var done = false
        rule.setContent { WaultTheme(dark = true) { ColorSheetContent(current = null, onPick = {}, onDone = { done = true }) } }
        rule.onNodeWithText("Bitti").performClick()
        assertEquals(true, done)
    }

    @Test fun gridMarksOnlyTheCardsOwnColourAsSelected() {
        rule.setContent { WaultTheme(dark = true) { PaletteGrid(base, onChange = {}, onCustom = {}, Modifier.padding(16.dp)) } }
        rule.onNodeWithContentDescription("Okyanus").assertIsSelected()
        rule.onNodeWithContentDescription("Gece").assertIsNotSelected()
        rule.onRoot().captureRoboImage(Shots.path("palette_grid"))
    }

    @Test fun gridClickRecolorsAndClearsPhotoFaces() {
        var changed: WaultCard? = null
        val photo = base.copy(faceSource = FaceSource.CUSTOM, faceRef = "0b7e6f4a-1c22-4f3a-9d1e-2a7f5c1d9e10.bin")
        rule.setContent { WaultTheme(dark = true) { PaletteGrid(photo, onChange = { changed = it }, onCustom = {}) } }
        rule.onNodeWithContentDescription("Mercan").performClick()
        assertEquals("sunset", changed?.artId)
        assertEquals(FaceSource.THEME, changed?.faceSource)
        assertNull(changed?.faceRef)
        assertNull(changed?.customColor)
    }

    @Test fun appColourIsHiddenWithoutAPaymentApp() {
        rule.setContent { WaultTheme(dark = true) { PaletteGrid(base, onChange = {}, onCustom = {}) } }
        assertEquals(0, rule.onAllNodes(hasContentDescription("Uygulama rengi")).fetchSemanticsNodes().size)
    }

    @Test fun appColourIsOfferedForAPaymentCardWithAnApp() {
        val paying = base.copy(kind = CardKind.PAYMENT, artId = "auto", payment = PaymentLink("com.example.bank", "x.Svc", "Banka"))
        rule.setContent { WaultTheme(dark = true) { PaletteGrid(paying, onChange = {}, onCustom = {}) } }
        rule.onNodeWithContentDescription("Uygulama rengi").assertIsSelected()
    }

    @Test fun quickRowRecolorsTheCard() {
        var changed: WaultCard? = null
        rule.setContent { WaultTheme(dark = true) { QuickColors(base, onChange = { changed = it }, onCustom = {}) } }
        rule.onNodeWithContentDescription("Grafit").performClick()
        assertNotNull(changed)
        assertEquals("graphite", changed?.artId)
    }
}
