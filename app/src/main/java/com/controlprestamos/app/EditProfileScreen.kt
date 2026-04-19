package com.controlprestamos.app

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun EditProfileScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val current = sessionStore.readProfile()

    var name by remember { mutableStateOf(current.name) }
    var lastName by remember { mutableStateOf(current.lastName) }
    var idNumber by remember { mutableStateOf(current.idNumber) }
    var phone by remember { mutableStateOf(current.phone) }
    var communicationPhone by remember { mutableStateOf(current.communicationPhone) }
    var mobilePaymentPhone by remember { mutableStateOf(current.mobilePaymentPhone) }
    var bankName by remember { mutableStateOf(current.bankName) }
    var bankAccount by remember { mutableStateOf(current.bankAccount) }
    var personalizedMessage by remember { mutableStateOf(current.personalizedMessage) }
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Editar perfil",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Text(
                text = "Datos personales",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = normalizeTextInput(it)
                    feedback = ""
                },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = normalizeTextInput(it)
                    feedback = ""
                },
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = idNumber,
                onValueChange = {
                    idNumber = normalizeTextInput(it)
                    feedback = ""
                },
                label = { Text("Cédula") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = sanitizePhoneInput(it)
                    feedback = ""
                },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = communicationPhone,
                onValueChange = {
                    communicationPhone = sanitizePhoneInput(it)
                    feedback = ""
                },
                label = { Text("Teléfono de comunicación") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        AppSectionCard {
            Text(
                text = "Datos de cobro",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = mobilePaymentPhone,
                onValueChange = {
                    mobilePaymentPhone = sanitizePhoneInput(it)
                    feedback = ""
                },
                label = { Text("Pago móvil") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = {
                    bankName = normalizeTextInput(it)
                    feedback = ""
                },
                label = { Text("Banco") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = bankAccount,
                onValueChange = {
                    bankAccount = normalizeTextInput(it)
                    feedback = ""
                },
                label = { Text("Cuenta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        AppSectionCard {
            Text(
                text = "Mensaje de cobro",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = personalizedMessage,
                onValueChange = {
                    personalizedMessage = normalizeTextInput(it)
                    feedback = ""
                },
                label = { Text("Mensaje personalizado") },
                modifier = Modifier.fillMaxWidth()
            )

            AppMutedText("La foto del perfil queda desactivada para mantener esta sección estable y evitar cierres.")
        }

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppPrimaryButton(
            text = "Guardar cambios",
            onClick = {
                val cleanName = normalizeTextInput(name).trim()
                val cleanLastName = normalizeTextInput(lastName).trim()
                val cleanIdNumber = sanitizeIntegerInput(idNumber)
                val cleanPhone = sanitizePhoneInput(phone)
                val cleanCommunicationPhone = sanitizePhoneInput(communicationPhone)
                val cleanMobilePaymentPhone = sanitizePhoneInput(mobilePaymentPhone)
                val cleanBankName = normalizeTextInput(bankName).trim()
                val cleanBankAccount = normalizeTextInput(bankAccount).trim()
                val cleanPersonalizedMessage = normalizeTextInput(personalizedMessage).trim().ifBlank {
                    UserProfileData().personalizedMessage
                }

                val validationMessage = validateProfileForm(
                    name = cleanName,
                    lastName = cleanLastName,
                    idNumber = cleanIdNumber,
                    phone = cleanPhone,
                    communicationPhone = cleanCommunicationPhone,
                    mobilePaymentPhone = cleanMobilePaymentPhone,
                    bankName = cleanBankName,
                    bankAccount = cleanBankAccount,
                    personalizedMessage = cleanPersonalizedMessage
                )

                if (validationMessage != null) {
                    feedback = validationMessage
                } else {
                    sessionStore.saveProfile(
                        UserProfileData(
                            photoUri = current.photoUri,
                            name = cleanName,
                            lastName = cleanLastName,
                            idNumber = cleanIdNumber,
                            phone = cleanPhone,
                            communicationPhone = cleanCommunicationPhone,
                            mobilePaymentPhone = cleanMobilePaymentPhone,
                            bankName = cleanBankName,
                            bankAccount = cleanBankAccount,
                            personalizedMessage = cleanPersonalizedMessage
                        )
                    )
                    navController.popBackStack()
                }
            }
        )

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}
