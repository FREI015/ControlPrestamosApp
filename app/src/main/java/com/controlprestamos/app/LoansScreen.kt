package com.controlprestamos.app

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
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
    var showAdvancedFilters by remember { mutableStateOf(false) }
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
        .filter { loan -> matchesLoanFilterRefined(loan, selectedFilter) }
        .filter { loan -> matchesLoanSearchRefined(loan, searchText) }
        .filter { loan -> matchesLoanFromDateRefined(loan, fromDate) }
        .filter { loan -> matchesLoanToDateRefined(loan, toDate) }

    val orderedLoans = when (selectedOrder) {
        ORDER_MAS_ANTIGUO -> filteredLoans.sortedBy { it.createdAt }
        ORDER_MAYOR_PENDIENTE -> filteredLoans.sortedByDescending { it.pendingAmount() }
        ORDER_MENOR_PENDIENTE -> filteredLoans.sortedBy { it.pendingAmount() }
        ORDER_ALFABETICO -> filteredLoans.sortedBy { it.fullName.lowercase(Locale.getDefault()) }
        ORDER_VENCIMIENTO_PROXIMO -> filteredLoans.sortedWith(
            compareBy<ManualLoanData> { parseLoanDateRefined(it.dueDate) ?: LocalDate.MAX }
                .thenByDescending { it.createdAt }
        )
        else -> filteredLoans.sortedByDescending { it.createdAt }
    }
    AppConfirmDialog(
        visible = pendingDeleteLoan != null,
        title = "Enviar a papelera",
        message = buildLoanDeleteMessageRefined(pendingDeleteLoan),
        confirmText = "Enviar",
        dismissText = "Cancelar",
        onConfirm = {
            val loan = pendingDeleteLoan
            if (loan != null) {
                sessionStore.softDeleteLoan(loan.id)
                if (sessionStore.readActiveLoanId() == loan.id) {
                    sessionStore.setActiveLoanId("")
                }
                reload()
            }
            pendingDeleteLoan = null
        },
        onDismiss = { pendingDeleteLoan = null }
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppTopBack(title = "Control")
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
                        AppFilterChip(
                            if (showAdvancedFilters) "Ocultar filtros" else "Más filtros",
                            showAdvancedFilters
                        ) {
                            showAdvancedFilters = !showAdvancedFilters
                        }
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

                    if (showAdvancedFilters) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DatePillRefined(
                                label = "Desde",
                                value = fromDate,
                                onPick = { openLoanDatePickerRefined(context, fromDate) { fromDate = it } },
                                onClear = { fromDate = "" }
                            )

                            DatePillRefined(
                                label = "Hasta",
                                value = toDate,
                                onPick = { openLoanDatePickerRefined(context, toDate) { toDate = it } },
                                onClear = { toDate = "" }
                            )
                        }

                        if (fromDate.isNotBlank() && toDate.isNotBlank() && !isLoanDateRangeValidRefined(fromDate, toDate)) {
                            AppMutedText("El rango de fechas no es válido.")
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
                    }
                }
            }

            if (orderedLoans.isEmpty()) {
                item {
                    AppSectionCard {
                        Text("No hay préstamos para mostrar.")
                        AppMutedText("Registra un préstamo nuevo o ajusta los filtros.")
                    }
                }
            } else {
                items(
                    items = orderedLoans,
                    key = { it.id }
                ) { loan ->
                    LoanCardRefined(
                        loan = loan,
                        onOpen = {
                            sessionStore.setActiveLoanId(loan.id)
                            navController.navigate(AppRoutes.LoanDetail)
                        },
                        onEdit = {
                            sessionStore.setActiveLoanId(loan.id)
                            navController.navigate(AppRoutes.EditLoan)
                        },
                        onDelete = {
                            pendingDeleteLoan = loan
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                navController.navigate(AppRoutes.NewLoan) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Nuevo préstamo",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}


@Composable
private fun LoanCardRefined(
    loan: ManualLoanData,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusText = loanStatusLabelRefined(loan)

    AppSectionCard(
        modifier = Modifier.clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = loan.fullName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium
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

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val cardWidth = (maxWidth - 10.dp) / 2

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LoanValueBoxRefined("Prestado", formatMoney(loan.loanAmount), Modifier.width(cardWidth))
                    LoanValueBoxRefined("A cobrar", formatMoney(loan.totalAmount()), Modifier.width(cardWidth))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LoanValueBoxRefined("Pagado", formatMoney(loan.paidAmount), Modifier.width(cardWidth))
                    LoanValueBoxRefined("Pendiente", formatMoney(loan.pendingAmount()), Modifier.width(cardWidth))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LoanValueBoxRefined("Ganancia", formatMoney(loan.interestAmount()), Modifier.width(cardWidth))
                    LoanValueBoxRefined("Porcentaje", formatPercentLoanRefined(loan.percent), Modifier.width(cardWidth))
                }

                if (loan.conditions.isNotBlank()) {
                    LoanInfoBoxRefined("Condiciones", loan.conditions, Modifier.fillMaxWidth())
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LoanActionBoxRefined("Detalle", Modifier.width(cardWidth)) { onOpen() }
                    LoanActionBoxRefined("Editar", Modifier.width(cardWidth)) { onEdit() }
                }

                LoanActionBoxRefined("Papelera", Modifier.fillMaxWidth()) { onDelete() }
            }
        }
    }
}

@Composable
private fun LoanValueBoxRefined(
    label: String,
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppMutedText(label)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun LoanInfoBoxRefined(
    label: String,
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppMutedText(label)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LoanActionBoxRefined(
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

@Composable
private fun DatePillRefined(
    label: String,
    value: String,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.pill
            )
            .clickable { onPick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier.widthIn(min = 96.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppMutedText(label)
            Text(
                text = if (value.isBlank()) "Seleccionar" else value,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (value.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onClear() }
            )
        }
    }
}

private fun openLoanDatePickerRefined(
    context: android.content.Context,
    initialValue: String,
    onDateSelected: (String) -> Unit
) {
    val now = LocalDate.now()
    val initial = parseLoanIsoDateRefined(initialValue) ?: now

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedMonth = (month + 1).toString().padStart(2, '0')
            val formattedDay = dayOfMonth.toString().padStart(2, '0')
            onDateSelected("$year-$formattedMonth-$formattedDay")
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}

private fun matchesLoanFilterRefined(loan: ManualLoanData, filter: String): Boolean {
    return when (filter) {
        FILTER_ACTIVOS -> loan.status == "ACTIVO" && !isLoanOverdueRefined(loan)
        FILTER_VENCIDOS -> isLoanOverdueRefined(loan)
        FILTER_COBRADOS -> loan.status == "COBRADO"
        FILTER_PERDIDOS -> loan.status == "PERDIDO"
        else -> true
    }
}

private fun matchesLoanSearchRefined(loan: ManualLoanData, search: String): Boolean {
    if (search.isBlank()) return true
    val term = search.trim().lowercase(Locale.getDefault())

    return loan.fullName.lowercase(Locale.getDefault()).contains(term) ||
        loan.phone.lowercase(Locale.getDefault()).contains(term) ||
        loan.idNumber.lowercase(Locale.getDefault()).contains(term)
}

private fun matchesLoanFromDateRefined(loan: ManualLoanData, fromDate: String): Boolean {
    if (fromDate.isBlank()) return true
    val loanDate = parseLoanIsoDateRefined(loan.loanDate) ?: return false
    val from = parseLoanIsoDateRefined(fromDate) ?: return true
    return !loanDate.isBefore(from)
}

private fun matchesLoanToDateRefined(loan: ManualLoanData, toDate: String): Boolean {
    if (toDate.isBlank()) return true
    val loanDate = parseLoanIsoDateRefined(loan.loanDate) ?: return false
    val to = parseLoanIsoDateRefined(toDate) ?: return true
    return !loanDate.isAfter(to)
}

private fun parseLoanIsoDateRefined(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: Exception) {
        null
    }
}

private fun parseLoanDateRefined(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun isLoanDateRangeValidRefined(fromDate: String, toDate: String): Boolean {
    val from = parseLoanIsoDateRefined(fromDate) ?: return true
    val to = parseLoanIsoDateRefined(toDate) ?: return true
    return !to.isBefore(from)
}

private fun formatPercentLoanRefined(value: Double): String {
    return String.format(Locale.US, "%.2f%%", value)
}

private fun isLoanOverdueRefined(loan: ManualLoanData): Boolean {
    return loan.status == "ACTIVO" && loan.isOverdue()
}

private fun loanStatusLabelRefined(loan: ManualLoanData): String {
    return when {
        loan.status == "COBRADO" -> "Cobrado"
        loan.status == "PERDIDO" -> "Perdido"
        isLoanOverdueRefined(loan) -> "Vencido"
        else -> "Activo"
    }
}

private fun buildLoanDeleteMessageRefined(loan: ManualLoanData?): String {
    if (loan == null) return "¿Seguro que deseas eliminar este préstamo?"
    val name = loan.fullName.ifBlank { "este préstamo" }
    return "¿Seguro que deseas eliminar el préstamo de $name? Esta acción no se puede deshacer."
}
