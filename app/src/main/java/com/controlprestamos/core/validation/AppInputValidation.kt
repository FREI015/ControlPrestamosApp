package com.controlprestamos.core.validation

import java.time.LocalDate
import java.time.format.DateTimeParseException

fun sanitizeDecimalInput(input: String, allowNegative: Boolean = false): String {
    val result = StringBuilder()
    var hasDecimalSeparator = false
    var hasSign = false

    input.forEachIndexed { index, char ->
        when {
            char.isDigit() -> result.append(char)
            (char == '.' || char == ',') && !hasDecimalSeparator -> {
                if (result.isEmpty()) result.append('0')
                result.append('.')
                hasDecimalSeparator = true
            }
            char == '-' && allowNegative && index == 0 && !hasSign -> {
                result.append(char)
                hasSign = true
            }
        }
    }

    return result.toString()
}

fun sanitizeIntegerInput(input: String): String {
    return input.filter { it.isDigit() }
}

fun sanitizePhoneInput(input: String): String {
    val builder = StringBuilder()
    input.forEachIndexed { index, char ->
        when {
            char.isDigit() -> builder.append(char)
            char == '+' && index == 0 -> builder.append(char)
            char == ' ' -> builder.append(char)
        }
    }
    return builder.toString()
}

fun normalizeTextInput(input: String): String {
    return input.replace("\r", "")
}

fun sanitizeDateInput(input: String): String {
    val clean = input.filter { it.isDigit() || it == '-' }
    return if (clean.length <= 10) clean else clean.substring(0, 10)
}

fun parseMoneyOrNull(input: String): Double? {
    val normalized = sanitizeDecimalInput(input)
    if (normalized.isBlank()) return null
    return normalized.toDoubleOrNull()
}

fun parsePercentOrNull(input: String): Double? {
    val normalized = sanitizeDecimalInput(input)
    if (normalized.isBlank()) return null
    return normalized.toDoubleOrNull()
}

