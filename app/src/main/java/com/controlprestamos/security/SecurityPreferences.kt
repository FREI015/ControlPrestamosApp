package com.controlprestamos.security

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPinCreated(): Boolean {
        return !getPin().isNullOrBlank()
    }

    fun savePin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(): String? {
        return prefs.getString(KEY_PIN, null)
    }

    fun validatePin(pin: String): Boolean {
        return getPin() == pin
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setAuthenticated(authenticated: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply()
    }

    fun isAuthenticated(): Boolean {
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_PIN = "key_pin"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_IS_AUTHENTICATED = "key_is_authenticated"
    }
}