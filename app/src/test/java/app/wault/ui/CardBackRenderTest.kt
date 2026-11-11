package app.wault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wault.data.CardKind
import app.wault.data.WaultCard
import app.wault.design.WaultTheme
import app.wault.ui.stack.CardBack
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Kart bilgileri yalnızca kartın arkasında, kopyalama düğmeleriyle görünür. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CardBackRenderTest {
    @get:Rule val rule = createComposeRule()

    private val card = WaultCard(
        kind = CardKind.PAYMENT, title = "Yapı Kredi", artId = "violet", last4 = "8851",
        pan = "4000000000008851", cvv = "123", expiry = "03/29",
    )

    private fun show(revealed: Boolean) = rule.setContent {
        WaultTheme(dark = false) {
            Box(Modifier.padding(16.dp).testTag("back")) { CardBack(card, revealed, {}, {}, {}) }
        }
    }

    @Test fun revealedBackShowsNumberExpiryAndCvvWithCopyButtons() {
        show(true)
        rule.onNodeWithText("4000 0000 0000 8851").assertIsDisplayed()
        rule.onNodeWithText("123").assertIsDisplayed()
        rule.onNodeWithText("03/29").assertIsDisplayed()
        rule.onNodeWithText("Bilgileri gizle").assertIsDisplayed()
        rule.onNodeWithTag("back").captureRoboImage(Shots.path("card_back_revealed"))
    }

    @Test fun networkLogosRenderLarge() {
        rule.setContent {
            WaultTheme(dark = false) {
                androidx.compose.foundation.layout.Column(Modifier.padding(16.dp).testTag("nets")) {
                    listOf(app.wault.data.CardNetwork.VISA, app.wault.data.CardNetwork.MASTERCARD, app.wault.data.CardNetwork.AMEX).forEach { n ->
                        app.wault.design.CardSurface(card.copy(title = n.label, network = n, artId = "ocean"))
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                    }
                }
            }
        }
        rule.onNodeWithTag("nets").captureRoboImage(Shots.path("network_logos"))
    }

    @Test fun hiddenBackShowsNothingSensitive() {
        show(false)
        rule.onNodeWithText("Kart bilgilerini göster").assertIsDisplayed()
        assert(rule.onAllNodesWithTextCount("4000 0000 0000 8851") == 0)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(t: String) =
        onAllNodes(androidx.compose.ui.test.hasText(t)).fetchSemanticsNodes().size
}
