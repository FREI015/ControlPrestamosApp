package com.controlprestamos.app

import com.controlprestamos.features.loans.*

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
import androidx.navigation.NavController

private const val ORDER_FREQUENT_NAME = "NOMBRE"
private const val ORDER_FREQUENT_NEWEST = "RECIENTES"

@Composable
fun FrequentUsersScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    var users by remember { mutableStateOf(sessionStore.readFrequentUsers()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf(ORDER_FREQUENT_NEWEST) }

    var editingId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var mobilePaymentPhone by remember { mutableStateOf("") }
    var paymentAlias by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FrequentUserPaymentData?>(null) }

    fun reload() {
        users = sessionStore.readFrequentUsers()
    }

    fun clearForm() {
        editingId = ""
        fullName = ""
        idNumber = ""
        phone = ""
        bankName = ""
        bankAccount = ""
        mobilePaymentPhone = ""
        paymentAlias = ""
        notes = ""
        errorMessage = ""
    }

    fun loadForEdit(user: FrequentUserPaymentData) {
        editingId = user.id
        fullName = user.fullName
        idNumber = user.idNumber
        phone = user.phone
        bankName = user.bankName
        bankAccount = user.bankAccount
        mobilePaymentPhone = user.mobilePaymentPhone
        paymentAlias = user.paymentAlias
        notes = user.notes
        errorMessage = ""
        feedback = ""
    }

    fun hasPaymentData(): Boolean {
        return bankName.trim().isNotBlank() ||
            bankAccount.trim().isNotBlank() ||
            mobilePaymentPhone.trim().isNotBlank() ||
            paymentAlias.trim().isNotBlank()
    }

    fun alreadyExists(
        currentId: String,
        fullName: String,
        idNumber: String,
        phone: String
    ): Boolean {
        val cleanName = fullName.trim().lowercase()
        val cleanId = idNumber.trim()
        val cleanPhone = phone.trim()

        return users.any { user ->
            user.id != currentId &&
                (
                    (cleanId.isNotBlank() && user.idNumber.equals(cleanId, ignoreCase = true)) ||
                    (cleanPhone.isNotBlank() && user.phone.equals(cleanPhone, ignoreCase = true)) ||
                    (
                        cleanName.isNotBlank() &&
                        user.fullName.trim().lowercase() == cleanName &&
                        (cleanId.isBlank() || user.idNumber.isBlank() || user.idNumber.equals(cleanId, ignoreCase = true))
                    )
                )
        }
    }

    val filteredUsers = users.filter { user ->
        searchQuery.isBlank() ||
            user.fullName.contains(searchQuery, ignoreCase = true) ||
            user.idNumber.contains(searchQuery, ignoreCase = true) ||
            user.phone.contains(searchQuery, ignoreCase = true) ||
            user.bankName.contains(searchQuery, ignoreCase = true) ||
            user.mobilePaymentPhone.contains(searchQuery, ignoreCase = true)
    }

    val orderedUsers = when (selectedOrder) {
        ORDER_FREQUENT_NAME -> filteredUsers.sortedBy { it.fullName.lowercase() }
        else -> filteredUsers.sortedByDescending { it.createdAt }
    }

    AppConfirmDialog(
        visible = pendingDelete != null,
        title = "Eliminar usuario frecuente",
        message = "¿Seguro que deseas eliminar a ${pendingDelete?.fullName ?: "este usuario"}?",
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            val user = pendingDelete
            if (user != null) {
                sessionStore.deleteFrequentUser(user.id)
                if (editingId == user.id) {
                    clearForm()
                }
                feedback = "Usuario frecuente eliminado."
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
            AppTopBack(
                title = "Usuarios frecuentes",
                onBack = { navController.popBackStack() }
            )
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
                Text(if (editingId.isBlank()) "Nuevo usuario frecuente" else "Editar usuario frecuente")

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
                    value = bankName,
                    onValueChange = {
                        bankName = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Banco") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = {
                        bankAccount = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Cuenta / referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobilePaymentPhone,
                    onValueChange = {
                        mobilePaymentPhone = sanitizePhoneInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Pago móvil") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = paymentAlias,
                    onValueChange = {
                        paymentAlias = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Alias de pago") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth()
                )

                AppPrimaryButton(
                    text = if (editingId.isBlank()) "Guardar usuario" else "Guardar cambios",
                    onClick = {
                        val cleanName = normalizeTextInput(fullName).trim()
                        val cleanId = sanitizeIntegerInput(idNumber)
                        val cleanPhone = sanitizePhoneInput(phone)
                        val cleanBank = normalizeTextInput(bankName).trim()
                        val cleanAccount = normalizeTextInput(bankAccount).trim()
                        val cleanMobilePayment = sanitizePhoneInput(mobilePaymentPhone)
                        val cleanAlias = normalizeTextInput(paymentAlias).trim()
                        val cleanNotes = normalizeTextInput(notes).trim()

                        val validationMessage = validateFrequentUserForm(
                            fullName = cleanName,
                            idNumber = cleanId,
                            phone = cleanPhone,
                            bankName = cleanBank,
                            bankAccount = cleanAccount,
                            mobilePaymentPhone = cleanMobilePayment,
                            paymentAlias = cleanAlias,
                            notes = cleanNotes
                        )

                        when {
                            validationMessage != null -> errorMessage = validationMessage
                            alreadyExists(editingId, cleanName, cleanId, cleanPhone) ->
                                errorMessage = "Ya existe un usuario frecuente muy parecido."
                            else -> {
                                sessionStore.saveFrequentUser(
                                    FrequentUserPaymentData(
                                        id = editingId.ifBlank { java.util.UUID.randomUUID().toString() },
                                        fullName = cleanName,
                                        idNumber = cleanId,
                                        phone = cleanPhone,
                                        bankName = cleanBank,
                                        bankAccount = cleanAccount,
                                        mobilePaymentPhone = cleanMobilePayment,
                                        paymentAlias = cleanAlias,
                                        notes = cleanNotes
                                    )
                                )
                                feedback = if (editingId.isBlank()) {
                                    "Usuario frecuente guardado."
                                } else {
                                    "Usuario frecuente actualizado."
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
                Text("Búsqueda y orden")
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = normalizeTextInput(it) },
                    label = { Text("Buscar por nombre, cédula, teléfono o banco") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Recientes", selectedOrder == ORDER_FREQUENT_NEWEST) {
                        selectedOrder = ORDER_FREQUENT_NEWEST
                    }
                    AppFilterChip("A-Z", selectedOrder == ORDER_FREQUENT_NAME) {
                        selectedOrder = ORDER_FREQUENT_NAME
                    }
                }

                AppMutedText("Total guardados: ${users.size}")
                AppMutedText("Mostrando: ${orderedUsers.size}")
            }
        }

        if (orderedUsers.isEmpty()) {
            item {
                AppSectionCard {
                    Text("Todavía no hay usuarios frecuentes guardados.")
                    AppMutedText("Aquí vas a guardar clientes o personas con datos de pago reutilizables.")
                }
            }
        } else {
            items(orderedUsers, key = { it.id }) { user ->
                AppSectionCard {
                    Text(user.fullName.ifBlank { "Sin nombre" })
                    if (user.idNumber.isNotBlank()) AppMutedText("Cédula: ${user.idNumber}")
                    if (user.phone.isNotBlank()) AppMutedText("Teléfono: ${user.phone}")
                    if (user.bankName.isNotBlank()) AppMutedText("Banco: ${user.bankName}")
                    if (user.bankAccount.isNotBlank()) AppMutedText("Cuenta: ${user.bankAccount}")
                    if (user.mobilePaymentPhone.isNotBlank()) AppMutedText("Pago móvil: ${user.mobilePaymentPhone}")
                    if (user.paymentAlias.isNotBlank()) AppMutedText("Alias: ${user.paymentAlias}")
                    if (user.notes.isNotBlank()) AppMutedText("Notas: ${user.notes}")

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Editar",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { loadForEdit(user) }
                        )
                        Text(
                            text = "Eliminar",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { pendingDelete = user }
                        )
                    }
                }
            }
        }

        item {
            AppBottomBack(onClick = { navController.popBackStack() })
        }
    }
}
