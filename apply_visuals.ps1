$ProjectRoot = "C:\Users\freil\Downloads\ControlPrestamosApp_kotlin_fix\mnt\data\ControlPrestamosApp"

if (!(Test-Path $ProjectRoot)) { throw "No existe la carpeta del proyecto: $ProjectRoot" }

Set-Location $ProjectRoot

if (Test-Path ".\app\src\main\res\values\Image Asset") {
    Remove-Item ".\app\src\main\res\values\Image Asset" -Recurse -Force
}

@'
<resources>
    <string name="app_name">Control de Préstamos</string>
    <string name="home_title">Panel de cartera</string>
    <string name="home_subtitle">Resumen quincenal, mensual, trimestral y semestral</string>
</resources>
'@ | Set-Content ".\app\src\main\res\values\strings.xml" -Encoding UTF8

@'
package com.controlprestamos.ui.theme

import androidx.compose.ui.graphics.Color

val NightBlack = Color(0xFF07090F)
val DeepBlack = Color(0xFF10131A)
val CardDark = Color(0xFF171B24)
val CardBorder = Color(0xFF2A3140)

val NeonGreen = Color(0xFF7DFFB3)
val SoftGreen = Color(0xFF39D98A)
val PurpleGlow = Color(0xFF9B6DFF)
val PurpleDark = Color(0xFF5F36C7)

val Danger = Color(0xFFFF6B81)
val Success = Color(0xFF7DFFB3)
val Warning = Color(0xFFFFD166)

val TextPrimaryDark = Color(0xFFF6F7FB)
val TextSecondaryDark = Color(0xFFB9C0D4)
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\ui\theme\Color.kt" -Encoding UTF8

@'
package com.controlprestamos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = NeonGreen,
    secondary = PurpleGlow,
    tertiary = SoftGreen,
    background = NightBlack,
    surface = DeepBlack,
    surfaceVariant = CardDark,
    onPrimary = NightBlack,
    onSecondary = TextPrimaryDark,
    onTertiary = NightBlack,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Danger
)

@Composable
fun ControlPrestamosTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\ui\theme\Theme.kt" -Encoding UTF8

@'
package com.controlprestamos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.controlprestamos.ui.theme.CardBorder
import com.controlprestamos.ui.theme.CardDark
import com.controlprestamos.ui.theme.Danger
import com.controlprestamos.ui.theme.Success
import com.controlprestamos.ui.theme.Warning

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
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
fun LabeledProgress(label: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun StatusChip(text: String, tone: StatusTone) {
    val container = when (tone) {
        StatusTone.Success -> Success.copy(alpha = 0.18f)
        StatusTone.Warning -> Warning.copy(alpha = 0.18f)
        StatusTone.Danger -> Danger.copy(alpha = 0.18f)
        StatusTone.Info -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    }

    val label = when (tone) {
        StatusTone.Success -> Success
        StatusTone.Warning -> Warning
        StatusTone.Danger -> Danger
        StatusTone.Info -> MaterialTheme.colorScheme.secondary
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = container,
            disabledLabelColor = label
        )
    )
}

enum class StatusTone { Success, Warning, Danger, Info }

@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun LoadingBlock() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
fun EmptyState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\ui\components\UiCommon.kt" -Encoding UTF8

