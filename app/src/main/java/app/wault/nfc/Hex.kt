package app.wault.nfc

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

fun String.hexToBytes(): ByteArray {
    val clean = filter { !it.isWhitespace() && it != ':' }
    require(clean.length % 2 == 0) { "Tek sayıda hex karakteri" }
    return ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
