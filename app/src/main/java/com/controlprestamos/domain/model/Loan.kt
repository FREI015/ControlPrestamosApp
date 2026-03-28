package com.controlprestamos.domain.model

import java.time.LocalDate

data class Loan(
    val id: Long,
    val customerName: String,
    val phone: String,
    val idNumber: String,
    val principalAmount: Double,
    val interestRate: Double,
    val profitAmount: Double,
    val totalToRepay: Double,
    val loanDate: LocalDate,
    val dueDate: LocalDate,
    val status: LoanStatus,
    val observations: String,
    val payments: List<PaymentRecord>,
    val totalPaid: Double,
    val pendingAmount: Double,
    val isOverdue: Boolean,
    val daysOverdue: Long,
    val isBlacklisted: Boolean,
    val closedAt: LocalDate?
)