@'
package com.controlprestamos.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.controlprestamos.ui.screens.BlacklistScreen
import com.controlprestamos.ui.screens.DashboardScreen
import com.controlprestamos.ui.screens.HistoryScreen
import com.controlprestamos.ui.screens.LoanDetailScreen
import com.controlprestamos.ui.screens.LoanFormScreen
import com.controlprestamos.ui.screens.LoansScreen
import com.controlprestamos.ui.theme.NightBlack
import com.controlprestamos.ui.theme.NeonGreen
import com.controlprestamos.ui.theme.PurpleGlow

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun ControlPrestamosRoot() {
    val navController = rememberNavController()

    val bottomDestinations = listOf(
        BottomDestination(Route.Dashboard.route, "Panel", { Icon(Icons.Default.BarChart, null) }),
        BottomDestination(Route.Loans.route, "Préstamos", { Icon(Icons.Default.RequestQuote, null) }),
        BottomDestination(Route.Blacklist.route, "Lista negra", { Icon(Icons.Default.Block, null) }),
        BottomDestination(Route.History.route, "Archivados", { Icon(Icons.Default.History, null) })
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NightBlack,
                        PurpleGlow.copy(alpha = 0.28f),
                        NeonGreen.copy(alpha = 0.14f),
                        NightBlack
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                val currentDestination by navController.currentBackStackEntryAsState()
                val route = currentDestination?.destination
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = route?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(Route.Dashboard.route) { saveState = true }
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = Route.Dashboard.route) {
                composable(Route.Dashboard.route) {
                    DashboardScreen(paddingValues = innerPadding)
                }
                composable(Route.Loans.route) {
                    LoansScreen(
                        paddingValues = innerPadding,
                        onAddLoan = { navController.navigate(Route.NewLoan.route) },
                        onOpenLoan = { navController.navigate(Route.LoanDetail.create(it)) }
                    )
                }
                composable(Route.NewLoan.route) {
                    LoanFormScreen(
                        paddingValues = innerPadding,
                        onBackToLoans = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Route.LoanDetail.route,
                    arguments = listOf(navArgument("loanId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L
                    LoanDetailScreen(
                        loanId = loanId,
                        paddingValues = innerPadding,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Route.Blacklist.route) {
                    BlacklistScreen(paddingValues = innerPadding)
                }
                composable(Route.History.route) {
                    HistoryScreen(
                        paddingValues = innerPadding,
                        onOpenLoan = { navController.navigate(Route.LoanDetail.create(it)) }
                    )
                }
            }
        }
    }
}
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\ui\navigation\ControlPrestamosRoot.kt" -Encoding UTF8

@'
package com.controlprestamos.domain.model

enum class DashboardPeriod(val label: String, val daysBack: Long) {
    BIWEEKLY("Quincenal", 15),
    MONTHLY("Mensual", 30),
    QUARTERLY("Trimestral", 90),
    SEMIANNUAL("Semestral", 180)
}
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\domain\model\DashboardPeriod.kt" -Encoding UTF8

@'
package com.controlprestamos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.controlprestamos.LoanApp
import com.controlprestamos.domain.model.DashboardPeriod
import com.controlprestamos.ui.components.FilterChipItem
import com.controlprestamos.ui.components.LabeledProgress
import com.controlprestamos.ui.components.SummaryCard
import com.controlprestamos.ui.viewmodel.DashboardViewModel
import com.controlprestamos.util.CurrencyUtils

@Composable
fun DashboardScreen(
    paddingValues: PaddingValues
) {
    val app = LocalContext.current.applicationContext as LoanApp
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(app.container.loanRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Panel de cartera",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Resumen quincenal, mensual, trimestral y semestral",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardPeriod.entries.forEach { period ->
                    FilterChipItem(
                        label = period.label,
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) }
                    )
                }
            }
        }
        item { SummaryCard("Total prestado", CurrencyUtils.usd(uiState.summary.totalLoaned)) }
        item { SummaryCard("Total recuperado", CurrencyUtils.usd(uiState.summary.totalRecovered)) }
        item { SummaryCard("Intereses proyectados", CurrencyUtils.usd(uiState.summary.projectedInterest)) }
        item { SummaryCard("Total a cobrar", CurrencyUtils.usd(uiState.summary.projectedToCollect)) }
        item { SummaryCard("Pérdidas", CurrencyUtils.usd(uiState.summary.lossesAmount)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Indicadores", style = MaterialTheme.typography.titleMedium)
                LabeledProgress("Avance de cobro", uiState.summary.collectionProgress)
                Text("Retrasos: ${uiState.summary.overdueCount}")
                Text("Pendientes por cobrar: ${uiState.summary.activeCount}")
                Text("Cobrados: ${uiState.summary.collectedCount}")
                Text("Perdidos: ${uiState.summary.lostCount}")
            }
        }
    }
}
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\ui\screens\DashboardScreen.kt" -Encoding UTF8

@'
package com.controlprestamos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanStatus
import com.controlprestamos.ui.theme.CardBorder
import com.controlprestamos.ui.theme.CardDark
import com.controlprestamos.util.CurrencyUtils
import com.controlprestamos.util.DateUtils

@Composable
fun LoanCard(
    loan: Loan,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(loan.customerName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Identificación: ${loan.idNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    loan.status == LoanStatus.COLLECTED -> StatusChip("Cobrado", StatusTone.Success)
                    loan.status == LoanStatus.LOST -> StatusChip("Perdido", StatusTone.Danger)
                    loan.isOverdue -> StatusChip("${loan.daysOverdue} días de atraso", StatusTone.Warning)
                    else -> StatusChip("Pendiente por cobrar", StatusTone.Info)
                }
            }

            Text("Prestado: ${CurrencyUtils.usd(loan.principalAmount)}")
            Text("A regresar: ${CurrencyUtils.usd(loan.totalToRepay)}")
            Text("Pendiente: ${CurrencyUtils.usd(loan.pendingAmount)}")
            Text("Desde: ${DateUtils.format(loan.loanDate)} · Hasta: ${DateUtils.format(loan.dueDate)}")

            if (loan.isBlacklisted) {
                Text(
                    "Cliente también está en lista negra.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
'@ | Set-Content ".\app\src\main\java\com\controlprestamos\ui\components\LoanCard.kt" -Encoding UTF8

@'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".LoanApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ControlPrestamos">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ControlPrestamos">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
'@ | Set-Content ".\app\src\main\AndroidManifest.xml" -Encoding UTF8

.\gradlew.bat clean
.\gradlew.bat assembleDebug
We need answer based on pasted file. Explain again they pasted script content directly after gradlew. Need maybe propose one safer method: create ps1 via notepad and run. Could also give exact command to create file from the already uploaded markdown? But user asks with no explicit question, likely wants next step. We need concise but helpful. Need cite file. Maybe mention that terminal literally executed `assembleDebug$ProjectRoot...` and later `assembleDebugC:\Users...` due concatenation. Need perhaps instruct to cancel current process and rerun only after saving script. Could include exact commands:
1 cd project
2 notepad apply_visuals.ps1
3 paste the script from previous answer
4 save
5 run `powershell -ExecutionPolicy Bypass -File .\apply_visuals.ps1`
Alternative if script already created, just run it. Maybe inspect file existence? Can't. Could suggest `Test-Path .\apply_visuals.ps1` then `Get-Content -Head 5 .\apply_visuals.ps1`. Maybe user didn't create it. Need maybe not overload.

Need cite file lines? We have no line numbers from file_search because prompt didn't show. Need maybe use file_search.msearch/mclick to get lines. Let's do msearch for specific text in file uploaded. Must cite. We need line ranges. Use file_search on uploaded files maybe. Let's do msearch for "assembleDebug$ProjectRoot" and "assembleDebugC". +#+#+#+#+#+ to=file_search.msearch analysis  乐亚ម្មան {"queries":["assembleDebug$ProjectRoot apply_visuals.ps1 assembleDebugC user pasted text", "assembleDebugC:\\Users ControlPrestamosApp build failed pasted text"], "source_filter":["files_uploaded_in_conversation"]}