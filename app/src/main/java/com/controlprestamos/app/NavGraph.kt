package com.controlprestamos.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppNavGraph(
    activity: FragmentActivity,
    navController: NavHostController,
    sessionStore: SessionStore
) {
    var darkMode by remember { mutableStateOf(sessionStore.isDarkMode()) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.Dashboard

    MaterialTheme(
        colorScheme = if (darkMode) ControlDarkScheme else ControlLightScheme
    ) {
        if (sessionStore.isUnlocked() || !sessionStore.hasPin()) {
            androidx.compose.material3.Scaffold(
                bottomBar = {
                    AppBottomBar(
                        current = currentRoute,
                        navController = navController
                    )
                }
            ) { inner ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.Dashboard,
                    modifier = Modifier.padding(inner)
                ) {
                    composable(AppRoutes.Dashboard) {
                        DashboardScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.Loans) {
                        LoansScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.Referrals) {
                        ReferralsScreen(sessionStore = sessionStore)
                    }

                    composable(AppRoutes.More) {
                        MoreScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.History) {
                        HistoryScreen(
                            sessionStore = sessionStore,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.Blacklist) {
                        BlacklistScreen(
                            onBack = { navController.popBackStack() },
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.EditProfile) {
                        EditProfileScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.ProfileScreen) {
                        ProfileScreen(
                            navController = navController,
                            sessionStore = sessionStore,
                            onThemeChanged = {
                                darkMode = it
                            }
                        )
                    }

                    composable(AppRoutes.LoanDetail) {
                        LoanDetailScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.LoanCollectionNotice) {
                        LoanCollectionNoticeScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.NewLoan) {
                        NewLoanScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.EditLoan) {
                        EditLoanScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.SecuritySettings) {
                        SecuritySettingsScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.FrequentUsers) {
                        FrequentUsersScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.LockScreen) {
                        LockScreen(
                            activity = activity,
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                }
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.LockScreen
            ) {
                composable(AppRoutes.LockScreen) {
                    LockScreen(
                        activity = activity,
                        navController = navController,
                        sessionStore = sessionStore
                    )
                }
            }
        }
    }
}
