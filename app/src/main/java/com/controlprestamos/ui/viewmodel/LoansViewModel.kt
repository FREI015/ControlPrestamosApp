package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class LoanFilter { ALL, ACTIVE, OVERDUE, COLLECTED, LOST, BLACKLISTED }

data class LoansUiState(
    val search: String = "",
    val filter: LoanFilter = LoanFilter.ALL,
    val fromDate: String = "",
    val toDate: String = "",
    val loans: List<Loan> = emptyList()
)

class LoansViewModel(
    private val repository: LoanRepository
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val filter = MutableStateFlow(LoanFilter.ALL)
    private val fromDate = MutableStateFlow("")
    private val toDate = MutableStateFlow("")

    val uiState = combine(
        repository.observeLoans(),
        search,
        filter,
        fromDate,
        toDate
    ) { loans, searchValue, filterValue, fromValue, toValue ->
        val start = runCatching { LocalDate.parse(fromValue) }.getOrNull()
        val end = runCatching { LocalDate.parse(toValue) }.getOrNull()

        val filtered = loans.filter { loan ->
            val matchesText = searchValue.isBlank() ||
                loan.customerName.contains(searchValue, ignoreCase = true)
            val matchesFilter = when (filterValue) {
                LoanFilter.ALL -> true
                LoanFilter.ACTIVE -> loan.status == LoanStatus.ACTIVE && !loan.isOverdue
                LoanFilter.OVERDUE -> loan.isOverdue
                LoanFilter.COLLECTED -> loan.status == LoanStatus.COLLECTED
                LoanFilter.LOST -> loan.status == LoanStatus.LOST
                LoanFilter.BLACKLISTED -> loan.isBlacklisted
            }
            val matchesStart = start == null || !loan.loanDate.isBefore(start)
            val matchesEnd = end == null || !loan.loanDate.isAfter(end)
            matchesText && matchesFilter && matchesStart && matchesEnd
        }

        LoansUiState(
            search = searchValue,
            filter = filterValue,
            fromDate = fromValue,
            toDate = toValue,
            loans = filtered
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LoansUiState()
    )

    fun updateSearch(value: String) {
        search.value = value
    }

    fun updateFilter(value: LoanFilter) {
        filter.value = value
    }

    fun updateFromDate(value: String) {
        fromDate.value = value
    }

    fun updateToDate(value: String) {
        toDate.value = value
    }

    companion object {
        fun factory(repository: LoanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoansViewModel(repository) as T
                }
            }
    }
}
