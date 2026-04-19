package com.controlprestamos.features.profile

import com.controlprestamos.features.backup.*
import com.controlprestamos.features.security.*
import com.controlprestamos.features.settings.*
import com.controlprestamos.features.dashboard.*
import com.controlprestamos.features.people.*
import com.controlprestamos.features.search.*
import com.controlprestamos.features.more.*
import com.controlprestamos.core.validation.*

import com.controlprestamos.core.format.*

import com.controlprestamos.app.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ProfileScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    val profile = sessionStore.readProfile()

    val fullName = listOf(profile.name, profile.lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Usuario principal" }

    val initials = (
        (profile.name.firstOrNull()?.uppercase() ?: "") +
        (profile.lastName.firstOrNull()?.uppercase() ?: "")
    ).ifBlank { "U" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Mi perfil",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge
                )

                AppMutedText("Perfil del usuario")
            }
        }

        AppSectionCard {
            Text(
                text = "Datos personales",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileInfoCard("Nombres", profile.name.ifBlank { "-" }, Modifier.width(cardWidth))
                        ProfileInfoCard("Apellidos", profile.lastName.ifBlank { "-" }, Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileInfoCard("Cédula", profile.idNumber.ifBlank { "-" }, Modifier.width(cardWidth))
                        ProfileInfoCard("Teléfono principal", profile.phone.ifBlank { "-" }, Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileInfoCard("Teléfono de comunicación", profile.communicationPhone.ifBlank { "-" }, Modifier.width(cardWidth))
                        Spacer(modifier = Modifier.width(cardWidth))
                    }
                }
            }
        }

        AppSectionCard {
            Text(
                text = "Datos de cobro",
                style = MaterialTheme.typography.titleMedium
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cardWidth = (maxWidth - 12.dp) / 2

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileInfoCard("Pago móvil", profile.mobilePaymentPhone.ifBlank { "-" }, Modifier.width(cardWidth))
                        ProfileInfoCard("Banco", profile.bankName.ifBlank { "-" }, Modifier.width(cardWidth))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileInfoCard("Cuenta", profile.bankAccount.ifBlank { "-" }, Modifier.width(cardWidth))
                        Spacer(modifier = Modifier.width(cardWidth))
                    }

                    ProfileInfoCard(
                        "Mensaje personalizado",
                        profile.personalizedMessage.ifBlank { "-" },
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }

        AppPrimaryButton(
            text = "Editar perfil",
            onClick = {
                navController.navigate(AppRoutes.EditProfile) {
                    launchSingleTop = true
                }
            }
        )

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

@Composable
private fun ProfileInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AppShapes.field
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppMutedText(title)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
