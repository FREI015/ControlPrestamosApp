package com.controlprestamos.features.loans

import com.controlprestamos.app.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate

@Composable
fun CollectionAgendaScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var feedback by remember { mutableStateOf("") }

    val targetDate = parseIsoDateOrNull(selectedDate)
    val activeLoans = remember(selectedDate, feedback) {
        sessionStore.readLoans().filter { it.status == "ACTIVO" }
    }

    val dueThatDay = if (targetDate != null) {
        activeLoans.filter { parseIsoDateOrNull(it.dueDate) == targetDate }
            .sortedByDescending { it.pendingAmount() }
    } else {
        emptyList()
    }

    val overdueBeforeThatDay = if (targetDate != null) {
        activeLoans.filter {
            val due = parseIsoDateOrNull(it.dueDate)
            due != null && due.isBefore(targetDate)
        }.sortedByDescending { it.pendingAmount() }
    } else {
        emptyList()
    }

    val pendingForThatDay = dueThatDay.sumOf { it.pendingAmount() }
    val pendingOverdue = overdueBeforeThatDay.sumOf { it.pendingAmount() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Agenda de cobros",
            onBack = { navController.popBackStack() }
        )

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppSectionCard {
            Text(
                text = "Fecha objetivo",
                style = MaterialTheme.typography.titleMedium
            )

            AppDateField(
                value = selectedDate,
                label = "Fecha a revisar *",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = {
                    selectedDate = it
                    feedback = ""
                }
            )

            if (targetDate == null) {
                AppMutedText("Selecciona una fecha válida.")
            } else {
                AppMutedText("Mostrando agenda para: $selectedDate")
            }
        }

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
                        AgendaMetricCard("Vencen ese día", dueThatDay.size.toString(), Modifier.width(cardWidth))
                        AgendaMetricCard("Pendiente del día", formatMoney(pendingForThatDay), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgendaMetricCard("Vencidos previos", overdueBeforeThatDay.size.toString(), Modifier.width(cardWidth))
                        AgendaMetricCard("Pendiente vencido", formatMoney(pendingOverdue), Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Cobros del día",
                style = MaterialTheme.typography.titleMedium
            )

            if (dueThatDay.isEmpty()) {
                AppMutedText("No hay préstamos que venzan en esa fecha.")
            } else {
                dueThatDay.forEachIndexed { index, loan ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    AgendaLoanCard(
                        loan = loan,
                        tag = "Vence ese día",
                        navController = navController,
                        sessionStore = sessionStore
                    )
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Vencidos previos",
                style = MaterialTheme.typography.titleMedium
            )

            if (overdueBeforeThatDay.isEmpty()) {
                AppMutedText("No hay préstamos vencidos antes de esa fecha.")
            } else {
                overdueBeforeThatDay.take(20).forEachIndexed { index, loan ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    AgendaLoanCard(
                        loan = loan,
                        tag = "Vencido",
                        navController = navController,
                        sessionStore = sessionStore
                    )
                }

                if (overdueBeforeThatDay.size > 20) {
                    AppMutedText("Se muestran los primeros 20 registros vencidos.")
                }
            }
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun AgendaMetricCard(
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
private fun AgendaLoanCard(
    loan: ManualLoanData,
    tag: String,
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
                AppMutedText("Vencimiento: ${loan.dueDate.ifBlank { "-" }}")
            }

            AppStatusChip(tag)
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val cardWidth = (maxWidth - 12.dp) / 2

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AgendaMetricCard("Pendiente", formatMoney(loan.pendingAmount()), Modifier.width(cardWidth))
                AgendaMetricCard("Total", formatMoney(loan.totalAmount()), Modifier.width(cardWidth))
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
