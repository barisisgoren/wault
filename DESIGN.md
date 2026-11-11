# Wault tasarım notları

Yön: **Apple Wallet hissi, düz renkler, sakin ve profesyonel.** Arayüz nötr durur; renk kartlarda. Gradyan yok: kartlar varsayılan olarak tek renk,
gradyan ve ışık efektleri isteyen için seçenek (Ayarlar > Görünüm). Cesaret tek yerde harcanır: kartın kendisi ve yığının fiziği.
(v1 "basılı kart" yönü reddedildi: guilloche, sahte çip, kabartma rakamlar ve geniş başlıklar telefonda ucuz durdu. Ardından gelen gradyanlı kartlar da "hâlâ gradyan" diye eleştirildi.)

## Etkileşim modeli (ana ekran)
Yığın `ui/stack/StackScreen.kt` içinde kendi fiziğiyle çalışır. Konumlar tek bir durumdan türetilir ve yerleşim aşamasında okunur; parmak hareket ederken yeniden birleştirme olmaz.

| Hareket | Ne olur |
|---|---|
| Dokun | Kart yaylanarak üste çıkar, diğerleri altta kenar olarak toplanır. Bağlı ödeme kartıysa telefonun varsayılan ödeme uygulaması o karta geçer. |
| Yukarı çek (yığın ekrana sığıyorsa) | Kart parmağı birebir izler, diğerleri yelpaze gibi aşağı iner. Yarıyı geçince ya da hızlı atılınca seçilir, yoksa yayla geri döner. |
| Seçili kartı aşağı çek | Aynı ilerleme tersine işler; kart ve yığın parmakla açılır. Geri hareketi de aynı ilerlemeyi sürer (`PredictiveBackHandler`). |
| Yığını kaydır (kart çoksa) | Sınırda direnç, bırakınca yaylanan taşma, fırlatma. Üstte başlığın altına kayarken yumuşak geçiş. |
| Uzun bas ve sürükle | Kart kalkar (ölçek, gölge, titreşim), diğerleri canlı yer değiştirir, her geçişte hafif titreşim. Kenara yaklaşınca yığın kendiliğinden kayar. Bırakınca sıra kaydedilir. |
| Ses okuyucu | Her kartta "Yukarı taşı / Aşağı taşı" eylemleri var. Ayarlar > Kartları sırala ayrı bir liste olarak durur. |

Az kart varsa şeritler genişler (en çok 92dp), çok kart varsa en az 64dp kalır ve yığın kayar. Yığının altında tek satırlık **hazır çubuğu** (`ReadyBar`) hangi kartın okuyucuya hazır olduğunu ya da NFC'nin kapalı olduğunu söyler.

## Kart yüzeyi (`design/CardSurface.kt`)
- **Düz (varsayılan):** tek renk, 1dp ince kenar (koyu kartta beyaz %14, açık kartta siyah %9: üst üste aynı renkli kartlar ve açık zeminde beyaz kart ayrışsın), yumuşak gölge. Başka hiçbir katman yok.
- **Gradyan (isteğe bağlı, kart başına ya da genel):** aynı renkten türetilen açık ve koyu ton, köşede ikinci renk lekesi, alt kenarda koyulaşma, bantlanmayı kıran ince tanecik. "Işık efektleri" açıksa telefonu eğince kayan parlama (`design/Tilt.kt`).
- Yazı rengi zeminden seçilir: beyazla kontrast en az 3:1 ise beyaz, değilse koyu (`prefersLightInk`). Kart başına "Beyaz" ya da "Siyah" olarak değiştirilebilir.
- Sol kimlik karesi: ödeme kartında bankanın gerçek uygulama ikonu, sadakatte baş harf, kodda QR, NFC kopyada telefon simgesi.
- Varsayılan (okuyucuya hazır) kartın temassız işareti dolu daire içinde parlak, diğerlerinin sönük.
- Ağ işareti: TROY resmi vektör, Mastercard ve Amex tek renkli işaret, Visa yazı (bkz. `assets-src/SOURCES.md`).
- Kendi fotoğrafın ya da bankanın tanıtım görseli kullanılırsa yüzey yerine görsel çizilir (`CardImages`, şifreli saklanır).

## Kişiselleştirme
Amaç: kişi her şeyi en az dokunuşla değiştirebilsin.
- **Kart detayında "Renk" satırı:** kart açıkken tek dokunuşla boyanır, editöre girmeye gerek yok. Sonda "Özel renk" çarkı.
- **Editörde Kart görünümü:** 6 sütunlu renk noktaları (bankanın uygulama rengi, 21 hazır renk, özel), kart başına Stil (Varsayılan, Düz, Gradyan), Yazı rengi (Otomatik, Beyaz, Siyah), ayrıca Modeller ve Fotoğraf.
- **Özel renk sayfası (`ColorSheet`):** 8 ton ailesi × 5 kademe + gri satırı ve renk kodu alanı. Kartın canlı önizlemesi sayfanın arkasında görünür kalır.
- **Ayarlar > Görünüm:** Tema (Sistem, Açık, Koyu), Kart stili (Düz, Gradyan), Vurgu rengi (8 renk), Işık efektleri (kapalı), örnek kartla canlı önizleme.
- Renk paleti `design/CardArt.kt`: eski gradyan görsellerle aynı kimlikler (kayıtlı kartlar bozulmaz) ama düz renk. Yeni kart "Uygulama rengi" ile başlar; uygulama seçilince önizleme bankanın rengine boyanır (`CardArts.fromIcon`: ikondaki doygun pikseller 24 renk dilimine ayrılır, en ağır dilim kazanır).
- Kayıt: kartın `artId`, `customColor`, `style`, `ink` alanları (kasa sürümü 4); varsayılan değerler dosyaya yazılmaz. Genel ayarlar `Prefs`'te.

