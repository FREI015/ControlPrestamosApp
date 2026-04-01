package com.controlprestamos.app

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

private const val FILTER_TODOS = "TODOS"
private const val FILTER_ACTIVOS = "ACTIVOS"
private const val FILTER_VENCIDOS = "VENCIDOS"
private const val FILTER_COBRADOS = "COBRADOS"
private const val FILTER_PERDIDOS = "PERDIDOS"

private const val ORDER_MAS_RECIENTE = "MAS_RECIENTE"
private const val ORDER_MAS_ANTIGUO = "MAS_ANTIGUO"
private const val ORDER_MAYOR_PENDIENTE = "MAYOR_PENDIENTE"
private const val ORDER_MENOR_PENDIENTE = "MENOR_PENDIENTE"
private const val ORDER_ALFABETICO = "ALFABETICO"
private const val ORDER_VENCIMIENTO_PROXIMO = "VENCIMIENTO_PROXIMO"

@Composable
fun LoansScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedFilter by remember { mutableStateOf(FILTER_TODOS) }
    var selectedOrder by remember { mutableStateOf(ORDER_MAS_RECIENTE) }
    var searchText by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var loans by remember { mutableStateOf(sessionStore.readLoans()) }
    var pendingDeleteLoan by remember { mutableStateOf<ManualLoanData?>(null) }

    fun reload() {
        loans = sessionStore.readLoans()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredLoans = loans
        .filter { loan -> matchesFilter(loan, selectedFilter) }
        .filter { loan -> matchesSearch(loan, searchText) }
        .filter { loan -> matchesFromDate(loan, fromDate) }
        .filter { loan -> matchesToDate(loan, toDate) }

    val orderedLoans = when (selectedOrder) {
        ORDER_MAS_ANTIGUO -> filteredLoans.sortedBy { it.createdAt }
        ORDER_MAYOR_PENDIENTE -> filteredLoans.sortedByDescending { it.pendingAmount() }
        ORDER_MENOR_PENDIENTE -> filteredLoans.sortedBy { it.pendingAmount() }
        ORDER_ALFABETICO -> filteredLoans.sortedBy { it.fullName.lowercase(Locale.getDefault()) }
        ORDER_VENCIMIENTO_PROXIMO -> filteredLoans.sortedWith(
            compareBy<ManualLoanData> { parseDateForSort(it.dueDate) ?: LocalDate.MAX }
                .thenByDescending { it.createdAt }
        )
        else -> filteredLoans.sortedByDescending { it.createdAt }
    }

    val totalLoaned = orderedLoans.sumOf { it.loanAmount }
    val totalToCollect = orderedLoans.sumOf { it.totalAmount() }
    val totalPaid = orderedLoans.sumOf { it.paidAmount }
    val totalPending = orderedLoans.sumOf { it.pendingAmount() }
    val totalOverdue = orderedLoans.count { isLoanOverdue(it) }

    AppConfirmDialog(
        visible = pendingDeleteLoan != null,
        title = "Eliminar préstamo",
        message = buildDeleteMessage(pendingDeleteLoan),
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            val loan = pendingDeleteLoan
            if (loan != null) {
                sessionStore.deleteLoan(loan.id)
                if (sessionStore.readActiveLoanId() == loan.id) {
                    sessionStore.setActiveLoanId("")
                }
                reload()
            }
            pendingDeleteLoan = null
        },
        onDismiss = {
            pendingDeleteLoan = null
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTopBack(title = "Control de préstamos")
        }

        item {
            AppSectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Todos", selectedFilter == FILTER_TODOS) { selectedFilter = FILTER_TODOS }
                    AppFilterChip("Activos", selectedFilter == FILTER_ACTIVOS) { selectedFilter = FILTER_ACTIVOS }
                    AppFilterChip("Vencidos", selectedFilter == FILTER_VENCIDOS) { selectedFilter = FILTER_VENCIDOS }
                    AppFilterChip("Cobrados", selectedFilter == FILTER_COBRADOS) { selectedFilter = FILTER_COBRADOS }
                    AppFilterChip("Perdidos", selectedFilter == FILTER_PERDIDOS) { selectedFilter = FILTER_PERDIDOS }
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar por nombre, teléfono o cédula") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatePill(
                        label = "Desde",
                        value = fromDate,
                        onPick = { openDatePicker(context, fromDate) { fromDate = it } },
                        onClear = { fromDate = "" }
                    )

                    DatePill(
                        label = "Hasta",
                        value = toDate,
                        onPick = { openDatePicker(context, toDate) { toDate = it } },
                        onClear = { toDate = "" }
                    )
                }

                if (fromDate.isNotBlank() && toDate.isNotBlank() && !isDateRangeValid(fromDate, toDate)) {
                    AppMutedText("El rango de fechas no es válido. La fecha final no puede ser menor que la inicial.")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Más reciente", selectedOrder == ORDER_MAS_RECIENTE) { selectedOrder = ORDER_MAS_RECIENTE }
                    AppFilterChip("Más antiguo", selectedOrder == ORDER_MAS_ANTIGUO) { selectedOrder = ORDER_MAS_ANTIGUO }
                    AppFilterChip("Mayor pendiente", selectedOrder == ORDER_MAYOR_PENDIENTE) { selectedOrder = ORDER_MAYOR_PENDIENTE }
                    AppFilterChip("Menor pendiente", selectedOrder == ORDER_MENOR_PENDIENTE) { selectedOrder = ORDER_MENOR_PENDIENTE }
                    AppFilterChip("A-Z", selectedOrder == ORDER_ALFABETICO) { selectedOrder = ORDER_ALFABETICO }
                    AppFilterChip("Vencimiento próximo", selectedOrder == ORDER_VENCIMIENTO_PROXIMO) { selectedOrder = ORDER_VENCIMIENTO_PROXIMO }
                }

                AppPrimaryButton(
                    text = "Registrar nuevo préstamo",
                    onClick = { navController.navigate("newLoan") }
                )
            }
        }

        item {
            AppSectionCard {
                Text(
                    text = "Resumen del listado",
                    style = MaterialTheme.typography.titleMedium
                )

                AppMutedText("Cantidad: ${orderedLoans.size}")
                AppMutedText("Pagado: ${formatMoney(totalPaid)}")
                AppMutedText("Pendiente: ${formatMoney(totalPending)}")
                AppMutedText("Total: ${formatMoney(totalToCollect)}")
                AppMutedText("Prestado: ${formatMoney(totalLoaned)}")
                AppMutedText("Vencidos: $totalOverdue")
            }
        }

        if (orderedLoans.isEmpty()) {
            item {
                AppSectionCard {
                    Text("No hay préstamos para mostrar.")
                    AppMutedText("Registra un préstamo nuevo o ajusta los filtros para ver resultados.")
                }
            }
        } else {
            items(
                items = orderedLoans,
                key = { it.id }
            ) { loan ->
                LoanCard(
                    loan = loan,
                    onOpen = {
                        sessionStore.setActiveLoanId(loan.id)
                        navController.navigate("loanDetail")
                    },
                    onEdit = {
                        sessionStore.setActiveLoanId(loan.id)
                        navController.navigate("editLoan")
                    },
                    onDelete = {
                        pendingDeleteLoan = loan
                    }
                )
            }
        }
    }
}

