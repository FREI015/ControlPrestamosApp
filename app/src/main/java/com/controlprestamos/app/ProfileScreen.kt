package com.controlprestamos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ProfileScreen(
    navController: NavController,
    sessionStore: SessionStore,
    onThemeChanged: (Boolean) -> Unit
) {
    val profile = sessionStore.readProfile()
    val missingPaymentFields = buildMissingPaymentFields(profile)
    val missingPersonalFields = buildMissingPersonalFields(profile)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppTopBack(title = "Mi perfil")

        AppSectionCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buildInitials(profile),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Text(
                    text = buildFullProfileName(profile),
                    style = MaterialTheme.typography.titleLarge
                )

                AppMutedText("Cédula: ${profile.idNumber.ifBlank { "-" }}")
                AppMutedText("Teléfono principal: ${profile.phone.ifBlank { "-" }}")
                AppMutedText("Teléfono de comunicación: ${profile.communicationPhone.ifBlank { "-" }}")
            }
        }

        AppSectionCard {
            Text("Estado del perfil", style = MaterialTheme.typography.titleMedium)

            if (missingPersonalFields.isEmpty() && missingPaymentFields.isEmpty()) {
                AppStatusChip("Perfil listo")
                AppMutedText("Tus datos principales y de cobro están completos.")
            } else {
                AppStatusChip("Perfil incompleto")
                if (missingPersonalFields.isNotEmpty()) {
                    AppMutedText("Faltan datos personales: ${missingPersonalFields.joinToString(", ")}")
                }
                if (missingPaymentFields.isNotEmpty()) {
                    AppMutedText("Faltan datos de cobro: ${missingPaymentFields.joinToString(", ")}")
                }
            }
        }

        AppSectionCard {
            Text("Datos de cobro", style = MaterialTheme.typography.titleMedium)
            AppMutedText("Pago móvil: ${profile.mobilePaymentPhone.ifBlank { "-" }}")
            AppMutedText("Banco: ${profile.bankName.ifBlank { "-" }}")
            AppMutedText("Cuenta: ${profile.bankAccount.ifBlank { "-" }}")
            HorizontalDivider()
            AppMutedText("Mensaje base de cobro:")
            AppMutedText(profile.personalizedMessage.ifBlank { "-" })
        }

        AppSectionCard {
            Text("Accesos", style = MaterialTheme.typography.titleMedium)

            ProfileActionRow("Editar perfil") {
                navController.navigate("editProfile")
            }

            ProfileActionRow("Seguridad") {
                navController.navigate("securitySettings")
            }

            ProfileActionRow("Historial") {
                navController.navigate("history")
            }

            ProfileActionRow("Lista negra") {
                navController.navigate("blacklist")
            }

            ProfileActionRow("Preparar aviso de cobro") {
                val activeLoan = sessionStore.readActiveLoan()
                if (activeLoan != null) {
                    navController.navigate("loanCollectionNotice")
                } else {
                    navController.navigate("loans")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tema oscuro")
                Switch(
                    checked = sessionStore.isDarkMode(),
                    onCheckedChange = {
                        sessionStore.setDarkMode(it)
                        onThemeChanged(it)
                    }
                )
            }

            Text(
                "Cerrar sesión",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        sessionStore.logout()
                        navController.navigate("lockScreen") {
                            popUpTo(0)
                        }
                    }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ProfileActionRow(
    title: String,
    onClick: () -> Unit
) {
    Text(
        title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    )
}

private fun buildFullProfileName(profile: UserProfileData): String {
    return listOf(profile.name, profile.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Usuario principal" }
}

private fun buildInitials(profile: UserProfileData): String {
    return (
        (profile.name.firstOrNull()?.uppercase() ?: "") +
        (profile.lastName.firstOrNull()?.uppercase() ?: "")
    ).ifBlank { "U" }
}

private fun buildMissingPersonalFields(profile: UserProfileData): List<String> {
    val result = mutableListOf<String>()
    if (profile.name.isBlank()) result.add("nombre")
    if (profile.lastName.isBlank()) result.add("apellido")
    if (profile.idNumber.isBlank()) result.add("cédula")
    if (profile.phone.isBlank()) result.add("teléfono")
    return result
}

private fun buildMissingPaymentFields(profile: UserProfileData): List<String> {
    val result = mutableListOf<String>()
    if (profile.mobilePaymentPhone.isBlank()) result.add("pago móvil")
    if (profile.bankName.isBlank()) result.add("banco")
    if (profile.bankAccount.isBlank()) result.add("cuenta")
    return result
}
