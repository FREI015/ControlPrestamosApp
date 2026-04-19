package com.controlprestamos.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.controlprestamos.R

@Composable
fun PrivacyPolicyScreen(
    navController: NavController
) {
    val privacyUrl = stringResource(id = R.string.privacy_policy_public_url)
    val supportEmail = stringResource(id = R.string.privacy_policy_support_email)
    val lastUpdated = stringResource(id = R.string.privacy_policy_last_updated)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppTopBack(
            title = "Privacidad y datos",
            onBack = { navController.popBackStack() }
        )

        AppSectionCard {
            Text(
                text = "Enfoque del producto",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Esta aplicación está planteada como una herramienta de gestión de cartera, cobranza y control operativo de préstamos registrados por el usuario.")
            AppMutedText("No está presentada dentro de la app como una plataforma pública de originación de préstamos ni como un servicio bancario.")
        }

        AppSectionCard {
            Text(
                text = "Datos que puede almacenar la app",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Perfil del usuario: nombre, apellido, cédula, teléfonos, datos de cobro y mensaje personalizado.")
            AppMutedText("Operación: préstamos, pagos, estados, historial, lista negra, referidos y usuarios frecuentes.")
            AppMutedText("Resguardos locales: configuración de seguridad, recordatorios y archivos exportados generados desde la app.")
        }

        AppSectionCard {
            Text(
                text = "Uso de la información",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("La información se usa para registrar préstamos, calcular saldos, generar avisos, mantener historial operativo, preparar reportes y facilitar respaldos iniciados por el usuario.")
            AppMutedText("En esta versión, la operación principal está orientada a almacenamiento local en el dispositivo.")
        }

        AppSectionCard {
            Text(
                text = "Permisos y funcionamiento",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("POST_NOTIFICATIONS: se usa para recordatorios y avisos relacionados con vencimientos y cobros.")
            AppMutedText("RECEIVE_BOOT_COMPLETED: se usa para reprogramar recordatorios después de reiniciar el dispositivo o actualizar la app.")
            AppMutedText("Los archivos de respaldo y exportación solo se comparten cuando el usuario lo solicita manualmente.")
        }

        AppSectionCard {
            Text(
                text = "Respaldo del sistema",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Por privacidad, el respaldo automático del sistema queda desactivado en esta versión.")
            AppMutedText("La migración y los respaldos se harán mediante el flujo interno de exportación y, más adelante, restauración controlada.")
        }

        AppSectionCard {
            Text(
                text = "Compartición y terceros",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("La app no vende información del usuario.")
            AppMutedText("La app no comparte automáticamente la base operativa con terceros.")
            AppMutedText("Puede existir exposición de datos si el usuario decide compartir manualmente archivos exportados o mensajes fuera de la app.")
        }

        AppSectionCard {
            Text(
                text = "Política pública para Play",
                style = MaterialTheme.typography.titleMedium
            )
            AppMutedText("Última actualización interna: $lastUpdated")
            AppMutedText("Correo de soporte: $supportEmail")
            AppMutedText("URL pública declarada: $privacyUrl")
            AppMutedText("Antes de publicar, reemplaza estos valores por tu correo real y por una URL HTML pública, activa y no geobloqueada.")
        }

        AppBottomBack(onClick = { navController.popBackStack() })
    }
}
