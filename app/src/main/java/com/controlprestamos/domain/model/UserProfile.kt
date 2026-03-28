package com.controlprestamos.domain.model

data class UserProfile(
    val fullName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val bankName: String = "",
    val paymentMobilePhone: String = "",
    val accountNumber: String = "",
    val paymentNotes: String = "",
    val isConfigured: Boolean = false
)
