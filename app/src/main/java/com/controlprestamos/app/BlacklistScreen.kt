package com.controlprestamos.app

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BlacklistScreen(
    onBack: () -> Unit,
    sessionStore: SessionStore
) {
    var records by remember { mutableStateOf(sessionStore.readBlacklist()) }
    var searchQuery by remember { mutableStateOf("") }

    var editingId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<BlacklistRecordData?>(null) }

    fun reload() {
        records = sessionStore.readBlacklist()
    }

    fun clearForm() {
        editingId = ""
        fullName = ""
        idNumber = ""
        phone = ""
        reason = ""
        notes = ""
        errorMessage = ""
    }

    fun loadForEdit(item: BlacklistRecordData) {
        editingId = item.id
        fullName = item.fullName
        idNumber = item.idNumber
        phone = item.phone
        reason = item.reason
        notes = item.notes
        errorMessage = ""
        feedback = ""
    }

    fun alreadyExists(
        currentId: String,
        fullName: String,
        idNumber: String,
        phone: String
    ): Boolean {
        val targetName = fullName.trim().lowercase()
        val targetId = idNumber.trim()
        val targetPhone = phone.trim()

        return records.any { item ->
            item.id != currentId &&
                (
                    (targetId.isNotBlank() && item.idNumber.equals(targetId, ignoreCase = true)) ||
                    (targetPhone.isNotBlank() && item.phone.equals(targetPhone, ignoreCase = true)) ||
                    (
                        targetName.isNotBlank() &&
                        item.fullName.trim().lowercase() == targetName &&
                        (
                            targetId.isBlank() ||
                            item.idNumber.isBlank() ||
                            item.idNumber.equals(targetId, ignoreCase = true)
                        )
                    )
                )
        }
    }

    val filteredRecords = records.filter { item ->
        searchQuery.isBlank() ||
            item.fullName.contains(searchQuery, ignoreCase = true) ||
            item.idNumber.contains(searchQuery, ignoreCase = true) ||
            item.phone.contains(searchQuery, ignoreCase = true) ||
            item.reason.contains(searchQuery, ignoreCase = true)
    }

    AppConfirmDialog(
        visible = pendingDelete != null,
        title = "Retirar de lista negra",
        message = "¿Seguro que deseas retirar a ${pendingDelete?.fullName ?: "este registro"} de la lista negra?",
        confirmText = "Retirar",
        dismissText = "Cancelar",
        onConfirm = {
            val item = pendingDelete
            if (item != null) {
                sessionStore.deleteBlacklistRecord(item.id)
                if (editingId == item.id) {
                    clearForm()
                }
                feedback = "Registro retirado correctamente."
                reload()
            }
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTopBack(title = "Lista negra", onBack = onBack)
        }

        if (feedback.isNotBlank()) {
            item {
                AppSectionCard {
                    Text(feedback)
                }
            }
        }

        if (errorMessage.isNotBlank()) {
            item {
                AppSectionCard {
                    Text(errorMessage)
                }
            }
        }

        item {
            AppSectionCard {
                Text(if (editingId.isBlank()) "Agregar registro" else "Editar registro")

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Nombre completo *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = idNumber,
                    onValueChange = {
                        idNumber = sanitizeIntegerInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Cédula") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = sanitizePhoneInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Motivo *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth()
                )

                AppPrimaryButton(
                    text = if (editingId.isBlank()) "Guardar en lista negra" else "Guardar cambios",
                    onClick = {
                        val cleanName = normalizeTextInput(fullName).trim()
                        val cleanId = sanitizeIntegerInput(idNumber)
                        val cleanPhone = sanitizePhoneInput(phone)
                        val cleanReason = normalizeTextInput(reason).trim()
                        val cleanNotes = normalizeTextInput(notes).trim()

                        val validationMessage = validateBlacklistForm(
                            fullName = cleanName,
                            idNumber = cleanId,
                            phone = cleanPhone,
                            reason = cleanReason,
                            notes = cleanNotes
                        )

                        when {
                            validationMessage != null ->
                                errorMessage = validationMessage
                            alreadyExists(editingId, cleanName, cleanId, cleanPhone) ->
                                errorMessage = "Ya existe un registro muy parecido en lista negra."
                            else -> {
                                sessionStore.saveBlacklistRecord(
                                    BlacklistRecordData(
                                        id = editingId.ifBlank { java.util.UUID.randomUUID().toString() },
                                        fullName = cleanName,
                                        idNumber = cleanId,
                                        phone = cleanPhone,
                                        reason = cleanReason,
                                        notes = cleanNotes
                                    )
                                )
                                feedback = if (editingId.isBlank()) {
                                    "Registro guardado correctamente."
                                } else {
                                    "Registro actualizado correctamente."
                                }
                                reload()
                                clearForm()
                            }
                        }
                    }
                )

                AppSecondaryButton(
                    text = if (editingId.isBlank()) "Limpiar formulario" else "Cancelar edición",
                    onClick = { clearForm() }
                )
            }
        }

        item {
            AppSectionCard {
                Text("Buscar y resumen")
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = normalizeTextInput(it) },
                    label = { Text("Buscar por nombre, cédula, teléfono o motivo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                AppMutedText("Total en lista negra: ${records.size}")
                AppMutedText("Mostrando: ${filteredRecords.size}")
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                AppSectionCard {
                    Text("No hay clientes en lista negra.")
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { item ->
                AppSectionCard {
                    Text(item.fullName.ifBlank { "Sin nombre" })
                    AppMutedText("Motivo: ${item.reason}")
                    if (item.idNumber.isNotBlank()) AppMutedText("Cédula: ${item.idNumber}")
                    if (item.phone.isNotBlank()) AppMutedText("Teléfono: ${item.phone}")
                    AppMutedText("Fecha: ${item.addedDate}")
                    if (item.notes.isNotBlank()) AppMutedText("Notas: ${item.notes}")

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Editar",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { loadForEdit(item) }
                        )
                        Text(
                            text = "Retirar",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { pendingDelete = item }
                        )
                    }
                }
            }
        }

        item {
            AppBottomBack(onClick = onBack)
        }
    }
}
