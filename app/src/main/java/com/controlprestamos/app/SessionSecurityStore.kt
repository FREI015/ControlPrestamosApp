package com.controlprestamos.app

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class SessionSecurityStore(
    private val prefs: SharedPreferences
) {
    fun isDarkMode(): Boolean = prefs.getBoolean("isDarkMode", false)

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("isDarkMode", isDark).apply()
    }

    fun hasPin(): Boolean {
        val hashedPin = prefs.getString("pin_hash", "") ?: ""
        val legacyPin = prefs.getString("pin", "") ?: ""
        return hashedPin.isNotBlank() || legacyPin.isNotBlank()
    }

    fun savePin(pin: String) {
        val normalized = pin.filter(Char::isDigit)
        require(normalized.length in 4..6) { "El PIN debe tener entre 4 y 6 dígitos." }

        val salt = randomSalt()
        val hash = hashPin(normalized, salt)

        prefs.edit()
            .remove("pin")
            .putString("pin_salt", salt)
            .putString("pin_hash", hash)
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until", 0L)
            .apply()
    }

    fun validatePin(pin: String): Boolean {
        val normalized = pin.filter(Char::isDigit)
        if (normalized.isBlank()) return false

        val storedHash = prefs.getString("pin_hash", "") ?: ""
        val storedSalt = prefs.getString("pin_salt", "") ?: ""

        if (storedHash.isNotBlank() && storedSalt.isNotBlank()) {
            return hashPin(normalized, storedSalt) == storedHash
        }

        val legacyPin = prefs.getString("pin", "") ?: ""
        val matchesLegacy = legacyPin.isNotBlank() && legacyPin == normalized
        if (matchesLegacy) {
            savePin(normalized)
        }
        return matchesLegacy
    }

    fun clearPin() {
        prefs.edit()
            .remove("pin")
            .remove("pin_salt")
            .remove("pin_hash")
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until", 0L)
            .putBoolean("unlocked", false)
            .apply()
    }

    fun isUnlocked(): Boolean = prefs.getBoolean("unlocked", false)

    fun setUnlocked(value: Boolean) {
        prefs.edit().putBoolean("unlocked", value).apply()
    }

    fun isPinTemporarilyLocked(): Boolean {
        val lockUntil = prefs.getLong("pin_lockout_until", 0L)
        return lockUntil > System.currentTimeMillis()
    }

    fun getRemainingPinLockSeconds(): Long {
        val remainingMillis = (prefs.getLong("pin_lockout_until", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
        return if (remainingMillis == 0L) 0L else ((remainingMillis + 999L) / 1000L)
    }

    fun registerFailedPinAttempt(): Long {
        if (isPinTemporarilyLocked()) return getRemainingPinLockSeconds()

        val attempts = prefs.getInt("pin_failed_attempts", 0) + 1
        return if (attempts >= 5) {
            val lockUntil = System.currentTimeMillis() + 60_000L
            prefs.edit()
                .putInt("pin_failed_attempts", 0)
                .putLong("pin_lockout_until", lockUntil)
                .apply()
            getRemainingPinLockSeconds()
        } else {
            prefs.edit()
                .putInt("pin_failed_attempts", attempts)
                .apply()
            0L
        }
    }

    fun clearPinFailures() {
        prefs.edit()
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until", 0L)
            .apply()
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, saltBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        digest.update(salt)
        return Base64.encodeToString(digest.digest(pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
}
