package com.controlprestamos.security

import android.content.Context
import androidx.biometric.BiometricManager

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)

        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatusMessage(context: Context): String {
        val biometricManager = BiometricManager.from(context)

        return when (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometría disponible"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "El dispositivo no tiene hardware biométrico"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "El hardware biométrico no está disponible"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No hay huellas o biometría registradas en el dispositivo"
            else -> "Biometría no disponible"
        }
    }
}