package app.wault.ui.settings

import app.wault.ui.common.ColorDot
import app.wault.design.Tilt
import app.wault.design.ChoiceRow
import app.wault.design.CardSurface
import app.wault.data.WaultCard
import app.wault.data.ThemeMode
import app.wault.data.CardStyle
import app.wault.data.CardNetwork
import app.wault.data.AccentChoice
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import app.wault.data.FaceSource
import app.wault.ui.common.AppIcon
import app.wault.design.Segmented
import app.wault.DevOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.wault.BuildConfig
import app.wault.MainActivity
import app.wault.data.CardKind
import app.wault.design.BarButton
import app.wault.design.Glyph
import app.wault.design.Group
import app.wault.design.GroupRow
import app.wault.design.IconTile
import app.wault.design.LargeTitleBar
import app.wault.design.NavBar
import app.wault.design.PrimaryButton
import app.wault.design.Separator
import app.wault.design.SwitchRow
import app.wault.design.Wault
import app.wault.design.WaultType
import app.wault.nfc.PaymentSwitcher
import app.wault.nfc.ShizukuBridge
import app.wault.notify.PaidEvent
import app.wault.notify.PaymentEvents
import app.wault.notify.PaymentNotificationListener
import app.wault.security.BiometricGate
import app.wault.ui.Environment

