package com.controlprestamos.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun DailySummaryScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val today = LocalDate.now()

    val loans = sessionStore.readLoans()
    val activeLoans = loans.filter { it.status == "ACTIVO" }
    val allPayments = loans
        .flatMap { sessionStore.readLoanPaymentHistory(it.id) }
        .sortedByDescending { it.createdAt }

    val paymentsToday = allPayments.filter { parseDailyDateSafe(it.paymentDate) == today }
    val recoveredToday = paymentsToday.sumOf { it.amount }

    val dueTodayLoans = activeLoans.filter { parseDailyDateSafe(it.dueDate) == today }
    val overdueLoans = activeLoans.filter {
        val due = parseDailyDateSafe(it.dueDate)
        due != null && due.isBefore(today)
    }

    val actionLoans = (overdueLoans.sortedByDescending { it.pendingAmount() } + dueTodayLoans.sortedByDescending { it.pendingAmount() })
        .distinctBy { it.id }
        .take(10)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Resumen del día",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Text(
                text = "Resumen",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DailyMetricCard("Cobrado hoy", formatMoney(recoveredToday), Modifier.width(cardWidth))
                        DailyMetricCard("Pagos hoy", paymentsToday.size.toString(), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DailyMetricCard("Vencen hoy", dueTodayLoans.size.toString(), Modifier.width(cardWidth))
                        DailyMetricCard("Vencidos", overdueLoans.size.toString(), Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Cobranza prioritaria",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Se muestran vencidos y préstamos que vencen hoy.")

            if (actionLoans.isEmpty()) {
                AppMutedText("No hay cobros prioritarios para hoy.")
            } else {
                actionLoans.forEachIndexed { index, loan ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    DailyLoanCard(
                        loan = loan,
                        onPrepare = {
                            sessionStore.setActiveLoanId(loan.id)
                            navController.navigate(AppRoutes.LoanCollectionNotice) {
                                launchSingleTop = true
                            }
                        },
                        onDetail = {
                            sessionStore.setActiveLoanId(loan.id)
                            navController.navigate(AppRoutes.LoanDetail) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Pagos registrados hoy",
                style = MaterialTheme.typography.titleMedium
            )

            if (paymentsToday.isEmpty()) {
                AppMutedText("Todavía no hay pagos registrados hoy.")
            } else {
                paymentsToday.take(12).forEachIndexed { index, payment ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    Text(
                        text = payment.clientName.ifBlank { "Pago registrado" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    AppMutedText("Monto: ${formatMoney(payment.amount)}")
                    AppMutedText("Fecha: ${payment.paymentDate}")
                    AppMutedText("Tipo: ${payment.paymentType}")
                }
            }
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun DailyMetricCard(
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
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun DailyLoanCard(
    loan: ManualLoanData,
    onPrepare: () -> Unit,
    onDetail: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = loan.fullName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleSmall
                )
                if (loan.phone.isNotBlank()) {
                    AppMutedText("Teléfono: ${loan.phone}")
                }
                AppMutedText("Vence: ${loan.dueDate.ifBlank { "-" }}")
            }

            AppStatusChip(
                if (loan.isOverdue()) "Vencido" else "Activo"
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val cardWidth = (maxWidth - 12.dp) / 2

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DailyMetricCard("Pendiente", formatMoney(loan.pendingAmount()), Modifier.width(cardWidth))
                DailyMetricCard("Total", formatMoney(loan.totalAmount()), Modifier.width(cardWidth))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DailyActionCard(
                text = "Cobro",
                modifier = Modifier.weightlessHalf(),
                onClick = onPrepare
            )
            DailyActionCard(
                text = "Detalle",
                modifier = Modifier.weightlessHalf(),
                onClick = onDetail
            )
        }
    }
}

@Composable
private fun DailyActionCard(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.pill
            )
            .clickableSafe(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun Modifier.weightlessHalf(): Modifier = this.fillMaxWidth()

private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier {
    return this.then(androidx.compose.ui.Modifier.clickable(onClick = onClick))
}

private fun parseDailyDateSafe(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

