package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CollectionNoticeFormState(
    val exchangeRateText: String = "",
    val lateFeePercentText: String = "",
    val lateFeeFixedText: String = "",
    val noteText: String = ""
)

data class CollectionNoticeUiState(
    val loanId: Long = 0,
    val customerName: String = "",
    val phone: String = "",
    val pendingAmountUsd: Double = 0.0,
    val dueDateText: String = "",
    val isOverdue: Boolean = false,
    val daysOverdue: Long = 0,
    val exchangeRateText: String = "",
    val lateFeePercentText: String = "",
    val lateFeeFixedText: String = "",
    val noteText: String = "",
    val previewLateFeePercentAmount: Double = 0.0,
    val previewLateFeeFixedAmount: Double = 0.0,
    val totalToChargeUsd: Double = 0.0,
    val totalToChargeVes: Double = 0.0,
    val shareMessage: String = "",
    val reminderMarkedThisSession: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CollectionNoticeViewModel(
    private val loanId: Long,
    private val repository: LoanRepository
) : ViewModel() {

    private val formState = MutableStateFlow(CollectionNoticeFormState())
    private val errorMessage = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)
    private val reminderMarkedThisSession = MutableStateFlow(false)

    val uiState = combine(
        repository.observeLoan(loanId),
        formState,
        errorMessage,
        successMessage,
        reminderMarkedThisSession
    ) { loan, form, error, success, reminderMarked ->
        if (loan == null) {
            CollectionNoticeUiState(
                loanId = loanId,
                isLoading = false,
                errorMessage = "No se encontró el préstamo."
            )
        } else {
            val exchangeRate = form.exchangeRateText.toDoubleOrNull() ?: 0.0
            val lateFeePercent = form.lateFeePercentText.toDoubleOrNull() ?: 0.0
            val lateFeeFixed = form.lateFeeFixedText.toDoubleOrNull() ?: 0.0

            val extraPercentAmount = loan.pendingAmount * (lateFeePercent / 100.0)
            val extraFixedAmount = lateFeeFixed
            val totalUsd = loan.pendingAmount + extraPercentAmount + extraFixedAmount
            val totalVes = if (exchangeRate > 0.0) totalUsd * exchangeRate else 0.0

            CollectionNoticeUiState(
                loanId = loan.id,
                customerName = loan.customerName,
                phone = loan.phone,
                pendingAmountUsd = loan.pendingAmount,
                dueDateText = loan.dueDate.format(dateFormatter),
                isOverdue = loan.isOverdue,
                daysOverdue = loan.daysOverdue,
                exchangeRateText = form.exchangeRateText,
                lateFeePercentText = form.lateFeePercentText,
                lateFeeFixedText = form.lateFeeFixedText,
                noteText = form.noteText,
                previewLateFeePercentAmount = extraPercentAmount,
                previewLateFeeFixedAmount = extraFixedAmount,
                totalToChargeUsd = totalUsd,
                totalToChargeVes = totalVes,
                shareMessage = buildShareMessage(
                    customerName = loan.customerName,
                    dueDateText = loan.dueDate.format(dateFormatter),
                    pendingUsd = loan.pendingAmount,
                    lateFeePercent = lateFeePercent,
                    extraPercentAmount = extraPercentAmount,
                    lateFeeFixed = lateFeeFixed,
                    totalUsd = totalUsd,
                    exchangeRate = exchangeRate,
                    totalVes = totalVes
                ),
                reminderMarkedThisSession = reminderMarked,
                isLoading = false,
                errorMessage = error,
                successMessage = success
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CollectionNoticeUiState()
    )

    fun onExchangeRateChange(value: String) {
        formState.update { it.copy(exchangeRateText = sanitizeDecimal(value)) }
    }

    fun onLateFeePercentChange(value: String) {
        formState.update { it.copy(lateFeePercentText = sanitizeDecimal(value)) }
    }

    fun onLateFeeFixedChange(value: String) {
        formState.update { it.copy(lateFeeFixedText = sanitizeDecimal(value)) }
    }

    fun onNoteChange(value: String) {
        formState.update { it.copy(noteText = value) }
    }

    fun applyLateFeePercent() {
        val percent = formState.value.lateFeePercentText.toDoubleOrNull()
        if (percent == null || percent <= 0.0) {
            errorMessage.value = "Ingresa un porcentaje válido."
            return
        }

        viewModelScope.launch {
            repository.applyLateFeePercent(loanId, percent)
                .onSuccess {
                    formState.update { it.copy(lateFeePercentText = "") }
                    successMessage.value = "Recargo porcentual aplicado."
                }
                .onFailure {
                    errorMessage.value = it.message ?: "No se pudo aplicar el recargo porcentual."
                }
        }
    }

    fun applyLateFeeFixed() {
        val amount = formState.value.lateFeeFixedText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errorMessage.value = "Ingresa un recargo fijo válido."
            return
        }

        viewModelScope.launch {
            repository.applyLateFeeFixedAmount(loanId, amount)
                .onSuccess {
                    formState.update { it.copy(lateFeeFixedText = "") }
                    successMessage.value = "Recargo fijo aplicado."
                }
                .onFailure {
                    errorMessage.value = it.message ?: "No se pudo aplicar el recargo fijo."
                }
        }
    }

    fun markReminderSent() {
        viewModelScope.launch {
            repository.markReminderSent(loanId, LocalDate.now())
                .onSuccess {
                    reminderMarkedThisSession.value = true
                    successMessage.value = "Recordatorio marcado como enviado."
                }
                .onFailure {
                    errorMessage.value = it.message ?: "No se pudo marcar el recordatorio."
                }
        }
    }

    fun markAsCollected() {
        val exchangeRate = formState.value.exchangeRateText.toDoubleOrNull()
        val note = formState.value.noteText

        viewModelScope.launch {
            val result = if (exchangeRate != null && exchangeRate > 0.0) {
                repository.markLoanAsCollectedWithExchange(
                    loanId = loanId,
                    collectedDate = LocalDate.now(),
                    exchangeRate = exchangeRate,
                    note = note
                )
            } else {
                repository.markLoanAsCollected(
                    loanId = loanId,
                    collectedDate = LocalDate.now(),
                    note = note
                )
            }

            result.onSuccess {
                successMessage.value = "Préstamo marcado como cobrado."
            }.onFailure {
                errorMessage.value = it.message ?: "No se pudo marcar como cobrado."
            }
        }
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun factory(
            loanId: Long,
            repository: LoanRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CollectionNoticeViewModel(loanId, repository) as T
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

        private fun buildShareMessage(
            customerName: String,
            dueDateText: String,
            pendingUsd: Double,
            lateFeePercent: Double,
            extraPercentAmount: Double,
            lateFeeFixed: Double,
            totalUsd: Double,
            exchangeRate: Double,
            totalVes: Double
        ): String {
            val lines = mutableListOf<String>()

            lines += "Hola, $customerName."
            lines += "Te escribo por el cobro pendiente de tu préstamo."
            lines += "Fecha de pago: $dueDateText"
            lines += "Monto base en USD: ${formatMoney(pendingUsd)}"

            if (lateFeePercent > 0.0) {
                lines += "Recargo por retraso (${formatMoney(lateFeePercent)}%): ${formatMoney(extraPercentAmount)} USD"
            }

            if (lateFeeFixed > 0.0) {
                lines += "Recargo fijo: ${formatMoney(lateFeeFixed)} USD"
            }

            lines += "Total a pagar en USD: ${formatMoney(totalUsd)}"

            if (exchangeRate > 0.0) {
                lines += "Tasa del día: ${formatMoney(exchangeRate)} Bs"
                lines += "Total equivalente en bolívares: ${formatMoney(totalVes)} Bs"
            }

            lines += "Por favor, confirma tu pago a la brevedad."
            return lines.joinToString("\n")
        }

        private fun formatMoney(value: Double): String {
            return String.format("%.2f", value)
        }
    }
}