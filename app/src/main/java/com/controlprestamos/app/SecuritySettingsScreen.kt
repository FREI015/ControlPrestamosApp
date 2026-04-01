package com.controlprestamos.app

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SecuritySettingsScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val context = LocalContext.current
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var confirmRemovePin by remember { mutableStateOf(false) }

    AppConfirmDialog(
        visible = confirmRemovePin,
        title = "Quitar PIN",
        message = "¿Seguro que deseas quitar el PIN? La app quedará sin esta capa de seguridad hasta que configures uno nuevo.",
        confirmText = "Quitar PIN",
        dismissText = "Cancelar",
        onConfirm = {
            if (!sessionStore.validatePin(currentPin)) {
                message = "Debes colocar el PIN actual para quitarlo."
            } else {
                sessionStore.clearPin()
                sessionStore.setUnlocked(true)
                sessionStore.appendHistory("PIN eliminado")
                currentPin = ""
                newPin = ""
                confirmPin = ""
                message = "PIN eliminado correctamente."
            }
            confirmRemovePin = false
        },
        onDismiss = { confirmRemovePin = false }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Seguridad",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Text("Estado actual")
            AppMutedText(if (sessionStore.hasPin()) "PIN configurado" else "Sin PIN")
            AppMutedText(if (biometricAvailable) "Biometría disponible" else "Biometría no disponible en este dispositivo")
            AppMutedText("Recomendación: usa un PIN de 4 a 6 dígitos y mantenlo junto con biometría si tu teléfono lo permite.")
        }

        if (message.isNotBlank()) {
            AppSectionCard {
                Text(message)
            }
        }

        AppSectionCard {
            Text(if (sessionStore.hasPin()) "Cambiar PIN" else "Crear PIN")

            if (sessionStore.hasPin()) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        if (it.length <= 6) currentPin = it.filter(Char::isDigit)
                    },
                    label = { Text("PIN actual") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = newPin,
                onValueChange = {
                    if (it.length <= 6) newPin = it.filter(Char::isDigit)
                },
                label = { Text(if (sessionStore.hasPin()) "Nuevo PIN" else "PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 6) confirmPin = it.filter(Char::isDigit)
                },
                label = { Text("Confirmar PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AppPrimaryButton(
                text = if (sessionStore.hasPin()) "Actualizar PIN" else "Crear PIN",
                onClick = {
                    when {
                        sessionStore.hasPin() && !sessionStore.validatePin(currentPin) ->
                            message = "El PIN actual no es correcto."
                        newPin.length < 4 ->
                            message = "El PIN debe tener al menos 4 dígitos."
                        newPin != confirmPin ->
                            message = "Los PIN no coinciden."
                        else -> {
                            val hadPin = sessionStore.hasPin()
                            sessionStore.savePin(newPin)
                            sessionStore.setUnlocked(true)
                            sessionStore.appendHistory(if (hadPin) "PIN actualizado" else "PIN inicial creado")
                            currentPin = ""
                            newPin = ""
                            confirmPin = ""
                            message = if (hadPin) "PIN actualizado correctamente." else "PIN creado correctamente."
                        }
                    }
                }
            )

            if (sessionStore.hasPin()) {
                AppSecondaryButton(
                    text = "Quitar PIN",
                    onClick = {
                        confirmRemovePin = true
                    }
                )
            }
        }

        AppSectionCard {
            Text("Biometría")
            if (biometricAvailable) {
                AppMutedText("La biometría está disponible y se puede usar como acceso rápido desde la pantalla de bloqueo.")
            } else {
                AppMutedText("Este dispositivo no reporta biometría fuerte disponible. El fallback será el PIN.")
            }
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}
