package com.controlprestamos.app

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun BackupExportScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val context = LocalContext.current

    val loans = remember { sessionStore.readLoans() }
    val payments = remember(loans) {
        loans.flatMap { sessionStore.readLoanPaymentHistory(it.id) }
            .sortedByDescending { it.createdAt }
    }
    val blacklist = remember { sessionStore.readBlacklist() }
    val referrals = remember { sessionStore.readReferrals() }
    val frequentUsers = remember { sessionStore.readFrequentUsers() }
    val profile = remember { sessionStore.readProfile() }

    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Respaldo y exportación",
            onBack = { navController.popBackStack() }
        )

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppSectionCard {
            Text(
                text = "Advertencia de privacidad",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Los respaldos y exportaciones pueden incluir nombres, teléfonos, cédulas, cuentas, montos, pagos e historial operativo.")
            AppMutedText("La aplicación no comparte estos archivos de forma automática. Solo se generan y comparten cuando el usuario lo decide manualmente.")
            AppMutedText("Compártelos únicamente con destinatarios autorizados y evita enviarlos por canales inseguros.")
        }

        AppSectionCard {
            Text(
                text = "Contenido disponible",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExportMetricCard("Préstamos", loans.size.toString(), Modifier.width(cardWidth))
                        ExportMetricCard("Pagos", payments.size.toString(), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExportMetricCard("Lista negra", blacklist.size.toString(), Modifier.width(cardWidth))
                        ExportMetricCard("Referidos", referrals.size.toString(), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExportMetricCard("Usuarios frecuentes", frequentUsers.size.toString(), Modifier.width(cardWidth))
                        ExportMetricCard(
                            "Perfil",
                            if (profile.name.isNotBlank() || profile.lastName.isNotBlank()) "Configurado" else "Básico",
                            Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Respaldo completo",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Genera un ZIP con la información principal de la aplicación.")

            AppPrimaryButton(
                text = "Compartir respaldo completo (.zip)",
                onClick = {
                    runCatching {
                        val file = buildCompleteBackupZip(context, sessionStore)
                        shareGeneratedFile(
                            context = context,
                            file = file,
                            mimeType = "application/zip",
                            chooserTitle = "Compartir respaldo completo"
                        )
                        feedback = "Respaldo completo generado correctamente."
                    }.onFailure {
                        feedback = "No se pudo generar el respaldo completo."
                    }
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Exportaciones rápidas",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Útil para abrir los datos en Excel o revisarlos fuera de la app.")

            AppSecondaryButton(
                text = "Compartir préstamos (.csv)",
                onClick = {
                    runCatching {
                        val file = buildSingleCsvFile(
                            context = context,
                            fileNamePrefix = "prestamos",
                            csvContent = buildLoansCsv(sessionStore.readLoans())
                        )
                        shareGeneratedFile(
                            context = context,
                            file = file,
                            mimeType = "text/csv",
                            chooserTitle = "Compartir préstamos"
                        )
                        feedback = "Archivo de préstamos generado correctamente."
                    }.onFailure {
                        feedback = "No se pudo generar el archivo de préstamos."
                    }
                }
            )

            AppSecondaryButton(
                text = "Compartir pagos (.csv)",
                onClick = {
                    runCatching {
                        val file = buildSingleCsvFile(
                            context = context,
                            fileNamePrefix = "pagos",
                            csvContent = buildPaymentsCsv(
                                sessionStore.readLoans().flatMap { sessionStore.readLoanPaymentHistory(it.id) }
                            )
                        )
                        shareGeneratedFile(
                            context = context,
                            file = file,
                            mimeType = "text/csv",
                            chooserTitle = "Compartir pagos"
                        )
                        feedback = "Archivo de pagos generado correctamente."
                    }.onFailure {
                        feedback = "No se pudo generar el archivo de pagos."
                    }
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Recomendación",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Haz un respaldo completo antes de cambios grandes o antes de cambiar de teléfono.")
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun ExportMetricCard(
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun buildCompleteBackupZip(
    context: Context,
    sessionStore: SessionStore
): File {
    val loans = sessionStore.readLoans()
    val payments = loans.flatMap { sessionStore.readLoanPaymentHistory(it.id) }.sortedByDescending { it.createdAt }
    val blacklist = sessionStore.readBlacklist()
    val referrals = sessionStore.readReferrals()
    val frequentUsers = sessionStore.readFrequentUsers()
    val profile = sessionStore.readProfile()

    val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val zipFile = File(exportDir, "respaldo_controlprestamos_$timestamp.zip")

    ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
        writeZipEntry(zip, "perfil.csv", buildProfileCsv(profile))
        writeZipEntry(zip, "prestamos.csv", buildLoansCsv(loans))
        writeZipEntry(zip, "pagos.csv", buildPaymentsCsv(payments))
        writeZipEntry(zip, "lista_negra.csv", buildBlacklistCsv(blacklist))
        writeZipEntry(zip, "referidos.csv", buildReferralsCsv(referrals))
        writeZipEntry(zip, "usuarios_frecuentes.csv", buildFrequentUsersCsv(frequentUsers))
        writeZipEntry(zip, "resumen.txt", buildBackupSummaryText(profile, loans, payments, blacklist, referrals, frequentUsers))
    }

    return zipFile
}

private fun buildSingleCsvFile(
    context: Context,
    fileNamePrefix: String,
    csvContent: String
): File {
    val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val file = File(exportDir, "${fileNamePrefix}_$timestamp.csv")
    file.writeText(csvContent)
    return file
}

private fun writeZipEntry(
    zip: ZipOutputStream,
    entryName: String,
    content: String
) {
    val entry = ZipEntry(entryName)
    zip.putNextEntry(entry)
    zip.write(content.toByteArray())
    zip.closeEntry()
}

private fun shareGeneratedFile(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String
) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun buildProfileCsv(profile: UserProfileData): String {
    val rows = listOf(
        listOf("nombres", profile.name),
        listOf("apellidos", profile.lastName),
        listOf("cedula", profile.idNumber),
        listOf("telefono_principal", profile.phone),
        listOf("telefono_comunicacion", profile.communicationPhone),
        listOf("pago_movil", profile.mobilePaymentPhone),
        listOf("banco", profile.bankName),
        listOf("cuenta", profile.bankAccount),
        listOf("mensaje_personalizado", profile.personalizedMessage)
    )

    return buildString {
        appendLine("campo,valor")
        rows.forEach { row ->
            appendLine("${csvCell(row[0])},${csvCell(row[1])}")
        }
    }
}

private fun buildLoansCsv(loans: List<ManualLoanData>): String {
    return buildString {
        appendLine("id,nombre,cedula,telefono,monto,porcentaje,interes,total,pagado,pendiente,fecha_prestamo,fecha_vencimiento,tasa,estado,condiciones,creado_en")
        loans.forEach { loan ->
            appendLine(
                listOf(
                    loan.id,
                    loan.fullName,
                    loan.idNumber,
                    loan.phone,
                    formatPlainNumber(loan.loanAmount),
                    formatPlainNumber(loan.percent),
                    formatPlainNumber(loan.interestAmount()),
                    formatPlainNumber(loan.totalAmount()),
                    formatPlainNumber(loan.paidAmount),
                    formatPlainNumber(loan.pendingAmount()),
                    loan.loanDate,
                    loan.dueDate,
                    loan.exchangeRate,
                    loan.status,
                    loan.conditions,
                    loan.createdAt.toString()
                ).joinToString(",") { csvCell(it) }
            )
        }
    }
}

private fun buildPaymentsCsv(payments: List<LoanPaymentRecordData>): String {
    return buildString {
        appendLine("id,prestamo_id,cliente,monto,fecha_pago,tipo,pagado_antes,pagado_despues,pendiente_antes,pendiente_despues,nota,creado_en")
        payments.forEach { payment ->
            appendLine(
                listOf(
                    payment.id,
                    payment.loanId,
                    payment.clientName,
                    formatPlainNumber(payment.amount),
                    payment.paymentDate,
                    payment.paymentType,
                    formatPlainNumber(payment.previousPaidAmount),
                    formatPlainNumber(payment.newPaidAmount),
                    formatPlainNumber(payment.previousPendingAmount),
                    formatPlainNumber(payment.newPendingAmount),
                    payment.note,
                    payment.createdAt.toString()
                ).joinToString(",") { csvCell(it) }
            )
        }
    }
}

private fun buildBlacklistCsv(records: List<BlacklistRecordData>): String {
    return buildString {
        appendLine("id,nombre,cedula,telefono,motivo,notas,fecha_agregado,creado_en")
        records.forEach { item ->
            appendLine(
                listOf(
                    item.id,
                    item.fullName,
                    item.idNumber,
                    item.phone,
                    item.reason,
                    item.notes,
                    item.addedDate,
                    item.createdAt.toString()
                ).joinToString(",") { csvCell(it) }
            )
        }
    }
}

private fun buildReferralsCsv(records: List<ReferralRecordData>): String {
    return buildString {
        appendLine("id,fecha,cliente_referido,referido_por,monto,porcentaje_comision,monto_comision,estado,notas,creado_en")
        records.forEach { item ->
            appendLine(
                listOf(
                    item.id,
                    item.referralDate,
                    item.referredClient,
                    item.referredBy,
                    formatPlainNumber(item.loanAmount),
                    formatPlainNumber(item.commissionPercent),
                    formatPlainNumber(item.commissionAmount()),
                    item.status,
                    item.notes,
                    item.createdAt.toString()
                ).joinToString(",") { csvCell(it) }
            )
        }
    }
}

private fun buildFrequentUsersCsv(records: List<FrequentUserPaymentData>): String {
    return buildString {
        appendLine("id,nombre,cedula,telefono,banco,cuenta,pago_movil,alias,notas,creado_en")
        records.forEach { item ->
            appendLine(
                listOf(
                    item.id,
                    item.fullName,
                    item.idNumber,
                    item.phone,
                    item.bankName,
                    item.bankAccount,
                    item.mobilePaymentPhone,
                    item.paymentAlias,
                    item.notes,
                    item.createdAt.toString()
                ).joinToString(",") { csvCell(it) }
            )
        }
    }
}

private fun buildBackupSummaryText(
    profile: UserProfileData,
    loans: List<ManualLoanData>,
    payments: List<LoanPaymentRecordData>,
    blacklist: List<BlacklistRecordData>,
    referrals: List<ReferralRecordData>,
    frequentUsers: List<FrequentUserPaymentData>
): String {
    val owner = listOf(profile.name, profile.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "No configurado" }

    return buildString {
        appendLine("Control Prestamos - Respaldo")
        appendLine("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        appendLine("Perfil: $owner")
        appendLine("Prestamos: ${loans.size}")
        appendLine("Pagos: ${payments.size}")
        appendLine("Lista negra: ${blacklist.size}")
        appendLine("Referidos: ${referrals.size}")
        appendLine("Usuarios frecuentes: ${frequentUsers.size}")
    }
}

private fun csvCell(value: String): String {
    return "\"" + value.replace("\"", "\"\"") + "\""
}

private fun formatPlainNumber(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

