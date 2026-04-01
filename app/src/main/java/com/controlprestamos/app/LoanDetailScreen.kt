package com.controlprestamos.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
        message = "¿Seguro que deseas eliminar el préstamo de ${currentLoan.fullName.ifBlank { "este cliente" }}? Esta acción no se puede deshacer.",
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            sessionStore.deleteLoan(currentLoan.id)
            sessionStore.setActiveLoanId("")
            confirmDelete = false
            navController.navigate("loans") {
                popUpTo("loans") { inclusive = true }
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
            Text("Resumen del préstamo", style = MaterialTheme.typography.titleMedium)

            AppLabelValue("Teléfono", currentLoan.phone.ifBlank { "-" })
            AppLabelValue("Cédula", currentLoan.idNumber.ifBlank { "-" })
            AppLabelValue("Estado", detailStatusLabel(currentLoan))
            AppLabelValue("Préstamo", currentLoan.loanDate.ifBlank { "-" })
            AppLabelValue("Vencimiento", currentLoan.dueDate.ifBlank { "-" })

            HorizontalDivider()

            AppLabelValue("Prestado", formatMoney(currentLoan.loanAmount))
            AppLabelValue("Ganancia", formatMoney(currentLoan.interestAmount()))
            AppLabelValue("Total", formatMoney(currentLoan.totalAmount()))
            AppLabelValue("Abonado", formatMoney(currentLoan.paidAmount))
            AppLabelValue("Pendiente", formatMoney(currentLoan.pendingAmount()))

            if (currentLoan.exchangeRate.isNotBlank()) {
                AppLabelValue("Tasa", currentLoan.exchangeRate)
            }

            if (currentLoan.conditions.isNotBlank()) {
                HorizontalDivider()
                AppMutedText(currentLoan.conditions)
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

            AppPrimaryButton(
                text = "Preparar cobro",
                onClick = { navController.navigate("loanCollectionNotice") }
            )

            AppSecondaryButton(
                text = "Editar préstamo",
                onClick = {
                    sessionStore.setActiveLoanId(currentLoan.id)
                    navController.navigate("editLoan")
                }
            )

            AppSecondaryButton(
                text = "Marcar como cobrado",
                onClick = {
                    feedback = ""
                    confirmCollected = true
                }
            )

            AppSecondaryButton(
                text = "Marcar como perdido",
                onClick = {
                    feedback = ""
                    confirmLost = true
                }
            )
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

        AppBottomBack(
            onClick = { navController.popBackStack() }
        )
    }
}

private fun detailStatusLabel(loan: ManualLoanData): String {
    return when {
        loan.status == "COBRADO" -> "COBRADO"
        loan.status == "PERDIDO" -> "PERDIDO"
        loan.isOverdue() -> "VENCIDO"
        else -> "ACTIVO"
    }
}
