package app.wault

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wault.data.AccentChoice
import app.wault.data.CardStyle
import app.wault.data.Prefs
import app.wault.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PrefsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun freshInstallIsFlatSystemThemedAndEffectFree() {
        context.getSharedPreferences("wault", Context.MODE_PRIVATE).edit().clear().commit()
        val p = Prefs(context).state.value
        assertEquals(CardStyle.FLAT, p.cardStyle)
        assertEquals(ThemeMode.SYSTEM, p.themeMode)
        assertEquals(AccentChoice.BLUE, p.accent)
        assertFalse(p.lightEffects)
    }

    @Test fun personalisationSurvivesARestart() {
        context.getSharedPreferences("wault", Context.MODE_PRIVATE).edit().clear().commit()
        Prefs(context).update { it.copy(themeMode = ThemeMode.DARK, cardStyle = CardStyle.GRADIENT, accent = AccentChoice.PINK, lightEffects = true) }
        val again = Prefs(context).state.value
        assertEquals(ThemeMode.DARK, again.themeMode)
        assertEquals(CardStyle.GRADIENT, again.cardStyle)
        assertEquals(AccentChoice.PINK, again.accent)
        assertTrue(again.lightEffects)
    }

    @Test fun unknownStoredNamesFallBackToDefaults() {
        context.getSharedPreferences("wault", Context.MODE_PRIVATE).edit()
            .putString("cardStyle", "HOLOGRAM").putString("accent", "NEON").putString("themeMode", "").commit()
        val p = Prefs(context).state.value
        assertEquals(CardStyle.FLAT, p.cardStyle)
        assertEquals(AccentChoice.BLUE, p.accent)
        assertEquals(ThemeMode.SYSTEM, p.themeMode)
    }
}
