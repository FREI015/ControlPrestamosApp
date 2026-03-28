package com.controlprestamos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanStatus
import com.controlprestamos.util.CurrencyUtils
import com.controlprestamos.util.DateUtils

@Composable
fun LoanCard(
    loan: Loan,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = loan.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (loan.idNumber.isNotBlank()) {
                        Text(
                            text = "Cédula: ${loan.idNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                when {
                    loan.status == LoanStatus.COLLECTED ->
                        StatusChip("Cobrado", StatusTone.Success)

                    loan.status == LoanStatus.LOST ->
                        StatusChip("Perdido", StatusTone.Danger)

                    loan.isOverdue ->
                        StatusChip("${loan.daysOverdue} día(s) atraso", StatusTone.Warning)

                    else ->
                        StatusChip("Activo", StatusTone.Info)
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LoanMetric(
                    title = "Prestado",
                    value = CurrencyUtils.usd(loan.principalAmount)
                )
                LoanMetric(
                    title = "A cobrar",
                    value = CurrencyUtils.usd(loan.totalToRepay)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LoanMetric(
                    title = "Pagado",
                    value = CurrencyUtils.usd(loan.totalPaid)
                )
                LoanMetric(
                    title = "Pendiente",
                    value = CurrencyUtils.usd(loan.pendingAmount)
                )
            }

            HorizontalDivider()

            Text(
                text = "Préstamo: ${DateUtils.format(loan.loanDate)}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Vencimiento: ${DateUtils.format(loan.dueDate)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (loan.isOverdue) {
                Text(
                    text = "Este préstamo tiene retraso activo y requiere seguimiento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (loan.isBlacklisted) {
                Text(
                    text = "Cliente también está en lista negra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (loan.observations.isNotBlank()) {
                Text(
                    text = "Obs: ${loan.observations}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Toca para ver detalle",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LoanMetric(
    title: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}