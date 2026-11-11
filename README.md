# Wault

Android için cüzdan: ödeme kartların, sadakat kartların, QR/barkodların ve NFC kartların tek yerde, Apple Wallet gibi üst üste dizili.

- **Kart yığını:** dokun, yukarı çek, aşağı çek, basılı tutup sürükleyerek sırala. Kart parmağı birebir izler.
- **Ödeme:** ödeme kartına dokununca telefonun varsayılan NFC ödeme uygulaması o bankanın uygulamasına geçer. Ödemeyi bankanın uygulaması yapar, onayı Wault gösterir.
- **Kişiselleştirme:** kartlar varsayılan olarak düz renk. 21 hazır renk, bankanın uygulama rengi ya da istediğin renk kodu; isteyen kart başına gradyan seçer. Tema, vurgu rengi ve yazı rengi de değişir (Ayarlar > Görünüm).
- **Kartlar:** ödeme, sadakat, QR/barkod, NFC kopya, vitrin. Kasa telefonun güvenli anahtarıyla şifrelenir.

Kotlin, Jetpack Compose. `minSdk 28`, `targetSdk 36`.

## Derleme ve test

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # birim ve arayüz testleri (telefonsuz)
./gradlew lintDebug
```

Arayüz testleri Robolectric ile bilgisayarda çalışır: uygulamanın bütünü açılır, dokunma, çekme ve sürükleme denenir, kartların gerçekten düz çizildiği piksellerden doğrulanır. Ekran görüntüleri `app/build/test-screenshots/` altına yazılır. Telefon gerekmez.

Windows kullanıcı adında ASCII dışı karakter varsa Gradle için `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=<proje>/.tmp` verilmeli.

## Debug derlemede geliştirme seçenekleri

Ayarlar > Geliştirici (yalnızca debug): **deneme modu** (karta dokunmak telefonun gerçek ödeme ayarını değiştirmez), örnek kartlar, tema.
`adb shell am start -n app.wault/.MainActivity --ez dry_run true --ez demo true --ei demo_count 15 --es theme light`

## Notlar

- Tasarım kararları: [DESIGN.md](DESIGN.md). Görsel kaynakları ve lisans notları: [assets-src/SOURCES.md](assets-src/SOURCES.md).
- Sessiz ödeme geçişi için [Shizuku](https://shizuku.rikka.app) gerekir; yoksa Android'in sistem seçicisi açılır.
- Bankaların tanıtım görselleri ve kart ağı işaretleri üçüncü tarafların markalarıdır; uygulama yayınlanacaksa izinleri gözden geçirilmeli.
