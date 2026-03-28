package com.controlprestamos.data.repository

import com.controlprestamos.domain.model.BlacklistDraft
import com.controlprestamos.domain.model.BlacklistEntry
import com.controlprestamos.domain.model.DashboardPeriod
import com.controlprestamos.domain.model.DashboardSummary
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LoanRepository {
    fun observeLoans(): Flow<List<Loan>>
    fun observeLoan(loanId: Long): Flow<Loan?>
    fun observeBlacklist(): Flow<List<BlacklistEntry>>
    fun observeDashboard(period: DashboardPeriod): Flow<DashboardSummary>

    suspend fun addLoan(draft: LoanDraft): Result<Unit>

    suspend fun registerPayment(
        loanId: Long,
        amount: Double,
        paymentDate: LocalDate,
        note: String
    ): Result<Unit>

    suspend fun registerPaymentWithExchange(
        loanId: Long,
        amountUsd: Double,
        exchangeRate: Double,
        paymentDate: LocalDate,
        note: String,
        wasReminderMessageSent: Boolean = false
    ): Result<Unit>

    suspend fun markLoanAsCollected(
        loanId: Long,
        collectedDate: LocalDate,
        note: String
    ): Result<Unit>

    suspend fun markLoanAsCollectedWithExchange(
        loanId: Long,
        collectedDate: LocalDate,
        exchangeRate: Double,
        note: String
    ): Result<Unit>

    suspend fun markLoanAsLost(loanId: Long, note: String): Result<Unit>

    suspend fun markReminderSent(
        loanId: Long,
        reminderDate: LocalDate = LocalDate.now()
    ): Result<Unit>

    suspend fun applyLateFeePercent(
        loanId: Long,
        percent: Double
    ): Result<Unit>

    suspend fun applyLateFeeFixedAmount(
        loanId: Long,
        amount: Double
    ): Result<Unit>

    suspend fun archiveLoan(
        loanId: Long,
        archiveDate: LocalDate = LocalDate.now()
    ): Result<Unit>

    suspend fun addBlacklist(draft: BlacklistDraft): Result<Unit>
    suspend fun deactivateBlacklist(id: Long)
    suspend fun findBlacklistConflict(idNumber: String, phone: String): BlacklistEntry?

    suspend fun seedIfEmpty()
}