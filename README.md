# Wault 💳⚡

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![NFC](https://img.shields.io/badge/NFC-Shizuku%20HCE%20Silent%20Switch-FF6F00?style=for-the-badge&logo=googlepay&logoColor=white)
![Database](https://img.shields.io/badge/Storage-Room%20(100%25%20Offline)-00E599?style=for-the-badge&logo=sqlite&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)

**Android için Apple Wallet Zarafetinde Yeni Nesil Dijital Cüzdan ve Temassız Ödeme Yöneticisi.**

*Android'in parçalı bankacılık ve NFC ekosistemini Apple Wallet akıcılığı, Shizuku sessiz geçiş altyapısı ve AMOLED siyah zarafetiyle tek bir kasada birleştirir.*

</div>

---

## 🌟 Neden Wault?

Android'de her bankanın (Garanti, Akbank, Yapı Kredi, Ziraat, İş Bankası, vb.) kendi temassız ödeme servisi bulunur ve sistem varsayılan kartı değiştirmek için kullanıcıyı derin Android ayar menülerine yönlendirir.

**Wault**, Apple Wallet'ın fiziksel kart destesi fiziğini ve odak modunu Android'e getirir; **Shizuku** entegrasyonu sayesinde sistem ayarlarını açmaya gerek kalmadan, **tek bir dokunuşla arka planda varsayılan temassız kartı değiştirir**. Üstelik tüm sadakat kartlarınızı, biniş kartlarınızı ve barkodlarınızı internet bağlantısına ihtiyaç duymadan, tamamen çevrimdışı ve şifreli saklar.

---

## ✨ Temel Özellikler

### 1. 🎴 Apple Wallet Tarzı Kart Yığını & Fizik Matematiği
- **Düz ve Paralel Liste Sıralaması:** Kartlar yukarıya doğru küçülmez (`scale = 1.0f`), tüm kartlar ekran genişliğinde eşit ve paralel basamaklarla listelenir.
- **56.dp Deste Basamaklaması (Peek Step):** Her kartın başlığı ve banka kimliği rahatça okunabilecek ferahlıkta basamaklandırılır.
- **Yelpaze Açılımı (Pull-down Accordion Fan-out):** Aktif kart aşağı çekildiğinde deste yay fiziğiyle (`spring(dampingRatio = 0.82f, stiffness = 380f)`) yelpaze gibi yukarıya açılarak kart başlıklarını sergiler.
- **Sürtünmesiz Sürükle-Bırak Sıralama:** Kartlar sürüklenirken z-index ve Y koordinatları anlık telafi edilir (`delta compensation`), liste titremesi veya animasyon sıçraması yaşanmaz.

### 2. ⚡ Shizuku ile Tek Dokunuşta Sessiz NFC Değişimi
- **Kusursuz Arka Plan Geçişi:** Herhangi bir banka kartına dokunulduğunda Android'in varsayılan NFC HCE ödeme servisi anında değiştirilir.
- **Tamamen Sessiz Operasyon:** Arayüzde gereksiz *"varsayılan yapıldı"* toast veya snackbar bildirimleri gösterilmez; kart seçimi anında ve sessizce aktif olur.
- **60 Saniyelik Geçici Mod:** Ana kartınızı değiştirmeden, ikinci bir kartla sadece 1 dakikalığına ödeme yapıp otomatik olarak ana karta geri dönmenizi sağlar.

### 3. ⭐ Kart Anatomisi & Favorilendirme
- **Yumuşak 20.dp Köşeler ve Metalik Pırıltı:** Derin gölgeler, radyal metalik ışık ve üst kenarda cam yansıması.
- **İnteraktif Favori Yıldızı:** Kartın sağ üst köşesindeki yıldıza dokunularak kart favoriye alınabilir; favori kartlar yığın içinde otomatik olarak en ön sıraya taşınır.
- **İnteraktif Kart Görseli Kırpıcı (Card Image Cropper):** Galeriden seçilen fotoğraflar, ID-1 standart kart oranına (1.586:1) göre serbestçe kaydırılabilir (pan), yakınlaştırılabilir (pinch-to-zoom), 90° döndürülebilir ve kart şablonuna tam oturacak şekilde yüksek çözünürlükte (1200x757) kırpılabilir.
- **Gerçekçi Ödeme Kartı Düzeni:**
  - **Sol Üst:** Banka logosu ve temizlenmiş başlık.
  - **Sağ Üst:** Altın favori yıldızı.
  - **Orta:** 3D altın kaplama EMV mikroçip + temassız ödeme dalgaları.
  - **Sol Alt:** Maskeli kart numarası (`•••• 2639`).
  - **Sağ Alt:** Yüksek çözünürlüklü şema logosu (**VISA**, **Mastercard**, **TROY**, **AMEX**).
- **Yalın Kimlik:** Bakiye, limit, IBAN gibi görsel kalabalık yaratan bankacılık detayları yer almaz.

### 4. 🎯 Apple Pay Odak Modu (Spotlight / Payment Ready)
- Karta çift dokunulduğunda kart ekranın en üstüne (`topOffset = 24.dp`) yaylanır ve arka plan `%92` karartılır.
- **Sade POS İstemi:** Kartın hemen altında `54.dp` boyutunda dairesel tek bir radar halkası ve *"Okuyucuya Yaklaştırın"* çağrısı yer alır.
- **Minimal Onay Mikro-Animasyonu:** POS işleminde parçacık veya konfeti kalabalığı olmadan, halkanın içinde saat yönünde zarifçe çizilen vektörel yeşil onay işareti (`#30D158`) ve *"Tamamlandı"* bildirimi gösterilir.

### 5. 🏷️ Kategori Filtreleme Sistemi (Category Bar)
Üst barın hemen altında yer alan yatay kaydırılabilir, yarı saydam buzlu cam sekmeler:
- **Tüm Kartlar:** Hem banka kartlarını hem alt kasa biletlerini listeler.
- **Banka & Kredi:** Sadece temassız ödeme banka kartlarını gösterir.
- **Ulaşım:** Kentkart, İstanbulkart, metro ve ulaşım kartlarını filtreler.
- **Üyelikler & Diğer:** Kahve, spor salonu, perakende ve üyelik barkodlarını filtreler.

### 6. ⚙️ Sadeleştirilmiş Header & Ayarlar Çarkı
- **Header:** Sol tarafta sade ve modern `Wault` logosu; sağ tarafta **Arama (`Search`)**, **Hızlı Ekle (`+`)** ve **Ayarlar (`Settings` Çarkı)** butonları.
- **Ayarlar Modalı:** Sağ üstteki çark butonuna dokunulduğunda Shizuku durumu, Android Temassız Ödeme ayarları ve gizlenen kartlar yöneticisi açılır.
- **Alt Navigasyon Barı:**
  - **Tümü (`GridView`):** Ana kart destesi.
  - **Ödeme (`Bolt`):** Anında temassız ödeme modu.
  - **Diğer (`QrCode2`):** Optik biletler ve sadakat kartları kasası.

### 7. 🎟️ Optik Barkod ve Bilet Kasası (Pass Vault)
- **1D/2D Barkod Desteği:** QR Code, Code 128, EAN-13, PDF417, Aztec, Code 39.
- **Akıllı Kamera & Galeri Tarayıcısı:** Google ML Kit ve CameraX ile tek dokunuşta barkod okuma.
- **Otomatik Ekran Parlaklığı:** Barkod tam ekran açıldığında turnike ve okuyucularda kolay taranabilmesi için ekran parlaklığını otomatik olarak maksimum seviyeye çıkarır.

### 8. 🔒 %100 Çevrimdışı ve Güvenli
- Hiçbir sunucuya bağlanmaz, telemetri veya analitik toplamaz.
- Tüm veriler cihazınızda yerel SQLite (Room Database) üzerinde şifrelenerek saklanır.

---

## 🏗️ Teknik Mimari & Kullanılan Teknolojiler

```
com.baris.wault
├── data
│   ├── nfc           # HCE tarayıcı, Shizuku yöneticisi, NFC geçiş yürütücüsü
│   └── vault         # Room veritabanı, Sadakat kartları, Barkod jeneratörü
├── ui
│   ├── components    # Kart destesi, Üst bar, Kategori filtresi, Ayarlar modalı
│   ├── theme         # AMOLED siyah, Cam gradyanları, Tipografi ve Şekiller
│   ├── WaultMainScreen.kt
│   └── WaultViewModel.kt
└── util              # Parlaklık kontrolcüsü, Resim saklama, ZXing barkod motoru
```

| Bileşen | Teknoloji | Amaç |
|---|---|---|
| **Arayüz (UI)** | Jetpack Compose + Material 3 | Bildirime dayalı (declarative) modern arayüz |
| **Sistem Entegrasyonu** | Shizuku API | `WRITE_SECURE_SETTINGS` ile sessiz NFC servis değişimi |
| **Yerel Veritabanı** | Room + KSP | Kart özelleştirmeleri, sıralamalar ve bilet kasası |
| **Barkod Motoru** | ZXing Core + Google ML Kit | Yüksek çözünürlüklü barkod oluşturma ve kamera tarama |
| **Kamera Altyapısı** | CameraX | Gerçek zamanlı barkod vizörü |
| **Renk Çıkarımı** | AndroidX Palette | Kart logolarından dinamik renk gradyanı üretimi |

---

## 🚀 Kurulum ve Derleme

### Gereksinimler:
- Android Studio Ladybug (2024.2+) veya üzeri
- JDK 17+
- Android SDK 35 (Minimum SDK: 26 / Android 8.0 Oreo)

### Derleme Adımları:
```bash
# Depoyu klonlayın
git clone https://github.com/barisisgoren/wault.git
cd wault

# Debug APK derleyin
./gradlew assembleDebug

# Cihaza yükleyin
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Kablosuz ADB ile Yükleme:
```bash
adb connect <TELEFON_IP>:<PORT>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚡ Shizuku Kurulumu (İsteğe Bağlı ama Önerilir)

Wault, Shizuku olmadan da standart Android fallback menüsü ile çalışır. Ancak tek dokunuşla sessiz kart değişimi için:
1. Google Play veya GitHub üzerinden **Shizuku** uygulamasını indirin.
2. Kablosuz Hata Ayıklama (Wireless Debugging) üzerinden Shizuku'yu başlatın.
3. Wault'u açıp sağ üstteki **Ayarlar (Çark)** menüsünden *"İzin Ver"* butonuna dokunun.
4. Artık dilediğiniz banka kartına dokunduğunuzda varsayılan NFC kartınız anında güncellenir!

---

## 📜 Lisans

Bu proje **Apache License 2.0** altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına göz atabilirsiniz.

