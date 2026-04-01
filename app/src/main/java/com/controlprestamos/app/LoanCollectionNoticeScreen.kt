package com.controlprestamos.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.Locale

@Composable
fun LoanCollectionNoticeScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val context = LocalContext.current
    val loan = sessionStore.readActiveLoan()
    val profile = sessionStore.readProfile()

    if (loan == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppTopBack(
                title = "Aviso de cobro",
                onBack = { navController.popBackStack() }
            )

            AppSectionCard {
                Text("No hay un préstamo seleccionado.")
                AppMutedText("Primero entra en un préstamo y luego abre esta pantalla.")
            }

            AppBottomBack(onClick = { navController.popBackStack() })
        }
        return
    }

    val missingProfileData = remember(profile) { buildMissingCollectionProfileFields(profile) }

    var messageText by remember(
        loan.id,
        loan.loanAmount,
        loan.percent,
        loan.loanDate,
        loan.dueDate,
        loan.paidAmount,
        loan.status,
        loan.conditions,
        loan.exchangeRate,
        profile.name,
        profile.lastName,
        profile.idNumber,
        profile.phone,
        profile.communicationPhone,
        profile.mobilePaymentPhone,
        profile.bankName,
        profile.bankAccount,
        profile.personalizedMessage
    ) {
        mutableStateOf(buildCollectionMessage(loan, profile))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Aviso de cobro",
            onBack = { navController.popBackStack() }
        )

        if (missingProfileData.isNotEmpty()) {
            AppSectionCard {
                Text("Perfil incompleto")
                AppMutedText("Faltan datos del perfil para un cobro más profesional: ${missingProfileData.joinToString(", ")}")
                AppSecondaryButton(
                    text = "Completar perfil",
                    onClick = { navController.navigate("editProfile") }
                )
            }
        }

        AppSectionCard {
            Text("Resumen del préstamo")
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailLine("Cliente", loan.fullName.ifBlank { "-" })
                DetailLine("Estado", loanCollectionStatusLabel(loan))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailLine("Prestado", formatMoneyCollection(loan.loanAmount))
                DetailLine("Ganancia", formatMoneyCollection(loan.interestAmount()))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailLine("Total", formatMoneyCollection(loan.totalAmount()))
                DetailLine("Abonado", formatMoneyCollection(loan.paidAmount))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailLine("Pendiente", formatMoneyCollection(loan.pendingAmount()))
                DetailLine("Vence", loan.dueDate.ifBlank { "-" })
            }

            if (loan.exchangeRate.isNotBlank()) {
                AppMutedText("Tasa del día: ${loan.exchangeRate}")
            }

            if (loan.conditions.isNotBlank()) {
                HorizontalDivider()
                AppMutedText("Observaciones: ${loan.conditions}")
            }
        }

        AppSectionCard {
            Text("Datos de pago")
            HorizontalDivider()

            AppMutedText("Titular: ${paymentOwner(profile)}")
            AppMutedText("Cédula: ${profile.idNumber.ifBlank { "No configurada" }}")
            AppMutedText("Teléfono de comunicación: ${profile.communicationPhone.ifBlank { profile.phone.ifBlank { "No configurado" } }}")
            AppMutedText("Pago móvil: ${profile.mobilePaymentPhone.ifBlank { "No configurado" }}")
            AppMutedText("Banco: ${profile.bankName.ifBlank { "No configurado" }}")
            AppMutedText("Cuenta: ${profile.bankAccount.ifBlank { "No configurada" }}")
        }

        AppSectionCard {
            Text("Mensaje de cobro")
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Texto del mensaje") }
            )

            AppSecondaryButton(
                text = "Restablecer mensaje",
                onClick = { messageText = buildCollectionMessage(loan, profile) }
            )
        }

        AppPrimaryButton(
            text = "Copiar mensaje",
            onClick = {
                copyToClipboard(context, "Aviso de cobro", messageText)
            }
        )

        AppSecondaryButton(
            text = "Compartir mensaje",
            onClick = {
                shareGeneric(context, "Aviso de cobro", messageText)
            }
        )

        AppSecondaryButton(
            text = "Enviar por WhatsApp",
            onClick = {
                shareToSpecificApp(
                    context = context,
                    packageName = "com.whatsapp",
                    fallbackTitle = "Compartir por WhatsApp",
                    text = messageText
                )
            }
        )

        AppSecondaryButton(
            text = "Enviar por Telegram",
            onClick = {
                shareToSpecificApp(
                    context = context,
                    packageName = "org.telegram.messenger",
                    fallbackTitle = "Compartir por Telegram",
                    text = messageText
                )
            }
        )

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun DetailLine(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.45f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppMutedText(title)
        Text(value)
    }
}

