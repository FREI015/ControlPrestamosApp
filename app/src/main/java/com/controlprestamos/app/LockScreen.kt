package com.controlprestamos.app

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController

@Composable
fun LockScreen(
    activity: FragmentActivity,
    navController: NavController,
    sessionStore: SessionStore
) {
    var pin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(title = "Seguridad")

        AppSectionCard {
            Text("Aplicación protegida")
            AppMutedText(
                if (sessionStore.hasPin()) {
                    "Desbloquea con tu PIN o tu biometría."
                } else {
                    "Crea tu PIN inicial para proteger la app."
                }
            )
        }

        if (!sessionStore.hasPin()) {
            AppSectionCard {
                Text("Crea tu PIN inicial")

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6) newPin = it.filter(Char::isDigit)
                        error = ""
                    },
                    label = { Text("PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6) confirmPin = it.filter(Char::isDigit)
                        error = ""
                    },
                    label = { Text("Confirmar PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error.isNotBlank()) {
                    Text(error)
                }

                AppPrimaryButton(
                    text = "Guardar PIN y entrar",
                    onClick = {
                        when {
                            newPin.length < 4 -> error = "El PIN debe tener al menos 4 dígitos."
                            newPin != confirmPin -> error = "Los PIN no coinciden."
                            else -> {
                                sessionStore.savePin(newPin)
                                sessionStore.setUnlocked(true)
                                sessionStore.appendHistory("PIN inicial creado")
                                navController.navigate("dashboard") { popUpTo(0) }
                            }
                        }
                    }
                )

                if (biometricAvailable) {
                    AppMutedText("Luego podrás usar biometría para entrar más rápido.")
                } else {
                    AppMutedText("Este dispositivo usará el PIN como método principal.")
                }
            }
        } else {
            AppSectionCard {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6) pin = it.filter(Char::isDigit)
                        error = ""
                    },
                    label = { Text("PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error.isNotBlank()) {
                    Text(error)
                }

                AppPrimaryButton(
                    text = "Entrar con PIN",
                    onClick = {
                        if (sessionStore.validatePin(pin)) {
                            sessionStore.setUnlocked(true)
                            navController.navigate("dashboard") { popUpTo(0) }
                        } else {
                            error = "PIN incorrecto."
                        }
                    }
                )

                if (biometricAvailable) {
                    AppSecondaryButton(
                        text = "Usar biometría",
                        onClick = {
                            val executor = ContextCompat.getMainExecutor(context)
                            val prompt = BiometricPrompt(
                                activity,
                                executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        sessionStore.setUnlocked(true)
                                        navController.navigate("dashboard") { popUpTo(0) }
                                    }

                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                        error = errString.toString()
                                    }

                                    override fun onAuthenticationFailed() {
                                        error = "No se pudo autenticar con biometría."
                                    }
                                }
                            )

                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Desbloqueo biométrico")
                                .setSubtitle("Usa tu huella o biometría")
                                .setNegativeButtonText("Cancelar")
                                .build()

                            prompt.authenticate(promptInfo)
                        }
                    )
                } else {
                    AppMutedText("La biometría no está disponible. Usa tu PIN para entrar.")
                }
            }
        }
    }
}
