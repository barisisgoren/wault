package app.wault.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricGate {
    private const val AUTH = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** Kart numarası gibi hassas veri için: parmak izi (güçlü) ya da PIN/desen/parola; yüz tanıma tek başına yetmez (Android 11+). */
    private fun authFor(strong: Boolean) =
        if (strong && android.os.Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else AUTH

    fun available(activity: FragmentActivity, strong: Boolean = false): Boolean =
        BiometricManager.from(activity).canAuthenticate(authFor(strong)) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onFail: (String) -> Unit = {},
        strong: Boolean = false,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(code: Int, msg: CharSequence) = onFail(msg.toString())
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(authFor(strong))
            .build()
        prompt.authenticate(info)
    }
}
