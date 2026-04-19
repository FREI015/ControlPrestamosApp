package com.controlprestamos.app

import com.controlprestamos.core.validation.*

import com.controlprestamos.core.format.*

import com.controlprestamos.features.loans.*

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate

@Composable
fun ReportsScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    var fromDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var toDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var feedback by remember { mutableStateOf("") }

    val allLoans = remember(fromDate, toDate) { sessionStore.readLoans() }
    val allPayments = remember(fromDate, toDate) {
        sessionStore.readLoans().flatMap { sessionStore.readLoanPaymentHistory(it.id) }
    }

    val from = parseIsoDateOrNull(fromDate)
    val to = parseIsoDateOrNull(toDate)

    val loansInRange = if (from != null && to != null && !to.isBefore(from)) {
        allLoans.filter { loan ->
            val date = parseIsoDateOrNull(loan.loanDate)
            date != null && !date.isBefore(from) && !date.isAfter(to)
        }
    } else {
        emptyList()
    }

    val paymentsInRange = if (from != null && to != null && !to.isBefore(from)) {
        allPayments.filter { payment ->
            val date = parseIsoDateOrNull(payment.paymentDate)
            date != null && !date.isBefore(from) && !date.isAfter(to)
        }
    } else {
        emptyList()
    }

    val dueInRange = if (from != null && to != null && !to.isBefore(from)) {
        allLoans.filter { loan ->
            val date = parseIsoDateOrNull(loan.dueDate)
            date != null && !date.isBefore(from) && !date.isAfter(to)
        }
    } else {
        emptyList()
    }

    val activePortfolioInRange = loansInRange.filter { it.status != "PERDIDO" }

    val capitalLoaned = loansInRange.sumOf { it.loanAmount }
    val totalExpected = loansInRange.filter { it.status != "PERDIDO" }.sumOf { it.totalAmount() }
    val totalInterest = loansInRange.filter { it.status != "PERDIDO" }.sumOf { it.interestAmount() }
    val totalPaidInRange = paymentsInRange.sumOf { it.amount }
    val pendingCurrent = activePortfolioInRange.sumOf { it.pendingAmount() }
    val overdueCurrent = activePortfolioInRange.filter { it.isOverdue() }.sumOf { it.pendingAmount() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Reportes",
            onBack = { navController.popBackStack() }
        )

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppSectionCard {
            Text(
                text = "Rango de análisis",
                style = MaterialTheme.typography.titleMedium
            )

            AppDateField(
                value = fromDate,
                label = "Desde *",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = {
                    fromDate = it
                    feedback = ""
                }
            )

            AppDateField(
                value = toDate,
                label = "Hasta *",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = {
                    toDate = it
                    feedback = ""
                }
            )

            if (from == null || to == null) {
                AppMutedText("Debes usar fechas válidas.")
            } else if (to.isBefore(from)) {
                AppMutedText("La fecha final no puede ser menor que la inicial.")
            } else {
                AppMutedText("Rango listo para análisis.")
            }
        }

        AppSectionCard {
            Text(
                text = "Cartera en el rango",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportMetricCard("Préstamos", loansInRange.size.toString(), Modifier.width(cardWidth))
                        ReportMetricCard("Capital", formatMoney(capitalLoaned), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportMetricCard("Ganancia", formatMoney(totalInterest), Modifier.width(cardWidth))
                        ReportMetricCard("Total esperado", formatMoney(totalExpected), Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Cobranza y vencimientos",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportMetricCard("Pagos en rango", formatMoney(totalPaidInRange), Modifier.width(cardWidth))
                        ReportMetricCard("Vencen en rango", dueInRange.size.toString(), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportMetricCard("Pendiente actual", formatMoney(pendingCurrent), Modifier.width(cardWidth))
                        ReportMetricCard("Mora actual", formatMoney(overdueCurrent), Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Lectura rápida",
                style = MaterialTheme.typography.titleMedium
            )

            AppMutedText("Este módulo te permite revisar la operación por período sin alterar tus registros.")
            AppMutedText("Úsalo para cierres, seguimiento semanal o revisión mensual.")
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun ReportMetricCard(
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