## Tokenlar (`design/Tokens.kt`)
| Ad | Açık | Koyu |
|---|---|---|
| bg | `#F3F4F6` | `#000000` |
| surface | `#FFFFFF` | `#1C1C1E` |
| label | `#17191C` | `#F5F5F7` |
| secondary | `#686B73` | `#98989F` |
| accent | seçilebilir, varsayılan `#007AFF` | seçilebilir, varsayılan `#409CFF` |
| danger | `#E5484D` | `#FF453A` |

## Tipografi
Tek aile: Instrument Sans (400/500/600/700). iOS ölçeği: largeTitle 32, title 20, headline 17, body 16, subhead 15, footnote 13, caption 12.
Büyük harfli etiket yok, mono yok, "·" ile meta zinciri yok.

## Biçim
- Kart oranı ISO/IEC 7810 ID-1, köşe 14dp. Gruplu liste: yuvarlatılmış gruplar, ayraçlar ikon hizasından başlar.
- Başlatıcı ikonu da düz: koyu zemin, üç düz renkli kart şeridi.
- İkonlar `design/Glyphs.kt` içinde elle çizilir.

## Hareket
Kullanıcıya cevap veren: kart seçme, çekme, sıralama, basınca küçülme (0,985), ekleme sheet'i, ekran geçişleri.
Tek orkestre an: **Ödendi** (`ui/pay/PaidMoment.kt`): halka çizilir, işaret çekilir, tutar belirir, `CONFIRM` titreşimi. Karşılama ekranının ikinci adımı aynı bileşenle önizlemesini gösterir.
Karşılama ilk adımında seçilen uygulamalar canlı bir mini yığın olarak belirir.
"Hazır" nabızları birkaç tur oynayıp durur (sonsuz animasyon pil yer). Sistem animasyonları kapalıysa her şey durağan.

## Dürüstlük kuralları
- Banka kartı kopyalanamaz; okunursa söylenir ve bankanın uygulaması önerilir.
- Vitrin kartı ödeme yapmaz ve bunu kendi ekranında söyler; sahte "ödendi" üretilmez.
- Android varsayılan ödeme uygulamasını sessizce değiştirmeye izin vermez. Shizuku hazırsa rol komutuyla ekran açılmadan değişir, değilse sistem seçicisi açılır.
- Geçersiz barkod girişte engellenir (özellikle EAN-13 kontrol hanesi).
- Bankanın tanıtım görselinde örnek ad/numara basılıdır; seçicide bu söylenir.

## Test ve doğrulama
- **Telefonsuz:** `./gradlew testDebugUnitTest`. Robolectric (SDK 34, yerel grafik) ile uygulamanın bütünü bilgisayarda açılır; Compose testleri dokunma, yukarı çekme, uzun basıp sürükleme, ayarları dener. Roborazzi ekran görüntülerini `app/build/test-screenshots/` altına yazar, yani telefonun ekranı kapalıyken de arayüz gözle doğrulanır.
- **Piksel testi:** `CardStyleRenderTest` kartı gerçekten çizip düz kartın her noktada aynı renk, gradyan kartın değişen renk olduğunu doğrular (gradyan geri sızmasın).
- **Telefonda:** debug derlemede deneme modu ve örnek kartlarla (gizli sanal ekran, scrcpy); gerçek ödeme ayarı değişmez. Xiaomi ana ekran kapalıyken uygulamayı dondurur, bu yüzden cihaz denemeleri ekran açıkken yapılır.

## Geliştirme seçenekleri (yalnızca debug)
Telefonda arayüz denerken kullanıcının gerçek ödeme ayarı bozulmasın diye (`DevOptions.kt`, Ayarlar > Geliştirici):
- **Deneme modu**: karta dokunmak sistemde hiçbir şeyi değiştirmez, geçiş yalnızca uygulamada simüle edilir.
- **Örnek kartlar**: 9 hazır kart (ya da `demo_count`), diske yazılmaz.
- **Tema**: Sistem, Açık, Koyu.
- `adb shell am start -n app.wault/.MainActivity --ez dry_run true --ez demo true --ei demo_count 15 --es theme light`

## Bilinen eksikler
- Gerçek NFC ödemesi, bildirim yakalama ve kamera taraması cihazda denenmedi.
- Bankaların tanıtım görselleri düşük çözünürlüklü ve örnek veri içeriyor; yayın öncesi karar gerekir.
- Özel renk sayfası (iletişim kutusu) Robolectric'te pencere olarak değil, içeriği ayrı denendi.