@Composable
fun SettingsScreen(
    activity: MainActivity,
    env: Environment,
    onBack: () -> Unit,
    onReorder: () -> Unit,
    onPreviewOnboarding: () -> Unit = {},
) {
    val c = Wault.colors
    val container = activity.app.container
    val prefs by container.prefs.state.collectAsState()
    val bioAvailable = remember { BiometricGate.available(activity) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NavBar("", leading = { BarButton("Cüzdan", onBack) })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()
        ) {
            Text("Ayarlar", style = WaultType.largeTitle, color = c.label, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(16.dp))

            Group(header = "Görünüm") {
                ChoiceRow("Tema") {
                    Segmented(ThemeMode.entries, prefs.themeMode, { it.label }, { m -> DevOptions.darkOverride = null; container.prefs.update { it.copy(themeMode = m) } })
                }
                Separator()
                ChoiceRow("Vurgu rengi") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AccentChoice.entries.forEach { a ->
                            ColorDot(
                                Color(if (c.isDark) a.dark else a.light), prefs.accent == a, a.label,
                                { container.prefs.update { it.copy(accent = a) } }, size = 28.dp,
                            )
                        }
                    }
                }
                Separator()
                SwitchRow(
                    "Işık efektleri", prefs.lightEffects,
                    { on ->
                        container.prefs.update { it.copy(lightEffects = on) }
                        if (on) Tilt.start(activity) else Tilt.stop()
                    },
                    subtitle = "Seçili kartın zemine yayılan ışığı ve gradyan kartlarda eğimle parlama",
                )
            }
            Spacer(Modifier.height(28.dp))

            Group(header = "Ödeme") {
                GroupRow(
                    "NFC", leading = { IconTile(Glyph.CONTACTLESS, Color(0xFF0A84FF)) },
                    value = when { !env.nfcAvailable -> "Yok"; env.nfcEnabled -> "Açık"; else -> "Kapalı" },
                    chevron = env.nfcAvailable,
                    onClick = if (env.nfcAvailable) ({ PaymentSwitcher.openNfcSettings(activity) }) else null,
                )
                Separator(60.dp)
                GroupRow(
                    "Varsayılan ödeme uygulaması", leading = { IconTile(Glyph.CARD, Color(0xFF30B158)) },
                    chevron = true, onClick = { PaymentSwitcher.openPaymentSettings(activity) },
                )
                Separator(60.dp)
                GroupRow(
                    "Ödeme onayları", leading = { IconTile(Glyph.BELL, Color(0xFFFF3B30)) },
                    value = if (env.notificationAccess) "Açık" else "Kapalı",
                    chevron = true, onClick = { PaymentNotificationListener.openAccessSettings(activity) },
                )
            }
            Spacer(Modifier.height(10.dp))
            Group(footer = "Açıkken bankanın ödeme bildirimi kaldırılır, yerine Wault'un onayı gelir.") {
                SwitchRow(
                    "Banka bildirimini değiştir", prefs.replaceBankNotification,
                    { on -> container.prefs.update { it.copy(replaceBankNotification = on) } },
                    enabled = env.notificationAccess,
                )
            }

            Spacer(Modifier.height(28.dp))
            val shizuku = env.shizuku
            Group(
                header = "Sessiz geçiş",
                footer = "Shizuku hazırken karta dokununca ödeme uygulaması ekran açılmadan değişir. " +
                    "Hazır değilse Android'in seçici ekranı açılır. Wault yalnızca cüzdan uygulaması ayarını değiştirir.",
            ) {
                GroupRow(
                    "Shizuku", leading = { IconTile(Glyph.LOCK, Color(0xFF5E5CE6)) },
                    value = when (shizuku) {
                        ShizukuBridge.State.NOT_INSTALLED -> "Kurulu değil"
                        ShizukuBridge.State.NOT_RUNNING -> "Çalışmıyor"
                        ShizukuBridge.State.NEEDS_PERMISSION -> "İzin gerekli"
                        ShizukuBridge.State.READY -> "Hazır"
                    },
                    subtitle = when (shizuku) {
                        ShizukuBridge.State.NOT_INSTALLED -> "Shizuku uygulamasını kur ve başlat"
                        ShizukuBridge.State.NOT_RUNNING -> "Shizuku'yu aç ve başlat, sonra buraya dön"
                        ShizukuBridge.State.NEEDS_PERMISSION -> "Wault'a Shizuku izni ver"
                        ShizukuBridge.State.READY -> null
                    },
                    chevron = shizuku == ShizukuBridge.State.NOT_RUNNING || shizuku == ShizukuBridge.State.NEEDS_PERMISSION,
                    onClick = when (shizuku) {
                        ShizukuBridge.State.NOT_RUNNING -> ({ ShizukuBridge.openShizukuApp(activity) })
                        ShizukuBridge.State.NEEDS_PERMISSION -> ({ ShizukuBridge.requestPermission() })
                        else -> null
                    },
                )
                Separator()
                SwitchRow(
                    "Vitrin kartı seçilince ödemeyi kapat", prefs.showcaseTurnsOffPayment,
                    { on -> container.prefs.update { it.copy(showcaseTurnsOffPayment = on) } },
                    subtitle = "Telefon okuyucuya ödeme uygulaması sunmaz",
                    enabled = shizuku == ShizukuBridge.State.READY,
                )
            }

            Spacer(Modifier.height(28.dp))
            var recolored by remember { mutableStateOf(false) }
            Group(
                header = "Kartlar",
                footer = "Kartları ana ekranda yukarı ya da aşağı sürükleyerek de sıralayabilirsin. Kart çoksa sağ kenardaki çubukla kaydırırsın. " +
                    "Renk yenileme ödeme kartlarını bağlı uygulamanın rengine boyar; fotoğraf ve hazır modeller değişmez.",
            ) {
                GroupRow("Kartları sırala", leading = { IconTile(Glyph.HANDLE, Color(0xFFFF9F0A)) }, chevron = true, onClick = onReorder)
                Separator(60.dp)
                GroupRow(
                    "Renkleri bankaya göre yenile", leading = { IconTile(Glyph.SLIDERS, Color(0xFF5E5CE6)) },
                    trailing = { if (recolored) Text("Yenilendi", style = WaultType.body, color = c.secondary) },
                    onClick = {
                        container.store.cards.value
                            .filter { it.kind == CardKind.PAYMENT && it.payment != null && it.faceSource == FaceSource.THEME && it.artId != "auto" }
                            .forEach { container.store.upsert(it.copy(artId = "auto")) }
                        recolored = true
                    },
                )
            }

            Spacer(Modifier.height(28.dp))
            Group(
                header = "Güvenlik",
                footer = if (bioAvailable) "Wault 30 saniyeden uzun arka planda kalırsa yeniden kilitlenir."
                else "Kilidi kullanmak için önce telefonda ekran kilidi ayarla.",
            ) {
                SwitchRow(
                    "Açılışta kilit", prefs.biometricLock,
                    { on ->
                        BiometricGate.prompt(activity, if (on) "Kilidi aç" else "Kilidi kapat", onSuccess = {
                            container.prefs.update { it.copy(biometricLock = on) }
                        })
                    },
                    subtitle = "Parmak izi, yüz ya da ekran kilidi",
                    enabled = bioAvailable,
                )
            }

            Spacer(Modifier.height(28.dp))
            Group(footer = "Deneme modu, örnek kartlar ve karşılama ekranı gibi araçları açar.") {
                SwitchRow(
                    "Geliştirici modu", prefs.developerMode,
                    { on -> container.prefs.update { it.copy(developerMode = on) }; if (!on) { DevOptions.dryRun = false; DevOptions.pretended = null; DevOptions.darkOverride = null; if (DevOptions.demoActive) container.store.leaveDemo() } },
                )
            }

            if (prefs.developerMode) {
                Spacer(Modifier.height(28.dp))
                Group(
                    header = "Geliştirici",
                    footer = "Deneme modunda karta dokunmak telefonun gerçek ödeme ayarını değiştirmez. Ödeme anını denemek için sahte bir kart oluşturup okut.",
                ) {
                    SwitchRow(
                        "Deneme modu", DevOptions.dryRun,
                        { on -> DevOptions.dryRun = on; if (!on) DevOptions.pretended = null },
                    )
                    Separator()
                    GroupRow(
                        if (DevOptions.demoActive) "Örnek kartları kapat" else "Örnek kartları göster",
                        titleColor = c.accent,
                        onClick = {
                            if (DevOptions.demoActive) container.store.leaveDemo()
                            else {
                                DevOptions.dryRun = true
                                container.store.showDemo(app.wault.data.DemoCards.all())
                            }
                        },
                    )
                    Separator()
                    GroupRow("Karşılama ekranını göster", titleColor = c.accent, onClick = onPreviewOnboarding)
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Kartların sadece bu telefonda, telefonun güvenli anahtarıyla şifreli durur. Kart numarası ve güvenlik kodunu istersen kaydedersin; görmek için parmak izi ya da PIN gerekir. Ödeme anahtarı saklanmaz, ödemeyi bankanın uygulaması yapar.",
                style = WaultType.footnote, color = c.secondary, modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(activity.packageName, 72.dp)
                Spacer(Modifier.height(8.dp))
                Text("Wault ${BuildConfig.VERSION_NAME}", style = WaultType.footnote, color = c.tertiary)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun LockScreen(activity: MainActivity) {
    val c = Wault.colors
    var error by remember { mutableStateOf<String?>(null) }
    val unlock = {
        BiometricGate.prompt(activity, "Wault'u aç",
            onSuccess = { activity.locked.value = false; error = null },
            onFail = { error = it })
    }
    LaunchedEffect(Unit) { unlock() }
    Box(
        Modifier.fillMaxSize().background(c.bg).clickable(enabled = false) { }
            .statusBarsPadding().navigationBarsPadding().padding(24.dp)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            IconTile(Glyph.LOCK, c.label, Modifier.size(64.dp))
            Spacer(Modifier.height(20.dp))
            Text("Wault kilitli", style = WaultType.title, color = c.label)
            Spacer(Modifier.height(6.dp))
            Text("Kartlarını görmek için kilidi aç.", style = WaultType.subhead, color = c.secondary, textAlign = TextAlign.Center)
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = WaultType.footnote, color = c.danger, textAlign = TextAlign.Center)
            }
        }
        PrimaryButton("Kilidi aç", { unlock() }, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
    }
}
