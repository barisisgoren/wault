package app.wault.nfc

/**
 * Cüzdan rolünü (varsayılan NFC ödeme uygulaması) değiştiren kabuk komutları.
 * Saf mantık: Android'e dokunmaz, birim testleri bunu sürer.
 */
object RoleShell {
    const val WALLET_ROLE = "android.app.role.WALLET"
    private val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun isValidPackage(name: String) = packagePattern.matches(name)

    fun addHolder(pkg: String): List<String> {
        require(isValidPackage(pkg)) { "Geçersiz paket adı" }
        return listOf("cmd", "role", "add-role-holder", "--user", "0", WALLET_ROLE, pkg)
    }

    fun removeHolder(pkg: String): List<String> {
        require(isValidPackage(pkg)) { "Geçersiz paket adı" }
        return listOf("cmd", "role", "remove-role-holder", "--user", "0", WALLET_ROLE, pkg)
    }

    fun getHolders(): List<String> = listOf("cmd", "role", "get-role-holders", "--user", "0", WALLET_ROLE)

    /** `get-role-holders` çıktısı: her satırda bir paket adı (boş olabilir). */
    fun parseHolders(output: String): List<String> =
        output.lineSequence().map { it.trim() }.filter { isValidPackage(it) }.toList()
}
