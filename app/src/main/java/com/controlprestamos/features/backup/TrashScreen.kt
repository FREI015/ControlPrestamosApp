package com.controlprestamos.features.backup

import com.controlprestamos.features.security.*
import com.controlprestamos.features.settings.*
import com.controlprestamos.features.dashboard.*
import com.controlprestamos.features.people.*
import com.controlprestamos.features.search.*
import com.controlprestamos.features.more.*
import com.controlprestamos.core.validation.*
import com.controlprestamos.app.*
import com.controlprestamos.features.profile.*

import com.controlprestamos.core.format.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrashScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val retentionDays = sessionStore.trashRetentionDays()
    var deletedLoans by remember { mutableStateOf(sessionStore.readDeletedLoans()) }
    var pendingRestore by remember { mutableStateOf<DeletedLoanSnapshotData?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<DeletedLoanSnapshotData?>(null) }
    var feedback by remember { mutableStateOf("") }

    fun reload() {
        deletedLoans = sessionStore.readDeletedLoans()
    }

    AppConfirmDialog(
        visible = pendingRestore != null,
        title = "Restaurar préstamo",
        message = "Se restaurará el préstamo junto con su historial de pagos guardado. Si ya existe uno muy parecido activo, la restauración se bloqueará para evitar duplicados.",
        confirmText = "Restaurar",
        dismissText = "Cancelar",
        onConfirm = {
            val item = pendingRestore
            if (item != null) {
                val result = sessionStore.restoreDeletedLoanDetailed(item.trashId)
                feedback = result.message
                reload()
            }
            pendingRestore = null
        },
        onDismiss = { pendingRestore = null }
    )

    AppConfirmDialog(
        visible = pendingPermanentDelete != null,
        title = "Eliminar definitivamente",
        message = "Esta acción borrará de la papelera el préstamo y su historial de pagos guardado.",
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            val item = pendingPermanentDelete
            if (item != null) {
                val ok = sessionStore.permanentlyDeleteTrashedLoan(item.trashId)
                feedback = if (ok) "Préstamo eliminado definitivamente." else "No se pudo eliminar definitivamente."
                reload()
            }
            pendingPermanentDelete = null
        },
        onDismiss = { pendingPermanentDelete = null }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTopBack(
                title = "Papelera",
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

        item {
            AppSectionCard {
                Text(
                    text = "Resumen",
                    style = MaterialTheme.typography.titleMedium
                )
                AppMutedText("Aquí se guardan temporalmente los préstamos eliminados para que puedas restaurarlos.")
                AppMutedText("La retención actual es de $retentionDays días. Después de ese plazo, la app los depura automáticamente.")

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cardWidth = (maxWidth - 12.dp) / 2

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrashMetricCard("Préstamos en papelera", deletedLoans.size.toString(), Modifier.width(cardWidth))
                        TrashMetricCard(
                            "Pendiente total",
                            formatMoney(deletedLoans.sumOf { it.loan.pendingAmount() }),
                            Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }

        if (deletedLoans.isEmpty()) {
            item {
                AppSectionCard {
                    Text("La papelera está vacía.")
                }
            }
        } else {
            items(deletedLoans, key = { it.trashId }) { item ->
                val remainingDays = remainingTrashDays(item.deletedAt, retentionDays)
                AppSectionCard {
                    Text(
                        text = item.loan.fullName.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleMedium
                    )

                    AppMutedText("Eliminado: ${formatTrashDate(item.deletedAt)}")
                    AppMutedText("Se depura automáticamente: ${formatTrashExpiryDate(item.deletedAt, retentionDays)}")
                    AppMutedText(
                        if (remainingDays > 0) {
                            "Tiempo restante aproximado: $remainingDays día(s)"
                        } else {
                            "Este registro está en el límite de depuración."
                        }
                    )

                    if (item.loan.idNumber.isNotBlank()) AppMutedText("Cédula: ${item.loan.idNumber}")
                    if (item.loan.phone.isNotBlank()) AppMutedText("Teléfono: ${item.loan.phone}")
                    AppStatusChip("Eliminado")

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val cardWidth = (maxWidth - 12.dp) / 2

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrashInfoCard("Prestado", formatMoney(item.loan.loanAmount), Modifier.width(cardWidth))
                                TrashInfoCard("A cobrar", formatMoney(item.loan.totalAmount()), Modifier.width(cardWidth))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrashInfoCard("Pagado", formatMoney(item.loan.paidAmount), Modifier.width(cardWidth))
                                TrashInfoCard("Pendiente", formatMoney(item.loan.pendingAmount()), Modifier.width(cardWidth))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrashInfoCard("Pagos guardados", item.payments.size.toString(), Modifier.width(cardWidth))
                                TrashInfoCard("Estado", item.loan.status, Modifier.width(cardWidth))
                            }
                        }
                    }

                    AppSecondaryButton(
                        text = "Restaurar préstamo",
                        onClick = { pendingRestore = item }
                    )

                    AppDangerButton(
                        text = "Eliminar definitivamente",
                        onClick = { pendingPermanentDelete = item }
                    )
                }
            }
        }

        item {
            AppBottomBack(onClick = { navController.popBackStack() })
        }
    }
}

@Composable
private fun TrashMetricCard(
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
private fun TrashInfoCard(
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

private fun formatTrashDate(value: Long): String {
    return try {
        Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (_: Exception) {
        "Sin fecha"
    }
}

private fun formatTrashExpiryDate(value: Long, retentionDays: Int): String {
    return try {
        Instant.ofEpochMilli(value + (retentionDays.toLong() * 24L * 60L * 60L * 1000L))
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (_: Exception) {
        "Sin fecha"
    }
}

private fun remainingTrashDays(value: Long, retentionDays: Int): Long {
    val retentionMillis = retentionDays.toLong() * 24L * 60L * 60L * 1000L
    val remainingMillis = (value + retentionMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    return if (remainingMillis == 0L) 0L else ((remainingMillis + (24L * 60L * 60L * 1000L) - 1L) / (24L * 60L * 60L * 1000L))
}
