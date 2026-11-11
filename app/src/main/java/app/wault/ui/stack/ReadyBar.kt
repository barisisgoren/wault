package app.wault.ui.stack

import app.wault.ui.common.rememberPulse
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.wault.data.CardKind
import app.wault.data.WaultCard
import app.wault.design.Glyph
import app.wault.design.GlyphIcon
import app.wault.design.SwitchOn
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.ui.Environment
import app.wault.ui.common.AppIcon

/**
 * Yığının altındaki tek satırlık durum çubuğu: şu an okuyucuya hangi kartın hazır olduğunu söyler.
 * Hiçbir şey seçili değilken telefonun ne yapacağı ekranda belirsiz kalmasın diye var.
 */
@Composable
fun ReadyBar(cards: List<WaultCard>, env: Environment, onNfcSettings: () -> Unit, modifier: Modifier = Modifier) {
    if (!env.nfcAvailable) return
    val c = Wault.colors
    val ready = cards.firstOrNull { it.id in env.defaultIds }
    val hasPayment = cards.any { it.kind == CardKind.PAYMENT && it.payment != null }
    if (env.nfcEnabled && ready == null && !hasPayment) return

    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(if (c.isDark) 0.dp else 3.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(c.surface)
            .border(0.5.dp, c.separator, shape)
            .then(if (!env.nfcEnabled) Modifier.clickable(role = Role.Button, onClick = onNfcSettings) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            !env.nfcEnabled -> {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(c.danger.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.CONTACTLESS, c.danger, size = 20.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("NFC kapalı", style = WaultType.headline, color = c.label)
                    Text("Temassız ödeme için aç", style = WaultType.footnote, color = c.secondary)
                }
                Text("Aç", style = WaultType.headline, color = c.accent, modifier = Modifier.padding(end = 6.dp))
            }
            ready != null -> {
                val link = ready.payment
                AppIcon(link?.packageName, 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ödemeye hazır", style = WaultType.headline, color = c.label)
                    Text(
                        ready.title.ifBlank { link?.appLabel ?: "" },
                        style = WaultType.footnote, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                LiveDot(modifier = Modifier.padding(end = 8.dp))
            }
            else -> {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(c.surfaceAlt), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.CONTACTLESS, c.secondary, size = 20.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ödeme kartı seçilmedi", style = WaultType.headline, color = c.label)
                    Text("Bir karta dokununca ödeme ona geçer", style = WaultType.footnote, color = c.secondary)
                }
            }
        }
    }
}

/** Yeşil nokta ve dışa yayılan halka; yalnızca ödeme gerçekten hazırken görünür, birkaç tur oynayıp durur. */
@Composable
private fun LiveDot(modifier: Modifier = Modifier) {
    val pulse = rememberPulse(cycles = 4, durationMs = 1900)
    Box(
        modifier
            .size(22.dp)
            .drawBehind {
                val p = pulse.value(rest = 0.4f)
                drawCircle(SwitchOn.copy(alpha = (1f - p) * 0.4f), radius = size.minDimension / 2 * (0.45f + 0.55f * p))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(10.dp).background(SwitchOn, CircleShape))
    }
}
