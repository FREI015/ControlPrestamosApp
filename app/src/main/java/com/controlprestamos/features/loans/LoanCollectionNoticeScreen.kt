package com.controlprestamos.features.loans

import com.controlprestamos.features.profile.*

import com.controlprestamos.app.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
                title = "Cobro",
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

    val missingProfileData = remember(profile) { buildMissingCollectionProfileFieldsRefined(profile) }

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
        mutableStateOf(buildCollectionMessageRefined(loan, profile))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Cobro",
            onBack = { navController.popBackStack() }
        )

        if (missingProfileData.isNotEmpty()) {
            AppSectionCard {
                Text("Perfil incompleto")
                AppMutedText("Faltan datos del perfil para un cobro más profesional: ${missingProfileData.joinToString(", ")}")
                AppSecondaryButton(
                    text = "Completar perfil",
                    onClick = { navController.navigate(AppRoutes.EditProfile) }
                )
            }
        }

        AppSectionCard {
            Text("Resumen de cobro", style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionInfoCardRefined("Cliente", loan.fullName.ifBlank { "-" }, Modifier.width(cardWidth))
                        CollectionInfoCardRefined("Estado", loanCollectionStatusLabelRefined(loan), Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionInfoCardRefined("Pendiente", formatMoneyCollectionRefined(loan.pendingAmount()), Modifier.width(cardWidth))
                        CollectionInfoCardRefined("Vence", loan.dueDate.ifBlank { "-" }, Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionInfoCardRefined("Prestado", formatMoneyCollectionRefined(loan.loanAmount), Modifier.width(cardWidth))
                        CollectionInfoCardRefined("Total", formatMoneyCollectionRefined(loan.totalAmount()), Modifier.width(cardWidth))
                    }

                    if (loan.conditions.isNotBlank()) {
                        CollectionInfoCardRefined("Observaciones", loan.conditions, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        AppSectionCard {
            Text("Datos de pago", style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionInfoCardRefined("Titular", paymentOwnerRefined(profile), Modifier.width(cardWidth))
                        CollectionInfoCardRefined("Cédula", profile.idNumber.ifBlank { "No configurada" }, Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionInfoCardRefined(
                            "Teléfono",
                            profile.communicationPhone.ifBlank { profile.phone.ifBlank { "No configurado" } },
                            Modifier.width(cardWidth)
                        )
                        CollectionInfoCardRefined(
                            "Pago móvil",
                            profile.mobilePaymentPhone.ifBlank { "No configurado" },
                            Modifier.width(cardWidth)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionInfoCardRefined("Banco", profile.bankName.ifBlank { "No configurado" }, Modifier.width(cardWidth))
                        CollectionInfoCardRefined("Cuenta", profile.bankAccount.ifBlank { "No configurada" }, Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text("Mensaje de cobro", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Texto del mensaje") }
            )

            AppSecondaryButton(
                text = "Restablecer mensaje",
                onClick = { messageText = buildCollectionMessageRefined(loan, profile) }
            )
        }

        AppSectionCard {
            Text("Acciones", style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionActionCardRefined("Copiar", Modifier.width(cardWidth)) {
                            copyToClipboardRefined(context, "Aviso de cobro", messageText)
                        }
                        CollectionActionCardRefined("Compartir", Modifier.width(cardWidth)) {
                            shareGenericRefined(context, "Aviso de cobro", messageText)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionActionCardRefined("WhatsApp", Modifier.width(cardWidth)) {
                            shareToSpecificAppRefined(
                                context = context,
                                packageName = "com.whatsapp",
                                fallbackTitle = "Compartir por WhatsApp",
                                text = messageText
                            )
                        }
                        CollectionActionCardRefined("Telegram", Modifier.width(cardWidth)) {
                            shareToSpecificAppRefined(
                                context = context,
                                packageName = "org.telegram.messenger",
                                fallbackTitle = "Compartir por Telegram",
                                text = messageText
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionActionCardRefined("Instagram", Modifier.width(cardWidth)) {
                            shareToSpecificAppRefined(
                                context = context,
                                packageName = "com.instagram.android",
                                fallbackTitle = "Compartir por Instagram",
                                text = messageText
                            )
                        }
                        CollectionActionCardRefined("Messenger", Modifier.width(cardWidth)) {
                            shareToSpecificAppRefined(
                                context = context,
                                packageName = "com.facebook.orca",
                                fallbackTitle = "Compartir por Messenger",
                                text = messageText
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CollectionActionCardRefined("Correo", Modifier.width(cardWidth)) {
                            shareToSpecificAppRefined(
                                context = context,
                                packageName = "com.google.android.gm",
                                fallbackTitle = "Compartir por correo",
                                text = messageText
                            )
                        }
                        CollectionActionCardRefined("SMS", Modifier.width(cardWidth)) {
                            shareSmsRefined(context, messageText)
                        }
                    }
                }
            }
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun CollectionInfoCardRefined(
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
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CollectionActionCardRefined(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.field
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun buildCollectionMessageRefined(
    loan: ManualLoanData,
    profile: UserProfileData
): String {
    val baseMessage = profile.personalizedMessage.ifBlank {
        "Hola, buenos días. Le escribo por el vencimiento de su préstamo. A continuación le comparto los datos de pago. Muchas gracias."
    }

    val titular = paymentOwnerRefined(profile)
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
Estado: ${loanCollectionStatusLabelRefined(loan)}
Monto prestado: ${formatMoneyCollectionRefined(loan.loanAmount)}
Ganancia: ${formatMoneyCollectionRefined(loan.interestAmount())}
Total a pagar: ${formatMoneyCollectionRefined(loan.totalAmount())}
Abonado: ${formatMoneyCollectionRefined(loan.paidAmount)}
Pendiente: ${formatMoneyCollectionRefined(loan.pendingAmount())}
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

private fun paymentOwnerRefined(profile: UserProfileData): String {
    return listOf(profile.name, profile.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "No configurado" }
}

private fun buildMissingCollectionProfileFieldsRefined(profile: UserProfileData): List<String> {
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

private fun loanCollectionStatusLabelRefined(loan: ManualLoanData): String {
    return when {
        loan.status == "COBRADO" -> "COBRADO"
        loan.status == "PERDIDO" -> "PERDIDO"
        loan.isOverdue() -> "VENCIDO"
        else -> "ACTIVO"
    }
}

private fun copyToClipboardRefined(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Mensaje copiado", Toast.LENGTH_SHORT).show()
}

private fun shareGenericRefined(
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

private fun shareToSpecificAppRefined(
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

        shareGenericRefined(context, fallbackTitle, text)
    }
}

private fun shareSmsRefined(
    context: Context,
    text: String
) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:")
            putExtra("sms_body", text)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        shareGenericRefined(context, "Compartir por SMS", text)
    }
}

private fun formatMoneyCollectionRefined(value: Double): String {
    return "$" + String.format(Locale.US, "%.2f", value)
}
