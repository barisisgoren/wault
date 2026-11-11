package app.wault.ui.add

import app.wault.design.GlyphIcon
import app.wault.design.CardArts
import app.wault.design.CardArtSwatch
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.wault.data.CardKind
import app.wault.design.Glyph
import app.wault.design.Group
import app.wault.design.GroupRow
import app.wault.design.Separator
import app.wault.design.Wault
import app.wault.design.WaultType

/** Her seçenek, eklenecek kartın başlangıçtaki rengiyle küçük bir kart olarak gösterilir. */
private data class AddOption(val kind: CardKind, val glyph: Glyph, val title: String, val detail: String)

private val options = listOf(
    AddOption(CardKind.PAYMENT, Glyph.CONTACTLESS, "Ödeme kartı", "Bankanın uygulamasıyla temassız öde"),
    AddOption(CardKind.LOYALTY, Glyph.STAR, "Sadakat kartı", "Üye numarası, puan ve barkod"),
    AddOption(CardKind.CODE, Glyph.QR, "QR veya barkod", "Bilet, giriş kodu, kupon"),
    AddOption(CardKind.NFC_COPY, Glyph.NFC_PHONE, "NFC kartı kopyala", "Etiket, anahtarlık, NDEF kartlar"),
    AddOption(CardKind.SHOWCASE, Glyph.CARD, "Sahte kart (rol oyunu)", "İstediğin ad ve bankayla; tutar yazıp \"Ödendi\" göster"),
)

@Composable
private fun MiniCard(kind: CardKind, glyph: Glyph) {
    // Ödeme kartının gerçek rengi bankaya göre belirlenir; burada temsil olarak okyanus tonu
    val art = CardArts.byId(if (kind == CardKind.PAYMENT) "ocean" else CardArts.defaultFor(kind))
    Box(contentAlignment = Alignment.Center) {
        CardArtSwatch(art, Modifier.width(48.dp), bar = false)
        GlyphIcon(glyph, art.ink, size = 18.dp)
    }
}

@Composable
fun AddSheet(onDismiss: () -> Unit, onPick: (CardKind) -> Unit) {
    val c = Wault.colors
    val reduced = Wault.reducedMotion
    val rise = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 500f)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f * rise.value))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = (1f - rise.value) * size.height }
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(c.bg)
                .clickable(remember { MutableInteractionSource() }, null) { }
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.tertiary))
            Spacer(Modifier.height(18.dp))
            Text("Kart ekle", style = WaultType.title, color = c.label, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(14.dp))
            Group {
                options.forEachIndexed { i, o ->
                    GroupRow(
                        o.title, subtitle = o.detail,
                        leading = { MiniCard(o.kind, o.glyph) },
                        chevron = true, onClick = { onPick(o.kind) },
                    )
                    if (i < options.lastIndex) Separator(78.dp)
                }
            }
        }
    }
}
