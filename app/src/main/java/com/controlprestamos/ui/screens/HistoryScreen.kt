package com.controlprestamos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanStatus
import com.controlprestamos.ui.viewmodel.HistoryCycleItem
import com.controlprestamos.ui.viewmodel.HistoryViewModel
import com.controlprestamos.util.CurrencyUtils
import com.controlprestamos.util.DateUtils

@Composable
fun HistoryScreen(
    paddingValues: PaddingValues
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(app.container.loanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Historial por quincena",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cada bloque agrupa los movimientos por la quincena operativa del préstamo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (uiState.cycles.isEmpty()) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Todavía no hay movimientos históricos.")
                    }
                }
            }
        } else {
            items(uiState.cycles, key = { it.cycleKey }) { cycle ->
                val expanded = expandedMap[cycle.cycleKey] ?: false

                HistoryCycleCard(
                    cycle = cycle,
                    expanded = expanded,
                    onToggle = {
                        expandedMap[cycle.cycleKey] = !expanded
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryCycleCard(
    cycle: HistoryCycleItem,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = cycle.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric("Prestado", CurrencyUtils.usd(cycle.totalLoaned))
                MiniMetric("A cobrar", CurrencyUtils.usd(cycle.totalProjected))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric("Cobrado", CurrencyUtils.usd(cycle.totalRecovered))
                MiniMetric("Retrasos", cycle.overdueCount.toString())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric("Activos", cycle.activeCount.toString())
                MiniMetric("Cobrados", cycle.collectedCount.toString())
                MiniMetric("Perdidos", cycle.lostCount.toString())
            }

            Text(
                text = if (expanded) "Ocultar detalle" else "Ver detalle de la quincena",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (expanded) {
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cycle.loans.forEach { loan ->
                        HistoryLoanItem(loan = loan)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLoanItem(
    loan: Loan
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = loan.customerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text("Préstamo: ${CurrencyUtils.usd(loan.principalAmount)}")
            Text("Ganancia proyectada: ${CurrencyUtils.usd(loan.profitAmount)}")
            Text("Total a cobrar: ${CurrencyUtils.usd(loan.totalToRepay)}")
            Text("Pagado: ${CurrencyUtils.usd(loan.totalPaid)}")
            Text("Pendiente: ${CurrencyUtils.usd(loan.pendingAmount)}")
            Text("Fecha del préstamo: ${DateUtils.format(loan.loanDate)}")
            Text("Vencimiento: ${DateUtils.format(loan.dueDate)}")
            if (loan.closedAt != null) {
                Text("Cierre: ${DateUtils.format(loan.closedAt)}")
            }
            Text("Estado: ${historyStatusLabel(loan)}")

            if (loan.observations.isNotBlank()) {
                Text("Observaciones: ${loan.observations}")
            }
        }
    }
}

@Composable
private fun MiniMetric(
    title: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun historyStatusLabel(loan: Loan): String {
    return when {
        loan.isOverdue -> "Retrasado"
        loan.status == LoanStatus.COLLECTED -> "Cobrado"
        loan.status == LoanStatus.LOST -> "Perdido"
        loan.status == LoanStatus.ACTIVE -> "Activo"
        else -> loan.status.name
    }
}
