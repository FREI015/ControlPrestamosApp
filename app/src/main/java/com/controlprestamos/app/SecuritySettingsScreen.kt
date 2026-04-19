package com.controlprestamos.app

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

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
    var remainingLockSeconds by remember { mutableStateOf(sessionStore.getRemainingPinLockSeconds()) }

    LaunchedEffect(remainingLockSeconds) {
        if (remainingLockSeconds > 0) {
            delay(1000)
            remainingLockSeconds = sessionStore.getRemainingPinLockSeconds()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
            AppMutedText("Usa un PIN de 4 a 6 dígitos para mantener el acceso protegido.")
        }

        AppSectionCard {
            Text("Política de acceso")
            AppMutedText("La app ya no permite quitar el PIN desde ajustes. Para publicación vamos a mantener siempre una capa de seguridad activa.")
            AppMutedText("Después de varios intentos fallidos, el acceso por PIN se bloquea temporalmente.")
        }

        if (remainingLockSeconds > 0) {
            AppSectionCard {
                Text("Bloqueo temporal activo")
                AppMutedText("Debes esperar $remainingLockSeconds segundos antes de volver a probar con PIN.")
            }
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
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }

            OutlinedTextField(
                value = newPin,
                onValueChange = {
                    if (it.length <= 6) newPin = it.filter(Char::isDigit)
                },
                label = { Text(if (sessionStore.hasPin()) "Nuevo PIN" else "PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 6) confirmPin = it.filter(Char::isDigit)
                },
                label = { Text("Confirmar PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )

            AppPrimaryButton(
                text = if (sessionStore.hasPin()) "Actualizar PIN" else "Crear PIN",
                onClick = {
                    when {
                        remainingLockSeconds > 0 || sessionStore.isPinTemporarilyLocked() ->
                            message = "Debes esperar ${sessionStore.getRemainingPinLockSeconds()} segundos antes de intentar de nuevo."
                        sessionStore.hasPin() && !sessionStore.validatePin(currentPin) ->
                            message = "El PIN actual no es correcto."
                        newPin.length < 4 ->
                            message = "El PIN debe tener entre 4 y 6 dígitos."
                        newPin != confirmPin ->
                            message = "Los PIN no coinciden."
                        else -> {
                            val hadPin = sessionStore.hasPin()
                            sessionStore.savePin(newPin)
                            sessionStore.clearPinFailures()
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
        }

        AppSectionCard {
            Text("Biometría")
            if (biometricAvailable) {
                AppMutedText("La biometría está disponible y se puede usar desde la pantalla de bloqueo.")
            } else {
                AppMutedText("Este dispositivo no reporta biometría fuerte disponible. El acceso principal será por PIN.")
            }
        }

        AppSectionCard {
            Text("Bloqueo automático")
            AppMutedText("La sesión se cierra automáticamente por inactividad para reducir exposición accidental de datos.")
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}
