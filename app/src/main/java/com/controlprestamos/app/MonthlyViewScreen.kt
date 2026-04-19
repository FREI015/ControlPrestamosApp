package com.controlprestamos.app

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate

private const val MONTH_ORDER_CREATED = "CREADOS"
private const val MONTH_ORDER_DUE = "VENCEN"
private const val MONTH_ORDER_PENDING = "PENDIENTE"

@Composable
fun MonthlyViewScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var orderMode by remember { mutableStateOf(MONTH_ORDER_DUE) }

    val monthDate = parseIsoDateOrNull(selectedDate)
    val loans = sessionStore.readLoans()
    val allPayments = loans.flatMap { sessionStore.readLoanPaymentHistory(it.id) }

    val loansCreatedInMonth = if (monthDate != null) {
        loans.filter { loan ->
            val date = parseIsoDateOrNull(loan.loanDate)
            date != null && date.year == monthDate.year && date.month == monthDate.month
        }
    } else {
        emptyList()
    }

    val dueInMonth = if (monthDate != null) {
        loans.filter { loan ->
            val date = parseIsoDateOrNull(loan.dueDate)
            date != null && date.year == monthDate.year && date.month == monthDate.month
        }
    } else {
        emptyList()
    }

    val paymentsInMonth = if (monthDate != null) {
        allPayments.filter { payment ->
            val date = parseIsoDateOrNull(payment.paymentDate)
            date != null && date.year == monthDate.year && date.month == monthDate.month
        }
    } else {
        emptyList()
    }

    val orderedDueInMonth = when (orderMode) {
        MONTH_ORDER_CREATED -> dueInMonth.sortedByDescending { it.createdAt }
        MONTH_ORDER_PENDING -> dueInMonth.sortedByDescending { it.pendingAmount() }
        else -> dueInMonth.sortedBy { parseIsoDateOrNull(it.dueDate) ?: LocalDate.MAX }
    }

    val createdCapital = loansCreatedInMonth.sumOf { it.loanAmount }
    val dueExpected = dueInMonth.filter { it.status != "PERDIDO" }.sumOf { it.totalAmount() }
    val duePending = dueInMonth.filter { it.status != "PERDIDO" }.sumOf { it.pendingAmount() }
    val paidInMonth = paymentsInMonth.sumOf { it.amount }

    val monthLabel = if (monthDate != null) {
        "${monthDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthDate.year}"
    } else {
        "Mes no válido"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Vista mensual",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Text(
                text = "Mes de análisis",
                style = MaterialTheme.typography.titleMedium
            )

            AppDateField(
                value = selectedDate,
                label = "Selecciona una fecha del mes",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = { selectedDate = it }
            )

            AppMutedText("Mes actual: $monthLabel")
        }

        AppSectionCard {
            Text(
                text = "Resumen mensual",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonthlyMetricCard("Préstamos creados", loansCreatedInMonth.size.toString(), Modifier.width(cardWidth))
                        MonthlyMetricCard("Vencimientos", dueInMonth.size.toString(), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonthlyMetricCard("Capital", formatMoney(createdCapital), Modifier.width(cardWidth))
                        MonthlyMetricCard("Total esperado", formatMoney(dueExpected), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonthlyMetricCard("Pagado en mes", formatMoney(paidInMonth), Modifier.width(cardWidth))
                        MonthlyMetricCard("Pendiente del mes", formatMoney(duePending), Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Orden",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppFilterChip("Vencimiento", orderMode == MONTH_ORDER_DUE) {
                    orderMode = MONTH_ORDER_DUE
                }
                AppFilterChip("Creación", orderMode == MONTH_ORDER_CREATED) {
                    orderMode = MONTH_ORDER_CREATED
                }
                AppFilterChip("Pendiente", orderMode == MONTH_ORDER_PENDING) {
                    orderMode = MONTH_ORDER_PENDING
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Casos del mes",
                style = MaterialTheme.typography.titleMedium
            )

            if (orderedDueInMonth.isEmpty()) {
                AppMutedText("No hay préstamos que venzan en ese mes.")
            } else {
                orderedDueInMonth.forEachIndexed { index, loan ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    MonthlyLoanCard(
                        loan = loan,
                        navController = navController,
                        sessionStore = sessionStore
                    )
                }
            }
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun MonthlyMetricCard(
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
private fun MonthlyLoanCard(
    loan: ManualLoanData,
    navController: NavController,
    sessionStore: SessionStore
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
                AppMutedText("Préstamo: ${loan.loanDate.ifBlank { "-" }}")
                AppMutedText("Vence: ${loan.dueDate.ifBlank { "-" }}")
            }

            AppStatusChip(
                when {
                    loan.status == "COBRADO" -> "Cobrado"
                    loan.status == "PERDIDO" -> "Perdido"
                    loan.isOverdue() -> "Vencido"
                    else -> "Activo"
                }
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val cardWidth = (maxWidth - 12.dp) / 2

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonthlyMetricCard("Total", formatMoney(loan.totalAmount()), Modifier.width(cardWidth))
                    MonthlyMetricCard("Pendiente", formatMoney(loan.pendingAmount()), Modifier.width(cardWidth))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppSecondaryButton(
                text = "Cobro",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    sessionStore.setActiveLoanId(loan.id)
                    navController.navigate(AppRoutes.LoanCollectionNotice) {
                        launchSingleTop = true
                    }
                }
            )

            AppSecondaryButton(
                text = "Detalle",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    sessionStore.setActiveLoanId(loan.id)
                    navController.navigate(AppRoutes.LoanDetail) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
