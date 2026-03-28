package com.controlprestamos.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.domain.model.BlacklistEntry
import com.controlprestamos.ui.components.EmptyState
import com.controlprestamos.ui.viewmodel.BlacklistViewModel
import com.controlprestamos.util.DateUtils

@Composable
fun BlacklistScreen(
    paddingValues: PaddingValues
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val context = LocalContext.current

    val viewModel: BlacklistViewModel = viewModel(
        factory = BlacklistViewModel.factory(app.container.loanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showForm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
            showForm = false
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

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
                    text = "Lista negra",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bloquea personas para evitar nuevos préstamos y consulta sus motivos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = "Bloquear"
                            )
                            Text(
                                text = if (showForm) "Nuevo registro abierto" else "Agregar registro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { showForm = !showForm }
                        ) {
                            Icon(
                                imageVector = if (showForm) Icons.Outlined.Close else Icons.Outlined.Add,
                                contentDescription = if (showForm) "Cerrar formulario" else "Abrir formulario"
                            )
                        }
                    }

                    Text(
                        text = if (showForm) {
                            "Completa los datos para bloquear a una persona."
                        } else {
                            "Toca el botón + para desplegar el formulario."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = showForm) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider()

                            OutlinedTextField(
                                value = uiState.form.customerName,
                                onValueChange = { value ->
                                    viewModel.updateForm { it.copy(customerName = value) }
                                },
                                label = { Text("Nombre del cliente") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = uiState.form.phone,
                                onValueChange = { value ->
                                    viewModel.updateForm { it.copy(phone = value) }
                                },
                                label = { Text("Teléfono") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = uiState.form.idNumber,
                                onValueChange = { value ->
                                    viewModel.updateForm { it.copy(idNumber = value) }
                                },
                                label = { Text("Cédula o identificador") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = uiState.form.reason,
                                onValueChange = { value ->
                                    viewModel.updateForm { it.copy(reason = value) }
                                },
                                label = { Text("Motivo") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = uiState.form.addedDate,
                                onValueChange = { value ->
                                    viewModel.updateForm { it.copy(addedDate = value) }
                                },
                                label = { Text("Fecha (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = viewModel::save,
                                enabled = !uiState.saving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (uiState.saving) "Guardando..." else "Agregar a lista negra")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.search,
                onValueChange = viewModel::updateSearch,
                label = { Text("Buscar por nombre, teléfono, cédula o motivo") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Buscar"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Registros activos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (uiState.entries.isEmpty()) {
            item {
                EmptyState("No hay registros activos en lista negra.")
            }
        } else {
            items(uiState.entries, key = { it.id }) { entry ->
                BlacklistEntryCard(
                    entry = entry,
                    processing = uiState.processingIds.contains(entry.id),
                    onDeactivate = { viewModel.deactivate(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun BlacklistEntryCard(
    entry: BlacklistEntry,
    processing: Boolean,
    onDeactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (entry.idNumber.isNotBlank()) {
                Text("Cédula: ${entry.idNumber}")
            }

            if (entry.phone.isNotBlank()) {
                Text("Teléfono: ${entry.phone}")
            }

            Text("Motivo: ${entry.reason}")
            Text("Fecha de registro: ${DateUtils.format(entry.addedDate)}")

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDeactivate,
                    enabled = !processing
                ) {
                    Text(if (processing) "Procesando..." else "Retirar de lista negra")
                }
            }
        }
    }
}