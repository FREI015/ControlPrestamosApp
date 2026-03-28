package com.controlprestamos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.data.TenPercentPreferences
import com.controlprestamos.ui.viewmodel.TenPercentViewModel
import com.controlprestamos.util.CurrencyUtils

@Composable
fun TenPercentScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: TenPercentViewModel = viewModel(
        factory = TenPercentViewModel.factory(TenPercentPreferences(context))
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Módulo 10%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.date,
                        onValueChange = viewModel::updateDate,
                        label = { Text("Fecha (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.clientName,
                        onValueChange = viewModel::updateClientName,
                        label = { Text("Cliente referido") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.referredTo,
                        onValueChange = viewModel::updateReferredTo,
                        label = { Text("Prestamista / compañero") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.loanAmountUsd,
                        onValueChange = viewModel::updateLoanAmountUsd,
                        label = { Text("Monto del préstamo USD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.percent,
                        onValueChange = viewModel::updatePercent,
                        label = { Text("% de comisión") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.status,
                        onValueChange = viewModel::updateStatus,
                        label = { Text("Estado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar registro 10%")
                    }
                }
            }
        }

        item {
            Text("Registros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        if (uiState.entries.isEmpty()) {
            item { Text("No hay registros 10% todavía.") }
        } else {
            items(uiState.entries, key = { it.id }) { entry ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Fecha: ${entry.date}")
                        if (entry.referredTo.isNotBlank()) Text("Prestamista: ${entry.referredTo}")
                        Text("Monto: ${CurrencyUtils.usd(entry.loanAmountUsd)}")
                        Text("Comisión ${String.format("%.2f", entry.percent)}%: ${CurrencyUtils.usd(entry.commissionUsd)}")
                        Text("Estado: ${entry.status}")
                        if (entry.notes.isNotBlank()) Text("Notas: ${entry.notes}")
                        HorizontalDivider()
                        TextButton(onClick = { viewModel.delete(entry.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        }

        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}
