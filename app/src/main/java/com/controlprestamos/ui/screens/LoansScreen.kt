package com.controlprestamos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.ui.components.EmptyState
import com.controlprestamos.ui.components.FilterChipItem
import com.controlprestamos.ui.components.LoanCard
import com.controlprestamos.ui.viewmodel.LoanFilter
import com.controlprestamos.ui.viewmodel.LoansViewModel

@Composable
fun LoansScreen(
    paddingValues: PaddingValues,
    onAddLoan: () -> Unit,
    onOpenLoan: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val viewModel: LoansViewModel = viewModel(
        factory = LoansViewModel.factory(app.container.loanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Préstamos",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Consulta, filtra y abre el detalle de cada operación.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.search,
                    onValueChange = viewModel::updateSearch,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar por nombre") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Buscar"
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Filtros",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LoanFilter.entries.forEach { filter ->
                            FilterChipItem(
                                label = when (filter) {
                                    LoanFilter.ALL -> "Todos"
                                    LoanFilter.ACTIVE -> "Activos"
                                    LoanFilter.OVERDUE -> "Vencidos"
                                    LoanFilter.COLLECTED -> "Cobrados"
                                    LoanFilter.LOST -> "Perdidos"
                                    LoanFilter.BLACKLISTED -> "Lista negra"
                                },
                                selected = uiState.filter == filter,
                                onClick = { viewModel.updateFilter(filter) }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Rango de fechas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.fromDate,
                                onValueChange = viewModel::updateFromDate,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Desde") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = "Desde"
                                    )
                                }
                            )

                            OutlinedTextField(
                                value = uiState.toDate,
                                onValueChange = viewModel::updateToDate,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Hasta") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = "Hasta"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.loans.isEmpty()) {
                item {
                    EmptyState("No hay préstamos para el filtro seleccionado.")
                }
            } else {
                items(uiState.loans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        onClick = { onOpenLoan(loan.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddLoan,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp)
                .offset(y = (-6).dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Agregar préstamo"
            )
        }
    }
}
