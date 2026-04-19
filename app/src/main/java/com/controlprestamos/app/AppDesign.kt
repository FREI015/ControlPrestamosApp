package com.controlprestamos.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

object AppPalette {
    val Blue = Color(0xFF4A86E8)
    val BlueDark = Color(0xFF5B95F5)

    val LightBg = Color(0xFFF4F5F7)
    val LightCard = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFD9DDE3)
    val LightText = Color(0xFF111827)
    val LightMuted = Color(0xFF6B7280)

    val DarkBg = Color(0xFF0F1115)
    val DarkCard = Color(0xFF171A1F)
    val DarkBorder = Color(0xFF2A2F37)
    val DarkText = Color(0xFFF3F4F6)
    val DarkMuted = Color(0xFF9CA3AF)

    val Success = Color(0xFF1F8F4C)
    val Warning = Color(0xFFB56A00)
    val Danger = Color(0xFFB42318)
}

val ControlLightScheme = lightColorScheme(
    primary = AppPalette.Blue,
    onPrimary = Color.White,
    background = AppPalette.LightBg,
    onBackground = AppPalette.LightText,
    surface = AppPalette.LightCard,
    onSurface = AppPalette.LightText,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = AppPalette.LightMuted,
    outline = AppPalette.LightBorder
)

val ControlDarkScheme = darkColorScheme(
    primary = AppPalette.BlueDark,
    onPrimary = Color.White,
    background = AppPalette.DarkBg,
    onBackground = AppPalette.DarkText,
    surface = AppPalette.DarkCard,
    onSurface = AppPalette.DarkText,
    surfaceVariant = Color(0xFF1F242B),
    onSurfaceVariant = AppPalette.DarkMuted,
    outline = AppPalette.DarkBorder
)

object AppShapes {
    val card = RoundedCornerShape(26.dp)
    val field = RoundedCornerShape(18.dp)
    val chip = RoundedCornerShape(18.dp)
    val pill = RoundedCornerShape(999.dp)
}

@Composable
fun AppScreenTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun AppMutedText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun AppLabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppMutedText(label)
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun AppMetricCard(
    title: String,
    value: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppStatusChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val safeText = text.trim().ifBlank { "Sin estado" }
    val upper = safeText.uppercase()

    val tint = when {
        upper.contains("COBRADO") || upper.contains("PAGADO") || upper.contains("ACTIVO") -> AppPalette.Success
        upper.contains("VENCIDO") || upper.contains("PENDIENTE") -> AppPalette.Warning
        upper.contains("PERDIDO") || upper.contains("ELIMINAR") || upper.contains("MORA") -> AppPalette.Danger
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .background(
                color = tint.copy(alpha = 0.14f),
                shape = AppShapes.pill
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = safeText,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                AppShapes.pill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun AppPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.pill,
        contentPadding = PaddingValues(vertical = 18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.pill,
        contentPadding = PaddingValues(vertical = 18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppDangerButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.pill,
        contentPadding = PaddingValues(vertical = 18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPalette.Danger,
            contentColor = Color.White
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun AppTopBack(
    title: String,
    onBack: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (onBack != null) {
            Text(
                text = "Volver",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() }
            )
        }
        AppScreenTitle(title)
    }
}

@Composable
fun AppBottomBack(
    onClick: () -> Unit
) {
    Text(
        text = "Volver",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp),
    )
}

@Composable
fun AppBottomBar(
    current: String,
    navController: NavController
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBottomItem("Inicio", AppRoutes.Dashboard, current, navController, Icons.Rounded.Home)
            AppBottomItem("Control", AppRoutes.Loans, current, navController, Icons.Rounded.Payments)
            AppBottomItem("Referidos", AppRoutes.Referrals, current, navController, Icons.Rounded.GroupAdd)
            AppBottomItem("Más", AppRoutes.More, current, navController, Icons.Rounded.MoreHoriz)
        }
    }
}

@Composable
private fun AppBottomItem(
    label: String,
    route: String,
    current: String,
    navController: NavController,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val selected = current == route

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                AppShapes.pill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                navController.navigate(route) { launchSingleTop = true }
            }
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
