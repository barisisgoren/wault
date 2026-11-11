# Dış kaynaklı görseller ve lisans notları

| Dosya | Kaynak | Not |
|---|---|---|
| `assets-src/troy/*.svg`, `res/drawable/ic_scheme_troy*.xml` | TROY Medya Merkezi, `TROY-Logolar.zip` (https://www.troyodeme.com/en/about-troy/media-center) | Resmi logo, vektöre çevrildi, değiştirilmedi. TROY / BKM ticari markasıdır. Kullanım kuralları: TROY Marka Kılavuzu (aynı sayfa). |
| `res/drawable-nodpi/net_mastercard.png`, `net_visa.png`, `net_amex.png` | Proje sahibinin sağladığı Mastercard, Visa ve American Express logo görselleri (arka planı şeffaflaştırıldı, kırpıldı) | **Markalar ilgili şirketlerindir.** Mastercard ve Amex kendi renginde; Visa koyu kartta beyaza boyanır. |
| Banka logoları | Dosya yok | Ödeme uygulamalarının ikonu telefondan çalışma anında okunur (`AppIcon`). Kart rengi de aynı ikondan çıkarılır (`CardArts.fromIcon`). |
| Yazı tipi | Instrument Sans (SIL Open Font License 1.1) | `res/font/instrument_sans_*.ttf` |
| Kart arka planları | Kodla üretilir (`design/CardArt.kt`, `CardSurface.kt`) | Gradyan, köşe parıltısı, tanecik, eğimle kayan ışık; dış dosya yok. |

## Yayınlanırsa dikkat
- Visa dijital kart standartları: ağ işareti üst köşelerde olmalı (yığın görünümünde okunsun diye). Wault'ta işaret kartın alt köşesinde; yayın öncesi gözden geçirilmeli.
- TROY logosu için kılavuzdaki minimum boyut ve boşluk kuralları geçerli.
- Mastercard, Visa ve American Express işaretleri için marka kılavuzlarına uyulmalı ya da işaretler kaldırılmalı.
