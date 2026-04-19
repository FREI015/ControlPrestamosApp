package com.controlprestamos.features.settings

import com.controlprestamos.features.backup.*
import com.controlprestamos.features.security.*
import com.controlprestamos.features.dashboard.*
import com.controlprestamos.features.people.*
import com.controlprestamos.features.search.*
import com.controlprestamos.features.more.*
import com.controlprestamos.core.format.*
import com.controlprestamos.core.validation.*
import com.controlprestamos.app.*
import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ReminderSettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current

    var enabled by remember { mutableStateOf(LoanReminderManager.isEnabled(context)) }
    var hour by remember { mutableStateOf(LoanReminderManager.getHour(context)) }
    var minute by remember { mutableStateOf(LoanReminderManager.getMinute(context)) }
    var feedback by remember { mutableStateOf("") }

    val needsRuntimePermission = LoanReminderManager.needsNotificationPermission()
    val runtimePermissionGranted = LoanReminderManager.hasNotificationRuntimePermission(context)
    val appNotificationsEnabled = LoanReminderManager.areNotificationsEnabled(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && LoanReminderManager.areNotificationsEnabled(context)) {
            enabled = true
            LoanReminderManager.saveSettings(context, true, hour, minute)
            LoanReminderManager.scheduleDailyReminder(context)
            feedback = "Recordatorios activados correctamente."
        } else if (granted) {
            enabled = false
            LoanReminderManager.saveSettings(context, false, hour, minute)
            feedback = "El permiso fue concedido, pero las notificaciones de la app están desactivadas en el sistema."
        } else {
            enabled = false
            LoanReminderManager.saveSettings(context, false, hour, minute)
            feedback = "No se concedió el permiso de notificaciones."
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
            title = "Recordatorios",
            onBack = { navController.popBackStack() }
        )

        if (feedback.isNotBlank()) {
            AppSectionCard {
                Text(feedback)
            }
        }

        AppSectionCard {
            Text(
                text = "Estado",
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
                    Text("Recordatorios diarios")
                    AppMutedText(if (enabled) "Activos" else "Desactivados")
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            when {
                                needsRuntimePermission && !runtimePermissionGranted -> {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                !LoanReminderManager.areNotificationsEnabled(context) -> {
                                    enabled = false
                                    LoanReminderManager.saveSettings(context, false, hour, minute)
                                    feedback = "Las notificaciones están desactivadas a nivel de app. Ábrelas en ajustes del sistema."
                                }
                                else -> {
                                    enabled = true
                                    LoanReminderManager.saveSettings(context, true, hour, minute)
                                    LoanReminderManager.scheduleDailyReminder(context)
                                    feedback = "Recordatorios activados correctamente."
                                }
                            }
                        } else {
                            enabled = false
                            LoanReminderManager.saveSettings(context, false, hour, minute)
                            LoanReminderManager.cancelDailyReminder(context)
                            LoanReminderManager.resetDeliveryDedup(context)
                            feedback = "Recordatorios desactivados."
                        }
                    }
                )
            }

            AppMutedText(
                when {
                    needsRuntimePermission && !runtimePermissionGranted ->
                        "Falta conceder el permiso de notificaciones."
                    !appNotificationsEnabled ->
                        "La app no tiene notificaciones activas en ajustes del sistema."
                    else ->
                        "La app puede enviar notificaciones cuando el módulo está activo."
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Horario",
                style = MaterialTheme.typography.titleMedium
            )

            AppMutedText("Hora actual: ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")

            AppSecondaryButton(
                text = "Elegir hora del recordatorio",
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute ->
                            hour = selectedHour
                            minute = selectedMinute
                            LoanReminderManager.saveSettings(context, enabled, hour, minute)
                            if (enabled) {
                                LoanReminderManager.scheduleDailyReminder(context)
                            }
                            feedback = "Hora de recordatorio actualizada."
                        },
                        hour,
                        minute,
                        true
                    ).show()
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Qué se notificará",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("La app te avisará una vez al día si existen préstamos vencidos, préstamos que vencen hoy o préstamos que vencen mañana.")
            AppMutedText("Se evita repetir el mismo resumen varias veces en un mismo día cuando el contenido no cambia.")
        }

        AppSectionCard {
            Text(
                text = "Prueba rápida",
                style = MaterialTheme.typography.titleMedium
            )

            AppPrimaryButton(
                text = "Enviar prueba ahora",
                onClick = {
                    val result = when {
                        needsRuntimePermission && !LoanReminderManager.hasNotificationRuntimePermission(context) -> {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            null
                        }
                        else -> LoanReminderManager.sendPreviewNotification(context)
                    }

                    feedback = when (result) {
                        ReminderDispatchResult.SENT -> "Notificación de prueba enviada."
                        ReminderDispatchResult.BLOCKED_PERMISSION -> "No se pudo enviar la prueba porque falta el permiso de notificaciones."
                        ReminderDispatchResult.BLOCKED_APP_NOTIFICATIONS -> "No se pudo enviar la prueba porque las notificaciones de la app están desactivadas en el sistema."
                        null -> feedback
                        else -> "No se pudo enviar la prueba."
                    }
                }
            )

            AppSecondaryButton(
                text = "Evaluar lógica real ahora",
                onClick = {
                    val result = LoanReminderManager.sendDailySummaryIfNeeded(context)
                    feedback = when (result) {
                        ReminderDispatchResult.SENT -> "Se envió el resumen real de recordatorios."
                        ReminderDispatchResult.BLOCKED_DISABLED -> "Los recordatorios están desactivados."
                        ReminderDispatchResult.BLOCKED_PERMISSION -> "Falta el permiso de notificaciones."
                        ReminderDispatchResult.BLOCKED_APP_NOTIFICATIONS -> "Las notificaciones están desactivadas para la app en el sistema."
                        ReminderDispatchResult.NOTHING_TO_NOTIFY -> "No hay préstamos vencidos, para hoy o para mañana."
                        ReminderDispatchResult.SKIPPED_DUPLICATE -> "Ya se había enviado hoy el mismo resumen."
                    }
                }
            )
        }

        AppSectionCard {
            Text(
                text = "Ajustes del sistema",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Si negaste el permiso o desactivaste las notificaciones de la app, puedes corregirlo aquí.")

            AppSecondaryButton(
                text = "Abrir ajustes de notificaciones",
                onClick = {
                    openNotificationSettings(context)
                }
            )
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)
}
