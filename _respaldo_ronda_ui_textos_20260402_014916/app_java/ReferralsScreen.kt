package com.controlprestamos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private const val REF_STATUS_TODOS = "TODOS"
private const val REF_STATUS_PENDIENTE = "PENDIENTE"
private const val REF_STATUS_PAGADA = "PAGADA"
private const val REF_STATUS_ANULADA = "ANULADA"

@Composable
fun ReferralsScreen(
    sessionStore: SessionStore
) {
    var records by remember { mutableStateOf(sessionStore.readReferrals()) }

    var editingId by remember { mutableStateOf("") }
    var referralDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var referredClient by remember { mutableStateOf("") }
    var referredBy by remember { mutableStateOf("") }
    var loanAmount by remember { mutableStateOf("") }
    var commissionPercent by remember { mutableStateOf("10") }
    var status by remember { mutableStateOf(REF_STATUS_PENDIENTE) }
    var notes by remember { mutableStateOf("") }

    var selectedFilter by remember { mutableStateOf(REF_STATUS_TODOS) }
    var searchQuery by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ReferralRecordData?>(null) }

    fun reload() {
        records = sessionStore.readReferrals()
    }

    fun clearForm() {
        editingId = ""
        referralDate = LocalDate.now().toString()
        referredClient = ""
        referredBy = ""
        loanAmount = ""
        commissionPercent = "10"
        status = REF_STATUS_PENDIENTE
        notes = ""
        errorMessage = ""
    }

    fun loadForEdit(item: ReferralRecordData) {
        editingId = item.id
        referralDate = item.referralDate
        referredClient = item.referredClient
        referredBy = item.referredBy
        loanAmount = if (item.loanAmount <= 0.0) "" else String.format(Locale.US, "%.2f", item.loanAmount)
        commissionPercent = String.format(Locale.US, "%.2f", item.commissionPercent)
        status = item.status.ifBlank { REF_STATUS_PENDIENTE }
        notes = item.notes
        errorMessage = ""
        feedback = ""
    }

    fun alreadyExists(
        currentId: String,
        referralDate: String,
        referredClient: String,
        referredBy: String
    ): Boolean {
        val dateKey = referralDate.trim()
        val clientKey = referredClient.trim().lowercase()
        val byKey = referredBy.trim().lowercase()

        return records.any { item ->
            item.id != currentId &&
                item.referralDate.trim() == dateKey &&
                item.referredClient.trim().lowercase() == clientKey &&
                item.referredBy.trim().lowercase() == byKey
        }
    }

    val parsedLoanAmount = parseMoneyOrNull(loanAmount) ?: 0.0
    val parsedCommissionPercent = parsePercentOrNull(commissionPercent) ?: 0.0
    val projectedCommission = parsedLoanAmount * (parsedCommissionPercent / 100.0)

    val filteredRecords = records
        .filter { item ->
            selectedFilter == REF_STATUS_TODOS || item.status.equals(selectedFilter, ignoreCase = true)
        }
        .filter { item ->
            searchQuery.isBlank() ||
                item.referredClient.contains(searchQuery, ignoreCase = true) ||
                item.referredBy.contains(searchQuery, ignoreCase = true) ||
                item.status.contains(searchQuery, ignoreCase = true)
        }
        .sortedByDescending { it.createdAt }

    val totalLoanAmount = filteredRecords.sumOf { it.loanAmount }
    val totalCommission = filteredRecords.sumOf { it.commissionAmount() }
    val pendingCount = filteredRecords.count { it.status.equals(REF_STATUS_PENDIENTE, ignoreCase = true) }
    val paidCount = filteredRecords.count { it.status.equals(REF_STATUS_PAGADA, ignoreCase = true) }
    val canceledCount = filteredRecords.count { it.status.equals(REF_STATUS_ANULADA, ignoreCase = true) }

    AppConfirmDialog(
        visible = pendingDelete != null,
        title = "Eliminar referido",
        message = "¿Seguro que deseas eliminar el referido de ${pendingDelete?.referredClient ?: "este registro"}?",
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            val item = pendingDelete
            if (item != null) {
                sessionStore.deleteReferral(item.id)
                if (editingId == item.id) {
                    clearForm()
                }
                feedback = "Referido eliminado correctamente."
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
            AppTopBack(title = "Referidos")
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
                Text(
                    text = "Resumen",
                    style = MaterialTheme.typography.titleMedium
                )

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cardWidth = (maxWidth - 12.dp) / 2

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReferralMetricCard("Registros", filteredRecords.size.toString(), Modifier.width(cardWidth))
                            ReferralMetricCard("Monto referido", formatMoney(totalLoanAmount), Modifier.width(cardWidth))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReferralMetricCard("Comisión", formatMoney(totalCommission), Modifier.width(cardWidth))
                            ReferralMetricCard("Pendientes", pendingCount.toString(), Modifier.width(cardWidth))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReferralMetricCard("Pagadas", paidCount.toString(), Modifier.width(cardWidth))
                            ReferralMetricCard("Anuladas", canceledCount.toString(), Modifier.width(cardWidth))
                        }
                    }
                }
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = if (editingId.isBlank()) "Registrar referido" else "Editar referido",
                    style = MaterialTheme.typography.titleMedium
                )

                AppDateField(
                    value = referralDate,
                    label = "Fecha del referido *",
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = {
                        referralDate = it
                        errorMessage = ""
                    }
                )

                OutlinedTextField(
                    value = referredClient,
                    onValueChange = {
                        referredClient = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Cliente referido *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = referredBy,
                    onValueChange = {
                        referredBy = normalizeTextInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Referido por *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = loanAmount,
                    onValueChange = {
                        loanAmount = sanitizeDecimalInput(it)
                        errorMessage = ""
                    },
                    label = { Text("Monto del préstamo USD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = commissionPercent,
                    onValueChange = {
                        commissionPercent = sanitizeDecimalInput(it)
                        errorMessage = ""
                    },
                    label = { Text("% de comisión *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Pendiente", status == REF_STATUS_PENDIENTE) { status = REF_STATUS_PENDIENTE }
                    AppFilterChip("Pagada", status == REF_STATUS_PAGADA) { status = REF_STATUS_PAGADA }
                    AppFilterChip("Anulada", status == REF_STATUS_ANULADA) { status = REF_STATUS_ANULADA }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = normalizeTextInput(it)
                    },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth()
                )

                AppMutedText("Comisión calculada: ${formatMoney(projectedCommission)}")

                AppPrimaryButton(
                    text = if (editingId.isBlank()) "Guardar referido" else "Guardar cambios",
                    onClick = {
                        val cleanDate = referralDate.trim()
                        val cleanClient = normalizeTextInput(referredClient).trim()
                        val cleanBy = normalizeTextInput(referredBy).trim()
                        val cleanNotes = normalizeTextInput(notes).trim()
                        val cleanLoanAmountText = sanitizeDecimalInput(loanAmount)
                        val amount = parseMoneyOrNull(cleanLoanAmountText) ?: 0.0
                        val percent = parsePercentOrNull(commissionPercent)

                        val validationMessage = validateReferralForm(
                            referralDate = cleanDate,
                            referredClient = cleanClient,
                            referredBy = cleanBy,
                            loanAmountText = cleanLoanAmountText,
                            amount = amount,
                            commissionPercent = percent,
                            status = status,
                            notes = cleanNotes
                        )

                        when {
                            validationMessage != null -> {
                                errorMessage = validationMessage
                            }
                            alreadyExists(editingId, cleanDate, cleanClient, cleanBy) -> {
                                errorMessage = "Ya existe un referido igual para esa fecha."
                            }
                            else -> {
                                sessionStore.saveReferral(
                                    ReferralRecordData(
                                        id = editingId.ifBlank { UUID.randomUUID().toString() },
                                        referralDate = cleanDate,
                                        referredClient = cleanClient,
                                        referredBy = cleanBy,
                                        loanAmount = amount,
                                        commissionPercent = percent!!,
                                        status = status,
                                        notes = cleanNotes
                                    )
                                )
                                feedback = if (editingId.isBlank()) {
                                    "Referido guardado correctamente."
                                } else {
                                    "Referido actualizado correctamente."
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
                Text(
                    text = "Búsqueda y filtro",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = normalizeTextInput(it)
                    },
                    label = { Text("Buscar por cliente, referido por o estado") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Todos", selectedFilter == REF_STATUS_TODOS) { selectedFilter = REF_STATUS_TODOS }
                    AppFilterChip("Pendientes", selectedFilter == REF_STATUS_PENDIENTE) { selectedFilter = REF_STATUS_PENDIENTE }
                    AppFilterChip("Pagadas", selectedFilter == REF_STATUS_PAGADA) { selectedFilter = REF_STATUS_PAGADA }
                    AppFilterChip("Anuladas", selectedFilter == REF_STATUS_ANULADA) { selectedFilter = REF_STATUS_ANULADA }
                }

                AppMutedText("Mostrando: ${filteredRecords.size}")
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                AppSectionCard {
                    Text("No hay referidos registrados todavía.")
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { item ->
                AppSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.referredClient.ifBlank { "Sin nombre" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            AppMutedText("Fecha: ${item.referralDate}")
                            AppMutedText("Referido por: ${item.referredBy}")
                        }

                        AppStatusChip(item.status.ifBlank { REF_STATUS_PENDIENTE })
                    }

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val cardWidth = (maxWidth - 12.dp) / 2

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ReferralInfoCard("Monto", formatMoney(item.loanAmount), Modifier.width(cardWidth))
                                ReferralInfoCard("Comisión %", formatReferralPercent(item.commissionPercent), Modifier.width(cardWidth))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ReferralInfoCard("Comisión calculada", formatMoney(item.commissionAmount()), Modifier.width(cardWidth))
                                Spacer(modifier = Modifier.width(cardWidth))
                            }

                            if (item.notes.isNotBlank()) {
                                ReferralInfoCard("Notas", item.notes, Modifier.fillMaxWidth())
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReferralActionBox(
                            label = "Editar",
                            modifier = Modifier.weightlessHalf()
                        ) {
                            loadForEdit(item)
                        }

                        ReferralActionBox(
                            label = "Eliminar",
                            modifier = Modifier.weightlessHalf()
                        ) {
                            pendingDelete = item
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.field
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ReferralInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.field
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReferralActionBox(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.pill
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun Modifier.weightlessHalf(): Modifier = this.fillMaxWidth()

private fun formatReferralPercent(value: Double): String {
    val safe = if (value.isFinite()) value else 0.0
    return String.format(Locale.US, "%.2f%%", safe)
}
