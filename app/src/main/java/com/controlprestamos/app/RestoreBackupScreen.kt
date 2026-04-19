package com.controlprestamos.app

import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

private enum class RestoreMode {
    MERGE,
    REPLACE
}

private data class ImportedBackupData(
    val profile: UserProfileData? = null,
    val loans: List<ManualLoanData> = emptyList(),
    val payments: List<LoanPaymentRecordData> = emptyList(),
    val blacklist: List<BlacklistRecordData> = emptyList(),
    val referrals: List<ReferralRecordData> = emptyList(),
    val frequentUsers: List<FrequentUserPaymentData> = emptyList()
) {
    fun totalRecords(): Int {
        return (if (profile != null) 1 else 0) +
            loans.size +
            payments.size +
            blacklist.size +
            referrals.size +
            frequentUsers.size
    }
}

@Composable
fun RestoreBackupScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val context = LocalContext.current
    var feedback by remember { mutableStateOf("") }
    var pendingMode by remember { mutableStateOf(RestoreMode.MERGE) }

    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri == null) {
            feedback = "No seleccionaste ningún archivo."
            return@rememberLauncherForActivityResult
        }

        runCatching {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val imported = readImportedBackup(context, uri)
            val restoredCount = when (pendingMode) {
                RestoreMode.MERGE -> sessionStore.mergeImportedBackupData(
                    profile = imported.profile,
                    loans = imported.loans,
                    payments = imported.payments,
                    blacklist = imported.blacklist,
                    referrals = imported.referrals,
                    frequentUsers = imported.frequentUsers
                )
                RestoreMode.REPLACE -> sessionStore.replaceImportedBackupData(
                    profile = imported.profile,
                    loans = imported.loans,
                    payments = imported.payments,
                    blacklist = imported.blacklist,
                    referrals = imported.referrals,
                    frequentUsers = imported.frequentUsers
                )
            }

            feedback = when (pendingMode) {
                RestoreMode.MERGE ->
                    "Respaldo importado en modo fusión. Registros procesados: $restoredCount."
                RestoreMode.REPLACE ->
                    "Respaldo restaurado en modo reemplazo. Registros procesados: $restoredCount."
            }
        }.onFailure { error ->
            feedback = "No se pudo restaurar el respaldo: ${error.message ?: "archivo inválido o formato no soportado."}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Restaurar respaldo",
            onBack = { navController.popBackStack() }
        )

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppSectionCard {
            Text(
                text = "Qué hace esta restauración",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Importa un archivo ZIP generado por la propia app.")
            AppMutedText("Lee perfil, préstamos, pagos, lista negra, referidos y usuarios frecuentes.")
            AppMutedText("No restaura automáticamente la papelera ni archivos externos ajenos a este formato.")
        }

        AppSectionCard {
            Text(
                text = "Modo fusión",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Mezcla el contenido importado con lo que ya existe en el dispositivo.")
            AppMutedText("Si encuentra el mismo ID, prioriza el registro importado.")
            AppPrimaryButton(
                text = "Importar ZIP y fusionar",
                onClick = {
                    pendingMode = RestoreMode.MERGE
                    launcher.launch(arrayOf("application/zip", "application/octet-stream"))
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Modo reemplazo",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Sustituye la base operativa actual por el contenido del respaldo importado.")
            AppMutedText("Úsalo para migración entre teléfonos o restauración completa.")
            AppSecondaryButton(
                text = "Importar ZIP y reemplazar",
                onClick = {
                    pendingMode = RestoreMode.REPLACE
                    launcher.launch(arrayOf("application/zip", "application/octet-stream"))
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Recomendación",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Prueba primero el modo fusión si no estás completamente seguro de reemplazar la base actual.")
            AppMutedText("Haz una exportación nueva antes de restaurar, por si necesitas volver atrás.")
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

private fun readImportedBackup(
    context: Context,
    uri: Uri
): ImportedBackupData {
    val fileName = resolveFileName(uri).lowercase()
    require(fileName.endsWith(".zip")) {
        "Selecciona un ZIP generado por la app."
    }

    val entries = mutableMapOf<String, String>()

    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado.")

    input.use { stream ->
        ZipInputStream(stream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name.substringAfterLast('/').lowercase()
                    val output = ByteArrayOutputStream()
                    zip.copyTo(output)
                    entries[entryName] = output.toString(Charsets.UTF_8.name())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    if (entries.isEmpty()) {
        throw IllegalArgumentException("El ZIP está vacío o no se pudo leer.")
    }

    val imported = ImportedBackupData(
        profile = parseProfileCsv(entries["perfil.csv"].orEmpty()),
        loans = parseLoansCsv(entries["prestamos.csv"].orEmpty()),
        payments = parsePaymentsCsv(entries["pagos.csv"].orEmpty()),
        blacklist = parseBlacklistCsv(entries["lista_negra.csv"].orEmpty()),
        referrals = parseReferralsCsv(entries["referidos.csv"].orEmpty()),
        frequentUsers = parseFrequentUsersCsv(entries["usuarios_frecuentes.csv"].orEmpty())
    )

    require(imported.totalRecords() > 0) {
        "El ZIP no contiene datos reconocibles del formato de respaldo de la app."
    }

    return imported
}

private fun resolveFileName(uri: Uri): String {
    val raw = uri.lastPathSegment ?: return ""
    return raw.substringAfterLast('/')
}

private fun parseProfileCsv(csv: String): UserProfileData? {
    val rows = parseCsvRows(csv)
    if (rows.size <= 1) return null

    val map = rows.drop(1)
        .filter { it.size >= 2 }
        .associate { row -> row[0].trim().lowercase() to row[1] }

    if (map.isEmpty()) return null

    return UserProfileData(
        photoUri = "",
        name = map["nombres"].orEmpty(),
        lastName = map["apellidos"].orEmpty(),
        idNumber = map["cedula"].orEmpty(),
        phone = map["telefono_principal"].orEmpty(),
        communicationPhone = map["telefono_comunicacion"].orEmpty(),
        mobilePaymentPhone = map["pago_movil"].orEmpty(),
        bankName = map["banco"].orEmpty(),
        bankAccount = map["cuenta"].orEmpty(),
        personalizedMessage = map["mensaje_personalizado"].orEmpty()
    )
}

private fun parseLoansCsv(csv: String): List<ManualLoanData> {
    val rows = parseCsvRows(csv)
    if (rows.size <= 1) return emptyList()

    return rows.drop(1).mapNotNull { row ->
        if (row.isEmpty()) return@mapNotNull null
        ManualLoanData(
            id = csvValue(row, 0).ifBlank { UUID.randomUUID().toString() },
            fullName = csvValue(row, 1),
            idNumber = csvValue(row, 2),
            phone = csvValue(row, 3),
            loanAmount = csvValue(row, 4).toDoubleOrNull() ?: 0.0,
            percent = csvValue(row, 5).toDoubleOrNull() ?: 0.0,
            loanDate = csvValue(row, 10),
            dueDate = csvValue(row, 11),
            exchangeRate = csvValue(row, 12),
            conditions = csvValue(row, 14),
            paidAmount = csvValue(row, 8).toDoubleOrNull() ?: 0.0,
            status = csvValue(row, 13).ifBlank { "ACTIVO" },
            createdAt = csvValue(row, 15).toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}

private fun parsePaymentsCsv(csv: String): List<LoanPaymentRecordData> {
    val rows = parseCsvRows(csv)
    if (rows.size <= 1) return emptyList()

    return rows.drop(1).mapNotNull { row ->
        if (row.isEmpty()) return@mapNotNull null
        LoanPaymentRecordData(
            id = csvValue(row, 0).ifBlank { UUID.randomUUID().toString() },
            loanId = csvValue(row, 1),
            clientName = csvValue(row, 2),
            amount = csvValue(row, 3).toDoubleOrNull() ?: 0.0,
            paymentDate = csvValue(row, 4),
            paymentType = csvValue(row, 5).ifBlank { "ABONO" },
            previousPaidAmount = csvValue(row, 6).toDoubleOrNull() ?: 0.0,
            newPaidAmount = csvValue(row, 7).toDoubleOrNull() ?: 0.0,
            previousPendingAmount = csvValue(row, 8).toDoubleOrNull() ?: 0.0,
            newPendingAmount = csvValue(row, 9).toDoubleOrNull() ?: 0.0,
            note = csvValue(row, 10),
            createdAt = csvValue(row, 11).toLongOrNull() ?: System.currentTimeMillis()
        )
    }.filter { it.loanId.isNotBlank() }
}

private fun parseBlacklistCsv(csv: String): List<BlacklistRecordData> {
    val rows = parseCsvRows(csv)
    if (rows.size <= 1) return emptyList()

    return rows.drop(1).mapNotNull { row ->
        if (row.isEmpty()) return@mapNotNull null
        BlacklistRecordData(
            id = csvValue(row, 0).ifBlank { UUID.randomUUID().toString() },
            fullName = csvValue(row, 1),
            idNumber = csvValue(row, 2),
            phone = csvValue(row, 3),
            reason = csvValue(row, 4),
            notes = csvValue(row, 5),
            addedDate = csvValue(row, 6),
            createdAt = csvValue(row, 7).toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}

private fun parseReferralsCsv(csv: String): List<ReferralRecordData> {
    val rows = parseCsvRows(csv)
    if (rows.size <= 1) return emptyList()

    return rows.drop(1).mapNotNull { row ->
        if (row.isEmpty()) return@mapNotNull null
        ReferralRecordData(
            id = csvValue(row, 0).ifBlank { UUID.randomUUID().toString() },
            referralDate = csvValue(row, 1),
            referredClient = csvValue(row, 2),
            referredBy = csvValue(row, 3),
            loanAmount = csvValue(row, 4).toDoubleOrNull() ?: 0.0,
            commissionPercent = csvValue(row, 5).toDoubleOrNull() ?: 10.0,
            status = csvValue(row, 7).ifBlank { "PENDIENTE" },
            notes = csvValue(row, 8),
            createdAt = csvValue(row, 9).toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}

private fun parseFrequentUsersCsv(csv: String): List<FrequentUserPaymentData> {
    val rows = parseCsvRows(csv)
    if (rows.size <= 1) return emptyList()

    return rows.drop(1).mapNotNull { row ->
        if (row.isEmpty()) return@mapNotNull null
        FrequentUserPaymentData(
            id = csvValue(row, 0).ifBlank { UUID.randomUUID().toString() },
            fullName = csvValue(row, 1),
            idNumber = csvValue(row, 2),
            phone = csvValue(row, 3),
            bankName = csvValue(row, 4),
            bankAccount = csvValue(row, 5),
            mobilePaymentPhone = csvValue(row, 6),
            paymentAlias = csvValue(row, 7),
            notes = csvValue(row, 8),
            createdAt = csvValue(row, 9).toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}

private fun csvValue(row: List<String>, index: Int): String {
    return row.getOrElse(index) { "" }.trim()
}

private fun parseCsvRows(raw: String): List<List<String>> {
    if (raw.isBlank()) return emptyList()

    val rows = mutableListOf<MutableList<String>>()
    var currentRow = mutableListOf<String>()
    val currentCell = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < raw.length) {
        val char = raw[i]
        when {
            char == '"' -> {
                if (inQuotes && i + 1 < raw.length && raw[i + 1] == '"') {
                    currentCell.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            }

            char == ',' && !inQuotes -> {
                currentRow.add(currentCell.toString())
                currentCell.setLength(0)
            }

            (char == '\n' || char == '\r') && !inQuotes -> {
                if (char == '\r' && i + 1 < raw.length && raw[i + 1] == '\n') {
                    i++
                }
                currentRow.add(currentCell.toString())
                currentCell.setLength(0)
                rows.add(currentRow)
                currentRow = mutableListOf()
            }

            else -> currentCell.append(char)
        }
        i++
    }

    if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
        currentRow.add(currentCell.toString())
        rows.add(currentRow)
    }

    return rows.filterNot { row ->
        row.all { it.isBlank() }
    }
}
