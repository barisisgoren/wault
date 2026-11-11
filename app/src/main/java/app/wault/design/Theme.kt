package app.wault.design

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.wault.data.AccentChoice
import app.wault.data.CardStyle

/** Kullanıcının Ayarlar > Görünüm'de seçtikleri; tüm ekranlar buradan okur. */
@Immutable
data class Appearance(
    /** Kartın kendi stili yoksa geçerli olan yüzey. */
    val cardStyle: CardStyle = CardStyle.FLAT,
    /** Seçili kartın ışığı ve gradyan kartlarda eğimle parlama. */
    val lightEffects: Boolean = false,
)

val LocalAppearance = staticCompositionLocalOf { Appearance() }

@Composable
fun WaultTheme(
    dark: Boolean = isSystemInDarkTheme(),
    accent: AccentChoice = AccentChoice.BLUE,
    appearance: Appearance = Appearance(),
    content: @Composable () -> Unit,
) {
    val base = if (dark) DarkColors else LightColors
    val colors = base.copy(accent = Color(if (dark) accent.dark else accent.light))
    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(
        LocalWaultColors provides colors,
        LocalAppearance provides appearance,
        LocalReducedMotion provides reduced,
        LocalContentColor provides colors.label,
        LocalTextStyle provides WaultType.body.copy(color = colors.label),
        LocalTextSelectionColors provides TextSelectionColors(colors.accent, colors.accent.copy(alpha = 0.25f)),
        content = content,
    )
}

object Wault {
    val colors: WaultColors
        @Composable get() = LocalWaultColors.current
    val reducedMotion: Boolean
        @Composable get() = LocalReducedMotion.current
    val appearance: Appearance
        @Composable get() = LocalAppearance.current
}
