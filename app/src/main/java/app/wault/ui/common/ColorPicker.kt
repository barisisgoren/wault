package app.wault.ui.common

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.wault.data.FaceSource
import app.wault.data.WaultCard
import app.wault.design.BarButton
import app.wault.design.CardArt
import app.wault.design.CardArts
import app.wault.design.GroupShape
import app.wault.design.Glyph
import app.wault.design.GlyphIcon
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.design.colorFromArgbLong
import app.wault.design.hsvColor
import app.wault.design.parseHexColor
import app.wault.design.prefersLightInk
import app.wault.design.rememberCardArt
import app.wault.design.toArgbLong
import app.wault.design.toHex

/** Hazır renklerden birini seçer; fotoğraf ya da model yüzeyi varsa tema yüzeyine döner. */
fun WaultCard.withPreset(id: String): WaultCard =
    copy(artId = id, customColor = null, faceSource = FaceSource.THEME, faceRef = null)

fun WaultCard.withCustomColor(color: Color): WaultCard =
    copy(artId = "custom", customColor = color.toArgbLong(), faceSource = FaceSource.THEME, faceRef = null)

private val RingExtra = 10.dp

/** Düz renk noktası. Seçiliyse etrafında vurgu halkası ve onay işareti çıkar. */
@Composable
fun ColorDot(
    color: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = Wault.colors
    val ink = if (prefersLightInk(color)) Color.White else Color(0xFF17181C)
    Box(
        modifier
            .size(size + RingExtra)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(size + RingExtra).border(2.dp, c.accent, CircleShape))
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                // beyaz ve fildişi gibi açık renkler zeminde kaybolmasın
                .border(0.75.dp, c.separator, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            content()
            if (selected) GlyphIcon(Glyph.CHECK, ink, size = size * 0.45f)
        }
    }
}

private val WheelColors = listOf(0xFFE4474D, 0xFFF2A33A, 0xFFEDD83C, 0xFF3FBF6B, 0xFF3BA6E6, 0xFF7B4CCB)

/** "Özel renk" noktası: altı düz dilimli çark. Özel renk seçiliyse çekirdeği o renkte olur. */
@Composable
fun CustomColorDot(core: Color?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val c = Wault.colors
    Box(
        modifier
            .size(size + RingExtra)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Özel renk" },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(size + RingExtra).border(2.dp, c.accent, CircleShape))
        Canvas(Modifier.size(size).clip(CircleShape)) {
            WheelColors.forEachIndexed { i, col ->
                drawArc(Color(col), startAngle = -90f + i * 60f, sweepAngle = 60f, useCenter = true)
            }
            drawCircle(core ?: Color.White, radius = this.size.minDimension * 0.27f)
        }
    }
}

/** Seçilebilecek renkler: (varsa) uygulama rengi, hazır renkler, özel renk. */
private fun paletteOf(card: WaultCard): List<CardArt> =
    CardArts.all.filter { it.id != "auto" || card.payment != null }

/** Editörde: 6 sütunlu renk noktaları ızgarası. */
@Composable
fun PaletteGrid(card: WaultCard, onChange: (WaultCard) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier) {
    val themeFace = card.faceSource == FaceSource.THEME
    val autoArt = rememberCardArt(card.copy(artId = "auto", ink = app.wault.data.InkMode.AUTO))
    val custom = rememberCardArt(card.copy(artId = "custom"))
    val rows = paletteOf(card).map<CardArt, @Composable () -> Unit> { art ->
        {
            if (art.id == "auto") {
                ColorDot(autoArt.base, themeFace && card.artId == "auto", "Uygulama rengi", { onChange(card.withPreset("auto")) }) {
                    AppIcon(card.payment?.packageName, 22.dp)
                }
            } else {
                ColorDot(art.base, themeFace && card.artId == art.id, art.name, { onChange(card.withPreset(art.id)) })
            }
        }
    } + listOf<@Composable () -> Unit> {
        CustomColorDot(if (card.artId == "custom") custom.base else null, themeFace && card.artId == "custom", onCustom)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { it() }
                repeat(6 - row.size) { Spacer(Modifier.size(40.dp + RingExtra)) }
            }
        }
    }
}

