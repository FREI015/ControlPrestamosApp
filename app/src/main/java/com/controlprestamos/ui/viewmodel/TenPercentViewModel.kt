package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.controlprestamos.data.TenPercentPreferences
import com.controlprestamos.domain.model.TenPercentEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class TenPercentUiState(
    val date: String = LocalDate.now().toString(),
    val clientName: String = "",
    val referredTo: String = "",
    val loanAmountUsd: String = "",
    val percent: String = "10",
    val status: String = "Pendiente",
    val notes: String = "",
    val entries: List<TenPercentEntry> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class TenPercentViewModel(
    private val prefs: TenPercentPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TenPercentUiState(entries = prefs.getEntries())
    )
    val uiState: StateFlow<TenPercentUiState> = _uiState.asStateFlow()

    fun updateDate(value: String) { _uiState.value = _uiState.value.copy(date = value, message = null, error = null) }
    fun updateClientName(value: String) { _uiState.value = _uiState.value.copy(clientName = value, message = null, error = null) }
    fun updateReferredTo(value: String) { _uiState.value = _uiState.value.copy(referredTo = value, message = null, error = null) }
    fun updateLoanAmountUsd(value: String) { _uiState.value = _uiState.value.copy(loanAmountUsd = value.replace(",", "."), message = null, error = null) }
    fun updatePercent(value: String) { _uiState.value = _uiState.value.copy(percent = value.replace(",", "."), message = null, error = null) }
    fun updateStatus(value: String) { _uiState.value = _uiState.value.copy(status = value, message = null, error = null) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value, message = null, error = null) }

    fun save() {
        val amount = _uiState.value.loanAmountUsd.toDoubleOrNull()
        val percentValue = _uiState.value.percent.toDoubleOrNull()

        if (_uiState.value.clientName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Coloca el nombre del cliente.")
            return
        }
        if (amount == null || amount <= 0.0) {
            _uiState.value = _uiState.value.copy(error = "Coloca un monto válido.")
            return
        }
        if (percentValue == null || percentValue <= 0.0) {
            _uiState.value = _uiState.value.copy(error = "Coloca un porcentaje válido.")
            return
        }

        val commission = amount * (percentValue / 100.0)

        val newEntry = TenPercentEntry(
            id = System.currentTimeMillis(),
            date = _uiState.value.date,
            clientName = _uiState.value.clientName.trim(),
            referredTo = _uiState.value.referredTo.trim(),
            loanAmountUsd = amount,
            percent = percentValue,
            commissionUsd = commission,
            status = _uiState.value.status.trim(),
            notes = _uiState.value.notes.trim()
        )

        val updated = listOf(newEntry) + prefs.getEntries()
        prefs.saveEntries(updated)

        _uiState.value = TenPercentUiState(
            entries = prefs.getEntries(),
            message = "Registro 10% guardado."
        )
    }

    fun delete(id: Long) {
        val updated = prefs.getEntries().filterNot { it.id == id }
        prefs.saveEntries(updated)
        _uiState.value = _uiState.value.copy(entries = updated, message = "Registro eliminado.")
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    companion object {
        fun factory(prefs: TenPercentPreferences): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TenPercentViewModel(prefs) as T
                }
            }
    }
}
