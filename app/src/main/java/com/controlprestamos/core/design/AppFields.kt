package com.controlprestamos.core.design

import com.controlprestamos.core.validation.*

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
fun showDatePicker(
    context: Context,
    currentValue: String = "",
    onDateSelected: (String) -> Unit
) {
    val initialDate = parseIsoDateOrNull(currentValue) ?: LocalDate.now()

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
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
    val showError = value.isNotBlank() && !isValidIsoDate(value)

    OutlinedTextField(
        value = value,
        onValueChange = { onDateSelected(sanitizeDateInput(it)) },
        label = { Text(label) },
        placeholder = { Text("AAAA-MM-DD") },
        singleLine = true,
        isError = showError,
        supportingText = {
            Text(
                if (showError) {
                    "Fecha inválida. Usa AAAA-MM-DD."
                } else {
                    "Formato: AAAA-MM-DD"
                }
            )
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    showDatePicker(
                        context = context,
                        currentValue = value,
                        onDateSelected = onDateSelected
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "Elegir fecha"
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label. Campo de fecha"
            }
    )
}
