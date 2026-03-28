package com.controlprestamos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.controlprestamos.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val isPinCreated: Boolean = false,
    val biometricEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val securityPreferences = SecurityPreferences(application.applicationContext)

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isPinCreated = securityPreferences.isPinCreated(),
            biometricEnabled = securityPreferences.isBiometricEnabled(),
            isAuthenticated = securityPreferences.isAuthenticated()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onPinChanged(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(pin = value, errorMessage = null, successMessage = null)
        }
    }

    fun onConfirmPinChanged(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(confirmPin = value, errorMessage = null, successMessage = null)
        }
    }

    fun createPin() {
        val pin = _uiState.value.pin
        val confirmPin = _uiState.value.confirmPin

        when {
            pin.length < 4 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "El PIN debe tener al menos 4 dígitos"
                )
            }

            pin != confirmPin -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Los PIN no coinciden"
                )
            }

            else -> {
                securityPreferences.savePin(pin)
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    confirmPin = "",
                    isPinCreated = true,
                    errorMessage = null,
                    successMessage = "PIN creado correctamente"
                )
            }
        }
    }

    fun loginWithPin() {
        val pin = _uiState.value.pin

        if (pin.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Ingresa tu PIN"
            )
            return
        }

        if (securityPreferences.validatePin(pin)) {
            securityPreferences.setAuthenticated(true)
            _uiState.value = _uiState.value.copy(
                pin = "",
                isAuthenticated = true,
                errorMessage = null,
                successMessage = "Acceso concedido"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "PIN incorrecto",
                successMessage = null
            )
        }
    }

    fun loginWithBiometricSuccess() {
        securityPreferences.setAuthenticated(true)
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true,
            errorMessage = null,
            successMessage = "Autenticación biométrica exitosa"
        )
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityPreferences.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            biometricEnabled = enabled
        )
    }

    fun logout() {
        securityPreferences.logout()
        _uiState.value = _uiState.value.copy(
            pin = "",
            confirmPin = "",
            isAuthenticated = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}