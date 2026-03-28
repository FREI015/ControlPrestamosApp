package com.controlprestamos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.ui.viewmodel.LoanFormViewModel

@Composable
fun LoanFormScreen(
    paddingValues: PaddingValues,
    onBackToLoans: () -> Unit
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val viewModel: LoanFormViewModel = viewModel(
        factory = LoanFormViewModel.factory(app.container.loanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBackToLoans()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Registrar préstamo", style = MaterialTheme.typography.headlineSmall)

        uiState.blacklistConflict?.let { conflict ->
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cliente bloqueado", color = MaterialTheme.colorScheme.error)
                    Text("Nombre: ")
                    Text("Motivo: ")
                    Text("No se permitirá guardar mientras exista el conflicto.")
                }
            }
        }

        OutlinedTextField(
            value = uiState.form.customerName,
            onValueChange = { value -> viewModel.updateForm { it.copy(customerName = value) } },
            label = { Text("Nombre del cliente") }
        )
        OutlinedTextField(
            value = uiState.form.phone,
            onValueChange = { value -> viewModel.updateForm { it.copy(phone = value) } },
            label = { Text("Teléfono") }
        )
        OutlinedTextField(
            value = uiState.form.idNumber,
            onValueChange = { value -> viewModel.updateForm { it.copy(idNumber = value) } },
            label = { Text("Cédula o identificador") }
        )
        OutlinedTextField(
            value = uiState.form.principalAmount,
            onValueChange = { value -> viewModel.updateForm { it.copy(principalAmount = value) } },
            label = { Text("Monto prestado") }
        )
        OutlinedTextField(
            value = uiState.form.interestRate,
            onValueChange = { value -> viewModel.updateForm { it.copy(interestRate = value) } },
            label = { Text("% de interés") }
        )
        OutlinedTextField(
            value = uiState.form.loanDate,
            onValueChange = { value -> viewModel.updateForm { it.copy(loanDate = value) } },
            label = { Text("Fecha de préstamo (YYYY-MM-DD)") }
        )
        OutlinedTextField(
            value = uiState.form.dueDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha de vencimiento automática (quincena)") }
        )
        OutlinedTextField(
            value = uiState.form.observations,
            onValueChange = { value -> viewModel.updateForm { it.copy(observations = value) } },
            label = { Text("Observaciones") }
        )

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ganancia proyectada: ")
                Text("Total a regresar: ")
                Text("La fecha de vencimiento se calcula automáticamente a 15 días.")
            }
        }

        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = viewModel::save,
            enabled = !uiState.saving && uiState.blacklistConflict == null
        ) {
            Text(if (uiState.saving) "Guardando..." else "Guardar préstamo")
        }

        TextButton(onClick = onBackToLoans) {
            Text("Volver")
        }
    }
}
