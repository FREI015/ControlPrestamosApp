package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.BlacklistDraft
import com.controlprestamos.domain.model.BlacklistEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BlacklistFormState(
    val customerName: String = "",
    val phone: String = "",
    val idNumber: String = "",
    val reason: String = "",
    val addedDate: String = LocalDate.now().toString()
)

data class BlacklistUiState(
    val search: String = "",
    val form: BlacklistFormState = BlacklistFormState(),
    val entries: List<BlacklistEntry> = emptyList(),
    val saving: Boolean = false,
    val processingIds: Set<Long> = emptySet(),
    val message: String? = null,
    val error: String? = null
)

class BlacklistViewModel(
    private val repository: LoanRepository
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val form = MutableStateFlow(BlacklistFormState())
    private val saving = MutableStateFlow(false)
    private val processingIds = MutableStateFlow<Set<Long>>(emptySet())
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BlacklistUiState> = combine(
        repository.observeBlacklist(),
        search,
        form,
        saving,
        processingIds,
        message,
        error
    ) { values ->
        val entries = values[0] as List<BlacklistEntry>
        val searchValue = values[1] as String
        val formValue = values[2] as BlacklistFormState
        val savingValue = values[3] as Boolean
        val processingValue = values[4] as Set<Long>
        val messageValue = values[5] as String?
        val errorValue = values[6] as String?

        val normalizedSearch = searchValue.trim()

        val filteredEntries = if (normalizedSearch.isBlank()) {
            entries.sortedByDescending { it.addedDate }
        } else {
            entries.filter { entry ->
                entry.customerName.contains(normalizedSearch, ignoreCase = true) ||
                    entry.phone.contains(normalizedSearch, ignoreCase = true) ||
                    entry.idNumber.contains(normalizedSearch, ignoreCase = true) ||
                    entry.reason.contains(normalizedSearch, ignoreCase = true)
            }.sortedByDescending { it.addedDate }
        }

        BlacklistUiState(
            search = searchValue,
            form = formValue,
            entries = filteredEntries,
            saving = savingValue,
            processingIds = processingValue,
            message = messageValue,
            error = errorValue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BlacklistUiState()
    )

    fun updateSearch(value: String) {
        search.value = value
    }

    fun updateForm(transform: (BlacklistFormState) -> BlacklistFormState) {
        form.value = transform(form.value)
    }

    fun save() {
        saving.value = true
        message.value = null
        error.value = null

        viewModelScope.launch {
            val draft = BlacklistDraft(
                customerName = form.value.customerName,
                phone = form.value.phone,
                idNumber = form.value.idNumber,
                reason = form.value.reason,
                addedDate = form.value.addedDate
            )

            repository.addBlacklist(draft)
                .onSuccess {
                    form.value = BlacklistFormState(
                        addedDate = LocalDate.now().toString()
                    )
                    message.value = "Registro agregado a lista negra."
                }
                .onFailure {
                    error.value = it.message ?: "No se pudo guardar el registro."
                }

            saving.value = false
        }
    }

    fun deactivate(id: Long) {
        processingIds.value = processingIds.value + id
        message.value = null
        error.value = null

        viewModelScope.launch {
            runCatching {
                repository.deactivateBlacklist(id)
            }.onSuccess {
                message.value = "Registro retirado de lista negra."
            }.onFailure {
                error.value = it.message ?: "No se pudo desactivar el registro."
            }

            processingIds.value = processingIds.value - id
        }
    }

    fun clearFeedback() {
        message.value = null
        error.value = null
    }

    companion object {
        fun factory(repository: LoanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BlacklistViewModel(repository) as T
                }
            }
    }
}