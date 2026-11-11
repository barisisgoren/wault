package app.wault.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GroupShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(16.dp)
val SwitchOn = Color(0xFF34C759)

/** Basınca hafifçe küçülen dokunma geri bildirimi. */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 900f), label = "press")
    return s
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Wault.colors.accent,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .scale(pressScale(source))
            .heightIn(min = 52.dp)
            .clip(ButtonShape)
            .background(if (enabled) color else color.copy(alpha = 0.35f))
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = WaultType.headline, color = Color.White)
    }
}

/** İkincil eylem: vurgu renginin açık tonu üstünde vurgu rengi yazı. */
@Composable
fun TintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Wault.colors.accent,
    leading: Glyph? = null,
) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .scale(pressScale(source))
            .heightIn(min = 50.dp)
            .clip(ButtonShape)
            .background(color.copy(alpha = if (Wault.colors.isDark) 0.22f else 0.12f))
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) { GlyphIcon(leading, color, size = 20.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, style = WaultType.headline, color = color)
    }
}

/** Gezinme çubuğundaki yazı düğmesi ("Vazgeç", "Kaydet", "Bitti"). */
@Composable
fun BarButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, bold: Boolean = false, enabled: Boolean = true) {
    val c = Wault.colors
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (bold) WaultType.headline else WaultType.body, color = if (enabled) c.accent else c.tertiary)
    }
}

/** Yuvarlak, gri zeminli ikon düğmesi (başlıktaki + ve ⋯). */
@Composable
fun CircleButton(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Wault.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            .scale(pressScale(source))
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.surfaceAlt), contentAlignment = Alignment.Center) {
            GlyphIcon(glyph, c.label, size = 18.dp)
        }
    }
}

/** Büyük başlıklı ekran üstü: solda başlık, sağda eylemler. */
@Composable
fun LargeTitleBar(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = WaultType.largeTitle, color = Wault.colors.label, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Ortalanmış başlıklı gezinme çubuğu. */
@Composable
fun NavBar(
    title: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) { leading() }
        Text(title, style = WaultType.headline, color = Wault.colors.label, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/** Gruplu bölüm: başlık, beyaz yuvarlatılmış kutu, isteğe bağlı açıklama. */
@Composable
fun Group(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Wault.colors
    Column(modifier.fillMaxWidth()) {
        if (header != null) {
            Text(header, style = WaultType.footnote, color = c.secondary, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        }
        Column(Modifier.fillMaxWidth().clip(GroupShape).background(c.surface), content = content)
        if (footer != null) {
            Text(footer, style = WaultType.footnote, color = c.secondary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
        }
    }
}

@Composable
fun Separator(inset: Dp = 16.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(Wault.colors.separator))
}

/** Renkli kare ikon kutusu (ayarlar listelerindeki gibi). */
@Composable
fun IconTile(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
        GlyphIcon(glyph, Color.White, size = 18.dp)
    }
}

/** Grup satırı: başlık, sağda değer ve/veya ok. */
@Composable
fun GroupRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    titleColor: Color = Wault.colors.label,
    leading: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Wault.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = WaultType.body, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = WaultType.footnote, color = c.secondary, maxLines = 2)
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(value, style = WaultType.body, color = c.secondary, maxLines = 1, textAlign = TextAlign.End)
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
        if (chevron) { Spacer(Modifier.width(6.dp)); GlyphIcon(Glyph.CHEVRON, c.tertiary, size = 16.dp) }
    }
}

/** Formdaki satır: solda etiket, sağda yazı alanı. */
@Composable
fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    caps: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    error: Boolean = false,
) {
    val c = Wault.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = WaultType.body, color = if (error) c.danger else c.label, modifier = Modifier.width(132.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = WaultType.body.copy(color = c.label),
            cursorBrush = SolidColor(c.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = caps),
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = WaultType.body, color = c.tertiary, maxLines = 1)
                    inner()
                }
            },
        )
    }
}

/** iOS tarzı anahtar. */
@Composable
fun Switch(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Wault.colors
    val track by animateColorAsState(if (checked) SwitchOn else c.surfaceAlt, label = "track")
    val knob by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 700f), label = "knob")
    Box(modifier.size(51.dp, 31.dp).graphicsLayer { alpha = if (enabled) 1f else 0.4f }.clip(CircleShape).background(track)) {
        Box(
            Modifier.offset(x = knob, y = 2.dp).size(27.dp)
                .shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White)
        )
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null, enabled: Boolean = true) {
    val c = Wault.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = WaultType.body, color = if (enabled) c.label else c.tertiary)
            if (subtitle != null) Text(subtitle, style = WaultType.footnote, color = c.secondary)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked, enabled = enabled)
    }
}

/** Bölmeli seçim (segmented control). */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Wault.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surfaceAlt).padding(2.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(8.dp)) else Modifier)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) (if (c.isDark) Color(0xFF636366) else Color.White) else Color.Transparent)
                    .selectable(on, role = Role.RadioButton) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = WaultType.subheadMedium, color = c.label, maxLines = 1)
            }
        }
    }
}

/** Seçili satırın sağındaki onay işareti. */
@Composable
fun CheckMark(checked: Boolean) {
    if (checked) GlyphIcon(Glyph.CHECK, Wault.colors.accent, size = 20.dp) else Spacer(Modifier.size(20.dp))
}

/** Gruplu listede başlığı üstte, seçim kontrolü altta olan satır (tema, stil, renk gibi seçimler için). */
@Composable
fun ChoiceRow(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = WaultType.footnote, color = Wault.colors.secondary)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
