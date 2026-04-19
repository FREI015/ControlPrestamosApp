package com.controlprestamos.app

import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    activity: FragmentActivity,
    sessionStore: SessionStore,
    onUnlockSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var remainingLockSeconds by remember { mutableStateOf(sessionStore.getRemainingPinLockSeconds()) }

    val context = LocalContext.current
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

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
        AppTopBack(title = "Seguridad")

        AppSectionCard {
            Text("Aplicación protegida")
            AppMutedText(
                if (sessionStore.hasPin()) {
                    "Desbloquea con tu PIN o biometría."
                } else {
                    "Crea tu PIN inicial para proteger la aplicación."
                }
            )
        }

        if (remainingLockSeconds > 0) {
            AppSectionCard {
                Text("Acceso temporalmente bloqueado")
                AppMutedText("Demasiados intentos fallidos. Espera $remainingLockSeconds segundos para volver a intentar con PIN.")
                if (biometricAvailable) {
                    AppMutedText("La biometría sigue disponible si el dispositivo la permite.")
                }
            }
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
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6) confirmPin = it.filter(Char::isDigit)
                        error = ""
                    },
                    label = { Text("Confirmar PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (error.isNotBlank()) {
                    Text(error)
                }

                AppPrimaryButton(
                    text = "Guardar PIN y entrar",
                    onClick = {
                        when {
                            newPin.length < 4 -> error = "El PIN debe tener entre 4 y 6 dígitos."
                            newPin != confirmPin -> error = "Los PIN no coinciden."
                            else -> {
                                sessionStore.savePin(newPin)
                                sessionStore.clearPinFailures()
                                sessionStore.setUnlocked(true)
                                sessionStore.appendHistory("PIN inicial creado")
                                onUnlockSuccess()
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
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (error.isNotBlank()) {
                    Text(error)
                }

                AppPrimaryButton(
                    text = "Entrar con PIN",
                    onClick = {
                        if (remainingLockSeconds > 0 || sessionStore.isPinTemporarilyLocked()) {
                            remainingLockSeconds = sessionStore.getRemainingPinLockSeconds()
                            error = "Debes esperar $remainingLockSeconds segundos para volver a intentar."
                        } else if (sessionStore.validatePin(pin)) {
                            sessionStore.clearPinFailures()
                            sessionStore.setUnlocked(true)
                            onUnlockSuccess()
                        } else {
                            val lockSeconds = sessionStore.registerFailedPinAttempt()
                            remainingLockSeconds = sessionStore.getRemainingPinLockSeconds()
                            error = if (lockSeconds > 0) {
                                "PIN incorrecto. Se bloqueó temporalmente el acceso por $lockSeconds segundos."
                            } else {
                                "PIN incorrecto."
                            }
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
                                        sessionStore.clearPinFailures()
                                        sessionStore.setUnlocked(true)
                                        onUnlockSuccess()
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