@Composable
private fun LoanCard(
    loan: ManualLoanData,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusText = loanStatusLabel(loan)

    AppSectionCard(
        modifier = Modifier.clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = loan.fullName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleLarge
                )

                if (loan.idNumber.isNotBlank()) {
                    AppMutedText("Cédula: ${loan.idNumber}")
                }

                if (loan.phone.isNotBlank()) {
                    AppMutedText("Teléfono: ${loan.phone}")
                }
            }

            AppStatusChip(statusText)
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AppMutedText("Prestado")
                Text(formatMoney(loan.loanAmount), style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                AppMutedText("A cobrar")
                Text(formatMoney(loan.totalAmount()), style = MaterialTheme.typography.titleLarge)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AppMutedText("Pagado")
                Text(formatMoney(loan.paidAmount), style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                AppMutedText("Pendiente")
                Text(formatMoney(loan.pendingAmount()), style = MaterialTheme.typography.titleLarge)
            }
        }

        HorizontalDivider()

        AppMutedText("Préstamo: ${loan.loanDate.ifBlank { "Sin fecha" }}")
        AppMutedText("Vencimiento: ${loan.dueDate.ifBlank { "Sin fecha" }}")

        if (loan.conditions.isNotBlank()) {
            AppMutedText("Obs: ${loan.conditions}")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ver detalle", color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Editar",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onEdit() }
                )
                Text(
                    text = "Eliminar",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}

