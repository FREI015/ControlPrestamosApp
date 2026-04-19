package com.controlprestamos.app

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class GlobalSearchRow(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val tertiary: String = "",
    val loanId: String = ""
)

@Composable
fun GlobalSearchScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    var query by remember { mutableStateOf("") }

    val normalizedQuery = query.trim()

    val loanRows = remember(normalizedQuery) {
        sessionStore.readLoans()
            .filter { loan ->
                normalizedQuery.isBlank() ||
                    loan.fullName.contains(normalizedQuery, ignoreCase = true) ||
                    loan.idNumber.contains(normalizedQuery, ignoreCase = true) ||
                    loan.phone.contains(normalizedQuery, ignoreCase = true) ||
                    loan.conditions.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
            .take(30)
            .map { loan ->
                GlobalSearchRow(
                    id = "loan-${loan.id}",
                    category = "Préstamo",
                    title = loan.fullName.ifBlank { "Sin nombre" },
                    subtitle = buildLoanSearchSubtitle(loan),
                    tertiary = "Pendiente: ${formatMoney(loan.pendingAmount())}",
                    loanId = loan.id
                )
            }
    }

    val blacklistRows = remember(normalizedQuery) {
        sessionStore.readBlacklist()
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.fullName.contains(normalizedQuery, ignoreCase = true) ||
                    item.idNumber.contains(normalizedQuery, ignoreCase = true) ||
                    item.phone.contains(normalizedQuery, ignoreCase = true) ||
                    item.reason.contains(normalizedQuery, ignoreCase = true) ||
                    item.notes.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
            .take(20)
            .map { item ->
                GlobalSearchRow(
                    id = "blacklist-${item.id}",
                    category = "Lista negra",
                    title = item.fullName.ifBlank { "Sin nombre" },
                    subtitle = item.reason.ifBlank { "Sin motivo" },
                    tertiary = buildBlacklistTertiary(item)
                )
            }
    }

    val referralRows = remember(normalizedQuery) {
        sessionStore.readReferrals()
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.referredClient.contains(normalizedQuery, ignoreCase = true) ||
                    item.referredBy.contains(normalizedQuery, ignoreCase = true) ||
                    item.status.contains(normalizedQuery, ignoreCase = true) ||
                    item.notes.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
            .take(20)
            .map { item ->
                GlobalSearchRow(
                    id = "referral-${item.id}",
                    category = "Referido",
                    title = item.referredClient.ifBlank { "Sin cliente" },
                    subtitle = "Referido por: ${item.referredBy.ifBlank { "-" }}",
                    tertiary = "Estado: ${item.status.ifBlank { "-" }}"
                )
            }
    }

    val frequentUserRows = remember(normalizedQuery) {
        sessionStore.readFrequentUsers()
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.fullName.contains(normalizedQuery, ignoreCase = true) ||
                    item.idNumber.contains(normalizedQuery, ignoreCase = true) ||
                    item.phone.contains(normalizedQuery, ignoreCase = true) ||
                    item.bankName.contains(normalizedQuery, ignoreCase = true) ||
                    item.mobilePaymentPhone.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
            .take(20)
            .map { item ->
                GlobalSearchRow(
                    id = "frequent-${item.id}",
                    category = "Usuario frecuente",
                    title = item.fullName.ifBlank { "Sin nombre" },
                    subtitle = buildFrequentUserSubtitle(item),
                    tertiary = buildFrequentUserTertiary(item)
                )
            }
    }

    val allRows = remember(loanRows, blacklistRows, referralRows, frequentUserRows) {
        loanRows + blacklistRows + referralRows + frequentUserRows
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppTopBack(
                    title = "Buscador global",
                    onBack = { navController.popBackStack() }
                )
            }

            item {
                AppSectionCard {
                    Text(
                        text = "Buscar",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = normalizeTextInput(it) },
                        label = { Text("Nombre, cédula, teléfono, motivo, banco...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    AppMutedText("Busca en préstamos, lista negra, referidos y usuarios frecuentes.")
                }
            }

            item {
                AppSectionCard {
                    Text(
                        text = "Resultados",
                        style = MaterialTheme.typography.titleMedium
                    )
                    AppMutedText("Mostrando: ${allRows.size}")
                }
            }

            if (allRows.isEmpty()) {
                item {
                    AppSectionCard {
                        Text("No hay resultados para mostrar.")
                    }
                }
            } else {
                items(allRows, key = { it.id }) { row ->
                    AppSectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = row.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                AppMutedText(row.subtitle)
                                if (row.tertiary.isNotBlank()) {
                                    AppMutedText(row.tertiary)
                                }
                            }

                            AppStatusChip(row.category)
                        }

                        if (row.loanId.isNotBlank()) {
                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AppSecondaryButton(
                                    text = "Detalle",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        sessionStore.setActiveLoanId(row.loanId)
                                        navController.navigate(AppRoutes.LoanDetail) {
                                            launchSingleTop = true
                                        }
                                    }
                                )

                                AppSecondaryButton(
                                    text = "Cobro",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        sessionStore.setActiveLoanId(row.loanId)
                                        navController.navigate(AppRoutes.LoanCollectionNotice) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                AppBottomBack(onClick = { navController.popBackStack() })
            }
        }
    }
}

private fun buildLoanSearchSubtitle(loan: ManualLoanData): String {
    val parts = mutableListOf<String>()
    if (loan.idNumber.isNotBlank()) parts.add("Cédula: ${loan.idNumber}")
    if (loan.phone.isNotBlank()) parts.add("Teléfono: ${loan.phone}")
    parts.add("Estado: ${loan.status.ifBlank { "-" }}")
    return parts.joinToString(" · ")
}

private fun buildBlacklistTertiary(item: BlacklistRecordData): String {
    val parts = mutableListOf<String>()
    if (item.idNumber.isNotBlank()) parts.add("Cédula: ${item.idNumber}")
    if (item.phone.isNotBlank()) parts.add("Teléfono: ${item.phone}")
    return parts.joinToString(" · ")
}

private fun buildFrequentUserSubtitle(item: FrequentUserPaymentData): String {
    val parts = mutableListOf<String>()
    if (item.idNumber.isNotBlank()) parts.add("Cédula: ${item.idNumber}")
    if (item.phone.isNotBlank()) parts.add("Teléfono: ${item.phone}")
    return parts.joinToString(" · ")
}

private fun buildFrequentUserTertiary(item: FrequentUserPaymentData): String {
    val parts = mutableListOf<String>()
    if (item.bankName.isNotBlank()) parts.add("Banco: ${item.bankName}")
    if (item.mobilePaymentPhone.isNotBlank()) parts.add("Pago móvil: ${item.mobilePaymentPhone}")
    return parts.joinToString(" · ")
}
