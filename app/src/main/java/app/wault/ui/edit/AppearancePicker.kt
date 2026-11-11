package app.wault.ui.edit

import app.wault.design.rememberCardArt
import app.wault.design.ChoiceRow
import app.wault.ui.common.withCustomColor
import app.wault.ui.common.ColorSheet
import app.wault.ui.common.PaletteGrid
import app.wault.data.InkMode
import app.wault.data.CardStyle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.wault.data.*
import app.wault.design.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun AppearancePicker(card: WaultCard, onChange: (WaultCard) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Wault.colors
    var tab by remember { mutableIntStateOf(when (card.faceSource) { FaceSource.THEME -> 0; FaceSource.CATALOG -> 1; FaceSource.CUSTOM -> 2 }) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            loading = true
            runCatching { withContext(Dispatchers.IO) { CardImages.decodePhoto(context, uri) } }
                .onSuccess { photo = it; error = null }.onFailure { error = "Görsel açılamadı. Başka bir fotoğraf seç." }
            loading = false
        }
    }
    var sheet by remember { mutableStateOf(false) }
    val available by produceState(true, card.faceSource, card.faceRef) {
        value = card.faceSource == FaceSource.THEME || withContext(Dispatchers.IO) { CardImages.load(context, card.faceSource, card.faceRef) != null }
    }
    Group(
        header = "Kart görünümü",
        footer = error ?: if (!available) "Görsel bulunamadı; renk gösteriliyor. Görseli yeniden seçebilirsin." else "Görünüm banka bağlantısını ve ödeme ağını değiştirmez.",
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Segmented(listOf(0, 1, 2), tab, { listOf("Renkler", "Modeller", "Fotoğraf")[it] }, { tab = it })
            when (tab) {
                0 -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PaletteGrid(card, onChange, onCustom = { sheet = true })
                    ChoiceRow("Stil") {
                        Segmented(
                            listOf<CardStyle?>(null, CardStyle.FLAT, CardStyle.GRADIENT), card.style,
                            { it?.label ?: "Varsayılan" }, { onChange(card.copy(style = it)) },
                        )
                    }
                    ChoiceRow("Yazı rengi") {
                        Segmented(InkMode.entries, card.ink, { it.label }, { onChange(card.copy(ink = it)) })
                    }
                }
                1 -> {
                    val pkg = card.payment?.packageName
                    val faces = CardCatalog.all.filter { pkg == null || pkg.contains(it.packageHint, ignoreCase = true) }
                    if (faces.isEmpty()) {
                        Text(
                            "Bu banka için hazır kart görseli yok. Renk seçebilir ya da kendi kartının fotoğrafını kullanabilirsin.",
                            style = WaultType.subhead, color = c.secondary,
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(faces, key = { it.id }) { face ->
                                val on = card.faceSource == FaceSource.CATALOG && card.faceRef == face.id
                                Column(
                                    Modifier.width(216.dp).clip(RoundedCornerShape(14.dp))
                                        .selectable(on, role = Role.RadioButton) { onChange(card.copy(faceSource = FaceSource.CATALOG, faceRef = face.id)) }
                                        .padding(4.dp),
                                ) {
                                    CardSurface(card.copy(faceSource = FaceSource.CATALOG, faceRef = face.id), elevation = 2.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Text(face.product, style = WaultType.bodyMedium, color = if (on) c.accent else c.label)
                                    Text("${face.bank}, ${face.network.label}", style = WaultType.footnote, color = c.secondary)
                                }
                            }
                        }
                        Text(
                            "Hazır kart görselleridir.",
                            style = WaultType.footnote, color = c.secondary,
                        )
                    }
                }
                2 -> {
                    Text("Kartının fotoğrafını seç. Sonraki adımda kadrajı ayarlarsın.", style = WaultType.body, color = c.label)
                    PrimaryButton(if (loading) "Görsel açılıyor…" else "Fotoğraf seç", { if (!loading) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    Text("Fotoğraf yalnızca bu telefonda işlenir ve şifreli saklanır.", style = WaultType.footnote, color = c.secondary)
                }
            }
        }
    }
    if (sheet) {
        val art = rememberCardArt(card)
        ColorSheet(current = art.base, onPick = { onChange(card.withCustomColor(it)) }, onDone = { sheet = false })
    }
    photo?.let { bitmap ->
        PhotoCrop(bitmap, onCancel = { photo = null }, onConfirm = { cropped ->
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { CardImages.save(context, cropped) } }
                    .onSuccess { onChange(card.copy(faceSource = FaceSource.CUSTOM, faceRef = it)); photo = null; error = null }
                    .onFailure { error = "Fotoğraf kaydedilemedi. Yeniden deneyebilirsin."; photo = null }
            }
        })
    }
}

@Composable
private fun PhotoCrop(bitmap: Bitmap, onCancel: () -> Unit, onConfirm: (Bitmap) -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var saving by remember { mutableStateOf(false) }
    val c=Wault.colors
    Dialog(onDismissRequest=onCancel, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().padding(20.dp), verticalArrangement=Arrangement.spacedBy(24.dp)) {
            NavBar("Fotoğrafı ayarla", leading={ BarButton("Vazgeç",onCancel) })
            Spacer(Modifier.weight(1f))
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clip(RoundedCornerShape(14.dp)).background(c.surfaceAlt)
                .onSizeChanged { size=it }.pointerInput(bitmap) {
                    detectTransformGestures { _, pan, scale, _ ->
                        zoom=(zoom*scale).coerceIn(1f,5f)
                        val base=max(size.width.toFloat()/bitmap.width,size.height.toFloat()/bitmap.height)
                        val mx=max(0f,(bitmap.width*base*zoom-size.width)/2f)
                        val my=max(0f,(bitmap.height*base*zoom-size.height)/2f)
                        offset=Offset((offset.x+pan.x).coerceIn(-mx,mx),(offset.y+pan.y).coerceIn(-my,my))
                    }
                }) {
                val base=max(this.size.width/bitmap.width,this.size.height/bitmap.height)*zoom
                val w=bitmap.width*base; val h=bitmap.height*base
                drawImage(bitmap.asImageBitmap(), dstOffset=androidx.compose.ui.unit.IntOffset(((this.size.width-w)/2+offset.x).toInt(),((this.size.height-h)/2+offset.y).toInt()),dstSize=IntSize(w.toInt(),h.toInt()))
            }
            Text("Yakınlaştırmak için iki parmağını kullan. Kadrajı değiştirmek için fotoğrafı kaydır.",style=WaultType.body,color=c.secondary)
            Spacer(Modifier.weight(1f))
            PrimaryButton(if(saving) "Kaydediliyor…" else "Bu görünümü kullan", {
                if(!saving && size.width>0) {
                    saving=true
                    val output=Bitmap.createBitmap(1200,(1200/CARD_RATIO).toInt(),Bitmap.Config.ARGB_8888)
                    val factor=1200f/size.width
                    val scale=max(size.width.toFloat()/bitmap.width,size.height.toFloat()/bitmap.height)*zoom
                    val w=bitmap.width*scale; val h=bitmap.height*scale
                    val x=(size.width-w)/2+offset.x; val y=(size.height-h)/2+offset.y
                    Canvas(output).drawBitmap(bitmap,null,RectF(x*factor,y*factor,(x+w)*factor,(y+h)*factor),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    onConfirm(output)
                }
            })
        }
    }
}
