package app.wault.data

/**
 * Yalnızca debug derlemede arayüzü denemek için örnek kartlar (bkz. MainActivity.handle, DevOptions).
 * Kullanıcının kasasına yazılmaz; süreç kapanınca kaybolur.
 */
object DemoCards {

    private fun pay(
        title: String, subtitle: String, pkg: String, cls: String, app: String,
        net: CardNetwork, last4: String, holder: String, expiry: String, art: String,
    ) = WaultCard(
        id = "demo-$pkg", kind = CardKind.PAYMENT, title = title, subtitle = subtitle, artId = art,
        network = net, last4 = last4, holder = holder, expiry = expiry,
        pan = "400000000000" + last4, cvv = "123",
        payment = PaymentLink(pkg, cls, app),
    )

    /** Tanıtım kaydı için: gerçek banka adı ve kart sahibi içermeyen genel örnek kartlar. */
    fun promo(): List<WaultCard> = listOf(
        pay("Banka 1", "Platinum", "app.wault.demo.banka1", "app.wault.demo.Hce", "Banka 1", CardNetwork.MASTERCARD, "4417", "", "08/28", "forest"),
        pay("Banka 2", "Gold", "app.wault.demo.banka2", "app.wault.demo.Hce", "Banka 2", CardNetwork.VISA, "8851", "", "03/29", "crimson"),
        pay("Banka 3", "Classic", "app.wault.demo.banka3", "app.wault.demo.Hce", "Banka 3", CardNetwork.AMEX, "1007", "", "11/27", "indigo"),
        pay("Banka 4", "Bankkart", "app.wault.demo.banka4", "app.wault.demo.Hce", "Banka 4", CardNetwork.TROY, "6021", "", "06/28", "teal"),
        WaultCard(
            id = "demo-market", kind = CardKind.LOYALTY, title = "Market", subtitle = "Üyelik kartı",
            artId = "sunset", memberNo = "5300 1182 7741", points = "1.240 puan",
            codeFormat = CodeFormat.EAN_13, codeValue = ean13("869000000001"),
        ),
        WaultCard(
            id = "demo-ozel", kind = CardKind.SHOWCASE, title = "Özel Kart", subtitle = "Sahte kart",
            artId = "silver", network = CardNetwork.VISA, last4 = "1234", expiry = "12/29",
        ),
    )

    /** EAN-13 için kontrol hanesini hesaplar. */
    private fun ean13(first12: String): String {
        val sum = first12.mapIndexed { i, ch -> (ch - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return first12 + ((10 - sum % 10) % 10)
    }

    /** count kadar kart: ilk 9 hazır kart, fazlası numaralı vitrin kartları. Yığının uç durumlarını denemek için. */
    fun all(count: Int = 9): List<WaultCard> {
        val base = base()
        if (count <= base.size) return base.take(count.coerceAtLeast(0))
        val arts = listOf("ocean", "indigo", "rose", "crimson", "teal", "gold", "plum", "graphite")
        return base + (base.size until count).map { i ->
            WaultCard(
                id = "demo-extra-$i", kind = CardKind.SHOWCASE, title = "Kart ${i + 1}", subtitle = "Örnek",
                artId = arts[i % arts.size], last4 = (1000 + i * 37).toString().takeLast(4),
            )
        }
    }

    private fun base(): List<WaultCard> = listOf(
        pay(
            "Garanti BBVA", "Bonus Platinum", "com.garanti.cepsubesi",
            "com.garanti.pfm.activity.trans.payments.hce.HceService", "Garanti BBVA",
            CardNetwork.MASTERCARD, "4417", "AYŞE KAYA", "08/28", "forest",
        ),
        pay(
            "Akbank", "Axess", "com.akbank.android.apps.axess",
            "com.akbank.akbankx.services.HceService", "Axess",
            CardNetwork.MASTERCARD, "0392", "AYŞE KAYA", "11/27", "crimson",
        ),
        pay(
            "Yapı Kredi", "World Gold", "com.ykb.android",
            "com.ykb.mobilebanking.hce.services.HCEService", "Yapı Kredi",
            CardNetwork.VISA, "8851", "AYŞE KAYA", "03/29", "plum",
        ),
        pay(
            "Ziraat Bankası", "Bankkart", "com.ziraat.ziraatmobil",
            "com.veripark.ziraatmobile.common.nfc.ZiraatWalletHCEService", "Ziraat Mobil",
            CardNetwork.TROY, "6021", "AYŞE KAYA", "06/28", "teal",
        ),
        pay(
            "VakıfBank", "Platinum", "com.vakifbank.mobile",
            "com.vakifbank.mobile.hce.SMHCEService", "VakıfBank",
            CardNetwork.VISA, "2750", "AYŞE KAYA", "01/30", "gold",
        ),
        WaultCard(
            id = "demo-migros", kind = CardKind.LOYALTY, title = "Migros", subtitle = "Money Club",
            artId = "sunset", memberNo = "5300 1182 7741", points = "1.240 Money",
            codeFormat = CodeFormat.EAN_13, codeValue = ean13("869000000001"),
        ),
        WaultCard(
            id = "demo-sinema", kind = CardKind.CODE, title = "Sinema bileti", subtitle = "Cuma 21.30",
            artId = "midnight", note = "Salon 4, koltuk 12",
            codeFormat = CodeFormat.QR, codeValue = "WAULT-DEMO-TICKET-4412-S4-K12",
        ),
        WaultCard(
            id = "demo-ofis", kind = CardKind.NFC_COPY, title = "Ofis kapısı", subtitle = "3. kat",
            artId = "graphite",
            nfc = NfcCopy(
                uidHex = "04A224B2C15E80", techs = listOf("NfcA", "MifareUltralight", "Ndef"),
                ndefHex = "D1010B5402656E68656C6C6F", capability = NfcCapability.EMULATE,
            ),
        ),
        WaultCard(
            id = "demo-vitrin", kind = CardKind.SHOWCASE, title = "Vitrin", subtitle = "Sadece görünüm",
            artId = "silver", network = CardNetwork.VISA, last4 = "1234", holder = "AYŞE KAYA", expiry = "12/29",
        ),
    )
}
