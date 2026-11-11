package app.wault.data

import org.json.JSONArray
import org.json.JSONObject

/** Kartları JSON'a çevirir. Sıralama listedeki konumdan gelir. */
object CardCodec {

    fun encode(cards: List<WaultCard>): String {
        val arr = JSONArray()
        cards.forEach { arr.put(encodeCard(it)) }
        return JSONObject().put("v", 4).put("cards", arr).toString()
    }

    fun decode(json: String): List<WaultCard> {
        val arr = JSONObject(json).optJSONArray("cards") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { decodeCard(arr.getJSONObject(i)) }.getOrNull()
        }
    }

    private fun encodeCard(c: WaultCard): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("kind", c.kind.name)
        put("title", c.title)
        put("subtitle", c.subtitle)
        put("artId", c.artId)
        c.customColor?.let { put("customColor", it) }
        c.style?.let { put("style", it.name) }
        if (c.ink != InkMode.AUTO) put("ink", c.ink.name)
        put("faceSource", c.faceSource.name)
        c.faceRef?.let { put("faceRef", it) }
        put("network", c.network.name)
        put("last4", c.last4)
        put("holder", c.holder)
        put("expiry", c.expiry)
        if (c.pan.isNotEmpty()) put("pan", c.pan)
        if (c.cvv.isNotEmpty()) put("cvv", c.cvv)
        c.payment?.let {
            put("payment", JSONObject().apply {
                put("pkg", it.packageName)
                put("cls", it.serviceClass)
                put("label", it.appLabel)
            })
        }
        put("memberNo", c.memberNo)
        put("points", c.points)
        put("note", c.note)
        c.codeFormat?.let { put("codeFormat", it.name) }
        put("codeValue", c.codeValue)
        c.nfc?.let {
            put("nfc", JSONObject().apply {
                put("uid", it.uidHex)
                put("techs", JSONArray(it.techs))
                it.ndefHex?.let { n -> put("ndef", n) }
                put("cap", it.capability.name)
            })
        }
    }

    /** İlk sürümdeki düz kart rengini yeni kart görsellerinden en yakınına taşır. */
    private fun legacyArt(base: Long): String = when (base) {
        0xFFF5F7F9 -> "ivory"
        0xFFC9CED6 -> "silver"
        0xFF1E2A44 -> "ocean"
        0xFF3A3F47 -> "graphite"
        0xFF1F5560 -> "teal"
        0xFF0E7C66 -> "forest"
        0xFF4F6B3A -> "forest"
        0xFFE8B931 -> "gold"
        0xFF6E2233 -> "crimson"
        0xFF8C8FD1 -> "plum"
        else -> "midnight"
    }

    private fun decodeCard(o: JSONObject): WaultCard {
        val pay = o.optJSONObject("payment")
        val nfc = o.optJSONObject("nfc")
        return WaultCard(
            id = o.getString("id"),
            kind = CardKind.valueOf(o.getString("kind")),
            title = o.optString("title"),
            subtitle = o.optString("subtitle"),
            artId = o.optString("artId").ifEmpty { legacyArt(o.optJSONObject("face")?.optLong("base", 0L) ?: 0L) },
            customColor = if (o.has("customColor")) o.optLong("customColor") else null,
            style = runCatching { CardStyle.valueOf(o.optString("style")) }.getOrNull(),
            ink = runCatching { InkMode.valueOf(o.optString("ink")) }.getOrDefault(InkMode.AUTO),
            faceSource = runCatching { FaceSource.valueOf(o.optString("faceSource")) }.getOrDefault(FaceSource.THEME),
            faceRef = o.optString("faceRef").takeIf { it.isNotBlank() && it != "null" },
            network = runCatching { CardNetwork.valueOf(o.optString("network")) }.getOrDefault(CardNetwork.NONE),
            last4 = o.optString("last4"),
            holder = o.optString("holder"),
            expiry = o.optString("expiry"),
            pan = o.optString("pan"),
            cvv = o.optString("cvv"),
            payment = pay?.let { PaymentLink(it.getString("pkg"), it.getString("cls"), it.optString("label")) },
            memberNo = o.optString("memberNo"),
            points = o.optString("points"),
            note = o.optString("note"),
            codeFormat = o.optString("codeFormat").takeIf { it.isNotEmpty() }
                ?.let { runCatching { CodeFormat.valueOf(it) }.getOrNull() },
            codeValue = o.optString("codeValue"),
            nfc = nfc?.let {
                val techs = it.optJSONArray("techs")
                NfcCopy(
                    uidHex = it.optString("uid"),
                    techs = if (techs == null) emptyList() else (0 until techs.length()).map { i -> techs.getString(i) },
                    ndefHex = it.optString("ndef").takeIf { s -> s.isNotEmpty() },
                    capability = runCatching { NfcCapability.valueOf(it.optString("cap")) }
                        .getOrDefault(NfcCapability.RECORD_ONLY),
                )
            },
        )
    }
}
