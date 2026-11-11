package app.wault.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wault.DevOptions
import app.wault.MainActivity
import app.wault.WaultApplication
import app.wault.data.AccentChoice
import app.wault.data.CardStyle
import app.wault.data.DemoCards
import app.wault.data.ThemeMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Uygulamayı açmadan önce örnek kartları yükler; deneme modu açık olduğundan sistem ayarlarına dokunulmaz. */
class DemoApp(private val cards: Int = 9) : TestWatcher() {
    override fun starting(description: Description) {
        val app = ApplicationProvider.getApplicationContext<WaultApplication>()
        DevOptions.dryRun = true
        DevOptions.pretended = null
        app.getSharedPreferences("wault", Context.MODE_PRIVATE).edit().clear().commit()
        app.container.prefs.update { it.copy(onboarded = true) }
        app.container.store.showDemo(DemoCards.all(cards))
    }

    override fun finished(description: Description) {
        DevOptions.dryRun = false
        DevOptions.demoActive = false
        DevOptions.pretended = null
        DevOptions.darkOverride = null
    }
}

/** Uygulamanın bütününü (MainActivity) bilgisayarda çalıştırır: dokunma, çekme, sürükleme, ayarlar. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {
    @get:Rule(order = 0) val demo = DemoApp(9)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val store get() = (rule.activity.application as WaultApplication).container.store
    private val prefs get() = (rule.activity.application as WaultApplication).container.prefs

    /** Kartın üst şeridine dokunur: alttaki kartlar şeridi örtmez. */
    private fun AndroidComposeTestRule<*, *>.tapStrip(title: String) {
        onNodeWithText(title).performTouchInput { click(Offset(centerX, 30.dp.toPx())) }
    }

    private fun AndroidComposeTestRule<*, *>.waitForText(text: String) {
        waitUntil(5_000) { onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun resetPrefs() {
        prefs.update { it.copy(themeMode = ThemeMode.SYSTEM, cardStyle = CardStyle.FLAT, accent = AccentChoice.BLUE) }
    }

    @Test fun stackShowsAllCardsWithTheirTitles() {
        rule.waitForText("Migros")
        listOf("Garanti BBVA", "Akbank", "Yapı Kredi", "Ziraat Bankası", "VakıfBank", "Migros", "Sinema bileti", "Ofis kapısı", "Vitrin")
            .forEach { rule.onNodeWithText(it).assertExists() }
        rule.onRoot().captureRoboImage(Shots.path("flow_stack_light"))
    }

    @Test fun tappingACardOpensItsDetailAndDoneClosesIt() {
        rule.waitForText("Migros")
        rule.tapStrip("Migros")
        rule.waitForText("Üye numarası")
        rule.onNodeWithText("Bitti").assertExists()
        rule.onRoot().captureRoboImage(Shots.path("flow_detail_loyalty"))
        rule.onNodeWithText("Bitti").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Bitti")).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithContentDescription("Kart ekle").assertExists()
    }

    @Test fun quickColourRowRecoloursTheOpenCardAndSavesIt() {
        rule.waitForText("Migros")
        rule.tapStrip("Migros")
        rule.waitForText("Üye numarası")
        rule.onNode(hasContentDescription("Grafit")).performScrollTo().performClick()
        rule.waitUntil(5_000) { store.byId("demo-migros")?.artId == "graphite" }
        assertEquals("graphite", store.byId("demo-migros")?.artId)
        rule.onRoot().captureRoboImage(Shots.path("flow_detail_recolored"))
    }

    @Test fun settingsLetThePersonPickStyleThemeAndAccent() {
        rule.waitForText("Migros")
        rule.onNodeWithContentDescription("Ayarlar").performClick()
        rule.waitForText("Kart stili")
        rule.onNodeWithText("Gradyan").performClick()
        rule.waitUntil(5_000) { prefs.state.value.cardStyle == CardStyle.GRADIENT }
        // Geliştirici grubunda (debug) da "Koyu" var; ilki Görünüm > Tema
        rule.onAllNodes(hasText("Koyu"))[0].performClick()
        rule.waitUntil(5_000) { prefs.state.value.themeMode == ThemeMode.DARK }
        rule.onNodeWithContentDescription("Pembe").performClick()
        rule.waitUntil(5_000) { prefs.state.value.accent == AccentChoice.PINK }
        rule.onRoot().captureRoboImage(Shots.path("flow_settings_dark_gradient_pink"))
    }

    @Test fun aSelectedCardThatDisappearsReturnsToTheStack() {
        rule.waitForText("Migros")
        rule.tapStrip("Migros")
        rule.waitForText("Üye numarası")
        // Kasa yeniden okunup Migros kalmayınca seçili mod takılı kalmamalı
        rule.activity.runOnUiThread { store.showDemo(DemoCards.all(3)) }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Bitti")).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithContentDescription("Kart ekle").assertExists()
    }

    @Test fun defaultStyleIsFlatAndLightEffectsAreOff() {
        rule.waitForText("Migros")
        assertEquals(CardStyle.FLAT, prefs.state.value.cardStyle)
        assertTrue(!prefs.state.value.lightEffects)
    }
}

/** Az kartla (yığın sığar) yukarı çekme ve sürükleyerek sıralama. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StackGestureTest {
    @get:Rule(order = 0) val demo = DemoApp(3)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val store get() = (rule.activity.application as WaultApplication).container.store

    @Test fun pullingACardUpDoesNotOpenIt() {
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Akbank")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Akbank").performTouchInput {
            val start = Offset(centerX, 30.dp.toPx())
            swipe(start, Offset(centerX, start.y - 260.dp.toPx()), durationMillis = 350)
        }
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(hasText("Bitti")).fetchSemanticsNodes().size)
    }

    @Test fun holdingAndDraggingAcardReordersTheStack() {
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Akbank")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("Garanti BBVA", "Akbank", "Yapı Kredi"), store.cards.value.map { it.title })
        rule.onNodeWithText("Akbank").performTouchInput {
            down(Offset(centerX, 30.dp.toPx()))
            advanceEventTime(700) // uzun basış
            moveBy(Offset(0f, (-110).dp.toPx()), delayMillis = 16)
            moveBy(Offset(0f, (-10).dp.toPx()), delayMillis = 16)
            up()
        }
        rule.waitUntil(5_000) { store.cards.value.first().title == "Akbank" }
        assertEquals(listOf("Akbank", "Garanti BBVA", "Yapı Kredi"), store.cards.value.map { it.title })
        rule.onRoot().captureRoboImage(Shots.path("flow_reordered"))
    }
}