fun parseIsoDateOrNull(input: String): LocalDate? {
    val normalized = input.trim()
    if (normalized.isBlank()) return null
    return try {
        LocalDate.parse(normalized)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun isValidIsoDate(input: String): Boolean {
    return parseIsoDateOrNull(input) != null
}

fun isValidPositiveAmount(input: String): Boolean {
    val value = parseMoneyOrNull(input) ?: return false
    return value > 0.0
}

fun isValidNonNegativePercent(input: String): Boolean {
    val value = parsePercentOrNull(input) ?: return false
    return value >= 0.0
}

fun isDateRangeValid(startDate: String, endDate: String): Boolean {
    val start = parseIsoDateOrNull(startDate) ?: return false
    val end = parseIsoDateOrNull(endDate) ?: return false
    return !end.isBefore(start)
}

fun validateRequiredText(value: String): Boolean {
    return value.trim().isNotBlank()
}

fun validateMinLength(value: String, minLength: Int): Boolean {
    return value.trim().length >= minLength
}

fun validateNumericRange(value: Double?, min: Double? = null, max: Double? = null): Boolean {
    if (value == null) return false
    if (min != null && value < min) return false
    if (max != null && value > max) return false
    return true
}

private const val MAX_GENERIC_NOTES_LENGTH = 500
private const val MAX_PERSONALIZED_MESSAGE_LENGTH = 1000
private const val MAX_BANK_NAME_LENGTH = 80
private const val MAX_ALIAS_LENGTH = 60
private const val MAX_CONDITIONS_LENGTH = 500

fun validateIdNumberOptional(value: String, fieldLabel: String = "La cédula"): String? {
    if (value.isBlank()) return null
    val digits = value.filter(Char::isDigit)
    return when {
        digits.length !in 5..15 -> "$fieldLabel debe tener entre 5 y 15 dígitos."
        else -> null
    }
}

fun validatePhoneOptional(value: String, fieldLabel: String = "El teléfono"): String? {
    if (value.isBlank()) return null
    val digits = value.filter(Char::isDigit)
    return when {
        digits.length !in 7..15 -> "$fieldLabel debe tener entre 7 y 15 dígitos."
        else -> null
    }
}

fun validateBankAccountOptional(value: String): String? {
    if (value.isBlank()) return null
    val compact = value.filter { !it.isWhitespace() }
    return when {
        compact.length !in 6..34 -> "La cuenta bancaria debe tener entre 6 y 34 caracteres."
        else -> null
    }
}

fun validateTextMaxLength(value: String, maxLength: Int, fieldLabel: String): String? {
    return if (value.trim().length > maxLength) {
        "$fieldLabel supera el máximo permitido de $maxLength caracteres."
    } else {
        null
    }
}

fun validateLoanForm(
    clientName: String,
    idNumber: String,
    phone: String,
    amount: Double?,
    percentage: Double?,
    loanDate: String,
    dueDate: String,
    exchangeRate: String,
    conditions: String,
    existingPaidAmount: Double = 0.0
): String? {
    if (clientName.isBlank()) return "Debes colocar el nombre del cliente."
    if (clientName.trim().length < 2) return "El nombre del cliente es demasiado corto."

    validateIdNumberOptional(idNumber)?.let { return it }
    validatePhoneOptional(phone)?.let { return it }

    if (amount == null || amount <= 0.0) return "El monto prestado debe ser mayor que cero."
    if (!amount.isFinite()) return "El monto prestado no es válido."
    if (amount > 1_000_000_000.0) return "El monto prestado es demasiado alto."

    if (percentage == null || percentage < 0.0) return "El porcentaje no es válido."
    if (!percentage.isFinite()) return "El porcentaje no es válido."
    if (percentage > 1000.0) return "El porcentaje es demasiado alto."

    val loanLocalDate = parseIsoDateOrNull(loanDate) ?: return "La fecha del préstamo no es válida."
    val dueLocalDate = parseIsoDateOrNull(dueDate) ?: return "La fecha de vencimiento no es válida."

    if (dueLocalDate.isBefore(loanLocalDate)) {
        return "La fecha de vencimiento no puede ser menor que la fecha del préstamo."
    }

    if (exchangeRate.isNotBlank()) {
        val exchangeValue = parseMoneyOrNull(exchangeRate)
        if (exchangeValue == null || exchangeValue <= 0.0) {
            return "La tasa de cambio no es válida."
        }
    }

    validateTextMaxLength(conditions, MAX_CONDITIONS_LENGTH, "Las condiciones")?.let { return it }

    val newTotal = amount + (amount * (percentage / 100.0))
    if (existingPaidAmount > newTotal) {
        return "No puedes guardar un préstamo cuyo nuevo total quede por debajo de lo ya abonado."
    }

    return null
}

fun validateProfileForm(
    name: String,
    lastName: String,
    idNumber: String,
    phone: String,
    communicationPhone: String,
    mobilePaymentPhone: String,
    bankName: String,
    bankAccount: String,
    personalizedMessage: String
): String? {
    if (name.isBlank()) return "Debes colocar al menos un nombre."
    if (name.trim().length < 2) return "El nombre es demasiado corto."

    validateTextMaxLength(lastName, 80, "El apellido")?.let { return it }
    validateIdNumberOptional(idNumber)?.let { return it }
    validatePhoneOptional(phone, "El teléfono principal")?.let { return it }
    validatePhoneOptional(communicationPhone, "El teléfono de comunicación")?.let { return it }
    validatePhoneOptional(mobilePaymentPhone, "El teléfono de pago móvil")?.let { return it }

    validateTextMaxLength(bankName, MAX_BANK_NAME_LENGTH, "El banco")?.let { return it }
    validateBankAccountOptional(bankAccount)?.let { return it }
    validateTextMaxLength(personalizedMessage, MAX_PERSONALIZED_MESSAGE_LENGTH, "El mensaje personalizado")?.let { return it }

    return null
}

fun validateBlacklistForm(
    fullName: String,
    idNumber: String,
    phone: String,
    reason: String,
    notes: String
): String? {
    if (fullName.isBlank()) return "Debes colocar el nombre."
    if (fullName.trim().length < 2) return "El nombre es demasiado corto."

    validateIdNumberOptional(idNumber)?.let { return it }
    validatePhoneOptional(phone)?.let { return it }

    if (reason.isBlank()) return "Debes colocar el motivo."
    validateTextMaxLength(reason, 120, "El motivo")?.let { return it }
    validateTextMaxLength(notes, MAX_GENERIC_NOTES_LENGTH, "Las notas")?.let { return it }

    return null
}

fun validateFrequentUserForm(
    fullName: String,
    idNumber: String,
    phone: String,
    bankName: String,
    bankAccount: String,
    mobilePaymentPhone: String,
    paymentAlias: String,
    notes: String
): String? {
    if (fullName.isBlank()) return "Debes colocar el nombre del usuario."
    if (fullName.trim().length < 2) return "El nombre del usuario es demasiado corto."

    validateIdNumberOptional(idNumber)?.let { return it }
    validatePhoneOptional(phone)?.let { return it }
    validatePhoneOptional(mobilePaymentPhone, "El pago móvil")?.let { return it }
    validateTextMaxLength(bankName, MAX_BANK_NAME_LENGTH, "El banco")?.let { return it }
    validateBankAccountOptional(bankAccount)?.let { return it }
    validateTextMaxLength(paymentAlias, MAX_ALIAS_LENGTH, "El alias de pago")?.let { return it }
    validateTextMaxLength(notes, MAX_GENERIC_NOTES_LENGTH, "Las notas")?.let { return it }

    val hasPaymentData =
        bankName.isNotBlank() ||
            bankAccount.isNotBlank() ||
            mobilePaymentPhone.isNotBlank() ||
            paymentAlias.isNotBlank()

    if (!hasPaymentData) return "Debes guardar al menos un dato de pago."

    if (bankAccount.isNotBlank() && bankName.isBlank()) {
        return "Si colocas una cuenta bancaria, indica también el banco."
    }

    return null
}

fun validateReferralForm(
    referralDate: String,
    referredClient: String,
    referredBy: String,
    loanAmountText: String,
    amount: Double,
    commissionPercent: Double?,
    status: String,
    notes: String
): String? {
    if (parseIsoDateOrNull(referralDate) == null) return "La fecha del referido no es válida."
    if (referredClient.isBlank()) return "Debes colocar el nombre del cliente referido."
    if (referredClient.trim().length < 2) return "El nombre del cliente referido es demasiado corto."
    if (referredBy.isBlank()) return "Debes indicar quién lo refirió."
    if (referredBy.trim().length < 2) return "El nombre de quien refiere es demasiado corto."

    if (loanAmountText.isNotBlank()) {
        if (!amount.isFinite() || amount <= 0.0) {
            return "El monto del préstamo no es válido."
        }
    }

    if (commissionPercent == null || commissionPercent < 0.0 || !commissionPercent.isFinite()) {
        return "El porcentaje de comisión no es válido."
    }

    if (commissionPercent > 100.0) {
        return "El porcentaje de comisión no puede ser mayor que 100."
    }

    if (status !in setOf("PENDIENTE", "PAGADA", "ANULADA")) {
        return "El estado del referido no es válido."
    }

    validateTextMaxLength(notes, MAX_GENERIC_NOTES_LENGTH, "Las notas")?.let { return it }

    return null
}
