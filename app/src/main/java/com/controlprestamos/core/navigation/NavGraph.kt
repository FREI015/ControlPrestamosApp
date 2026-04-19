package com.controlprestamos.core.navigation

import com.controlprestamos.core.design.*

import com.controlprestamos.app.*

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
    var isAuthenticated by remember { mutableStateOf(sessionStore.isUnlocked()) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.Dashboard

    val mainRoutes = remember {
        setOf(
            AppRoutes.Dashboard,
            AppRoutes.Loans,
            AppRoutes.Referrals,
            AppRoutes.More
        )
    }

    fun lockApp() {
        sessionStore.setUnlocked(false)
        isAuthenticated = false
    }

    fun unlockApp() {
        sessionStore.setUnlocked(true)
        isAuthenticated = true
    }

    MaterialTheme(
        colorScheme = if (darkMode) ControlDarkScheme else ControlLightScheme
    ) {
        if (isAuthenticated) {
            val showBottomBar = currentRoute in mainRoutes

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        AppBottomBar(
                            current = currentRoute,
                            navController = navController
                        )
                    }
                }
            ) { inner ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.Dashboard,
                    modifier = Modifier.padding(inner)
                ) {
                    composable(AppRoutes.Dashboard) {
                        DashboardScreen(
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
                            sessionStore = sessionStore,
                            onThemeChanged = {
                                sessionStore.setDarkMode(it)
                                darkMode = it
                            },
                            onLockRequested = { lockApp() }
                        )
                    }

                    composable(AppRoutes.BackupExport) {
                        BackupExportScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.RestoreBackup) {
                        RestoreBackupScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.PrivacyPolicy) {
                        PrivacyPolicyScreen(
                            navController = navController
                        )
                    }
                    composable(AppRoutes.Reports) {
                        ReportsScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                    composable(AppRoutes.DailySummary) {
                        DailySummaryScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                    composable(AppRoutes.CollectionAgenda) {
                        CollectionAgendaScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                    composable(AppRoutes.MonthlyView) {
                        MonthlyViewScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                    composable(AppRoutes.GlobalSearch) {
                        GlobalSearchScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                    composable(AppRoutes.WeeklyView) {
                        WeeklyViewScreen(
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

                    composable(AppRoutes.Profile) {
                        ProfileScreen(
                            navController = navController,
                            sessionStore = sessionStore
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
                    composable(AppRoutes.ReminderSettings) {
                        ReminderSettingsScreen(
                            navController = navController
                        )
                    }

                    composable(AppRoutes.FrequentUsers) {
                        FrequentUsersScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }
                    composable(AppRoutes.Trash) {
                        TrashScreen(
                            navController = navController,
                            sessionStore = sessionStore
                        )
                    }

                    composable(AppRoutes.LockScreen) {
                        LockScreen(
                            activity = activity,
                            sessionStore = sessionStore,
                            onUnlockSuccess = { unlockApp() }
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
                            sessionStore = sessionStore,
                            onUnlockSuccess = { unlockApp() }
                        )
                }
            }
        }
    }
}
