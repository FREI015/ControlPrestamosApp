package com.controlprestamos.app

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
            subtitle = "Datos personales, clientes frecuentes, historial y control de riesgo."
        ) {
            MoreActionRow(
                title = "Mi perfil",
                subtitle = "Ver tu información personal y los datos de cobro."
            ) {
                navController.navigate(AppRoutes.Profile) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Usuarios frecuentes",
                subtitle = "Gestionar datos de pago guardados."
            ) {
                navController.navigate(AppRoutes.FrequentUsers) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Historial",
                subtitle = "Consultar movimientos financieros."
            ) {
                navController.navigate(AppRoutes.History) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Lista negra",
                subtitle = "Revisar clientes restringidos y observaciones."
            ) {
                navController.navigate(AppRoutes.Blacklist) { launchSingleTop = true }
            }
        }

        MoreGroupCard(
            title = "Operación y seguimiento",
            subtitle = "Herramientas para revisar la cartera, los cobros y el rendimiento."
        ) {
            MoreActionRow(
                title = "Reportes",
                subtitle = "Analizar la operación por rango de fechas."
            ) {
                navController.navigate(AppRoutes.Reports) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Resumen del día",
                subtitle = "Cobros, pagos y prioridades de hoy."
            ) {
                navController.navigate(AppRoutes.DailySummary) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Agenda de cobros",
                subtitle = "Organizar vencimientos por fecha."
            ) {
                navController.navigate(AppRoutes.CollectionAgenda) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Vista semanal",
                subtitle = "Revisar cartera, pagos y vencimientos por semana."
            ) {
                navController.navigate(AppRoutes.WeeklyView) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Vista mensual",
                subtitle = "Revisar cartera, pagos y vencimientos por mes."
            ) {
                navController.navigate(AppRoutes.MonthlyView) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Buscador global",
                subtitle = "Buscar clientes, préstamos, lista negra y más."
            ) {
                navController.navigate(AppRoutes.GlobalSearch) { launchSingleTop = true }
            }
        }

        MoreGroupCard(
            title = "Seguridad, respaldo y cumplimiento",
            subtitle = "Acceso, recordatorios, respaldo, restauración, privacidad y limpieza."
        ) {
            MoreActionRow(
                title = "Seguridad",
                subtitle = "PIN, biometría y control de acceso."
            ) {
                navController.navigate(AppRoutes.SecuritySettings) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Recordatorios",
                subtitle = "Avisos diarios de préstamos vencidos o próximos a vencer."
            ) {
                navController.navigate(AppRoutes.ReminderSettings) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Respaldo y exportación",
                subtitle = "Compartir respaldo completo o archivos CSV."
            ) {
                navController.navigate(AppRoutes.BackupExport) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Restaurar respaldo",
                subtitle = "Importar un ZIP generado por la app y fusionar o reemplazar datos."
            ) {
                navController.navigate(AppRoutes.RestoreBackup) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Privacidad y datos",
                subtitle = "Revisar permisos, tratamiento de datos y política interna."
            ) {
                navController.navigate(AppRoutes.PrivacyPolicy) { launchSingleTop = true }
            }

            MoreActionDivider()

            MoreActionRow(
                title = "Papelera",
                subtitle = "Restaurar o eliminar definitivamente préstamos borrados."
            ) {
                navController.navigate(AppRoutes.Trash) { launchSingleTop = true }
            }
        }

        AppSectionCard {
            Text(
                text = "Preferencias",
                style = MaterialTheme.typography.titleMedium
            )

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
        }

        AppSectionCard {
            Text(
                text = "Aplicación",
                style = MaterialTheme.typography.titleMedium
            )

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
    subtitle: String,
    content: @Composable () -> Unit
) {
    AppSectionCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                AppMutedText(subtitle)
            }

            content()
        }
    }
}

@Composable
private fun MoreActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        AppMutedText(subtitle)
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
