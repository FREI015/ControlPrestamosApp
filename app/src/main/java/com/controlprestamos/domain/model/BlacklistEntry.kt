package com.controlprestamos.domain.model

import java.time.LocalDate

data class BlacklistEntry(
    val id: Long,
    val customerName: String,
    val phone: String,
    val idNumber: String,
    val reason: String,
    val addedDate: LocalDate,
    val active: Boolean
)
