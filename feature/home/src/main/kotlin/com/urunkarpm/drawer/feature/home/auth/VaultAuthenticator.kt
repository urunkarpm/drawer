package com.urunkarpm.drawer.feature.home.auth

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object VaultAuthenticator {

    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            // Device has no lock screen PIN/biometric set up, grant access directly
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("Unlock Private Vault")
                .setDescription("Verify your identity to access hidden apps")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            val cancellationSignal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(activity)
            try {
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString?.toString() ?: "Authentication cancelled")
                        }
                    }
                )
            } catch (_: Exception) {
                onSuccess()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("Unlock Private Vault")
                .setDescription("Verify your identity to access hidden apps")
                .setNegativeButton("Cancel", ContextCompat.getMainExecutor(activity)) { _, _ ->
                    onError("Cancelled")
                }
                .build()

            val cancellationSignal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(activity)
            try {
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString?.toString() ?: "Authentication cancelled")
                        }
                    }
                )
            } catch (_: Exception) {
                onSuccess()
            }
        } else {
            @Suppress("DEPRECATION")
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Unlock Private Vault",
                "Verify your identity to access hidden apps"
            )
            if (intent != null) {
                activity.startActivity(intent)
            } else {
                onSuccess()
            }
        }
    }
}
