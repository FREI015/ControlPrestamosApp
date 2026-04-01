package com.controlprestamos.app

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

fun formatMoney(value: Double, currencySymbol: String = "$"): String {
    val safeValue = if (value.isFinite()) value else 0.0
    return currencySymbol + String.format(Locale.US, "%.2f", safeValue)
}

fun formatPercent(value: Double): String {
    val safeValue = if (value.isFinite()) value else 0.0
    return String.format(Locale.US, "%.2f%%", safeValue)
}

fun sanitizeDecimalInput(input: String, allowNegative: Boolean = false): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""

    val result = StringBuilder()
    var hasDecimalSeparator = false
    var hasSign = false

    trimmed.forEachIndexed { index, char ->
        when {
            char.isDigit() -> result.append(char)
            (char == '.' || char == ',') && !hasDecimalSeparator -> {
                if (result.isEmpty()) {
                    result.append('0')
                }
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
        }
    }
    return builder.toString()
}

fun normalizeTextInput(input: String): String {
    return input
        .replace("\r", "")
        .trim()
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

fun showDatePicker(
    context: Context,
    currentValue: String = "",
    onDateSelected: (String) -> Unit
) {
    val initialDate = parseIsoDateOrNull(currentValue) ?: LocalDate.now()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(
                LocalDate.of(year, month + 1, dayOfMonth).toString()
            )
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

@Composable
fun AppDateField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text("AAAA-MM-DD") },
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showDatePicker(
                    context = context,
                    currentValue = value,
                    onDateSelected = onDateSelected
                )
            },
        colors = TextFieldDefaults.colors()
    )
}
