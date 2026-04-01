package com.controlprestamos.app

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@Composable
fun EditProfileScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val current = sessionStore.readProfile()

    var photoUri by remember { mutableStateOf(current.photoUri) }
    var name by remember { mutableStateOf(current.name) }
    var lastName by remember { mutableStateOf(current.lastName) }
    var idNumber by remember { mutableStateOf(current.idNumber) }
    var phone by remember { mutableStateOf(current.phone) }
    var communicationPhone by remember { mutableStateOf(current.communicationPhone) }
    var mobilePaymentPhone by remember { mutableStateOf(current.mobilePaymentPhone) }
    var bankName by remember { mutableStateOf(current.bankName) }
    var bankAccount by remember { mutableStateOf(current.bankAccount) }
    var personalizedMessage by remember { mutableStateOf(current.personalizedMessage) }
    var errorMessage by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(title = "Editar perfil", onBack = { navController.popBackStack() })

        if (errorMessage.isNotBlank()) {
            AppSectionCard {
                Text(errorMessage)
            }
        }

        AppSectionCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (photoUri.isNotBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            imageView.setImageURI(Uri.parse(photoUri))
                        },
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Foto")
                    }
                }

                AppSecondaryButton(
                    text = "Cargar foto",
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }

        AppSectionCard {
            Text("Datos personales", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = normalizeTextInput(it)
                    errorMessage = ""
                },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = normalizeTextInput(it)
                    errorMessage = ""
                },
                label = { Text("Apellido *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = idNumber,
                onValueChange = {
                    idNumber = sanitizeIntegerInput(it)
                    errorMessage = ""
                },
                label = { Text("Cédula *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = sanitizePhoneInput(it)
                    errorMessage = ""
                },
                label = { Text("Teléfono principal *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        AppSectionCard {
            Text("Datos de cobro", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = communicationPhone,
                onValueChange = {
                    communicationPhone = sanitizePhoneInput(it)
                    errorMessage = ""
                },
                label = { Text("Teléfono de comunicación") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = mobilePaymentPhone,
                onValueChange = {
                    mobilePaymentPhone = sanitizePhoneInput(it)
                    errorMessage = ""
                },
                label = { Text("Teléfono pago móvil *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = {
                    bankName = normalizeTextInput(it)
                    errorMessage = ""
                },
                label = { Text("Banco *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = bankAccount,
                onValueChange = {
                    bankAccount = normalizeTextInput(it)
                    errorMessage = ""
                },
                label = { Text("Número de cuenta / referencia *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = personalizedMessage,
                onValueChange = {
                    personalizedMessage = it
                    errorMessage = ""
                },
                label = { Text("Mensaje personalizado de cobro") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AppPrimaryButton(
            text = "Guardar cambios",
            onClick = {
                val cleanName = normalizeTextInput(name)
                val cleanLastName = normalizeTextInput(lastName)
                val cleanId = sanitizeIntegerInput(idNumber)
                val cleanPhone = sanitizePhoneInput(phone)
                val cleanCommunicationPhone = sanitizePhoneInput(communicationPhone)
                val cleanMobilePaymentPhone = sanitizePhoneInput(mobilePaymentPhone)
                val cleanBank = normalizeTextInput(bankName)
                val cleanBankAccount = normalizeTextInput(bankAccount)
                val cleanMessage = personalizedMessage.trim().ifBlank {
                    "Hola, buenos días. Le escribo por el vencimiento de su préstamo. A continuación le comparto los datos de pago. Muchas gracias."
                }

                when {
                    cleanName.isBlank() -> errorMessage = "Debes colocar tu nombre."
                    cleanLastName.isBlank() -> errorMessage = "Debes colocar tu apellido."
                    cleanId.isBlank() -> errorMessage = "Debes colocar tu cédula."
                    cleanPhone.isBlank() -> errorMessage = "Debes colocar tu teléfono principal."
                    cleanMobilePaymentPhone.isBlank() -> errorMessage = "Debes colocar el teléfono de pago móvil."
                    cleanBank.isBlank() -> errorMessage = "Debes colocar el banco."
                    cleanBankAccount.isBlank() -> errorMessage = "Debes colocar la cuenta o referencia de cobro."
                    else -> {
                        sessionStore.saveProfile(
                            UserProfileData(
                                photoUri = photoUri,
                                name = cleanName,
                                lastName = cleanLastName,
                                idNumber = cleanId,
                                phone = cleanPhone,
                                communicationPhone = cleanCommunicationPhone,
                                mobilePaymentPhone = cleanMobilePaymentPhone,
                                bankName = cleanBank,
                                bankAccount = cleanBankAccount,
                                personalizedMessage = cleanMessage
                            )
                        )
                        navController.popBackStack()
                    }
                }
            }
        )

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}
