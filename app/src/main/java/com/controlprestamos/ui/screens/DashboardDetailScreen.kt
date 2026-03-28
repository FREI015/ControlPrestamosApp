package com.controlprestamos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.domain.model.DashboardDetailType
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.ui.viewmodel.DashboardDetailViewModel
import com.controlprestamos.util.CurrencyUtils
import com.controlprestamos.util.DateUtils

@Composable
fun DashboardDetailScreen(
    detailTypeValue: String,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onOpenLoan: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val detailType = DashboardDetailType.fromRouteValue(detailTypeValue)

    val viewModel: DashboardDetailViewModel = viewModel(
        factory = DashboardDetailViewModel.factory(
            repository = app.container.loanRepository,
            detailType = detailType
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (uiState.detailType) {
                        DashboardDetailType.TOTAL_LOANED -> {
                            Text("Total prestado: ${CurrencyUtils.usd(uiState.totalPrimary)}")
                            Text("Ganancia proyectada asociada: ${CurrencyUtils.usd(uiState.totalSecondary)}")
                        }

                        DashboardDetailType.PROJECTED_INTEREST -> {
                            Text("Interés total proyectado: ${CurrencyUtils.usd(uiState.totalPrimary)}")
                            Text("Total final asociado: ${CurrencyUtils.usd(uiState.totalSecondary)}")
                        }

                        DashboardDetailType.TOTAL_TO_COLLECT -> {
                            Text("Total general a cobrar: ${CurrencyUtils.usd(uiState.totalPrimary)}")
                            Text("Capital base asociado: ${CurrencyUtils.usd(uiState.totalSecondary)}")
                        }

                        DashboardDetailType.OVERDUE_PAYMENTS -> {
                            Text("Monto pendiente retrasado: ${CurrencyUtils.usd(uiState.totalPrimary)}")
                            Text("Suma de días de retraso: ${uiState.totalSecondary.toInt()}")
                        }
                    }
                }
            }
        }

        if (uiState.loans.isEmpty()) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No hay registros para esta vista.")
                    }
                }
            }
        } else {
            items(uiState.loans, key = { it.id }) { loan ->
                DashboardDetailLoanItem(
                    loan = loan,
                    detailType = uiState.detailType,
                    onOpenLoan = { onOpenLoan(loan.id) }
                )
            }
        }

        item {
            TextButton(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}

@Composable
private fun DashboardDetailLoanItem(
    loan: Loan,
    detailType: DashboardDetailType,
    onOpenLoan: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = loan.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text("Fecha préstamo: ${DateUtils.format(loan.loanDate)}")
            Text("Vencimiento: ${DateUtils.format(loan.dueDate)}")
            Text("Prestado: ${CurrencyUtils.usd(loan.principalAmount)}")

            when (detailType) {
                DashboardDetailType.TOTAL_LOANED -> {
                    Text("Interés proyectado: ${CurrencyUtils.usd(loan.profitAmount)}")
                    Text("Total a cobrar: ${CurrencyUtils.usd(loan.totalToRepay)}")
                }

                DashboardDetailType.PROJECTED_INTEREST -> {
                    Text("Interés proyectado: ${CurrencyUtils.usd(loan.profitAmount)}")
                    Text("Porcentaje: ${String.format("%.2f", loan.interestRate)}%")
                }

                DashboardDetailType.TOTAL_TO_COLLECT -> {
                    Text("Ganancia: ${CurrencyUtils.usd(loan.profitAmount)}")
                    Text("Total a cobrar: ${CurrencyUtils.usd(loan.totalToRepay)}")
                }

                DashboardDetailType.OVERDUE_PAYMENTS -> {
                    Text("Pendiente: ${CurrencyUtils.usd(loan.pendingAmount)}")
                    Text("Días de retraso: ${loan.daysOverdue}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onOpenLoan) {
                    Text("Ver préstamo")
                }
            }
        }
    }
}