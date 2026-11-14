package com.urunkarpm.drawer.feature.home.auth

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat

// ponytail: Uses native android.hardware.biometrics.BiometricPrompt without extra dependencies; ceiling: API 28 requires explicit cancel button without inline device credential fallback; upgrade path: adopt androidx.biometric if API < 29 legacy credential fallback is required.
object VaultAuthenticator {

    private const val TAG = "VaultAuthenticator"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onError("Activity unavailable")
            return
        }
        authenticate(activity, onSuccess, onError)
    }

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
            } catch (e: Exception) {
                Log.e(TAG, "Biometric authentication failed", e)
                onError(e.message ?: "Authentication failed")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("Unlock Private Vault")
                .setDescription("Verify your identity to access hidden apps")
                .setDeviceCredentialAllowed(true)
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
            } catch (e: Exception) {
                Log.e(TAG, "Biometric authentication failed", e)
                onError(e.message ?: "Authentication failed")
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
            } catch (e: Exception) {
                Log.e(TAG, "Biometric authentication failed", e)
                onError(e.message ?: "Authentication failed")
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
