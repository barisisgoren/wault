package app.wault

import app.wault.nfc.RoleShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleShellTest {
    @Test fun addHolderBuildsCommand() = assertEquals(
        listOf("cmd", "role", "add-role-holder", "--user", "0", "android.app.role.WALLET", "com.garanti.cepsubesi"),
        RoleShell.addHolder("com.garanti.cepsubesi"),
    )

    @Test fun removeHolderBuildsCommand() = assertEquals(
        listOf("cmd", "role", "remove-role-holder", "--user", "0", "android.app.role.WALLET", "com.akbank.android.apps.akbank_direkt"),
        RoleShell.removeHolder("com.akbank.android.apps.akbank_direkt"),
    )

    @Test fun validPackageNames() {
        assertTrue(RoleShell.isValidPackage("com.ykb.android"))
        assertTrue(RoleShell.isValidPackage("kentkart.mobile.cordova"))
    }

    @Test fun injectionAttemptsAreRejected() {
        assertFalse(RoleShell.isValidPackage("com.x; rm -rf /"))
        assertFalse(RoleShell.isValidPackage("com.x && id"))
        assertFalse(RoleShell.isValidPackage("nodots"))
        assertFalse(RoleShell.isValidPackage(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPackageThrows() { RoleShell.addHolder("a b") }

    @Test fun parsesHolderOutput() {
        assertEquals(listOf("com.akbank.android.apps.akbank_direkt"), RoleShell.parseHolders("com.akbank.android.apps.akbank_direkt\n"))
        assertEquals(emptyList<String>(), RoleShell.parseHolders("\n"))
        assertEquals(emptyList<String>(), RoleShell.parseHolders("Error: something failed"))
    }
}
