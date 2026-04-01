package com.controlprestamos.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MoreScreen(
    navController: NavController,
    sessionStore: SessionStore
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(title = "Más")

        AppSectionCard {
            MenuRow("Perfil") { navController.navigate(AppRoutes.PROFILE) }
            HorizontalDivider()
            MenuRow("Editar perfil") { navController.navigate(AppRoutes.EDIT_PROFILE) }
            HorizontalDivider()
            MenuRow("Seguridad") { navController.navigate(AppRoutes.SECURITY_SETTINGS) }
            HorizontalDivider()
            MenuRow("Usuarios frecuentes") { navController.navigate(AppRoutes.FREQUENT_USERS) }
            HorizontalDivider()
            MenuRow("Historial") { navController.navigate(AppRoutes.HISTORY) }
            HorizontalDivider()
            MenuRow("Lista negra") { navController.navigate(AppRoutes.BLACKLIST) }
        }

        AppSectionCard {
            AppPrimaryButton(
                text = "Bloquear aplicación",
                onClick = {
                    sessionStore.logout()
                    navController.navigate(AppRoutes.LOCK_SCREEN) {
                        popUpTo(AppRoutes.DASHBOARD) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
private fun MenuRow(
    title: String,
    onClick: () -> Unit
) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    )
}





