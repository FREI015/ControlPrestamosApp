package com.controlprestamos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.data.UserProfilePreferences
import com.controlprestamos.ui.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = UserProfilePreferences(context)
    val viewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModel.factory(preferences)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
            onSaved()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Perfil"
                    )
                    Text(
                        text = "Mi perfil",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aquí guardas tus datos personales y de cobro para usar la app como usuario único.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.form.fullName,
                onValueChange = { value -> viewModel.updateForm { it.copy(fullName = value) } },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.form.idNumber,
                onValueChange = { value -> viewModel.updateForm { it.copy(idNumber = value) } },
                label = { Text("Cédula") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.form.phone,
                onValueChange = { value -> viewModel.updateForm { it.copy(phone = value) } },
                label = { Text("Teléfono principal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.form.bankName,
                onValueChange = { value -> viewModel.updateForm { it.copy(bankName = value) } },
                label = { Text("Banco") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.form.paymentMobilePhone,
                onValueChange = { value -> viewModel.updateForm { it.copy(paymentMobilePhone = value) } },
                label = { Text("Teléfono pago móvil") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.form.accountNumber,
                onValueChange = { value -> viewModel.updateForm { it.copy(accountNumber = value) } },
                label = { Text("Número de cuenta") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = uiState.form.paymentNotes,
                onValueChange = { value -> viewModel.updateForm { it.copy(paymentNotes = value) } },
                label = { Text("Texto adicional de cobro") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = viewModel::save,
                enabled = !uiState.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.saving) "Guardando..." else "Guardar perfil")
            }
        }

        item {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }
}
