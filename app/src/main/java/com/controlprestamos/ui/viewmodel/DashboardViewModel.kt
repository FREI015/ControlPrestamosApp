package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.domain.model.DashboardPeriod
import com.controlprestamos.domain.model.DashboardSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val selectedPeriod: DashboardPeriod = DashboardPeriod.BIWEEKLY,
    val isLockedToCurrentBiweekly: Boolean = true,
    val showPeriodSelector: Boolean = false,
    val title: String = "Resumen de operaciones",
    val subtitle: String = "Quincena actual",
    val summary: DashboardSummary = DashboardSummary(label = DashboardPeriod.BIWEEKLY.label)
)

class DashboardViewModel(
    private val repository: LoanRepository
) : ViewModel() {

    private val lockedPeriod = DashboardPeriod.BIWEEKLY

    val uiState = repository.observeDashboard(lockedPeriod)
        .map { summary ->
            DashboardUiState(
                selectedPeriod = lockedPeriod,
                isLockedToCurrentBiweekly = true,
                showPeriodSelector = false,
                title = "Resumen de operaciones",
                subtitle = summary.currentCycleLabel.ifBlank { "Quincena actual" },
                summary = summary.copy(label = lockedPeriod.label)
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardUiState()
        )

    fun selectPeriod(period: DashboardPeriod) {
        // El dashboard principal queda anclado a la quincena actual.
        // Más adelante, si abrimos historial por quincena, esa navegación
        // se manejará desde otra pantalla y no desde este panel principal.
    }

    companion object {
        fun factory(repository: LoanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository) as T
                }
            }
    }
}