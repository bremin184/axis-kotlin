package com.axis.app.domain.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class SecurityManager(private val context: Context) {

    fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun isPinValid(input: String, storedPin: String): Boolean {
        // In a real app, use Scrypt or BCrypt. For now, simple equality.
        return input == storedPin
    }

    fun getBiometricPromptInfo(
        title: String = "Authenticate",
        subtitle: String = "Unlock Axis",
        negativeButtonText: String = "Use PIN"
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()
    }
}
