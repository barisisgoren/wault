# Wault tasarım notları

Yön: **Apple Wallet hissi, güzel renkler, sakin ve profesyonel.** Arayüz nötr durur; renk ve canlılık kartlarda.
Cesaret tek yerde harcanır: kartın kendisi ve yığının fiziği. Çevresindeki her şey sessiz ve disiplinli.
(v1 "basılı kart" yönü reddedildi: guilloche, sahte çip, kabartma rakamlar ve geniş başlıklar telefonda ucuz durdu.)

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
Katmanlar: renk geçişi, köşede ikinci renk lekesi, sol üstten düşen ışık, alt kenarda koyulaşma, tanecik, 1dp kenar parıltısı.
- **Işık telefonu eğince kayar** (`design/Tilt.kt`, yerçekimi sensörü). Dinlenme duruşu zamanla nötr sayılır; sistem animasyonları kapalıysa sabit.
- **Renk bankanın kendi renginden gelir** (`CardArts.fromIcon`): uygulama ikonundaki doygun pikseller 24 renk dilimine ayrılır, en ağır dilim kazanır. Sarı gibi açık markalar açık kart + koyu yazı olur.
- Sol kimlik karesi: ödeme kartında bankanın gerçek uygulama ikonu, sadakatte baş harf, kodda QR, NFC kopyada telefon simgesi.
- Varsayılan (okuyucuya hazır) kartın temassız işareti dolu daire içinde parlak, diğerlerinin sönük.
- Ağ işareti: TROY resmi vektör, Mastercard ve Amex tek renkli işaret, Visa yazı (bkz. `assets-src/SOURCES.md`).
- Kendi fotoğrafın ya da bankanın tanıtım görseli kullanılırsa yüzey yerine görsel çizilir (`CardImages`, şifreli saklanır).

## Seçili kartın ışığı
Seçili kartın rengi kartın altından yumuşak bir ışık olarak tüm zemine yayılır, temassız halkaları aynı renktedir. Ödendi anında aynı fikir yeşil. Bu süs değil, hangi kartın etkin olduğunu söyleyen bilgi.

## Tokenlar (`design/Tokens.kt`)
| Ad | Açık | Koyu |
|---|---|---|
| bg | `#F3F4F6` | `#000000` |
| surface | `#FFFFFF` | `#1C1C1E` |
| label | `#17191C` | `#F5F5F7` |
| secondary | `#686B73` | `#98989F` |
| accent | `#007AFF` | `#409CFF` |
| danger | `#E5484D` | `#FF453A` |

14 kart rengi (`CardArt.kt`): Gece, Grafit, Okyanus, Çivit, Turkuaz, Orman, Mürdüm, Gün batımı, Bordo, Altın, Pudra, Gümüş, Fildişi + "Uygulama rengi" (otomatik).
Yeni ödeme kartı "Uygulama rengi" ile başlar; uygulama seçilince önizleme bankanın rengine boyanır.

## Tipografi
Tek aile: Instrument Sans (400/500/600/700). iOS ölçeği: largeTitle 32, title 20, headline 17, body 16, subhead 15, footnote 13, caption 12.
Büyük harfli etiket yok, mono yok, "·" ile meta zinciri yok.

## Biçim
- Kart oranı ISO/IEC 7810 ID-1, köşe 14dp. Gruplu liste: yuvarlatılmış gruplar, ayraçlar ikon hizasından başlar.
- Editör: üstte canlı önizleme, ödeme uygulaması yatay ikon seçici, görünüm seçici üç bölmeli (Renkler, Modeller, Fotoğraf).
- İkonlar `design/Glyphs.kt` içinde elle çizilir.

## Hareket
Kullanıcıya cevap veren: kart seçme, çekme, sıralama, basınca küçülme (0,985), ekleme sheet'i, ekran geçişleri.
Tek orkestre an: **Ödendi** (`ui/pay/PaidMoment.kt`): halka çizilir, işaret çekilir, tutar belirir, `CONFIRM` titreşimi. Karşılama ekranının ikinci adımı aynı bileşenle önizlemesini gösterir.
Karşılama ilk adımında seçilen uygulamalar canlı bir mini yığın olarak belirir.
Hazır göstergesi sadece kart gerçekten hazırken yayılır. Sistem animasyonları kapalıysa her şey durağan.

## Dürüstlük kuralları
- Banka kartı kopyalanamaz; okunursa söylenir ve bankanın uygulaması önerilir.
- Vitrin kartı ödeme yapmaz ve bunu kendi ekranında söyler; sahte "ödendi" üretilmez.
- Android varsayılan ödeme uygulamasını sessizce değiştirmeye izin vermez. Shizuku hazırsa rol komutuyla ekran açılmadan değişir, değilse sistem seçicisi açılır.
- Geçersiz barkod girişte engellenir (özellikle EAN-13 kontrol hanesi).
- Bankanın tanıtım görselinde örnek ad/numara basılıdır; seçicide bu söylenir.

## Geliştirme seçenekleri (yalnızca debug)
Telefonda arayüz denerken kullanıcının gerçek ödeme ayarı bozulmasın diye (`DevOptions.kt`, Ayarlar > Geliştirici):
- **Deneme modu**: karta dokunmak sistemde hiçbir şeyi değiştirmez, geçiş yalnızca uygulamada simüle edilir.
- **Örnek kartlar**: 9 hazır kart (ya da `demo_count`), diske yazılmaz.
- **Tema**: Sistem, Açık, Koyu.
- `adb shell am start -n app.wault/.MainActivity --ez dry_run true --ez demo true --ei demo_count 15 --es theme light`

## Bilinen eksikler
- Gerçek NFC ödemesi, bildirim yakalama ve kamera taraması bu oturumda cihazda denenmedi.
- Bankaların tanıtım görselleri düşük çözünürlüklü ve örnek veri içeriyor; yayın öncesi karar gerekir.
