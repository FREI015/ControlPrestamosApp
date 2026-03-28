package com.controlprestamos.data

import android.content.Context
import android.content.SharedPreferences
import com.controlprestamos.domain.model.UserProfile

class UserProfilePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getProfile(): UserProfile {
        return UserProfile(
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            idNumber = prefs.getString(KEY_ID_NUMBER, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            bankName = prefs.getString(KEY_BANK_NAME, "") ?: "",
            paymentMobilePhone = prefs.getString(KEY_PAYMENT_MOBILE_PHONE, "") ?: "",
            accountNumber = prefs.getString(KEY_ACCOUNT_NUMBER, "") ?: "",
            paymentNotes = prefs.getString(KEY_PAYMENT_NOTES, "") ?: "",
            isConfigured = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        )
    }

    fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_FULL_NAME, profile.fullName)
            .putString(KEY_ID_NUMBER, profile.idNumber)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_BANK_NAME, profile.bankName)
            .putString(KEY_PAYMENT_MOBILE_PHONE, profile.paymentMobilePhone)
            .putString(KEY_ACCOUNT_NUMBER, profile.accountNumber)
            .putString(KEY_PAYMENT_NOTES, profile.paymentNotes)
            .putBoolean(KEY_IS_CONFIGURED, true)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_ID_NUMBER = "id_number"
        private const val KEY_PHONE = "phone"
        private const val KEY_BANK_NAME = "bank_name"
        private const val KEY_PAYMENT_MOBILE_PHONE = "payment_mobile_phone"
        private const val KEY_ACCOUNT_NUMBER = "account_number"
        private const val KEY_PAYMENT_NOTES = "payment_notes"
        private const val KEY_IS_CONFIGURED = "is_configured"
    }
}
