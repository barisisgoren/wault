package app.wault.design

import app.wault.data.CardStyle
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wault.R
import app.wault.data.CardImages
import app.wault.data.CardKind
import app.wault.data.CardNetwork
import app.wault.data.FaceSource
import app.wault.data.NfcCapability
import app.wault.data.WaultCard
import app.wault.ui.common.AppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ISO/IEC 7810 ID-1: 85.60 × 53.98 mm. */
const val CARD_RATIO = 85.60f / 53.98f

/** Yığında her kartın en az görünen üst şeridi. */
val CardPeek = 64.dp

/** Gerçek kartın yarıçapı genişliğin yaklaşık %3,7'si; telefon genişliğinde ~14dp. */
val CardCorner = 14.dp

private val CardPadding = 18.dp

/** Kartın renk görseli. "auto" ise bankanın uygulama ikonundan, "custom" ise kartın kendi renginden gelir. */
@Composable
fun rememberCardArt(card: WaultCard): CardArt {
    val context = LocalContext.current
    return remember(card.artId, card.customColor, card.ink, card.payment?.packageName) {
        val art = when (card.artId) {
            "auto" -> CardArts.fromIcon(context, card.payment?.packageName)
            "custom" -> CardArt("custom", "Özel", colorFromArgbLong(card.customColor ?: CardArts.custom.base.toArgbLong()))
            else -> CardArts.byId(card.artId)
        }
        art.withInk(card.ink)
    }
}

/**
 * Kartın yüzü. Varsayılan düz tek renk (Apple Wallet geçişleri gibi): renk, ince kenar çizgisi ve yumuşak gölge.
 * Kartın ya da uygulamanın stili gradyansa: renk geçişi, köşede ikinci renk lekesi, alt kenarda koyulaşma,
 * bantlanmayı kıran ince tanecik; "Işık efektleri" açıksa telefonu eğince kayan bir parlama.
 * Üstünde sadece kartı tanıtan bilgiler durur.
 */
@Composable
fun CardSurface(
    card: WaultCard,
    modifier: Modifier = Modifier,
    isDefault: Boolean = false,
    elevation: Dp = 8.dp,
    /** false: sadece kartın kendisi (alt yığındaki kenarlar için yazısız). */
    showFace: Boolean = true,
    /** true: kart numarası tam görünür (kimlik doğrulamadan sonra). */
    revealed: Boolean = false,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(CardCorner)
    val art = rememberCardArt(card)
    val appearance = Wault.appearance
    val style = card.style ?: appearance.cardStyle
    val photo by produceState<Bitmap?>(null, card.faceSource, card.faceRef) {
        value = if (card.faceSource == FaceSource.THEME) null
        else withContext(Dispatchers.IO) { CardImages.load(context, card.faceSource, card.faceRef) }
    }

    val bitmap = photo
    if (bitmap != null) {
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        Box(modifier.fillMaxWidth().aspectRatio(CARD_RATIO)) {
            Image(
                image, card.title,
                Modifier
                    .fillMaxSize()
                    .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(shape)
                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), shape),
                contentScale = ContentScale.Crop,
            )
            if (showFace && card.kind == CardKind.PAYMENT && isDefault) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp)
                        .background(Color.Black.copy(alpha = 0.42f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { GlyphIcon(Glyph.CONTACTLESS, Color.White, size = 20.dp) }
            }
        }
        return
    }

    val shine = appearance.lightEffects && !Wault.reducedMotion && style == CardStyle.GRADIENT
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_RATIO)
            .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .drawWithCache {
                val dark = art.dark
                val corner = CornerRadius(CardCorner.toPx())
                val rimStroke = Stroke(1.5.dp.toPx()) // yarısı kırpılır: görünen çizgi ~0,75dp
                if (style == CardStyle.FLAT) {
                    // Yığında üst üste duran aynı renkli kartlar ve açık zeminde beyaz kartlar için ince kenar
                    val rim = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.09f)
                    onDrawWithContent {
                        drawRect(art.base)
                        drawContent()
                        drawRoundRect(rim, cornerRadius = corner, style = rimStroke)
                    }
                } else {
                    val base = Brush.linearGradient(art.stops, Offset(0f, 0f), Offset(size.width, size.height))
                    val glow = Brush.radialGradient(
                        listOf(art.glow.copy(alpha = if (dark) 0.5f else 0.55f), Color.Transparent),
                        center = Offset(size.width * 1.0f, size.height * 1.1f),
                        radius = size.width * 0.85f,
                    )
                    val floor = Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (dark) 0.2f else 0.05f),
                    )
                    val rim = Brush.linearGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.34f else 0.85f),
                        0.45f to Color.White.copy(alpha = 0.05f),
                        1f to Color.White.copy(alpha = if (dark) 0.12f else 0.4f),
                        start = Offset(0f, 0f), end = Offset(size.width, size.height),
                    )
                    val sheenAlpha = if (dark) 0.15f else 0.5f
                    val grainAlpha = if (dark) 0.5f else 0.3f
                    onDrawWithContent {
                        drawRect(base)
                        drawRect(glow)
                        val tx = if (shine) Tilt.x else 0f
                        val ty = if (shine) Tilt.y else 0f
                        drawRect(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = sheenAlpha * (1f + 0.6f * kotlin.math.abs(tx))), Color.Transparent),
                                center = Offset(size.width * (0.14f + 0.34f * tx), size.height * (-0.1f + 0.2f * ty)),
                                radius = size.width * 0.92f,
                            )
                        )
                        drawRect(floor)
                        drawRect(GrainTile.brush, alpha = grainAlpha)
                        drawContent()
                        drawRoundRect(rim, cornerRadius = corner, style = rimStroke)
                    }
                }
            }
            .padding(CardPadding)
    ) {
        if (showFace) CardFace(card, art, isDefault, revealed)
    }
}

