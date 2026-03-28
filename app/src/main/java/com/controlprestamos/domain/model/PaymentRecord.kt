package com.controlprestamos.domain.model

import java.time.LocalDate

data class PaymentRecord(
    val id: Long,
    val loanId: Long,
    val amount: Double,
    val paymentDate: LocalDate,
    val note: String
)
