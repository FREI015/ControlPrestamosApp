package com.controlprestamos.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun EditLoanScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val loan = sessionStore.readActiveLoan()

    if (loan == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppTopBack(title = "Editar préstamo", onBack = { navController.popBackStack() })
            AppSectionCard { Text("No hay un préstamo seleccionado.") }
            AppBottomBack(onClick = { navController.popBackStack() })
        }
        return
    }

    val frequentUsers = remember { sessionStore.readFrequentUsers() }
    val paymentHistory = remember(loan.id) { sessionStore.readLoanPaymentHistory(loan.id) }

    var frequentSearch by remember { mutableStateOf("") }

    var fullName by remember { mutableStateOf(loan.fullName) }
    var idNumber by remember { mutableStateOf(loan.idNumber) }
    var phone by remember { mutableStateOf(loan.phone) }
    var loanAmount by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", loan.loanAmount)) }
    var percent by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", loan.percent)) }
    var loanDate by remember { mutableStateOf(loan.loanDate) }
    var dueDate by remember { mutableStateOf(loan.dueDate) }
    var exchangeRate by remember { mutableStateOf(loan.exchangeRate) }
    var conditions by remember { mutableStateOf(loan.conditions) }

    var errorMessage by remember { mutableStateOf("") }

    val filteredFrequentUsers = frequentUsers.filter { user ->
        frequentSearch.isBlank() ||
            user.fullName.contains(frequentSearch, ignoreCase = true) ||
            user.idNumber.contains(frequentSearch, ignoreCase = true) ||
            user.phone.contains(frequentSearch, ignoreCase = true)
    }

    val parsedAmount = parseMoneyOrNull(loanAmount) ?: 0.0
    val parsedPercent = parsePercentOrNull(percent) ?: 0.0
    val projectedInterest = parsedAmount * (parsedPercent / 100.0)
    val projectedTotal = parsedAmount + projectedInterest

    fun applyFrequentUser(user: FrequentUserPaymentData) {
        fullName = user.fullName
        idNumber = user.idNumber
        phone = user.phone

        val paymentSummary = buildFrequentUserPaymentSummary(user)
        if (paymentSummary.isNotBlank()) {
            conditions = injectFrequentPaymentReference(
                currentConditions = conditions,
                paymentSummary = paymentSummary
            )
        }

        errorMessage = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Editar préstamo",
            onBack = { navController.popBackStack() }
        )

        if (errorMessage.isNotBlank()) {
            AppSectionCard {
                Text(errorMessage)
            }
        }

        if (paymentHistory.isNotEmpty() || loan.paidAmount > 0.0) {
            AppSectionCard {
                Text("Abonos registrados")
                AppMutedText("Pagado acumulado: ${formatMoney(loan.paidAmount)}")
                AppMutedText("Pendiente actual: ${formatMoney(loan.pendingAmount())}")
                AppMutedText("Movimientos de pago: ${paymentHistory.size}")
                AppMutedText("Regla aplicada: puedes editar monto, porcentaje o fechas, pero el nuevo total no puede quedar por debajo de lo ya abonado.")
            }
        }

        AppSectionCard {
            Text("Usuarios frecuentes")

            if (frequentUsers.isEmpty()) {
                AppMutedText("No tienes usuarios frecuentes guardados todavía.")
            } else {
                OutlinedTextField(
                    value = frequentSearch,
                    onValueChange = {
                        frequentSearch = normalizeTextInput(it)
                    },
                    label = { Text("Buscar usuario frecuente") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                AppMutedText("Puedes reemplazar datos del cliente y reutilizar referencias de pago.")

                filteredFrequentUsers.take(8).forEachIndexed { index, user ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    Text(user.fullName.ifBlank { "Sin nombre" })
                    if (user.idNumber.isNotBlank()) AppMutedText("Cédula: ${user.idNumber}")
                    if (user.phone.isNotBlank()) AppMutedText("Teléfono: ${user.phone}")

                    val paymentSummary = buildFrequentUserPaymentSummary(user)
                    if (paymentSummary.isNotBlank()) {
                        AppMutedText(paymentSummary)
                    }

                    AppSecondaryButton(
                        text = "Usar este usuario",
                        onClick = { applyFrequentUser(user) }
                    )
                }

                if (filteredFrequentUsers.isEmpty()) {
                    AppMutedText("No hay resultados con esa búsqueda.")
                }

                if (frequentUsers.size > 8 && filteredFrequentUsers.size > 8) {
                    AppMutedText("Aquí solo se muestran los primeros 8 resultados.")
                }
            }
        }

        AppSectionCard {
            Text("Datos del cliente")

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = normalizeTextInput(it)
                    errorMessage = ""
                },
                label = { Text("Nombre completo *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = idNumber,
                onValueChange = {
                    idNumber = sanitizeIntegerInput(it)
                    errorMessage = ""
                },
                label = { Text("Cédula") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = sanitizePhoneInput(it)
                    errorMessage = ""
                },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        AppSectionCard {
            Text("Datos del préstamo")

            OutlinedTextField(
                value = loanAmount,
                onValueChange = {
                    loanAmount = sanitizeDecimalInput(it)
                    errorMessage = ""
                },
                label = { Text("Monto prestado USD *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = percent,
                onValueChange = {
                    percent = sanitizeDecimalInput(it)
                    errorMessage = ""
                },
                label = { Text("Porcentaje fijo *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AppDateField(
                value = loanDate,
                label = "Fecha del préstamo *",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = {
                    loanDate = it
                    errorMessage = ""
                }
            )

            AppDateField(
                value = dueDate,
                label = "Fecha de vencimiento *",
                modifier = Modifier.fillMaxWidth(),
                onDateSelected = {
                    dueDate = it
                    errorMessage = ""
                }
            )

            OutlinedTextField(
                value = exchangeRate,
                onValueChange = {
                    exchangeRate = sanitizeDecimalInput(it)
                },
                label = { Text("Tasa del día en bolívares") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = conditions,
                onValueChange = { conditions = it },
                label = { Text("Condiciones / observaciones") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AppSectionCard {
            Text("Resumen recalculado")
            AppMutedText("Interés proyectado: ${formatMoney(projectedInterest)}")
            AppMutedText("Nuevo total a cobrar: ${formatMoney(projectedTotal)}")
            AppMutedText("Ya abonado: ${formatMoney(loan.paidAmount)}")
            AppMutedText("Pendiente recalculado: ${formatMoney((projectedTotal - loan.paidAmount).coerceAtLeast(0.0))}")
        }

        AppPrimaryButton(
            text = "Guardar cambios",
            onClick = {
                val cleanName = normalizeTextInput(fullName).trim()
                val cleanIdNumber = sanitizeIntegerInput(idNumber)
                val cleanPhone = sanitizePhoneInput(phone)
                val cleanExchangeRate = sanitizeDecimalInput(exchangeRate)
                val cleanConditions = normalizeTextInput(conditions).trim()

                val amount = parseMoneyOrNull(loanAmount)
                val percentage = parsePercentOrNull(percent)
                val loanLocalDate = parseIsoDateOrNull(loanDate)
                val dueLocalDate = parseIsoDateOrNull(dueDate)

                val validationMessage = validateLoanForm(
                    clientName = cleanName,
                    idNumber = cleanIdNumber,
                    phone = cleanPhone,
                    amount = amount,
                    percentage = percentage,
                    loanDate = loanDate,
                    dueDate = dueDate,
                    exchangeRate = cleanExchangeRate,
                    conditions = cleanConditions,
                    existingPaidAmount = loan.paidAmount
                )

                when {
                    validationMessage != null -> {
                        errorMessage = validationMessage
                    }

                    loanLocalDate == null || dueLocalDate == null || amount == null || percentage == null -> {
                        errorMessage = "No se pudo procesar correctamente el formulario."
                    }

                    else -> {
                        sessionStore.saveLoan(
                            loan.copy(
                                fullName = cleanName,
                                idNumber = cleanIdNumber,
                                phone = cleanPhone,
                                loanAmount = amount,
                                percent = percentage,
                                loanDate = loanLocalDate.toString(),
                                dueDate = dueLocalDate.toString(),
                                exchangeRate = cleanExchangeRate,
                                conditions = cleanConditions
                            )
                        )

                        navController.navigate("loans") {
                            popUpTo("loans") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        )

        AppBottomBack(
            onClick = { navController.popBackStack() }
        )
    }
}

private fun buildFrequentUserPaymentSummary(user: FrequentUserPaymentData): String {
    val parts = mutableListOf<String>()
    if (user.bankName.isNotBlank()) parts.add("Banco: ${user.bankName}")
    if (user.bankAccount.isNotBlank()) parts.add("Cuenta: ${user.bankAccount}")
    if (user.mobilePaymentPhone.isNotBlank()) parts.add("Pago móvil: ${user.mobilePaymentPhone}")
    if (user.paymentAlias.isNotBlank()) parts.add("Alias: ${user.paymentAlias}")
    return parts.joinToString(" | ")
}

private fun injectFrequentPaymentReference(
    currentConditions: String,
    paymentSummary: String
): String {
    if (paymentSummary.isBlank()) return currentConditions
    val clean = currentConditions.trim()

    val newLine = "Referencia frecuente: $paymentSummary"
    if (clean.isBlank()) return newLine

    return if (clean.contains("Referencia frecuente:", ignoreCase = true)) {
        clean.substringBefore("Referencia frecuente:", clean).trim() + "\n" + newLine
    } else {
        clean + "\n" + newLine
    }.trim()
}
