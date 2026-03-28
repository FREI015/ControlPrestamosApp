package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.DashboardDetailType
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class DashboardDetailUiState(
    val detailType: DashboardDetailType = DashboardDetailType.TOTAL_LOANED,
    val title: String = "",
    val description: String = "",
    val loans: List<Loan> = emptyList(),
    val totalPrimary: Double = 0.0,
    val totalSecondary: Double = 0.0
)

class DashboardDetailViewModel(
    private val repository: LoanRepository,
    private val detailType: DashboardDetailType
) : ViewModel() {

    val uiState = repository.observeLoans()
        .map { loans ->
            val today = LocalDate.now()
            val currentRange = currentBiweeklyRange(today)

            val filteredLoans = when (detailType) {
                DashboardDetailType.TOTAL_LOANED -> {
                    loans.filter { it.loanDate in currentRange.first..currentRange.second }
                        .sortedByDescending { it.loanDate }
                }

                DashboardDetailType.PROJECTED_INTEREST -> {
                    loans.filter { it.loanDate in currentRange.first..currentRange.second }
                        .sortedByDescending { it.loanDate }
                }

                DashboardDetailType.TOTAL_TO_COLLECT -> {
                    loans.filter { it.loanDate in currentRange.first..currentRange.second }
                        .sortedByDescending { it.loanDate }
                }

                DashboardDetailType.OVERDUE_PAYMENTS -> {
                    loans.filter { it.isOverdue }
                        .sortedWith(
                            compareByDescending<Loan> { it.daysOverdue }
                                .thenByDescending { it.dueDate }
                        )
                }
            }

            val title = detailType.title
            val description = when (detailType) {
                DashboardDetailType.TOTAL_LOANED ->
                    "Aquí ves a quién se le prestó, cuándo y cuánto se prestó en la quincena actual."

                DashboardDetailType.PROJECTED_INTEREST ->
                    "Aquí ves cuánto interés proyecta cada préstamo de la quincena actual."

                DashboardDetailType.TOTAL_TO_COLLECT ->
                    "Aquí ves capital + ganancia esperada de cada préstamo de la quincena actual."

                DashboardDetailType.OVERDUE_PAYMENTS ->
                    "Aquí ves los préstamos retrasados, incluyendo los arrastrados de quincenas anteriores."
            }

            val totalPrimary = when (detailType) {
                DashboardDetailType.TOTAL_LOANED ->
                    filteredLoans.sumOf { it.principalAmount }

                DashboardDetailType.PROJECTED_INTEREST ->
                    filteredLoans.sumOf { it.profitAmount }

                DashboardDetailType.TOTAL_TO_COLLECT ->
                    filteredLoans.sumOf { it.totalToRepay }

                DashboardDetailType.OVERDUE_PAYMENTS ->
                    filteredLoans.sumOf { it.pendingAmount }
            }

            val totalSecondary = when (detailType) {
                DashboardDetailType.TOTAL_LOANED ->
                    filteredLoans.sumOf { it.profitAmount }

                DashboardDetailType.PROJECTED_INTEREST ->
                    filteredLoans.sumOf { it.totalToRepay }

                DashboardDetailType.TOTAL_TO_COLLECT ->
                    filteredLoans.sumOf { it.principalAmount }

                DashboardDetailType.OVERDUE_PAYMENTS ->
                    filteredLoans.sumOf { it.daysOverdue.toDouble() }
            }

            DashboardDetailUiState(
                detailType = detailType,
                title = title,
                description = description,
                loans = filteredLoans,
                totalPrimary = totalPrimary,
                totalSecondary = totalSecondary
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardDetailUiState(detailType = detailType)
        )

    companion object {
        fun factory(
            repository: LoanRepository,
            detailType: DashboardDetailType
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardDetailViewModel(repository, detailType) as T
                }
            }

        private fun currentBiweeklyRange(date: LocalDate): Pair<LocalDate, LocalDate> {
            return if (date.dayOfMonth <= 15) {
                date.withDayOfMonth(1) to date.withDayOfMonth(15)
            } else {
                val endOfMonth = YearMonth.from(date).atEndOfMonth()
                date.withDayOfMonth(16) to endOfMonth
            }
        }
    }
}