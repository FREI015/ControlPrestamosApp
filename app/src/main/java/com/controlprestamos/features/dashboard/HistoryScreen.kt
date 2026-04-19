package com.controlprestamos.features.dashboard

import com.controlprestamos.features.backup.*
import com.controlprestamos.features.security.*
import com.controlprestamos.features.settings.*
import com.controlprestamos.features.people.*
import com.controlprestamos.features.search.*
import com.controlprestamos.features.more.*
import com.controlprestamos.app.*
import com.controlprestamos.features.profile.*

import com.controlprestamos.core.validation.*

import com.controlprestamos.core.format.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val HISTORY_FIN_ALL = "TODOS"
private const val HISTORY_FIN_LOANS = "PRESTAMOS"
private const val HISTORY_FIN_PAYMENTS = "PAGOS"

private data class FinancialHistoryRow(
    val id: String,
    val timestamp: Long,
    val type: String,
    val title: String,
    val subtitle: String,
    val amount: Double
)

@Composable
fun HistoryScreen(
    sessionStore: SessionStore,
    onBack: () -> Unit
) {
    val rows = buildFinancialRows(sessionStore)
    val loans = sessionStore.readLoans()

    var selectedFilter by remember { mutableStateOf(HISTORY_FIN_ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRows = rows
        .filter {
            when (selectedFilter) {
                HISTORY_FIN_LOANS -> it.type == HISTORY_FIN_LOANS
                HISTORY_FIN_PAYMENTS -> it.type == HISTORY_FIN_PAYMENTS
                else -> true
            }
        }
        .filter {
            searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
        }

    val loansCount = rows.count { it.type == HISTORY_FIN_LOANS }
    val paymentsCount = rows.count { it.type == HISTORY_FIN_PAYMENTS }
    val totalLoaned = rows.filter { it.type == HISTORY_FIN_LOANS }.sumOf { it.amount }
    val totalPaid = rows.filter { it.type == HISTORY_FIN_PAYMENTS }.sumOf { it.amount }
    val totalPending = loans.sumOf { it.pendingAmount() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTopBack(title = "Historial financiero", onBack = onBack)
        }

        item {
            AppSectionCard {
                Text(
                    text = "Resumen",
                    style = MaterialTheme.typography.titleMedium
                )

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cardWidth = (maxWidth - 12.dp) / 2

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistoryMetricCard("Préstamos", loansCount.toString(), Modifier.width(cardWidth))
                            HistoryMetricCard("Pagos", paymentsCount.toString(), Modifier.width(cardWidth))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistoryMetricCard("Prestado", formatMoney(totalLoaned), Modifier.width(cardWidth))
                            HistoryMetricCard("Abonado", formatMoney(totalPaid), Modifier.width(cardWidth))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistoryMetricCard("Pendiente", formatMoney(totalPending), Modifier.width(cardWidth))
                            Box(modifier = Modifier.width(cardWidth))
                        }
                    }
                }
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = "Filtro y búsqueda",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = normalizeTextInput(it) },
                    label = { Text("Buscar en historial") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Todos", selectedFilter == HISTORY_FIN_ALL) {
                        selectedFilter = HISTORY_FIN_ALL
                    }
                    AppFilterChip("Préstamos", selectedFilter == HISTORY_FIN_LOANS) {
                        selectedFilter = HISTORY_FIN_LOANS
                    }
                    AppFilterChip("Pagos", selectedFilter == HISTORY_FIN_PAYMENTS) {
                        selectedFilter = HISTORY_FIN_PAYMENTS
                    }
                }

                AppMutedText("Mostrando: ${filteredRows.size}")
            }
        }

        if (filteredRows.isEmpty()) {
            item {
                AppSectionCard {
                    Text("No hay movimientos financieros para mostrar.")
                }
            }
        } else {
            items(filteredRows, key = { it.id }) { row ->
                AppSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppStatusChip(if (row.type == HISTORY_FIN_LOANS) "Préstamo" else "Pago")
                        HistoryAmountPill(formatMoney(row.amount))
                    }

                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    AppMutedText(row.subtitle)
                }
            }
        }

        item {
            AppBottomBack(onClick = onBack)
        }
    }
}

@Composable
private fun HistoryMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.field
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun HistoryAmountPill(value: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.pill
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun buildFinancialRows(sessionStore: SessionStore): List<FinancialHistoryRow> {
    val loans = sessionStore.readLoans()
    val result = mutableListOf<FinancialHistoryRow>()

    loans.forEach { loan ->
        result.add(
            FinancialHistoryRow(
                id = "loan-${loan.id}",
                timestamp = loan.createdAt,
                type = HISTORY_FIN_LOANS,
                title = loan.fullName.ifBlank { "Préstamo sin nombre" },
                subtitle = "Préstamo registrado · ${loan.loanDate}",
                amount = loan.loanAmount
            )
        )

        sessionStore.readLoanPaymentHistory(loan.id).forEach { payment ->
            result.add(
                FinancialHistoryRow(
                    id = "payment-${payment.id}",
                    timestamp = payment.createdAt,
                    type = HISTORY_FIN_PAYMENTS,
                    title = payment.clientName.ifBlank { loan.fullName.ifBlank { "Pago" } },
                    subtitle = "Pago registrado · ${payment.paymentDate}",
                    amount = payment.amount
                )
            )
        }
    }

    return result.sortedByDescending { it.timestamp }
}
