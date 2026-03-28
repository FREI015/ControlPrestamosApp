package com.controlprestamos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.controlprestamos.data.UserProfilePreferences
import com.controlprestamos.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfileUiState(
    val form: UserProfile = UserProfile(),
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class UserProfileViewModel(
    private val preferences: UserProfilePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UserProfileUiState(form = preferences.getProfile())
    )
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun updateForm(transform: (UserProfile) -> UserProfile) {
        _uiState.value = _uiState.value.copy(
            form = transform(_uiState.value.form),
            message = null,
            error = null
        )
    }

    fun save() {
        val form = _uiState.value.form

        if (form.fullName.trim().isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Debes colocar tu nombre.")
            return
        }

        if (form.phone.trim().isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Debes colocar tu teléfono.")
            return
        }

        _uiState.value = _uiState.value.copy(saving = true, message = null, error = null)

        preferences.saveProfile(
            form.copy(
                fullName = form.fullName.trim(),
                idNumber = form.idNumber.trim(),
                phone = form.phone.trim(),
                bankName = form.bankName.trim(),
                paymentMobilePhone = form.paymentMobilePhone.trim(),
                accountNumber = form.accountNumber.trim(),
                paymentNotes = form.paymentNotes.trim(),
                isConfigured = true
            )
        )

        _uiState.value = _uiState.value.copy(
            saving = false,
            form = preferences.getProfile(),
            message = "Perfil guardado correctamente."
        )
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    companion object {
        fun factory(preferences: UserProfilePreferences): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UserProfileViewModel(preferences) as T
                }
            }
    }
}
