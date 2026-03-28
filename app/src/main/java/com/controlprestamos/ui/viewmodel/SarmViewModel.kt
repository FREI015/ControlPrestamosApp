package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.controlprestamos.data.SarmPreferences
import com.controlprestamos.domain.model.SarmEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class SarmUiState(
    val date: String = LocalDate.now().toString(),
    val clientName: String = "",
    val amountUsd: String = "",
    val status: String = "Pendiente",
    val notes: String = "",
    val entries: List<SarmEntry> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class SarmViewModel(
    private val prefs: SarmPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SarmUiState(entries = prefs.getEntries())
    )
    val uiState: StateFlow<SarmUiState> = _uiState.asStateFlow()

    fun updateDate(value: String) { _uiState.value = _uiState.value.copy(date = value, message = null, error = null) }
    fun updateClientName(value: String) { _uiState.value = _uiState.value.copy(clientName = value, message = null, error = null) }
    fun updateAmountUsd(value: String) { _uiState.value = _uiState.value.copy(amountUsd = value.replace(",", "."), message = null, error = null) }
    fun updateStatus(value: String) { _uiState.value = _uiState.value.copy(status = value, message = null, error = null) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value, message = null, error = null) }

    fun save() {
        val amount = _uiState.value.amountUsd.toDoubleOrNull()
        if (_uiState.value.clientName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Coloca el nombre del cliente.")
            return
        }
        if (amount == null || amount <= 0.0) {
            _uiState.value = _uiState.value.copy(error = "Coloca un monto válido.")
            return
        }

        val newEntry = SarmEntry(
            id = System.currentTimeMillis(),
            date = _uiState.value.date,
            clientName = _uiState.value.clientName.trim(),
            amountUsd = amount,
            status = _uiState.value.status.trim(),
            notes = _uiState.value.notes.trim()
        )

        val updated = listOf(newEntry) + prefs.getEntries()
        prefs.saveEntries(updated)

        _uiState.value = SarmUiState(
            entries = prefs.getEntries(),
            message = "Registro SARM guardado."
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
        fun factory(prefs: SarmPreferences): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SarmViewModel(prefs) as T
                }
            }
    }
}
