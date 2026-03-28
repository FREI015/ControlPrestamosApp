package com.controlprestamos.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.data.UserProfilePreferences
import com.controlprestamos.domain.model.UserProfile
import com.controlprestamos.ui.viewmodel.CollectionNoticeViewModel
import com.controlprestamos.util.CurrencyUtils

@Composable
fun CollectionNoticeScreen(
    loanId: Long,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val context = LocalContext.current
    val profile = remember { UserProfilePreferences(context).getProfile() }

    val viewModel: CollectionNoticeViewModel = viewModel(
        factory = CollectionNoticeViewModel.factory(
            loanId = loanId,
            repository = app.container.loanRepository
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fullShareMessage = remember(uiState.shareMessage, profile) {
        buildFullShareMessage(uiState.shareMessage, profile)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Aviso de cobro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.customerName.ifBlank { "Cargando..." },
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState.phone.isNotBlank()) {
                    Text(
                        text = uiState.phone,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Resumen del cobro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    InfoLine("Vence", uiState.dueDateText)
                    InfoLine("Pendiente en USD", CurrencyUtils.usd(uiState.pendingAmountUsd))

                    if (uiState.isOverdue) {
                        InfoLine("Estado", "Retrasado • ${uiState.daysOverdue} día(s)")
                    } else {
                        InfoLine("Estado", "En seguimiento")
                    }

                    if (uiState.reminderMarkedThisSession) {
                        InfoLine("Recordatorio", "Marcado como enviado")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cálculo del día",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = uiState.exchangeRateText,
                        onValueChange = viewModel::onExchangeRateChange,
                        label = { Text("Tasa del día (Bs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.lateFeePercentText,
                        onValueChange = viewModel::onLateFeePercentChange,
                        label = { Text("Recargo % por retraso") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        onClick = { viewModel.applyLateFeePercent() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aplicar recargo porcentual")
                    }

                    OutlinedTextField(
                        value = uiState.lateFeeFixedText,
                        onValueChange = viewModel::onLateFeeFixedChange,
                        label = { Text("Recargo fijo en USD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        onClick = { viewModel.applyLateFeeFixed() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aplicar recargo fijo")
                    }

                    HorizontalDivider()

                    InfoLine("Extra por %", CurrencyUtils.usd(uiState.previewLateFeePercentAmount))
                    InfoLine("Extra fijo", CurrencyUtils.usd(uiState.previewLateFeeFixedAmount))
                    InfoLine("Total a cobrar USD", CurrencyUtils.usd(uiState.totalToChargeUsd))
                    InfoLine(
                        "Total en bolívares",
                        if (uiState.totalToChargeVes > 0.0) {
                            String.format("%.2f Bs", uiState.totalToChargeVes)
                        } else {
                            "Ingresa la tasa del día"
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Datos de cobro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (profile.isConfigured) {
                        if (profile.fullName.isNotBlank()) InfoLine("Titular", profile.fullName)
                        if (profile.idNumber.isNotBlank()) InfoLine("Cédula", profile.idNumber)
                        if (profile.bankName.isNotBlank()) InfoLine("Banco", profile.bankName)
                        if (profile.paymentMobilePhone.isNotBlank()) InfoLine("Pago móvil", profile.paymentMobilePhone)
                        if (profile.phone.isNotBlank()) InfoLine("Teléfono", profile.phone)
                        if (profile.accountNumber.isNotBlank()) InfoLine("Cuenta", profile.accountNumber)
                        if (profile.paymentNotes.isNotBlank()) {
                            Text(
                                text = profile.paymentNotes,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text(
                            text = "Configura primero tu perfil para que salgan tus datos de cobro.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Mensaje a compartir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = uiState.noteText,
                        onValueChange = viewModel::onNoteChange,
                        label = { Text("Nota interna para cierre") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = fullShareMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, fullShareMessage)
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Compartir cobro")
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Compartir mensaje")
                    }

                    FilledTonalButton(
                        onClick = { viewModel.markReminderSent() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Marcar recordatorio enviado")
                    }

                    Button(
                        onClick = { viewModel.markAsCollected() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Marcar como cobrado")
                    }

                    FilledTonalButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildFullShareMessage(
    baseMessage: String,
    profile: UserProfile
): String {
    if (!profile.isConfigured) return baseMessage

    val paymentLines = buildList {
        if (profile.fullName.isNotBlank()) add("Titular: ${profile.fullName}")
        if (profile.idNumber.isNotBlank()) add("Cédula: ${profile.idNumber}")
        if (profile.bankName.isNotBlank()) add("Banco: ${profile.bankName}")
        if (profile.paymentMobilePhone.isNotBlank()) add("Pago móvil: ${profile.paymentMobilePhone}")
        if (profile.phone.isNotBlank()) add("Teléfono: ${profile.phone}")
        if (profile.accountNumber.isNotBlank()) add("Cuenta: ${profile.accountNumber}")
        if (profile.paymentNotes.isNotBlank()) add(profile.paymentNotes)
    }

    return buildString {
        append(baseMessage.trim())
        if (paymentLines.isNotEmpty()) {
            append("\n\nDatos para pagar:\n")
            append(paymentLines.joinToString("\n"))
        }
    }
}
