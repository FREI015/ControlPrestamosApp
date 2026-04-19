package com.controlprestamos.features.loans

import com.controlprestamos.core.validation.*

import com.controlprestamos.core.format.*

import com.controlprestamos.app.*

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate

private const val WEEK_ORDER_DUE = "VENCE"
private const val WEEK_ORDER_PENDING = "PENDIENTE"
private const val WEEK_ORDER_CREATED = "CREADO"

@Composable
fun WeeklyViewScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var orderMode by remember { mutableStateOf(WEEK_ORDER_DUE) }

    val baseDate = parseIsoDateOrNull(selectedDate)
    val weekStart = baseDate?.minusDays(baseDate.dayOfWeek.value.toLong() - 1L)
    val weekEnd = weekStart?.plusDays(6)

    val loans = sessionStore.readLoans()
    val payments = loans.flatMap { sessionStore.readLoanPaymentHistory(it.id) }

    val loansCreatedInWeek = if (weekStart != null && weekEnd != null) {
        loans.filter { loan ->
            val date = parseIsoDateOrNull(loan.loanDate)
            date != null && !date.isBefore(weekStart) && !date.isAfter(weekEnd)
        }
    } else {
        emptyList()
    }

    val dueInWeek = if (weekStart != null && weekEnd != null) {
        loans.filter { loan ->
            val date = parseIsoDateOrNull(loan.dueDate)
            date != null && !date.isBefore(weekStart) && !date.isAfter(weekEnd)
        }
    } else {
        emptyList()
    }

    val paymentsInWeek = if (weekStart != null && weekEnd != null) {
        payments.filter { payment ->
            val date = parseIsoDateOrNull(payment.paymentDate)
            date != null && !date.isBefore(weekStart) && !date.isAfter(weekEnd)
        }
    } else {
        emptyList()
    }

    val orderedDueInWeek = when (orderMode) {
        WEEK_ORDER_PENDING -> dueInWeek.sortedByDescending { it.pendingAmount() }
        WEEK_ORDER_CREATED -> dueInWeek.sortedByDescending { it.createdAt }
        else -> dueInWeek.sortedBy { parseIsoDateOrNull(it.dueDate) ?: LocalDate.MAX }
    }

    val createdCapital = loansCreatedInWeek.sumOf { it.loanAmount }
    val dueExpected = dueInWeek.filter { it.status != "PERDIDO" }.sumOf { it.totalAmount() }
    val duePending = dueInWeek.filter { it.status != "PERDIDO" }.sumOf { it.pendingAmount() }
    val paidInWeek = paymentsInWeek.sumOf { it.amount }

    val weekLabel = if (weekStart != null && weekEnd != null) {
        "$weekStart al $weekEnd"
    } else {
        "Semana no válida"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Vista semanal",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Text(
                text = "Semana de análisis",
                style = MaterialTheme.typography.titleMedium
            )

            AppDateField(
                value = selectedDate,
                label = "Selecciona una fecha de la semana",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = { selectedDate = it }
            )

            AppMutedText("Rango semanal: $weekLabel")
        }

        AppSectionCard {
            Text(
                text = "Resumen semanal",
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
                        WeeklyMetricCard("Préstamos creados", loansCreatedInWeek.size.toString(), Modifier.width(cardWidth))
                        WeeklyMetricCard("Vencimientos", dueInWeek.size.toString(), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeeklyMetricCard("Capital", formatMoney(createdCapital), Modifier.width(cardWidth))
                        WeeklyMetricCard("Total esperado", formatMoney(dueExpected), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeeklyMetricCard("Pagado en semana", formatMoney(paidInWeek), Modifier.width(cardWidth))
                        WeeklyMetricCard("Pendiente semanal", formatMoney(duePending), Modifier.width(cardWidth))
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
                AppFilterChip("Vencimiento", orderMode == WEEK_ORDER_DUE) {
                    orderMode = WEEK_ORDER_DUE
                }
                AppFilterChip("Pendiente", orderMode == WEEK_ORDER_PENDING) {
                    orderMode = WEEK_ORDER_PENDING
                }
                AppFilterChip("Creación", orderMode == WEEK_ORDER_CREATED) {
                    orderMode = WEEK_ORDER_CREATED
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Casos de la semana",
                style = MaterialTheme.typography.titleMedium
            )

            if (orderedDueInWeek.isEmpty()) {
                AppMutedText("No hay préstamos que venzan en esa semana.")
            } else {
                orderedDueInWeek.forEachIndexed { index, loan ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    WeeklyLoanCard(
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
private fun WeeklyMetricCard(
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
private fun WeeklyLoanCard(
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeeklyMetricCard("Total", formatMoney(loan.totalAmount()), Modifier.width(cardWidth))
                WeeklyMetricCard("Pendiente", formatMoney(loan.pendingAmount()), Modifier.width(cardWidth))
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