@Composable
private fun CardFace(card: WaultCard, art: CardArt, isDefault: Boolean, revealed: Boolean = false) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarkTile(card, art)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    card.title, style = WaultType.cardIssuer, color = art.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (card.subtitle.isNotBlank()) {
                    Text(
                        card.subtitle, style = WaultType.cardType, color = art.inkSoft,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            TopMark(card, art, isDefault)
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BottomLines(card, art, revealed)
            }
            Spacer(Modifier.width(12.dp))
            NetworkBadge(card.network, art)
        }
    }
}

/** Kartın solundaki kimlik karesi: ödeme kartında bankanın uygulama ikonu, diğerlerinde baş harf ya da işlev simgesi. */
@Composable
private fun MarkTile(card: WaultCard, art: CardArt) {
    val size = 36.dp
    val link = card.payment
    if (card.kind == CardKind.PAYMENT && link != null) {
        AppIcon(link.packageName, size)
        return
    }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(9.dp)).background(art.ink.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        when (card.kind) {
            CardKind.CODE -> GlyphIcon(Glyph.QR, art.ink, size = 20.dp)
            CardKind.NFC_COPY -> GlyphIcon(Glyph.NFC_PHONE, art.ink, size = 20.dp)
            CardKind.PAYMENT -> GlyphIcon(Glyph.CARD, art.ink, size = 20.dp)
            else -> Text(monogram(card.title), style = WaultType.headline, color = art.ink)
        }
    }
}

private val TrLocale = java.util.Locale.forLanguageTag("tr-TR")

private fun monogram(title: String): String =
    title.trim().firstOrNull { it.isLetterOrDigit() }?.toString()?.uppercase(TrLocale) ?: "•"

