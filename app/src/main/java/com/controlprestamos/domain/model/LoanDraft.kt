package com.controlprestamos.domain.model

import java.time.LocalDate

data class LoanDraft(
    val customerName: String = "",
    val phone: String = "",
    val idNumber: String = "",
    val principalAmount: String = "",
    val interestRate: String = "",
    val loanDate: String = "",
    val dueDate: String = "",
    val observations: String = ""
) {
    fun parsedPrincipal(): Double? = principalAmount.replace(",", ".").toDoubleOrNull()
    fun parsedInterest(): Double? = interestRate.replace(",", ".").toDoubleOrNull()
    fun parsedLoanDate(): LocalDate? = runCatching { LocalDate.parse(loanDate) }.getOrNull()
    fun parsedDueDate(): LocalDate? = runCatching { LocalDate.parse(dueDate) }.getOrNull()
}
