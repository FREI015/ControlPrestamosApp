package com.controlprestamos.app

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MoreScreen(
    navController: NavController,
    sessionStore: SessionStore,
    onThemeChanged: (Boolean) -> Unit,
    onLockRequested: () -> Unit
) {
    var darkMode by remember { mutableStateOf(sessionStore.isDarkMode()) }

    var showAccount by remember { mutableStateOf(true) }
    var showOperation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showApp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(title = "Más")

        MoreGroupCard(
            title = "Cuenta y clientes",
            icon = {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            expanded = showAccount,
            onToggle = { showAccount = !showAccount }
        ) {
            MoreActionRow("Mi perfil") {
                navController.navigate(AppRoutes.Profile) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Usuarios frecuentes") {
                navController.navigate(AppRoutes.FrequentUsers) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Historial") {
                navController.navigate(AppRoutes.History) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Lista negra") {
                navController.navigate(AppRoutes.Blacklist) { launchSingleTop = true }
            }
        }

        MoreGroupCard(
            title = "Operación y seguimiento",
            icon = {
                Icon(
                    Icons.Rounded.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            expanded = showOperation,
            onToggle = { showOperation = !showOperation }
        ) {
            MoreActionRow("Reportes") {
                navController.navigate(AppRoutes.Reports) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Resumen del día") {
                navController.navigate(AppRoutes.DailySummary) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Agenda de cobros") {
                navController.navigate(AppRoutes.CollectionAgenda) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Vista semanal") {
                navController.navigate(AppRoutes.WeeklyView) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Vista mensual") {
                navController.navigate(AppRoutes.MonthlyView) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Buscador global") {
                navController.navigate(AppRoutes.GlobalSearch) { launchSingleTop = true }
            }
        }

        MoreGroupCard(
            title = "Configuraciones",
            icon = {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            expanded = showSettings,
            onToggle = { showSettings = !showSettings }
        ) {
            MoreActionRow("Seguridad") {
                navController.navigate(AppRoutes.SecuritySettings) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Recordatorios") {
                navController.navigate(AppRoutes.ReminderSettings) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Respaldo y exportación") {
                navController.navigate(AppRoutes.BackupExport) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Restaurar respaldo") {
                navController.navigate(AppRoutes.RestoreBackup) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Privacidad y datos") {
                navController.navigate(AppRoutes.PrivacyPolicy) { launchSingleTop = true }
            }
            MoreActionDivider()
            MoreActionRow("Papelera") {
                navController.navigate(AppRoutes.Trash) { launchSingleTop = true }
            }
        }

        MoreGroupCard(
            title = "Aplicación",
            icon = {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            expanded = showApp,
            onToggle = { showApp = !showApp }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Tema oscuro")
                    AppMutedText(if (darkMode) "Activo" else "Desactivado")
                }

                Switch(
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        sessionStore.setDarkMode(it)
                        onThemeChanged(it)
                    }
                )
            }

            MoreActionDivider()

            AppDangerButton(
                text = "Bloquear aplicación",
                onClick = {
                    sessionStore.logout()
                    onLockRequested()
                }
            )
        }
    }
}

@Composable
private fun MoreGroupCard(
    title: String,
    icon: @Composable () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    AppSectionCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                AppFilterChip(
                    label = if (expanded) "Ocultar" else "Ver",
                    selected = expanded,
                    onClick = onToggle
                )
            }

            if (expanded) {
                content()
            } else {
                AppMutedText("Toca en Ver para desplegar esta sección.")
            }
        }
    }
}

@Composable
private fun MoreActionRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoreActionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    )
}
