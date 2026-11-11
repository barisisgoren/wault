package app.wault.ui.edit

import app.wault.design.rememberCardArt
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.wault.data.Reorder
import app.wault.data.WaultCard
import app.wault.design.BarButton
import app.wault.design.CardArtSwatch
import app.wault.design.CardArts
import app.wault.design.Glyph
import app.wault.design.GlyphIcon
import app.wault.design.GroupShape
import app.wault.design.NavBar
import app.wault.design.Separator
import app.wault.design.Wault
import app.wault.design.WaultType

private val RowHeight = 68.dp

/** Kartları tutamaçtan sürükleyerek sıralama. En üstteki kart yığının başında durur. */
@Composable
fun ReorderScreen(cards: List<WaultCard>, onDone: (List<String>) -> Unit, onOpen: (WaultCard) -> Unit) {
    val c = Wault.colors
    val byId = remember(cards) { cards.associateBy { it.id } }
    var order by remember(cards) { mutableStateOf(cards.map { it.id }) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NavBar("Kartları sırala", trailing = { BarButton("Bitti", { onDone(order) }, bold = true) })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(GroupShape).background(c.surface)) {
                order.forEachIndexed { index, id ->
                    val card = byId[id] ?: return@forEachIndexed
                    key(id) {
                        val isDragging = dragging == id
                        val lift by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "lift")
                        Column(
                            Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                                .shadow(lift, GroupShape)
                                .background(c.surface)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(RowHeight)
                                    .clickable { onOpen(card) }
                                    .semantics {
                                        customActions = listOfNotNull(
                                            if (index > 0) CustomAccessibilityAction("Yukarı taşı") {
                                                order = Reorder.move(order, index, index - 1); true
                                            } else null,
                                            if (index < order.lastIndex) CustomAccessibilityAction("Aşağı taşı") {
                                                order = Reorder.move(order, index, index + 1); true
                                            } else null,
                                        )
                                    }
                                    .padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CardArtSwatch(rememberCardArt(card), Modifier.width(56.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(card.title, style = WaultType.body, color = c.label, maxLines = 1)
                                    Text(card.kind.label(), style = WaultType.footnote, color = c.secondary)
                                }
                                Box(
                                    Modifier
                                        .size(56.dp)
                                        .pointerInput(id) {
                                            detectDragGestures(
                                                onDragStart = { dragging = id; dragOffset = 0f },
                                                onDragEnd = { dragging = null; dragOffset = 0f },
                                                onDragCancel = { dragging = null; dragOffset = 0f },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y
                                                    val from = order.indexOf(id)
                                                    if (dragOffset > rowPx / 2 && from < order.lastIndex) {
                                                        order = Reorder.move(order, from, from + 1); dragOffset -= rowPx
                                                    } else if (dragOffset < -rowPx / 2 && from > 0) {
                                                        order = Reorder.move(order, from, from - 1); dragOffset += rowPx
                                                    }
                                                },
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                ) { GlyphIcon(Glyph.HANDLE, c.tertiary) }
                            }
                            if (index < order.lastIndex && !isDragging) Separator(86.dp)
                        }
                    }
                }
            }
            Text(
                "Tutamaçtan sürükle. En üstteki kart cüzdanın başında durur.",
                style = WaultType.footnote, color = c.secondary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
        }
    }
}