private fun buildCollectionMessage(
    loan: ManualLoanData,
    profile: UserProfileData
): String {
    val baseMessage = profile.personalizedMessage.ifBlank {
        "Hola, buenos días. Le escribo por el vencimiento de su préstamo. A continuación le comparto los datos de pago. Muchas gracias."
    }

    val titular = paymentOwner(profile)
    val cedula = profile.idNumber.ifBlank { "No configurada" }
    val communicationPhone = profile.communicationPhone.ifBlank { profile.phone.ifBlank { "No configurado" } }
    val paymentPhone = profile.mobilePaymentPhone.ifBlank { "No configurado" }
    val banco = profile.bankName.ifBlank { "No configurado" }
    val cuenta = profile.bankAccount.ifBlank { "No configurada" }

    val condiciones = if (loan.conditions.isNotBlank()) {
        "\nObservaciones: ${loan.conditions}"
    } else {
        ""
    }

    return """
$baseMessage

Cliente: ${loan.fullName}
Estado: ${loanCollectionStatusLabel(loan)}
Monto prestado: ${formatMoneyCollection(loan.loanAmount)}
Ganancia: ${formatMoneyCollection(loan.interestAmount())}
Total a pagar: ${formatMoneyCollection(loan.totalAmount())}
Abonado: ${formatMoneyCollection(loan.paidAmount)}
Pendiente: ${formatMoneyCollection(loan.pendingAmount())}
Fecha del préstamo: ${loan.loanDate}
Fecha de vencimiento: ${loan.dueDate}$condiciones

Datos de pago:
Titular: $titular
Cédula: $cedula
Teléfono de comunicación: $communicationPhone
Pago móvil: $paymentPhone
Banco: $banco
Cuenta: $cuenta
""".trimIndent()
}

private fun paymentOwner(profile: UserProfileData): String {
    return listOf(profile.name, profile.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "No configurado" }
}

private fun buildMissingCollectionProfileFields(profile: UserProfileData): List<String> {
    val result = mutableListOf<String>()
    if (profile.name.isBlank()) result.add("nombre")
    if (profile.lastName.isBlank()) result.add("apellido")
    if (profile.idNumber.isBlank()) result.add("cédula")
    if (profile.communicationPhone.isBlank() && profile.phone.isBlank()) result.add("teléfono de comunicación")
    if (profile.mobilePaymentPhone.isBlank()) result.add("pago móvil")
    if (profile.bankName.isBlank()) result.add("banco")
    if (profile.bankAccount.isBlank()) result.add("cuenta")
    return result
}

private fun loanCollectionStatusLabel(loan: ManualLoanData): String {
    return when {
        loan.status == "COBRADO" -> "COBRADO"
        loan.status == "PERDIDO" -> "PERDIDO"
        loan.isOverdue() -> "VENCIDO"
        else -> "ACTIVO"
    }
}

private fun copyToClipboard(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Mensaje copiado", Toast.LENGTH_SHORT).show()
}

private fun shareGeneric(
    context: Context,
    title: String,
    text: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    context.startActivity(Intent.createChooser(intent, title))
}

private fun shareToSpecificApp(
    context: Context,
    packageName: String,
    fallbackTitle: String,
    text: String
) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(packageName)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "La app no está disponible. Se abrirá el menú de compartir.",
            Toast.LENGTH_SHORT
        ).show()

        shareGeneric(context, fallbackTitle, text)
    }
}

private fun formatMoneyCollection(value: Double): String {
    return "$" + String.format(Locale.US, "%.2f", value)
}