/** Kart detayında: tek satır yatay kayan renk noktaları. Dokununca kart anında boyanır. */
@Composable
fun QuickColors(card: WaultCard, onChange: (WaultCard) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier) {
    val themeFace = card.faceSource == FaceSource.THEME
    val autoArt = rememberCardArt(card.copy(artId = "auto", ink = app.wault.data.InkMode.AUTO))
    val custom = rememberCardArt(card.copy(artId = "custom"))
    LazyRow(
        modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(paletteOf(card), key = { it.id }) { art ->
            if (art.id == "auto") {
                ColorDot(autoArt.base, themeFace && card.artId == "auto", "Uygulama rengi", { onChange(card.withPreset("auto")) }, size = 34.dp) {
                    AppIcon(card.payment?.packageName, 20.dp)
                }
            } else {
                ColorDot(art.base, themeFace && card.artId == art.id, art.name, { onChange(card.withPreset(art.id)) }, size = 34.dp)
            }
        }
        item(key = "custom") {
            CustomColorDot(if (card.artId == "custom") custom.base else null, themeFace && card.artId == "custom", onCustom, size = 34.dp)
        }
    }
}

// Renk tablosu: 8 ton ailesi × 5 kademe (açık pastelden koyuya) + gri satırı
private val Hues = listOf(2f, 24f, 48f, 100f, 168f, 205f, 258f, 328f)
private val Tones = listOf(0.28f to 1.0f, 0.55f to 1.0f, 0.85f to 0.95f, 0.9f to 0.75f, 0.9f to 0.5f)
private val Grays = listOf(1.0f, 0.86f, 0.72f, 0.58f, 0.44f, 0.30f, 0.18f, 0.08f)

/** Özel renk sayfası: renk tablosu ve renk kodu. Üstte kartın canlı önizlemesi görünür kalsın diye arkayı az karartır. */
@Composable
fun ColorSheet(current: Color?, onPick: (Color) -> Unit, onDone: () -> Unit) {
    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.16f) }
        Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDone)) {
            ColorSheetContent(current, onPick, onDone, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun ColorSheetContent(current: Color?, onPick: (Color) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val c = Wault.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(c.bg)
            .clickable(remember { MutableInteractionSource() }, null) { } // sayfa içine dokunmak kapatmasın
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.tertiary))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Özel renk", style = WaultType.title, color = c.label, modifier = Modifier.weight(1f).padding(start = 4.dp))
            BarButton("Bitti", onDone, bold = true)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Tones.forEach { (s, v) ->
                PaletteRow(Hues.map { hsvColor(it, s, v) }, current, onPick)
            }
            PaletteRow(Grays.map { Color(it, it, it) }, current, onPick)
        }
        Spacer(Modifier.height(14.dp))
        HexRow(current, onPick)
    }
}

@Composable
private fun PaletteRow(colors: List<Color>, current: Color?, onPick: (Color) -> Unit) {
    val c = Wault.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        colors.forEach { color ->
            val on = current != null && current.toHex() == color.toHex()
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.18f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(color)
                    .border(if (on) 2.5.dp else 0.5.dp, if (on) c.accent else c.separator, RoundedCornerShape(9.dp))
                    .selectable(on, role = Role.RadioButton) { onPick(color) }
                    .semantics { contentDescription = color.toHex() },
            )
        }
    }
}

@Composable
private fun HexRow(current: Color?, onPick: (Color) -> Unit) {
    val c = Wault.colors
    val initial = current?.toHex() ?: "#"
    // Metni ve imleci birlikte tutarız: temizlenen metinde imleç sona gitsin, yazılan haneler sırayla eklensin
    var field by remember(current) { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    Row(
        Modifier.fillMaxWidth().clip(GroupShape).background(c.surface).height(52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Renk kodu", style = WaultType.body, color = c.label, modifier = Modifier.width(112.dp))
        BasicTextField(
            value = field,
            onValueChange = { v ->
                val cleaned = "#" + v.text.filter { it in "0123456789abcdefABCDEF" }.take(6).uppercase()
                field = TextFieldValue(cleaned, TextRange(cleaned.length))
                if (cleaned.length == 7) parseHexColor(cleaned)?.let(onPick)
            },
            textStyle = WaultType.body.copy(color = c.label),
            cursorBrush = SolidColor(c.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.size(26.dp).clip(CircleShape).background(current ?: Color.Transparent).border(0.75.dp, c.separator, CircleShape))
    }
}
