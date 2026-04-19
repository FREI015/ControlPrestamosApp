package com.controlprestamos.features.loans

import com.controlprestamos.features.profile.*

import com.controlprestamos.core.validation.*

import com.controlprestamos.core.format.*

import com.controlprestamos.app.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController

@Composable
fun LoanDetailScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var loan by remember { mutableStateOf(sessionStore.readActiveLoan()) }
    var paymentAmount by remember { mutableStateOf("") }
    var blacklistReason by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmCollected by remember { mutableStateOf(false) }
    var confirmLost by remember { mutableStateOf(false) }

    fun reload() {
        loan = sessionStore.readActiveLoan()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentLoan = loan
    if (currentLoan == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppTopBack(title = "Detalle del préstamo", onBack = { navController.popBackStack() })
            AppSectionCard { Text("No hay un préstamo seleccionado.") }
            AppBottomBack(onClick = { navController.popBackStack() })
        }
        return
    }

    val paymentHistory = remember(currentLoan.id, currentLoan.paidAmount, currentLoan.status) {
        sessionStore.readLoanPaymentHistory(currentLoan.id)
    }

    AppConfirmDialog(
        visible = confirmDelete,
        title = "Eliminar préstamo",
        message = "¿Seguro que deseas enviar a papelera el préstamo de ${currentLoan.fullName.ifBlank { "este cliente" }}? Podrás restaurarlo desde la papelera durante 30 días.",
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            sessionStore.softDeleteLoan(currentLoan.id)
            sessionStore.setActiveLoanId("")
            confirmDelete = false
            navController.navigate(AppRoutes.Loans) {
                popUpTo(AppRoutes.Loans) { inclusive = true }
                launchSingleTop = true
            }
        },
        onDismiss = { confirmDelete = false }
    )

    AppConfirmDialog(
        visible = confirmCollected,
        title = "Marcar como cobrado",
        message = "Se marcará el préstamo como cobrado y el pendiente quedará en cero.",
        confirmText = "Confirmar",
        dismissText = "Cancelar",
        onConfirm = {
            val ok = sessionStore.markLoanAsCollected(currentLoan.id)
            feedback = if (ok) "Préstamo marcado como cobrado." else "No se pudo marcar como cobrado."
            confirmCollected = false
            reload()
        },
        onDismiss = { confirmCollected = false }
    )

    AppConfirmDialog(
        visible = confirmLost,
        title = "Marcar como perdido",
        message = "El préstamo quedará en estado perdido.",
        confirmText = "Confirmar",
        dismissText = "Cancelar",
        onConfirm = {
            val ok = sessionStore.markLoanAsLost(currentLoan.id)
            feedback = if (ok) "Préstamo marcado como perdido." else "No se pudo marcar como perdido."
            confirmLost = false
            reload()
        },
        onDismiss = { confirmLost = false }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = currentLoan.fullName.ifBlank { "Detalle del préstamo" },
            onBack = { navController.popBackStack() }
        )

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppSectionCard {
            Text("Identidad del préstamo", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppMutedText("Cliente")
                    Text(
                        text = currentLoan.fullName.ifBlank { "-" },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                AppStatusChip(detailStatusLabelRefined(currentLoan))
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailInfoCardRefined("Cédula", currentLoan.idNumber.ifBlank { "-" }, Modifier.width(cardWidth))
                    DetailInfoCardRefined("Teléfono", currentLoan.phone.ifBlank { "-" }, Modifier.width(cardWidth))
                }
            }
        }

        AppSectionCard {
            Text("Estado del préstamo", style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailInfoCardRefined("Préstamo", currentLoan.loanDate.ifBlank { "-" }, Modifier.width(cardWidth))
                        DetailInfoCardRefined("Vencimiento", currentLoan.dueDate.ifBlank { "-" }, Modifier.width(cardWidth))
                    }

                    if (currentLoan.exchangeRate.isNotBlank()) {
                        DetailInfoCardRefined("Tasa", currentLoan.exchangeRate, Modifier.fillMaxWidth())
                    }

                    if (currentLoan.conditions.isNotBlank()) {
                        DetailInfoCardRefined("Condiciones", currentLoan.conditions, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        AppSectionCard {
            Text("Resumen financiero", style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyCardRefined("Prestado", formatMoney(currentLoan.loanAmount), Modifier.width(cardWidth))
                        MoneyCardRefined("Ganancia", formatMoney(currentLoan.interestAmount()), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyCardRefined("Total", formatMoney(currentLoan.totalAmount()), Modifier.width(cardWidth))
                        MoneyCardRefined("Abonado", formatMoney(currentLoan.paidAmount), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyCardRefined("Pendiente", formatMoney(currentLoan.pendingAmount()), Modifier.width(cardWidth))
                        Spacer(modifier = Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text("Registrar pago", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = paymentAmount,
                onValueChange = {
                    paymentAmount = sanitizeDecimalInput(it)
                    feedback = ""
                },
                label = { Text("Monto del abono") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AppMutedText("Pendiente actual: ${formatMoney(currentLoan.pendingAmount())}")

            AppPrimaryButton(
                text = "Registrar pago parcial",
                onClick = {
                    val amount = parseMoneyOrNull(paymentAmount)
                    when {
                        amount == null || amount <= 0.0 -> {
                            feedback = "Debes colocar un monto válido."
                        }

                        amount > currentLoan.pendingAmount() -> {
                            feedback = "El pago no puede ser mayor al pendiente."
                        }

                        else -> {
                            val ok = sessionStore.registerPayment(currentLoan.id, amount)
                            feedback = if (ok) "Pago registrado correctamente." else "No se pudo registrar el pago."
                            paymentAmount = ""
                            reload()
                        }
                    }
                }
            )
        }

        AppSectionCard {
            Text("Historial de pagos", style = MaterialTheme.typography.titleMedium)

            if (paymentHistory.isEmpty()) {
                AppMutedText("Todavía no hay pagos registrados para este préstamo.")
            } else {
                paymentHistory.forEachIndexed { index, record ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    Text(
                        text = "${record.paymentDate} · ${record.paymentType}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    AppMutedText("Monto: ${formatMoney(record.amount)}")
                    AppMutedText("Pagado antes: ${formatMoney(record.previousPaidAmount)}")
                    AppMutedText("Pagado ahora: ${formatMoney(record.newPaidAmount)}")
                    AppMutedText("Pendiente antes: ${formatMoney(record.previousPendingAmount)}")
                    AppMutedText("Pendiente ahora: ${formatMoney(record.newPendingAmount)}")

                    if (record.note.isNotBlank()) {
                        AppMutedText(record.note)
                    }
                }
            }
        }

        AppSectionCard {
            Text("Acciones del préstamo", style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailActionCardRefined("Cobro", Modifier.width(cardWidth)) {
                            navController.navigate(AppRoutes.LoanCollectionNotice)
                        }
                        DetailActionCardRefined("Editar préstamo", Modifier.width(cardWidth)) {
                            sessionStore.setActiveLoanId(currentLoan.id)
                            navController.navigate(AppRoutes.EditLoan)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailActionCardRefined("Marcar como cobrado", Modifier.width(cardWidth)) {
                            feedback = ""
                            confirmCollected = true
                        }
                        DetailActionCardRefined("Marcar como perdido", Modifier.width(cardWidth)) {
                            feedback = ""
                            confirmLost = true
                        }
                    }
                }
            }
        }

        AppSectionCard {
            Text("Lista negra", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = blacklistReason,
                onValueChange = {
                    blacklistReason = it
                    feedback = ""
                },
                label = { Text("Motivo para lista negra") },
                modifier = Modifier.fillMaxWidth()
            )

            AppSecondaryButton(
                text = "Enviar a lista negra",
                onClick = {
                    val reason = blacklistReason.trim()
                    if (reason.isBlank()) {
                        feedback = "Debes colocar un motivo para lista negra."
                    } else {
                        sessionStore.saveBlacklistRecord(
                            BlacklistRecordData(
                                fullName = currentLoan.fullName,
                                idNumber = currentLoan.idNumber,
                                phone = currentLoan.phone,
                                reason = reason,
                                notes = currentLoan.conditions
                            )
                        )
                        feedback = "Cliente enviado a lista negra."
                        blacklistReason = ""
                    }
                }
            )
        }

        AppSectionCard {
            Text("Zona sensible", style = MaterialTheme.typography.titleMedium)
            AppMutedText("Eliminar este préstamo borrará también su historial de pagos guardado.")
            AppDangerButton(
                text = "Eliminar préstamo",
                onClick = {
                    feedback = ""
                    confirmDelete = true
                }
            )
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun DetailInfoCardRefined(
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MoneyCardRefined(
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
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun DetailActionCardRefined(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.field
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun detailStatusLabelRefined(loan: ManualLoanData): String {
    return when {
        loan.status == "COBRADO" -> "COBRADO"
        loan.status == "PERDIDO" -> "PERDIDO"
        loan.isOverdue() -> "VENCIDO"
        else -> "ACTIVO"
    }
}
