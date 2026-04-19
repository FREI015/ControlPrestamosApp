package com.controlprestamos.app

import com.controlprestamos.core.format.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

private data class DashboardMainMetric(
    val title: String,
    val value: String,
    val subtitle: String
)

private data class DashboardHealthMetric(
    val title: String,
    val value: String,
    val subtitle: String
)

private data class DashboardPeriodSummary(
    val label: String,
    val operations: Int,
    val loaned: Double,
    val projected: Double,
    val total: Double
)

@Composable
fun DashboardScreen(
    sessionStore: SessionStore
) {
    val loans = sessionStore.readLoans()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val currentFortnightStart = fortnightStartSafe(today)
    val currentFortnightEnd = fortnightEndSafe(today)
    val previousFortnightEnd = currentFortnightStart.minusDays(1)
    val previousFortnightStart = fortnightStartSafe(previousFortnightEnd)

    val activeLoans = loans.filter { it.status == "ACTIVO" && !it.isOverdue() }
    val overdueLoans = loans.filter { it.status == "ACTIVO" && it.isOverdue() }
    val collectedLoans = loans.filter { it.status == "COBRADO" }
    val lostLoans = loans.filter { it.status == "PERDIDO" }
    val nonLostLoans = loans.filter { it.status != "PERDIDO" }

    val capitalLoaned = nonLostLoans.sumOf { it.loanAmount }
    val projectedProfit = nonLostLoans.sumOf { it.interestAmount() }
    val totalToCollect = nonLostLoans.sumOf { it.totalAmount() }
    val totalPaid = loans.sumOf { it.paidAmount }
    val totalPending = nonLostLoans.sumOf { it.pendingAmount() }
    val overduePending = overdueLoans.sumOf { it.pendingAmount() }

    val recoveryPercent = if (totalToCollect > 0.0) (totalPaid / totalToCollect) * 100.0 else 0.0
    val averageTicket = if (nonLostLoans.isNotEmpty()) capitalLoaned / nonLostLoans.size else 0.0

    val dueTodayCount = activeLoans.count { parseDashboardDateSafe(it.dueDate) == today }
    val dueTomorrowCount = activeLoans.count { parseDashboardDateSafe(it.dueDate) == tomorrow }

    val recoveredPreviousFortnight = loans.sumOf { loan ->
        sessionStore.readLoanPaymentHistory(loan.id)
            .filter { record ->
                val date = parseDashboardDateSafe(record.paymentDate)
                date != null && !date.isBefore(previousFortnightStart) && !date.isAfter(previousFortnightEnd)
            }
            .sumOf { it.amount }
    }

    val investedCurrentFortnight = loans
        .filter {
            val loanDate = parseDashboardDateSafe(it.loanDate)
            loanDate != null && !loanDate.isBefore(currentFortnightStart) && !loanDate.isAfter(currentFortnightEnd)
        }
        .sumOf { it.loanAmount }

    val reinvestedFortnightAmount = minOf(recoveredPreviousFortnight, investedCurrentFortnight)
    val reinvestedFortnightPercent = if (recoveredPreviousFortnight > 0.0) {
        (reinvestedFortnightAmount / recoveredPreviousFortnight) * 100.0
    } else {
        0.0
    }

    val todayLoans = loans.filter { parseDashboardDateSafe(it.loanDate) == today }
    val weekLoans = loans.filter { matchesThisWeekSafe(parseDashboardDateSafe(it.loanDate), today) }
    val fortnightLoans = loans.filter { matchesThisFortnightSafe(parseDashboardDateSafe(it.loanDate), today) }
    val monthLoans = loans.filter { matchesThisMonthSafe(parseDashboardDateSafe(it.loanDate), today) }

    val mainMetrics = listOf(
        DashboardMainMetric("Prestado", formatMoney(capitalLoaned), "Capital activo"),
        DashboardMainMetric("A cobrar", formatMoney(totalToCollect), "Total esperado"),
        DashboardMainMetric("Pagado", formatMoney(totalPaid), "Monto abonado"),
        DashboardMainMetric("Pendiente", formatMoney(totalPending), "Saldo actual")
    )

    val healthMetrics = listOf(
        DashboardHealthMetric(
            "Recuperación",
            formatPercentCompact(recoveryPercent),
            "Efectividad actual"
        ),
        DashboardHealthMetric(
            "Reinvertido",
            formatMoney(reinvestedFortnightAmount),
            "${formatPercentCompact(reinvestedFortnightPercent)} de la quincena anterior"
        ),
        DashboardHealthMetric("En mora", formatMoney(overduePending), "Saldo vencido"),
        DashboardHealthMetric("Vencen hoy", dueTodayCount.toString(), "Cobros del día"),
        DashboardHealthMetric("Vencen mañana", dueTomorrowCount.toString(), "Cobros próximos"),
        DashboardHealthMetric("Ticket promedio", formatMoney(averageTicket), "Promedio por préstamo"),
        DashboardHealthMetric("Ganancia", formatMoney(projectedProfit), "Proyección")
    )

    val stateMetrics = listOf(
        DashboardHealthMetric("Activos", activeLoans.size.toString(), "Operando"),
        DashboardHealthMetric("Vencidos", overdueLoans.size.toString(), "Con atraso"),
        DashboardHealthMetric("Cobrados", collectedLoans.size.toString(), "Cerrados"),
        DashboardHealthMetric("Perdidos", lostLoans.size.toString(), "Marcados")
    )

    val periodOptions = listOf(
        DashboardPeriodSummary(
            label = "Hoy",
            operations = todayLoans.size,
            loaned = todayLoans.sumOf { it.loanAmount },
            projected = todayLoans.sumOf { it.interestAmount() },
            total = todayLoans.sumOf { it.totalAmount() }
        ),
        DashboardPeriodSummary(
            label = "Semana",
            operations = weekLoans.size,
            loaned = weekLoans.sumOf { it.loanAmount },
            projected = weekLoans.sumOf { it.interestAmount() },
            total = weekLoans.sumOf { it.totalAmount() }
        ),
        DashboardPeriodSummary(
            label = "Quincena",
            operations = fortnightLoans.size,
            loaned = fortnightLoans.sumOf { it.loanAmount },
            projected = fortnightLoans.sumOf { it.interestAmount() },
            total = fortnightLoans.sumOf { it.totalAmount() }
        ),
        DashboardPeriodSummary(
            label = "Mes",
            operations = monthLoans.size,
            loaned = monthLoans.sumOf { it.loanAmount },
            projected = monthLoans.sumOf { it.interestAmount() },
            total = monthLoans.sumOf { it.totalAmount() }
        )
    )

    var selectedPeriod by remember { mutableStateOf(periodOptions.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppScreenTitle("Creditime")
                AppMutedText("Resumen operativo")
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = "Resumen principal",
                    style = MaterialTheme.typography.titleMedium
                )

                DashboardMetricGrid(
                    metrics = mainMetrics.map {
                        Triple(it.title, it.value, it.subtitle)
                    }
                )
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = "Salud de la cartera",
                    style = MaterialTheme.typography.titleMedium
                )

                DashboardMetricGrid(
                    metrics = healthMetrics.map {
                        Triple(it.title, it.value, it.subtitle)
                    }
                )
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = "Estados",
                    style = MaterialTheme.typography.titleMedium
                )

                DashboardMetricGrid(
                    metrics = stateMetrics.map {
                        Triple(it.title, it.value, it.subtitle)
                    }
                )
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = "Resumen por período",
                    style = MaterialTheme.typography.titleMedium
                )
                AppMutedText("Selecciona el tramo de tiempo que quieres revisar.")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    periodOptions.forEach { option ->
                        AppFilterChip(
                            label = option.label,
                            selected = selectedPeriod.label == option.label,
                            onClick = { selectedPeriod = option }
                        )
                    }
                }

                DashboardMetricGrid(
                    metrics = listOf(
                        Triple("Operaciones", selectedPeriod.operations.toString(), "Cantidad"),
                        Triple("Prestado", formatMoney(selectedPeriod.loaned), "Capital"),
                        Triple("Ganancia", formatMoney(selectedPeriod.projected), "Proyección"),
                        Triple("Total", formatMoney(selectedPeriod.total), "Esperado")
                    )
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricGrid(
    metrics: List<Triple<String, String, String>>
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val cardWidth = (maxWidth - 12.dp) / 2

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.chunked(2).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        AppMetricCard(
                            title = item.first,
                            value = item.second,
                            subtitle = item.third,
                            modifier = Modifier.width(cardWidth)
                        )
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

private fun parseDashboardDateSafe(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun matchesThisWeekSafe(date: LocalDate?, today: LocalDate): Boolean {
    if (date == null) return false
    val start = today.minusDays(today.dayOfWeek.value.toLong() - 1L)
    val end = start.plusDays(6)
    return !date.isBefore(start) && !date.isAfter(end)
}

private fun matchesThisFortnightSafe(date: LocalDate?, today: LocalDate): Boolean {
    if (date == null) return false
    val start = if (today.dayOfMonth <= 15) today.withDayOfMonth(1) else today.withDayOfMonth(16)
    val end = if (today.dayOfMonth <= 15) today.withDayOfMonth(15) else today.withDayOfMonth(today.lengthOfMonth())
    return !date.isBefore(start) && !date.isAfter(end)
}

private fun matchesThisMonthSafe(date: LocalDate?, today: LocalDate): Boolean {
    if (date == null) return false
    return date.year == today.year && date.month == today.month
}

private fun fortnightStartSafe(date: LocalDate): LocalDate {
    return if (date.dayOfMonth <= 15) date.withDayOfMonth(1) else date.withDayOfMonth(16)
}

private fun fortnightEndSafe(date: LocalDate): LocalDate {
    return if (date.dayOfMonth <= 15) date.withDayOfMonth(15) else date.withDayOfMonth(date.lengthOfMonth())
}

private fun formatPercentCompact(value: Double): String {
    val safe = if (value.isFinite()) value else 0.0
    return String.format(Locale.US, "%.1f%%", safe)
}