@Composable
private fun TopMark(card: WaultCard, art: CardArt, isDefault: Boolean) {
    when (card.kind) {
        CardKind.PAYMENT -> Box(
            Modifier.size(34.dp).background(if (isDefault) art.ink.copy(alpha = 0.18f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(Glyph.CONTACTLESS, art.ink.copy(alpha = if (isDefault) 1f else 0.5f), size = 22.dp)
        }
        CardKind.LOYALTY -> if (card.points.isNotBlank()) {
            Text(card.points, style = WaultType.headline, color = art.ink, maxLines = 1)
        }
        else -> Unit
    }
}

@Composable
private fun BottomLines(card: WaultCard, art: CardArt, revealed: Boolean = false) {
    when (card.kind) {
        CardKind.PAYMENT, CardKind.SHOWCASE -> {
            if (card.holder.isNotBlank() || card.expiry.isNotBlank()) {
                Text(
                    listOf(card.holder.uppercase(TrLocale), card.expiry).filter { it.isNotBlank() }.joinToString("    "),
                    style = WaultType.caption, color = art.inkSoft, maxLines = 1,
                )
            }
            if (revealed && card.pan.isNotBlank()) {
                Text(card.pan.chunked(4).joinToString(" "), style = WaultType.cardNumber.copy(fontSize = 21.sp), color = art.ink, maxLines = 1, softWrap = false)
            } else if (card.last4.isNotBlank()) {
                Text("•••• ${card.last4}", style = WaultType.cardNumber, color = art.ink)
            } else if (card.kind == CardKind.PAYMENT) {
                Text(
                    card.payment?.appLabel ?: "Ödeme kartı", style = WaultType.footnote,
                    color = art.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CardKind.LOYALTY -> if (card.memberNo.isNotBlank()) {
            Text("Üye no", style = WaultType.caption, color = art.inkSoft)
            Text(card.memberNo, style = WaultType.cardNumber, color = art.ink, maxLines = 1)
        }
        CardKind.CODE -> Text(
            card.note.ifBlank { "Kodu göstermek için dokun" }, style = WaultType.footnote,
            color = art.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        CardKind.NFC_COPY -> {
            Text(
                if (card.nfc?.capability == NfcCapability.EMULATE) "Telefondan okutulabilir" else "Sadece kayıt",
                style = WaultType.caption, color = art.inkSoft,
            )
            Text(
                card.nfc?.uidHex?.chunked(2)?.joinToString(" ") ?: "",
                style = WaultType.cardNumber.copy(fontSize = 14.sp), color = art.ink, maxLines = 1,
            )
        }
    }
}

/**
 * Kart ağı işareti.
 * TROY: resmi vektör logo (TROY medya merkezi); Mastercard ve Amex tek renkli işaret (bkz. assets-src/SOURCES.md);
 * Visa yazıyla. Hepsi kartın yazı renginde, yani koyu kartta beyaz.
 */
@Composable
private fun NetworkBadge(network: CardNetwork, art: CardArt) {
    when (network) {
        CardNetwork.NONE -> Unit
        CardNetwork.TROY -> Image(
            painterResource(if (art.dark) R.drawable.ic_scheme_troy_white else R.drawable.ic_scheme_troy),
            contentDescription = "Troy",
            modifier = Modifier.height(15.dp),
        )
        CardNetwork.MASTERCARD -> Image(
            painterResource(R.drawable.ic_mc), contentDescription = network.label,
            modifier = Modifier.height(26.dp), colorFilter = ColorFilter.tint(art.ink),
        )
        CardNetwork.AMEX -> Image(
            painterResource(R.drawable.ic_amex), contentDescription = network.label,
            modifier = Modifier.height(26.dp), colorFilter = ColorFilter.tint(art.ink),
        )
        CardNetwork.VISA -> Text(
            "VISA", style = WaultType.cardScheme.copy(fontSize = 17.sp, letterSpacing = 0.8.sp), color = art.ink,
        )
    }
}

/** Kartlara ince tanecik: koyu gradyanlardaki bantlanmayı kırar, yüzeye dokunulabilir bir his verir. */
private object GrainTile {
    private val bitmap by lazy {
        val size = 96
        val rnd = java.util.Random(7)
        val px = IntArray(size * size) {
            val alpha = rnd.nextInt(22)
            val c = if (rnd.nextBoolean()) 255 else 0
            (alpha shl 24) or (c shl 16) or (c shl 8) or c
        }
        Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    val brush by lazy { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
}

/** Kart seçiciler için küçük kart örneği: düz renk. */
@Composable
fun CardArtSwatch(art: CardArt, modifier: Modifier = Modifier, bar: Boolean = true) {
    Box(
        modifier
            .aspectRatio(CARD_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .background(art.base)
            .border(0.75.dp, if (art.dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    ) {
        if (bar) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(7.dp).width(20.dp).height(3.dp)
                    .clip(RoundedCornerShape(2.dp)).background(art.ink.copy(alpha = 0.6f))
            )
        }
    }
}
