package com.controlprestamos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.ui.components.LabeledProgress
import com.controlprestamos.ui.viewmodel.DashboardViewModel
import com.controlprestamos.util.CurrencyUtils

@Composable
fun DashboardScreen(
    paddingValues: PaddingValues,
    onOpenTotalLoaned: () -> Unit = {},
    onOpenProjectedInterest: () -> Unit = {},
    onOpenTotalToCollect: () -> Unit = {},
    onOpenOverduePayments: () -> Unit = {},
    onOpenSarm: () -> Unit = {},
    onOpenTenPercent: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(app.container.loanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = uiState.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                DashboardSummaryText(
                    activeCount = summary.activeCount,
                    overdueCount = summary.overdueCount,
                    carryOverOverdueCount = summary.carryOverOverdueCount,
                    collectedCount = summary.collectedCount
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Módulos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onOpenSarm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SARM")
                        }

                        Button(
                            onClick = onOpenTenPercent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("10%")
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Total prestado",
                        value = CurrencyUtils.usd(summary.totalLoaned),
                        subtitle = "Ver préstamos de la quincena",
                        onClick = onOpenTotalLoaned
                    )

                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Intereses proyectados",
                        value = CurrencyUtils.usd(summary.projectedInterest),
                        subtitle = "Ver ganancia esperada",
                        onClick = onOpenProjectedInterest
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Total a cobrar",
                        value = CurrencyUtils.usd(summary.projectedToCollect),
                        subtitle = "Capital + interés",
                        onClick = onOpenTotalToCollect
                    )

                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Pagos retrasados",
                        value = summary.overdueCount.toString(),
                        subtitle = CurrencyUtils.usd(summary.overdueAmount),
                        onClick = onOpenOverduePayments
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Indicadores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IndicatorItem("Activos", summary.activeCount.toString())
                        IndicatorItem("Cobrados", summary.collectedCount.toString())
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IndicatorItem("Retrasos", summary.overdueCount.toString())
                        IndicatorItem("Archivados", summary.archivedCount.toString())
                    }

                    if (summary.carryOverOverdueCount > 0) {
                        HorizontalDivider()
                        Text(
                            text = "Arrastrados de quincenas anteriores: ${summary.carryOverOverdueCount} • ${CurrencyUtils.usd(summary.carryOverOverdueAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Avance de cobro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    LabeledProgress(
                        label = "Cobrado en la quincena",
                        progress = summary.collectionProgress
                    )

                    Text(
                        text = "Total recuperado en la quincena: ${CurrencyUtils.usd(summary.totalRecovered)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun IndicatorItem(
    title: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DashboardSummaryText(
    activeCount: Int,
    overdueCount: Int,
    carryOverOverdueCount: Int,
    collectedCount: Int
) {
    val text = when {
        overdueCount > 0 && carryOverOverdueCount > 0 ->
            "Resumen: la quincena tiene retrasos activos y además arrastra cobros pendientes de quincenas anteriores."
        overdueCount > 0 ->
            "Resumen: hay pagos retrasados que requieren seguimiento en esta quincena."
        activeCount > 0 ->
            "Resumen: la quincena está en operación con préstamos activos en seguimiento."
        collectedCount > 0 ->
            "Resumen: ya se registran cobros dentro de la quincena actual."
        else ->
            "Resumen: todavía hay poco movimiento operativo registrado en esta quincena."
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
