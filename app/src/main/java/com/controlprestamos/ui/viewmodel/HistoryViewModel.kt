package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class HistoryCycleItem(
    val cycleKey: String,
    val title: String,
    val totalLoaned: Double,
    val totalProjected: Double,
    val totalRecovered: Double,
    val activeCount: Int,
    val collectedCount: Int,
    val overdueCount: Int,
    val lostCount: Int,
    val loans: List<Loan>
)

data class HistoryUiState(
    val cycles: List<HistoryCycleItem> = emptyList()
)

class HistoryViewModel(
    private val repository: LoanRepository
) : ViewModel() {

    val uiState = repository.observeLoans()
        .map { loans ->
            val grouped = loans
                .groupBy { operationalCycleKeyFor(it) }
                .toList()
                .sortedByDescending { (cycleKey, _) -> cycleKey }
                .map { (cycleKey, cycleLoans) ->
                    HistoryCycleItem(
                        cycleKey = cycleKey,
                        title = cycleTitleFromKey(cycleKey),
                        totalLoaned = cycleLoans.sumOf { it.principalAmount },
                        totalProjected = cycleLoans.sumOf { it.totalToRepay },
                        totalRecovered = cycleLoans.sumOf { it.totalPaid },
                        activeCount = cycleLoans.count { it.status == LoanStatus.ACTIVE && !it.isOverdue },
                        collectedCount = cycleLoans.count { it.status == LoanStatus.COLLECTED },
                        overdueCount = cycleLoans.count { it.isOverdue },
                        lostCount = cycleLoans.count { it.status == LoanStatus.LOST },
                        loans = cycleLoans.sortedWith(
                            compareByDescending<Loan> { operationalDateFor(it) }.thenByDescending { it.id }
                        )
                    )
                }

            HistoryUiState(cycles = grouped)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HistoryUiState()
        )

    companion object {
        fun factory(repository: LoanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }

        private fun operationalDateFor(loan: Loan): LocalDate {
            return when {
                loan.status == LoanStatus.COLLECTED && loan.closedAt != null -> loan.closedAt
                loan.status == LoanStatus.LOST && loan.closedAt != null -> loan.closedAt
                else -> loan.dueDate
            }
        }

        private fun operationalCycleKeyFor(loan: Loan): String {
            val date = operationalDateFor(loan)
            val half = if (date.dayOfMonth <= 15) "Q1" else "Q2"
            return "${date.year}-${date.monthValue.toString().padStart(2, '0')}-$half"
        }

        private fun cycleTitleFromKey(key: String): String {
            val parts = key.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: return key
            val month = parts.getOrNull(1)?.toIntOrNull() ?: return key
            val half = parts.getOrNull(2) ?: return key

            val yearMonth = YearMonth.of(year, month)
            val monthName = yearMonth.month
                .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                .replaceFirstChar { it.uppercase() }

            val startDay = if (half == "Q1") 1 else 16
            val endDay = if (half == "Q1") 15 else yearMonth.lengthOfMonth()

            return "Quincena $startDay al $endDay de $monthName $year"
        }
    }
}
