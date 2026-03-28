package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.BlacklistEntry
import com.controlprestamos.domain.model.LoanDraft
import com.controlprestamos.util.CurrencyUtils
import com.controlprestamos.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DEFAULT_INTEREST_RATE = "50"

data class LoanFormUiState(
    val form: LoanDraft = LoanDraft(
        interestRate = DEFAULT_INTEREST_RATE,
        loanDate = DateUtils.today().toString(),
        dueDate = DateUtils.today().plusDays(15).toString()
    ),
    val projectedProfit: String = CurrencyUtils.usd(0.0),
    val projectedTotal: String = CurrencyUtils.usd(0.0),
    val blacklistConflict: BlacklistEntry? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class LoanFormViewModel(
    private val repository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanFormUiState())
    val uiState: StateFlow<LoanFormUiState> = _uiState.asStateFlow()

    private var blacklistJob: Job? = null

    fun updateForm(update: (LoanDraft) -> LoanDraft) {
        var newForm = update(_uiState.value.form)

        val parsedLoanDate = runCatching { LocalDate.parse(newForm.loanDate) }.getOrNull()
        if (parsedLoanDate != null) {
            newForm = newForm.copy(dueDate = parsedLoanDate.plusDays(15).toString())
        }

        val principal = newForm.parsedPrincipal() ?: 0.0
        val interest = newForm.parsedInterest() ?: 0.0
        val profit = principal * (interest / 100.0)
        val total = principal + profit

        _uiState.value = _uiState.value.copy(
            form = newForm,
            projectedProfit = CurrencyUtils.usd(profit),
            projectedTotal = CurrencyUtils.usd(total),
            error = null,
            saved = false
        )

        blacklistJob?.cancel()
        blacklistJob = viewModelScope.launch {
            val conflict = repository.findBlacklistConflict(newForm.idNumber, newForm.phone)
            _uiState.value = _uiState.value.copy(blacklistConflict = conflict)
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null, saved = false)
            repository.addLoan(_uiState.value.form)
                .onSuccess {
                    _uiState.value = LoanFormUiState(
                        form = LoanDraft(
                            interestRate = DEFAULT_INTEREST_RATE,
                            loanDate = DateUtils.today().toString(),
                            dueDate = DateUtils.today().plusDays(15).toString()
                        ),
                        saved = true
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(error = throwable.message ?: "No se pudo guardar.")
                }
            _uiState.value = _uiState.value.copy(saving = false)
        }
    }

    companion object {
        fun factory(repository: LoanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoanFormViewModel(repository) as T
                }
            }
    }
}