private fun matchesFilter(loan: ManualLoanData, filter: String): Boolean {
    return when (filter) {
        FILTER_ACTIVOS -> loan.status == "ACTIVO" && !isLoanOverdue(loan)
        FILTER_VENCIDOS -> isLoanOverdue(loan)
        FILTER_COBRADOS -> loan.status == "COBRADO"
        FILTER_PERDIDOS -> loan.status == "PERDIDO"
        else -> true
    }
}

private fun matchesSearch(loan: ManualLoanData, search: String): Boolean {
    if (search.isBlank()) return true
    val term = search.trim().lowercase(Locale.getDefault())

    return loan.fullName.lowercase(Locale.getDefault()).contains(term) ||
        loan.phone.lowercase(Locale.getDefault()).contains(term) ||
        loan.idNumber.lowercase(Locale.getDefault()).contains(term)
}

private fun matchesFromDate(loan: ManualLoanData, fromDate: String): Boolean {
    if (fromDate.isBlank()) return true
    val loanDate = parseIsoDateOrNull(loan.loanDate) ?: return false
    val from = parseIsoDateOrNull(fromDate) ?: return true
    return !loanDate.isBefore(from)
}

private fun matchesToDate(loan: ManualLoanData, toDate: String): Boolean {
    if (toDate.isBlank()) return true
    val loanDate = parseIsoDateOrNull(loan.loanDate) ?: return false
    val to = parseIsoDateOrNull(toDate) ?: return true
    return !loanDate.isAfter(to)
}

private fun isLoanOverdue(loan: ManualLoanData): Boolean {
    return loan.status == "ACTIVO" && loan.isOverdue()
}

private fun parseDateForSort(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun loanStatusLabel(loan: ManualLoanData): String {
    return when {
        loan.status == "COBRADO" -> "Cobrado"
        loan.status == "PERDIDO" -> "Perdido"
        isLoanOverdue(loan) -> "Vencido"
        else -> "Activo"
    }
}

private fun buildDeleteMessage(loan: ManualLoanData?): String {
    if (loan == null) {
        return "¿Seguro que deseas eliminar este préstamo?"
    }

    val name = loan.fullName.ifBlank { "este préstamo" }
    return "¿Seguro que deseas eliminar el préstamo de $name? Esta acción no se puede deshacer."
}

@Composable
private fun DatePill(
    label: String,
    value: String,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, AppShapes.pill)
            .clickable { onPick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .widthIn(min = 110.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Text(if (value.isBlank()) label else value)
        if (value.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.clickable { onClear() }
            )
        }
    }
}

private fun openDatePicker(
    context: android.content.Context,
    current: String,
    onSelected: (String) -> Unit
) {
    val baseDate = parseIsoDateOrNull(current) ?: LocalDate.now()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
        },
        baseDate.year,
        baseDate.monthValue - 1,
        baseDate.dayOfMonth
    ).show()
}



