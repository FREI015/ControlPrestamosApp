package com.controlprestamos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.ui.viewmodel.LoanDetailViewModel
import com.controlprestamos.util.CurrencyUtils
import com.controlprestamos.util.DateUtils

@Composable
fun LoanDetailScreen(
    loanId: Long,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onOpenCollectionNotice: (Long) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val viewModel: LoanDetailViewModel = viewModel(
        factory = LoanDetailViewModel.factory(app.container.loanRepository, loanId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loan = uiState.loan

    if (loan == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("No se encontró el préstamo.")
            TextButton(onClick = onBack) { Text("Volver") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(loan.customerName, style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Teléfono: ${loan.phone}")
                    Text("Cédula: ${loan.idNumber}")
                    Text("Prestado: ${CurrencyUtils.usd(loan.principalAmount)}")
                    Text("Ganancia: ${CurrencyUtils.usd(loan.profitAmount)}")
                    Text("Total a regresar: ${CurrencyUtils.usd(loan.totalToRepay)}")
                    Text("Pagado: ${CurrencyUtils.usd(loan.totalPaid)}")
                    Text("Pendiente: ${CurrencyUtils.usd(loan.pendingAmount)}")
                    Text("Préstamo: ${DateUtils.format(loan.loanDate)}")
                    Text("Vencimiento: ${DateUtils.format(loan.dueDate)}")
                    if (loan.isOverdue) {
                        Text("Retraso: ${loan.daysOverdue} día(s)")
                    }
                    Text("Observaciones: ${loan.observations.ifBlank { "Sin observaciones" }}")
                }
            }
        }

        item {
            Button(
                onClick = { onOpenCollectionNotice(loan.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar / preparar cobro")
            }
        }

        item {
            Text("Pagos y cierre", style = MaterialTheme.typography.titleMedium)
        }

        item {
            OutlinedTextField(
                value = uiState.paymentAmount,
                onValueChange = viewModel::updatePaymentAmount,
                label = { Text("Monto del pago") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.paymentDate,
                onValueChange = viewModel::updatePaymentDate,
                label = { Text("Fecha del pago / cierre (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.paymentNote,
                onValueChange = viewModel::updatePaymentNote,
                label = { Text("Nota del pago o del cobro") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = viewModel::registerPayment,
                enabled = !uiState.processing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.processing) "Procesando..." else "Registrar pago")
            }
        }

        item {
            Button(
                onClick = viewModel::markAsCollected,
                enabled = !uiState.processing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Marcar como cobrado y archivar")
            }
        }

        item {
            Text("Marcar como perdido", style = MaterialTheme.typography.titleMedium)
        }

        item {
            OutlinedTextField(
                value = uiState.lossNote,
                onValueChange = viewModel::updateLossNote,
                label = { Text("Motivo / nota de pérdida") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = viewModel::markAsLost,
                enabled = !uiState.processing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Marcar como perdido")
            }
        }

        uiState.message?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }

        uiState.error?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Text("Historial de pagos", style = MaterialTheme.typography.titleMedium)
        }

        if (loan.payments.isEmpty()) {
            item { Text("No hay pagos registrados todavía.") }
        } else {
            items(loan.payments, key = { it.id }) { payment ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            CurrencyUtils.usd(payment.amount),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(DateUtils.format(payment.paymentDate))
                        if (payment.note.isNotBlank()) {
                            Text(payment.note)
                        }
                    }
                }
            }
        }

        item {
            TextButton(onClick = onBack) { Text("Volver") }
        }
    }
}