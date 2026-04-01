package com.controlprestamos.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val HISTORY_FILTER_TODOS = "TODOS"
private const val HISTORY_FILTER_PRESTAMOS = "PRESTAMOS"
private const val HISTORY_FILTER_PAGOS = "PAGOS"
private const val HISTORY_FILTER_LISTA_NEGRA = "LISTA_NEGRA"
private const val HISTORY_FILTER_REFERIDOS = "REFERIDOS"
private const val HISTORY_FILTER_USUARIOS = "USUARIOS"
private const val HISTORY_FILTER_PERFIL = "PERFIL"
private const val HISTORY_FILTER_SEGURIDAD = "SEGURIDAD"

@Composable
fun HistoryScreen(
    sessionStore: SessionStore,
    onBack: () -> Unit
) {
    val items = sessionStore.readOperationalHistory()
    var selectedFilter by remember { mutableStateOf(HISTORY_FILTER_TODOS) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = items.filter { item ->
        matchesHistoryFilter(item, selectedFilter) && matchesHistorySearch(item, searchQuery)
    }

    val summary = buildHistorySummary(items)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTopBack(title = "Resumen histórico", onBack = onBack)
        }

        item {
            AppSectionCard {
                Text("Movimientos operativos")
                AppMutedText("Aquí se muestran solo eventos útiles del sistema, ordenados del más reciente al más antiguo.")
            }
        }

        item {
            AppSectionCard {
                Text("Resumen", style = MaterialTheme.typography.titleMedium)
                AppMutedText("Total de eventos: ${items.size}")
                AppMutedText("Préstamos: ${summary.loans}")
                AppMutedText("Pagos: ${summary.payments}")
                AppMutedText("Lista negra: ${summary.blacklist}")
                AppMutedText("Referidos: ${summary.referrals}")
                AppMutedText("Usuarios frecuentes: ${summary.frequentUsers}")
                AppMutedText("Perfil: ${summary.profile}")
                AppMutedText("Seguridad: ${summary.security}")
            }
        }

        item {
            AppSectionCard {
                Text("Filtro y búsqueda", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = normalizeTextInput(it) },
                    label = { Text("Buscar en historial") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppFilterChip("Todos", selectedFilter == HISTORY_FILTER_TODOS) {
                        selectedFilter = HISTORY_FILTER_TODOS
                    }
                    AppFilterChip("Préstamos", selectedFilter == HISTORY_FILTER_PRESTAMOS) {
                        selectedFilter = HISTORY_FILTER_PRESTAMOS
                    }
                    AppFilterChip("Pagos", selectedFilter == HISTORY_FILTER_PAGOS) {
                        selectedFilter = HISTORY_FILTER_PAGOS
                    }
                    AppFilterChip("Lista negra", selectedFilter == HISTORY_FILTER_LISTA_NEGRA) {
                        selectedFilter = HISTORY_FILTER_LISTA_NEGRA
                    }
                    AppFilterChip("Referidos", selectedFilter == HISTORY_FILTER_REFERIDOS) {
                        selectedFilter = HISTORY_FILTER_REFERIDOS
                    }
                    AppFilterChip("Usuarios", selectedFilter == HISTORY_FILTER_USUARIOS) {
                        selectedFilter = HISTORY_FILTER_USUARIOS
                    }
                    AppFilterChip("Perfil", selectedFilter == HISTORY_FILTER_PERFIL) {
                        selectedFilter = HISTORY_FILTER_PERFIL
                    }
                    AppFilterChip("Seguridad", selectedFilter == HISTORY_FILTER_SEGURIDAD) {
                        selectedFilter = HISTORY_FILTER_SEGURIDAD
                    }
                }

                AppMutedText("Mostrando: ${filteredItems.size}")
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                AppSectionCard {
                    Text("No hay historial para mostrar.")
                    AppMutedText("Realiza operaciones en la app y aquí aparecerán registradas.")
                }
            }
        } else {
            items(filteredItems) { item ->
                val category = historyCategoryLabel(item)

                AppSectionCard {
                    AppStatusChip(category)
                    Text(item, style = MaterialTheme.typography.bodyLarge)
                    HorizontalDivider()
                    AppMutedText("Tipo: $category")
                }
            }
        }

        item {
            AppBottomBack(onClick = onBack)
        }
    }
}

private data class HistorySummary(
    val loans: Int = 0,
    val payments: Int = 0,
    val blacklist: Int = 0,
    val referrals: Int = 0,
    val frequentUsers: Int = 0,
    val profile: Int = 0,
    val security: Int = 0
)

