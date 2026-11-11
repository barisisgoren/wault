# Dış kaynaklı görseller ve lisans notları

| Dosya | Kaynak | Not |
|---|---|---|
| `assets-src/troy/*.svg`, `res/drawable/ic_scheme_troy*.xml` | TROY Medya Merkezi, `TROY-Logolar.zip` (https://www.troyodeme.com/en/about-troy/media-center) | Resmi logo, vektöre çevrildi, değiştirilmedi. TROY / BKM ticari markasıdır. Kullanım kuralları: TROY Marka Kılavuzu (aynı sayfa). |
| `res/drawable/ic_mc.xml`, `ic_amex.xml` | Simple Icons 13.21.0 (`mastercard.svg`, `americanexpress.svg`, https://simpleicons.org). Yol verisi birebir aynı, 2026-11-11'de karşılaştırıldı | Simple Icons dosyaları CC0 ama **markalar Mastercard ve American Express'indir**. Tek renkli işaret olarak kartın yazı renginde çizilir. |
| Visa | Dosya yok, kartlarda "VISA" yazısıyla gösterilir | Resmi logo sözleşme kabulü ve onaylı iş ortaklığı ister (Visa Brand Standards). |
| `app/src/main/assets/card_faces/{bonus,axess,world}.webp` | Bankaların kendi tanıtım sayfaları (adresler `data/CardImages.kt` içindeki `CardCatalog` listesinde) | **Telif ve marka bankalarındadır.** Kişisel kullanım için gömülü; uygulama yayınlanacaksa izin alınmalı ya da kaldırılmalı. Görsellerin üstünde örnek ad ve kart numarası basılıdır (ör. "ADA ULUKAYA", "5549 6012 3456 7890"); seçicide bu not düşülür. `axess.webp` 200 px genişliğinde, büyük ekranda bulanık durur. |
| Banka logoları | Dosya yok | Ödeme uygulamalarının ikonu telefondan çalışma anında okunur (`AppIcon`). Kart rengi de aynı ikondan çıkarılır (`CardArts.fromIcon`). |
| Yazı tipi | Instrument Sans (SIL Open Font License 1.1) | `res/font/instrument_sans_*.ttf` |
| Kart arka planları | Kodla üretilir (`design/CardArt.kt`, `CardSurface.kt`) | Gradyan, köşe parıltısı, tanecik, eğimle kayan ışık; dış dosya yok. |

## Yayınlanırsa dikkat
- Visa dijital kart standartları: ağ işareti üst köşelerde olmalı (yığın görünümünde okunsun diye). Wault'ta işaret kartın alt köşesinde; yayın öncesi gözden geçirilmeli.
- TROY logosu için kılavuzdaki minimum boyut ve boşluk kuralları geçerli.
- Mastercard ve American Express işaretleri için marka kılavuzlarına uyulmalı ya da işaretler kaldırılıp yazıya dönülmeli.
