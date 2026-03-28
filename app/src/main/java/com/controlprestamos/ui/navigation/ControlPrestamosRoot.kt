package com.controlprestamos.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.controlprestamos.data.UserProfilePreferences
import com.controlprestamos.domain.model.DashboardDetailType
import com.controlprestamos.ui.screens.AuthScreen
import com.controlprestamos.ui.screens.BlacklistScreen
import com.controlprestamos.ui.screens.CollectionNoticeScreen
import com.controlprestamos.ui.screens.DashboardDetailScreen
import com.controlprestamos.ui.screens.DashboardScreen
import com.controlprestamos.ui.screens.HistoryScreen
import com.controlprestamos.ui.screens.LoanDetailScreen
import com.controlprestamos.ui.screens.LoanFormScreen
import com.controlprestamos.ui.screens.LoansScreen
import com.controlprestamos.ui.screens.SarmScreen
import com.controlprestamos.ui.screens.TenPercentScreen
import com.controlprestamos.ui.screens.UserProfileScreen
import com.controlprestamos.ui.viewmodel.AuthViewModel

@Composable
fun ControlPrestamosRoot() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val profilePrefs = remember(context) { UserProfilePreferences(context) }
    val profileConfiguredState = remember { mutableStateOf(profilePrefs.getProfile().isConfigured) }

    when {
        !authState.isAuthenticated -> {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {}
            )
        }

        !profileConfiguredState.value -> {
            UserProfileScreen(
                paddingValues = PaddingValues(16.dp),
                onBack = {},
                onSaved = {
                    profileConfiguredState.value = true
                }
            )
        }

        else -> {
            AppNavigation()
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    val bottomItems = listOf(
        BottomNavItem(
            label = "Inicio",
            route = Route.Dashboard.route,
            selectedIcon = { Icon(Icons.Rounded.Home, contentDescription = "Inicio") },
            unselectedIcon = { Icon(Icons.Outlined.Home, contentDescription = "Inicio") }
        ),
        BottomNavItem(
            label = "Préstamos",
            route = Route.Loans.route,
            selectedIcon = { Icon(Icons.Rounded.Payments, contentDescription = "Préstamos") },
            unselectedIcon = { Icon(Icons.Outlined.Payments, contentDescription = "Préstamos") }
        ),
        BottomNavItem(
            label = "Bloqueados",
            route = Route.Blacklist.route,
            selectedIcon = { Icon(Icons.Rounded.Block, contentDescription = "Lista negra") },
            unselectedIcon = { Icon(Icons.Outlined.Block, contentDescription = "Lista negra") }
        ),
        BottomNavItem(
            label = "Historial",
            route = Route.History.route,
            selectedIcon = { Icon(Icons.Rounded.History, contentDescription = "Historial") },
            unselectedIcon = { Icon(Icons.Outlined.History, contentDescription = "Historial") }
        ),
        BottomNavItem(
            label = "Perfil",
            route = Route.Profile.route,
            selectedIcon = { Icon(Icons.Rounded.Person, contentDescription = "Perfil") },
            unselectedIcon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") }
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    val selected = currentRoute == item.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (selected) item.selectedIcon() else item.unselectedIcon()
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors()
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Dashboard.route) {
                DashboardScreen(
                    paddingValues = innerPadding,
                    onOpenTotalLoaned = {
                        navController.navigate(
                            Route.DashboardDetail.create(DashboardDetailType.TOTAL_LOANED.routeValue)
                        )
                    },
                    onOpenProjectedInterest = {
                        navController.navigate(
                            Route.DashboardDetail.create(DashboardDetailType.PROJECTED_INTEREST.routeValue)
                        )
                    },
                    onOpenTotalToCollect = {
                        navController.navigate(
                            Route.DashboardDetail.create(DashboardDetailType.TOTAL_TO_COLLECT.routeValue)
                        )
                    },
                    onOpenOverduePayments = {
                        navController.navigate(
                            Route.DashboardDetail.create(DashboardDetailType.OVERDUE_PAYMENTS.routeValue)
                        )
                    },
                    onOpenSarm = {
                        navController.navigate(Route.Sarm.route)
                    },
                    onOpenTenPercent = {
                        navController.navigate(Route.TenPercent.route)
                    }
                )
            }

            composable(Route.Loans.route) {
                LoansScreen(
                    paddingValues = innerPadding,
                    onAddLoan = {
                        navController.navigate(Route.NewLoan.route)
                    },
                    onOpenLoan = { loanId ->
                        navController.navigate(Route.LoanDetail.create(loanId))
                    }
                )
            }

            composable(Route.NewLoan.route) {
                LoanFormScreen(
                    paddingValues = innerPadding,
                    onBackToLoans = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Route.Blacklist.route) {
                BlacklistScreen(
                    paddingValues = innerPadding
                )
            }

            composable(Route.History.route) {
                HistoryScreen(
                    paddingValues = innerPadding
                )
            }

            composable(Route.Profile.route) {
                UserProfileScreen(
                    paddingValues = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaved = {}
                )
            }

            composable(Route.Sarm.route) {
                SarmScreen(
                    paddingValues = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Route.TenPercent.route) {
                TenPercentScreen(
                    paddingValues = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Route.LoanDetail.route,
                arguments = listOf(
                    navArgument("loanId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L

                LoanDetailScreen(
                    loanId = loanId,
                    paddingValues = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenCollectionNotice = { selectedLoanId ->
                        navController.navigate(Route.CollectionNotice.create(selectedLoanId))
                    }
                )
            }

            composable(
                route = Route.CollectionNotice.route,
                arguments = listOf(
                    navArgument("loanId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L

                CollectionNoticeScreen(
                    loanId = loanId,
                    paddingValues = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Route.DashboardDetail.route,
                arguments = listOf(
                    navArgument("detailType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val detailType = backStackEntry.arguments?.getString("detailType") ?: ""

                DashboardDetailScreen(
                    detailTypeValue = detailType,
                    paddingValues = innerPadding,
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenLoan = { loanId ->
                        navController.navigate(Route.LoanDetail.create(loanId))
                    }
                )
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)
