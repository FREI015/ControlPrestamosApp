package com.controlprestamos.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val loans = sessionStore.readLoans()

    val today = LocalDate.now()
    val activeLoans = loans.filter { it.status == "ACTIVO" && !it.isOverdue() }
    val overdueLoans = loans.filter { it.status == "ACTIVO" && it.isOverdue() }
    val collectedLoans = loans.filter { it.status == "COBRADO" }
    val lostLoans = loans.filter { it.status == "PERDIDO" }

    val capitalLoaned = loans.filter { it.status != "PERDIDO" }.sumOf { it.loanAmount }
    val projectedProfit = loans.filter { it.status != "PERDIDO" }.sumOf { it.interestAmount() }
    val totalToCollect = loans.filter { it.status != "PERDIDO" }.sumOf { it.totalAmount() }
    val totalPaid = loans.sumOf { it.paidAmount }
    val totalPending = loans.filter { it.status != "PERDIDO" }.sumOf { it.pendingAmount() }
    val overdueAmount = overdueLoans.sumOf { it.pendingAmount() }

    val progress = if (totalToCollect <= 0.0) {
        0.0
    } else {
        (totalPaid / totalToCollect).coerceIn(0.0, 1.0)
    }

    val upcomingLoans = loans
        .filter { it.status == "ACTIVO" }
        .mapNotNull { loan ->
            val due = parseDashboardDate(loan.dueDate) ?: return@mapNotNull null
            loan to due
        }
        .filter { (_, due) -> !due.isBefore(today) }
        .sortedBy { (_, due) -> due }
        .take(5)

    val topPendingClients = loans
        .filter { it.status != "PERDIDO" && it.pendingAmount() > 0.0 }
        .sortedByDescending { it.pendingAmount() }
        .take(5)

    val todayMetrics = loans.filter { parseDashboardDate(it.loanDate) == today }
    val weekMetrics = loans.filter { matchesThisWeek(parseDashboardDate(it.loanDate), today) }
    val fortnightMetrics = loans.filter { matchesThisFortnight(parseDashboardDate(it.loanDate), today) }
    val monthMetrics = loans.filter { matchesThisMonth(parseDashboardDate(it.loanDate), today) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTopBack(title = "Resumen de operaciones")
        }

        item {
            AppSectionCard {
                Text(currentPeriodText(), color = MaterialTheme.colorScheme.primary)
                AppMutedText(
                    if (loans.isEmpty()) {
                        "Todavía no hay préstamos cargados."
                    } else {
                        "Resumen operativo calculado con tus datos reales."
                    }
                )
            }
        }

        item {
            AppSectionCard {
                Text("Accesos rápidos", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Nuevo préstamo", false) {
                        navController.navigate("newLoan")
                    }

                    AppFilterChip("Control", false) {
                        navController.navigate("loans")
                    }

                    AppFilterChip("Historial", false) {
                        navController.navigate("history")
                    }

                    AppFilterChip("Lista negra", false) {
                        navController.navigate("blacklist")
                    }

                    AppFilterChip("Preparar cobro", false) {
                        val preferredLoan = overdueLoans.firstOrNull() ?: activeLoans.firstOrNull()
                        if (preferredLoan != null) {
                            sessionStore.setActiveLoanId(preferredLoan.id)
                            navController.navigate("loanCollectionNotice")
                        } else {
                            navController.navigate("loans")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppMetricCard(
                    title = "Capital prestado",
                    value = formatMoney(capitalLoaned),
                    subtitle = "Sin perdidos",
                    modifier = Modifier.weight(1f)
                )
                AppMetricCard(
                    title = "Ganancia proyectada",
                    value = formatMoney(projectedProfit),
                    subtitle = "Interés estimado",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppMetricCard(
                    title = "Total a cobrar",
                    value = formatMoney(totalToCollect),
                    subtitle = "Capital + ganancia",
                    modifier = Modifier.weight(1f)
                )
                AppMetricCard(
                    title = "Pagado",
                    value = formatMoney(totalPaid),
                    subtitle = "Recuperado",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppMetricCard(
                    title = "Pendiente",
                    value = formatMoney(totalPending),
                    subtitle = "Saldo actual",
                    modifier = Modifier.weight(1f)
                )
                AppMetricCard(
                    title = "Monto vencido",
                    value = formatMoney(overdueAmount),
                    subtitle = "Solo atrasados",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            AppSectionCard {
                Text("Estados", style = MaterialTheme.typography.titleMedium)

                AppMutedText("Activos: ${activeLoans.size}")
                AppMutedText("Vencidos: ${overdueLoans.size}")
                AppMutedText("Cobrados: ${collectedLoans.size}")
                AppMutedText("Perdidos: ${lostLoans.size}")

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Avance de cobro")
                    Text("${(progress * 100).toInt()}%")
                }

                AppMutedText("Recuperado: ${formatMoney(totalPaid)} de ${formatMoney(totalToCollect)}")
            }
        }

        item {
            AppSectionCard {
                Text("Próximos vencimientos", style = MaterialTheme.typography.titleMedium)

                if (upcomingLoans.isEmpty()) {
                    AppMutedText("No hay vencimientos próximos.")
                } else {
                    upcomingLoans.forEachIndexed { index, pair ->
                        val loan = pair.first
                        val due = pair.second

                        if (index > 0) {
                            HorizontalDivider()
                        }

                        Text(loan.fullName.ifBlank { "Sin nombre" })
                        AppMutedText("Vence: $due")
                        AppMutedText("Pendiente: ${formatMoney(loan.pendingAmount())}")

                        AppSecondaryButton(
                            text = "Abrir préstamo",
                            onClick = {
                                sessionStore.setActiveLoanId(loan.id)
                                navController.navigate("loanDetail")
                            }
                        )
                    }
                }
            }
        }

        item {
            AppSectionCard {
                Text("Clientes con mayor saldo pendiente", style = MaterialTheme.typography.titleMedium)

                if (topPendingClients.isEmpty()) {
                    AppMutedText("No hay saldos pendientes actualmente.")
                } else {
                    topPendingClients.forEachIndexed { index, loan ->
                        if (index > 0) {
                            HorizontalDivider()
                        }

                        Text(loan.fullName.ifBlank { "Sin nombre" })
                        if (loan.phone.isNotBlank()) {
                            AppMutedText("Teléfono: ${loan.phone}")
                        }
                        AppMutedText("Pendiente: ${formatMoney(loan.pendingAmount())}")

                        AppSecondaryButton(
                            text = "Ver detalle",
                            onClick = {
                                sessionStore.setActiveLoanId(loan.id)
                                navController.navigate("loanDetail")
                            }
                        )
                    }
                }
            }
        }

        item {
            AppSectionCard {
                Text("Resumen por período", style = MaterialTheme.typography.titleMedium)

                DashboardPeriodBlock(
                    title = "Hoy",
                    loans = todayMetrics
                )

                HorizontalDivider()

                DashboardPeriodBlock(
                    title = "Semana",
                    loans = weekMetrics
                )

                HorizontalDivider()

                DashboardPeriodBlock(
                    title = "Quincena",
                    loans = fortnightMetrics
                )

                HorizontalDivider()

                DashboardPeriodBlock(
                    title = "Mes",
                    loans = monthMetrics
                )
            }
        }
    }
}

@Composable
private fun DashboardPeriodBlock(
    title: String,
    loans: List<ManualLoanData>
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    AppMutedText("Cantidad: ${loans.size}")
    AppMutedText("Prestado: ${formatMoney(loans.sumOf { it.loanAmount })}")
    AppMutedText("Ganancia proyectada: ${formatMoney(loans.sumOf { it.interestAmount() })}")
    AppMutedText("Total: ${formatMoney(loans.sumOf { it.totalAmount() })}")
}

private fun currentPeriodText(): String {
    val now = LocalDate.now()
    return if (now.dayOfMonth <= 15) {
        "Quincena actual: ${now.withDayOfMonth(1)} - ${now.withDayOfMonth(15)}"
    } else {
        "Quincena actual: ${now.withDayOfMonth(16)} - ${now.withDayOfMonth(now.lengthOfMonth())}"
    }
}

private fun parseDashboardDate(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun matchesThisWeek(date: LocalDate?, today: LocalDate): Boolean {
    if (date == null) return false
    val start = today.minusDays(today.dayOfWeek.value.toLong() - 1L)
    val end = start.plusDays(6)
    return !date.isBefore(start) && !date.isAfter(end)
}

private fun matchesThisFortnight(date: LocalDate?, today: LocalDate): Boolean {
    if (date == null) return false
    val start = if (today.dayOfMonth <= 15) {
        today.withDayOfMonth(1)
    } else {
        today.withDayOfMonth(16)
    }

    val end = if (today.dayOfMonth <= 15) {
        today.withDayOfMonth(15)
    } else {
        today.withDayOfMonth(today.lengthOfMonth())
    }

    return !date.isBefore(start) && !date.isAfter(end)
}

private fun matchesThisMonth(date: LocalDate?, today: LocalDate): Boolean {
    if (date == null) return false
    return date.year == today.year && date.month == today.month
}

private fun formatMoney(value: Double): String {
    return "$" + String.format(Locale.US, "%.2f", value)
}