private fun matchesHistorySearch(
    item: String,
    searchQuery: String
): Boolean {
    if (searchQuery.isBlank()) return true
    return item.contains(searchQuery.trim(), ignoreCase = true)
}

private fun matchesHistoryFilter(
    item: String,
    filter: String
): Boolean {
    return when (filter) {
        HISTORY_FILTER_PRESTAMOS -> classifyHistoryType(item) == HISTORY_FILTER_PRESTAMOS
        HISTORY_FILTER_PAGOS -> classifyHistoryType(item) == HISTORY_FILTER_PAGOS
        HISTORY_FILTER_LISTA_NEGRA -> classifyHistoryType(item) == HISTORY_FILTER_LISTA_NEGRA
        HISTORY_FILTER_REFERIDOS -> classifyHistoryType(item) == HISTORY_FILTER_REFERIDOS
        HISTORY_FILTER_USUARIOS -> classifyHistoryType(item) == HISTORY_FILTER_USUARIOS
        HISTORY_FILTER_PERFIL -> classifyHistoryType(item) == HISTORY_FILTER_PERFIL
        HISTORY_FILTER_SEGURIDAD -> classifyHistoryType(item) == HISTORY_FILTER_SEGURIDAD
        else -> true
    }
}

private fun classifyHistoryType(item: String): String {
    val normalized = item.uppercase()

    return when {
        normalized.contains("PAGO REGISTRADO") -> HISTORY_FILTER_PAGOS
        normalized.contains("PRÉSTAMO REGISTRADO") ||
            normalized.contains("PRÉSTAMO ACTUALIZADO") ||
            normalized.contains("PRÉSTAMO COBRADO") ||
            normalized.contains("PRÉSTAMO PERDIDO") ||
            normalized.contains("PRÉSTAMO ELIMINADO") -> HISTORY_FILTER_PRESTAMOS

        normalized.contains("LISTA NEGRA AGREGADA") ||
            normalized.contains("LISTA NEGRA ACTUALIZADA") ||
            normalized.contains("LISTA NEGRA ELIMINADA") -> HISTORY_FILTER_LISTA_NEGRA

        normalized.contains("REFERIDO GUARDADO") ||
            normalized.contains("REFERIDO ACTUALIZADO") ||
            normalized.contains("REFERIDO ELIMINADO") -> HISTORY_FILTER_REFERIDOS

        normalized.contains("USUARIO FRECUENTE GUARDADO") ||
            normalized.contains("USUARIO FRECUENTE ACTUALIZADO") ||
            normalized.contains("USUARIO FRECUENTE ELIMINADO") -> HISTORY_FILTER_USUARIOS

        normalized.contains("PERFIL ACTUALIZADO") -> HISTORY_FILTER_PERFIL

        normalized.contains("PIN INICIAL CREADO") ||
            normalized.contains("PIN ACTUALIZADO") ||
            normalized.contains("PIN ELIMINADO") ||
            normalized.contains("BLOQUEO AUTOMÁTICO") -> HISTORY_FILTER_SEGURIDAD

        else -> HISTORY_FILTER_TODOS
    }
}

private fun historyCategoryLabel(item: String): String {
    return when (classifyHistoryType(item)) {
        HISTORY_FILTER_PRESTAMOS -> "Préstamos"
        HISTORY_FILTER_PAGOS -> "Pagos"
        HISTORY_FILTER_LISTA_NEGRA -> "Lista negra"
        HISTORY_FILTER_REFERIDOS -> "Referidos"
        HISTORY_FILTER_USUARIOS -> "Usuarios frecuentes"
        HISTORY_FILTER_PERFIL -> "Perfil"
        HISTORY_FILTER_SEGURIDAD -> "Seguridad"
        else -> "General"
    }
}

private fun buildHistorySummary(items: List<String>): HistorySummary {
    return HistorySummary(
        loans = items.count { classifyHistoryType(it) == HISTORY_FILTER_PRESTAMOS },
        payments = items.count { classifyHistoryType(it) == HISTORY_FILTER_PAGOS },
        blacklist = items.count { classifyHistoryType(it) == HISTORY_FILTER_LISTA_NEGRA },
        referrals = items.count { classifyHistoryType(it) == HISTORY_FILTER_REFERIDOS },
        frequentUsers = items.count { classifyHistoryType(it) == HISTORY_FILTER_USUARIOS },
        profile = items.count { classifyHistoryType(it) == HISTORY_FILTER_PERFIL },
        security = items.count { classifyHistoryType(it) == HISTORY_FILTER_SEGURIDAD }
    )
}

