package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.Loan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LoanDetailUiState(
    val loan: Loan? = null,
    val paymentAmount: String = "",
    val paymentDate: String = LocalDate.now().toString(),
    val paymentNote: String = "",
    val lossNote: String = "",
    val processing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class LoanDetailViewModel(
    private val repository: LoanRepository,
    private val loanId: Long
) : ViewModel() {

    private val paymentAmount = MutableStateFlow("")
    private val paymentDate = MutableStateFlow(LocalDate.now().toString())
    private val paymentNote = MutableStateFlow("")
    private val lossNote = MutableStateFlow("")
    private val processing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LoanDetailUiState> = combine(
        repository.observeLoan(loanId),
        paymentAmount,
        paymentDate,
        paymentNote,
        lossNote,
        processing,
        message,
        error
    ) { values ->
        val loan = values[0] as Loan?
        val amount = values[1] as String
        val date = values[2] as String
        val note = values[3] as String
        val loss = values[4] as String
        val processingValue = values[5] as Boolean
        val messageValue = values[6] as String?
        val errorValue = values[7] as String?

        LoanDetailUiState(
            loan = loan,
            paymentAmount = amount,
            paymentDate = date,
            paymentNote = note,
            lossNote = loss,
            processing = processingValue,
            message = messageValue,
            error = errorValue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LoanDetailUiState()
    )

    fun updatePaymentAmount(value: String) {
        paymentAmount.value = sanitizeDecimal(value)
        clearFeedback()
    }

    fun updatePaymentDate(value: String) {
        paymentDate.value = value
        clearFeedback()
    }

    fun updatePaymentNote(value: String) {
        paymentNote.value = value
        clearFeedback()
    }

    fun updateLossNote(value: String) {
        lossNote.value = value
        clearFeedback()
    }

    fun registerPayment() {
        val amount = paymentAmount.value.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            error.value = "Ingresa un monto de pago válido."
            return
        }

        val parsedDate = paymentDate.value.toLocalDateOrNull()
        if (parsedDate == null) {
            error.value = "La fecha del pago no es válida. Usa YYYY-MM-DD."
            return
        }

        processing.value = true
        clearFeedback()

        viewModelScope.launch {
            repository.registerPayment(
                loanId = loanId,
                amount = amount,
                paymentDate = parsedDate,
                note = paymentNote.value.trim()
            ).onSuccess {
                paymentAmount.value = ""
                paymentNote.value = ""
                paymentDate.value = LocalDate.now().toString()
                message.value = "Pago registrado correctamente."
            }.onFailure {
                error.value = it.message ?: "No se pudo registrar el pago."
            }

            processing.value = false
        }
    }

    fun markAsCollected() {
        val parsedDate = paymentDate.value.toLocalDateOrNull()
        if (parsedDate == null) {
            error.value = "La fecha de cierre no es válida. Usa YYYY-MM-DD."
            return
        }

        processing.value = true
        clearFeedback()

        viewModelScope.launch {
            repository.markLoanAsCollected(
                loanId = loanId,
                collectedDate = parsedDate,
                note = paymentNote.value.trim()
            ).onSuccess {
                repository.archiveLoan(
                    loanId = loanId,
                    archiveDate = parsedDate
                )
                paymentAmount.value = ""
                paymentNote.value = ""
                paymentDate.value = LocalDate.now().toString()
                message.value = "Préstamo marcado como cobrado y archivado."
            }.onFailure {
                error.value = it.message ?: "No se pudo marcar como cobrado."
            }

            processing.value = false
        }
    }

    fun markAsLost() {
        val note = lossNote.value.trim()
        if (note.isBlank()) {
            error.value = "Debes indicar el motivo o nota de pérdida."
            return
        }

        processing.value = true
        clearFeedback()

        viewModelScope.launch {
            repository.markLoanAsLost(
                loanId = loanId,
                note = note
            ).onSuccess {
                repository.archiveLoan(
                    loanId = loanId,
                    archiveDate = LocalDate.now()
                )
                lossNote.value = ""
                message.value = "Préstamo marcado como perdido y archivado."
            }.onFailure {
                error.value = it.message ?: "No se pudo marcar como perdido."
            }

            processing.value = false
        }
    }

    private fun clearFeedback() {
        message.value = null
        error.value = null
    }

    companion object {
        fun factory(
            repository: LoanRepository,
            loanId: Long
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoanDetailViewModel(repository, loanId) as T
                }
            }

        private fun sanitizeDecimal(value: String): String {
            val normalized = value.replace(",", ".")
            val result = StringBuilder()
            var hasDot = false

            normalized.forEach { char ->
                when {
                    char.isDigit() -> result.append(char)
                    char == '.' && !hasDot -> {
                        result.append(char)
                        hasDot = true
                    }
                }
            }
            return result.toString()
        }

        private fun String.toLocalDateOrNull(): LocalDate? {
            return runCatching { LocalDate.parse(this.trim()) }.getOrNull()
        }
    }
}